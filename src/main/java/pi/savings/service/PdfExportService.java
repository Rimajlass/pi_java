package pi.savings.service;

import pi.entities.CalendarEvent;
import pi.savings.repository.SavingAccountRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PdfExportService {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public Path exportSavingAccountsPdf(
            List<SavingAccountRepository.SavingAccountDetails> accounts,
            CurrencyRateService.RateSnapshot rateSnapshot,
            Path exportDirectory
    ) {
        try {
            Files.createDirectories(exportDirectory);
            Path file = exportDirectory.resolve("saving-accounts-" + LocalDateTime.now().format(FILE_STAMP) + ".pdf");
            PdfDocument pdf = new PdfDocument();
            pdf.startPage("Saving Accounts Report", "Decide$ desktop export enriched with Currency API");
            pdf.drawMetricCards(List.of(
                    new MetricCard("Accounts", String.valueOf(accounts.size())),
                    new MetricCard("Currency", rateSnapshot.currency()),
                    new MetricCard("Rate", format(rateSnapshot.rateToTnd()) + " TND"),
                    new MetricCard("Rate Date", rateSnapshot.rateDate().toString())
            ));
            pdf.drawSectionTitle("Summary");
            BigDecimal totalBalance = accounts.stream().map(SavingAccountRepository.SavingAccountDetails::balance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            pdf.drawKeyValueGrid(List.of(
                    "Total balance", money(totalBalance),
                    "Converted balance", money(convert(totalBalance, rateSnapshot)),
                    "Rate provider", rateSnapshot.provider(),
                    "Generated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            ));
            pdf.drawSectionTitle("Saving Accounts");
            pdf.drawTable(
                    List.of("Balance", "Interest", "Creation", "User", "Email", "Currency", "Converted TND"),
                    List.of(70f, 52f, 70f, 110f, 145f, 52f, 78f),
                    accountRows(accounts, rateSnapshot)
            );
            Files.write(file, pdf.build().getBytes(StandardCharsets.ISO_8859_1));
            return file;
        } catch (IOException exception) {
            throw new PdfExportException("Impossible d'exporter les saving accounts en PDF.", exception);
        }
    }

    public Path exportGoalsPdf(
            List<SavingsStatsService.GoalDetails> goals,
            CurrencyRateService.RateSnapshot rateSnapshot,
            List<CalendarEvent> holidays,
            Path exportDirectory
    ) {
        try {
            Files.createDirectories(exportDirectory);
            Path file = exportDirectory.resolve("financial-goals-" + LocalDateTime.now().format(FILE_STAMP) + ".pdf");
            PdfDocument pdf = new PdfDocument();
            pdf.startPage("Financial Goals Report", "Decide$ desktop export enriched with Currency + Calendar APIs");
            pdf.drawMetricCards(List.of(
                    new MetricCard("Goals", String.valueOf(goals.size())),
                    new MetricCard("Currency", rateSnapshot.currency()),
                    new MetricCard("Rate", format(rateSnapshot.rateToTnd()) + " TND"),
                    new MetricCard("Holidays", String.valueOf(holidays.size()))
            ));
            pdf.drawSectionTitle("Summary");
            BigDecimal totalTarget = goals.stream().map(SavingsStatsService.GoalDetails::targetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalCurrent = goals.stream().map(SavingsStatsService.GoalDetails::currentAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            pdf.drawKeyValueGrid(List.of(
                    "Target total", money(totalTarget),
                    "Current total", money(totalCurrent),
                    "Converted target", money(convert(totalTarget, rateSnapshot)),
                    "Rate date", rateSnapshot.rateDate().toString()
            ));
            pdf.drawSectionTitle("Goals");
            pdf.drawTable(
                    List.of("Goal", "Target", "Current", "Remaining", "Deadline", "Priority", "Status", "Progress", "Holiday", "Converted TND"),
                    List.of(102f, 52f, 52f, 58f, 62f, 48f, 52f, 48f, 90f, 66f),
                    goalRows(goals, rateSnapshot, holidays)
            );
            Files.write(file, pdf.build().getBytes(StandardCharsets.ISO_8859_1));
            return file;
        } catch (IOException exception) {
            throw new PdfExportException("Impossible d'exporter les financial goals en PDF.", exception);
        }
    }

    private List<List<String>> accountRows(List<SavingAccountRepository.SavingAccountDetails> accounts, CurrencyRateService.RateSnapshot rateSnapshot) {
        List<List<String>> rows = new ArrayList<>();
        for (SavingAccountRepository.SavingAccountDetails account : accounts) {
            rows.add(List.of(
                    money(account.balance()),
                    format(account.interestRate()) + "%",
                    account.createdOn().toString(),
                    safe(account.userName()),
                    safe(account.userEmail()),
                    rateSnapshot.currency(),
                    money(convert(account.balance(), rateSnapshot))
            ));
        }
        return rows;
    }

    private List<List<String>> goalRows(
            List<SavingsStatsService.GoalDetails> goals,
            CurrencyRateService.RateSnapshot rateSnapshot,
            List<CalendarEvent> holidays
    ) {
        List<List<String>> rows = new ArrayList<>();
        for (SavingsStatsService.GoalDetails goal : goals) {
            BigDecimal remaining = goal.targetAmount().subtract(goal.currentAmount()).max(BigDecimal.ZERO);
            rows.add(List.of(
                    safe(goal.name()),
                    money(goal.targetAmount()),
                    money(goal.currentAmount()),
                    money(remaining),
                    goal.deadline() == null ? "--/--/----" : goal.deadline().toString(),
                    "P" + goal.priority(),
                    SavingsStatsService.goalStatus(goal, LocalDate.now()),
                    format(SavingsStatsService.progress(goal)) + "%",
                    holidayFlag(goal.deadline(), holidays),
                    money(convert(goal.currentAmount(), rateSnapshot))
            ));
        }
        return rows;
    }

    private String holidayFlag(LocalDate deadline, List<CalendarEvent> holidays) {
        return holidays.stream()
                .anyMatch(event -> deadline != null && Math.abs(java.time.temporal.ChronoUnit.DAYS.between(deadline, event.getDate())) <= 1)
                ? "Near holiday"
                : "Clear";
    }

    private BigDecimal convert(BigDecimal amount, CurrencyRateService.RateSnapshot rateSnapshot) {
        return BigDecimal.valueOf(amount.doubleValue() * rateSnapshot.rateToTnd()).setScale(2, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal value) {
        return format(value) + " TND";
    }

    private String format(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String format(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private record MetricCard(String label, String value) {
    }

    public static final class PdfExportException extends RuntimeException {
        public PdfExportException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final class PdfDocument {
        private static final float PAGE_WIDTH = 595f;
        private static final float PAGE_HEIGHT = 842f;
        private static final float LEFT = 34f;
        private static final float RIGHT = 34f;
        private static final float TOP = 34f;
        private static final float BOTTOM = 40f;
        private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT - RIGHT;

        private static final String COLOR_NAVY = "0.04 0.11 0.25";
        private static final String COLOR_CYAN = "0.00 0.75 0.90";
        private static final String COLOR_CYAN_SOFT = "0.91 0.98 1.00";
        private static final String COLOR_TEXT = "0.13 0.22 0.33";
        private static final String COLOR_MUTED = "0.43 0.50 0.56";
        private static final String COLOR_LINE = "0.84 0.91 0.95";
        private static final String COLOR_WHITE = "1.00 1.00 1.00";

        private final List<StringBuilder> pageStreams = new ArrayList<>();
        private StringBuilder current;
        private float cursorY;
        private String reportTitle;
        private String reportSubtitle;

        void startPage(String title, String subtitle) {
            reportTitle = title;
            reportSubtitle = subtitle;
            current = new StringBuilder();
            pageStreams.add(current);
            cursorY = PAGE_HEIGHT - TOP;
            drawPageChrome();
        }

        void drawMetricCards(List<MetricCard> cards) {
            ensureSpace(90f);
            float cardWidth = (CONTENT_WIDTH - 24f) / 4f;
            float x = LEFT;
            float y = cursorY - 76f;
            for (MetricCard card : cards) {
                fillRect(x, y, cardWidth, 64f, COLOR_CYAN_SOFT);
                strokeRect(x, y, cardWidth, 64f, COLOR_LINE, 1f);
                drawText(card.label(), x + 12f, y + 42f, 10f, "F1", COLOR_MUTED);
                drawText(card.value(), x + 12f, y + 22f, 16f, "F2", COLOR_NAVY);
                x += cardWidth + 8f;
            }
            cursorY = y - 18f;
        }

        void drawSectionTitle(String title) {
            ensureSpace(26f);
            drawText(title, LEFT, cursorY - 12f, 16f, "F2", COLOR_NAVY);
            fillRect(LEFT, cursorY - 20f, 56f, 3f, COLOR_CYAN);
            cursorY -= 34f;
        }

        void drawKeyValueGrid(List<String> values) {
            ensureSpace(80f);
            float rowHeight = 24f;
            float boxHeight = ((values.size() / 2f) / 2f) * rowHeight + 16f;
            float y = cursorY - boxHeight;
            fillRect(LEFT, y, CONTENT_WIDTH, boxHeight, COLOR_WHITE);
            strokeRect(LEFT, y, CONTENT_WIDTH, boxHeight, COLOR_LINE, 1f);

            float colWidth = CONTENT_WIDTH / 2f;
            float textY = y + boxHeight - 24f;
            for (int i = 0; i < values.size(); i += 2) {
                float x = LEFT + ((i / 2) % 2) * colWidth + 14f;
                drawText(values.get(i), x, textY, 10f, "F1", COLOR_MUTED);
                drawText(values.get(i + 1), x, textY - 14f, 13f, "F2", COLOR_TEXT);
                if ((i / 2) % 2 == 1) {
                    textY -= rowHeight;
                }
            }
            cursorY = y - 18f;
        }

        void drawTable(List<String> headers, List<Float> widths, List<List<String>> rows) {
            List<Float> normalizedWidths = normalizeWidths(widths);
            if (rows.isEmpty()) {
                ensureSpace(44f);
                fillRect(LEFT, cursorY - 34f, CONTENT_WIDTH, 28f, COLOR_CYAN_SOFT);
                drawText("No rows available for the current export.", LEFT + 12f, cursorY - 18f, 11f, "F1", COLOR_TEXT);
                cursorY -= 46f;
                return;
            }

            drawTableHeader(headers, normalizedWidths);
            for (List<String> row : rows) {
                float rowHeight = computeRowHeight(row, normalizedWidths);
                ensureTableSpace(headers, normalizedWidths, rowHeight);
                drawTableRow(row, normalizedWidths, rowHeight);
            }
        }

        String build() {
            List<String> objects = new ArrayList<>();
            objects.add("<< /Type /Catalog /Pages 2 0 R >>");

            StringBuilder kids = new StringBuilder();
            int firstPageObject = 3;
            int firstContentObject = firstPageObject + pageStreams.size();
            for (int i = 0; i < pageStreams.size(); i++) {
                kids.append(firstPageObject + i).append(" 0 R ");
            }
            objects.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pageStreams.size() + " >>");

            for (int i = 0; i < pageStreams.size(); i++) {
                objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                        + "/Resources << /Font << /F1 " + (firstContentObject + pageStreams.size()) + " 0 R "
                        + "/F2 " + (firstContentObject + pageStreams.size() + 1) + " 0 R >> >> "
                        + "/Contents " + (firstContentObject + i) + " 0 R >>");
            }

            for (StringBuilder pageStream : pageStreams) {
                objects.add("<< /Length " + pageStream.length() + " >>\nstream\n" + pageStream + "\nendstream");
            }

            objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
            objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>");

            StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
            List<Integer> offsets = new ArrayList<>();
            for (int i = 0; i < objects.size(); i++) {
                offsets.add(pdf.length());
                pdf.append(i + 1).append(" 0 obj\n");
                pdf.append(objects.get(i)).append("\n");
                pdf.append("endobj\n");
            }

            int xrefOffset = pdf.length();
            pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
            pdf.append("0000000000 65535 f \n");
            for (Integer offset : offsets) {
                pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
            }
            pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
            pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF");
            return pdf.toString();
        }

        private void drawPageChrome() {
            fillRect(0f, PAGE_HEIGHT - 108f, PAGE_WIDTH, 108f, COLOR_NAVY);
            fillRect(0f, PAGE_HEIGHT - 112f, PAGE_WIDTH, 4f, COLOR_CYAN);
            drawText("Decide$", LEFT, PAGE_HEIGHT - 44f, 24f, "F2", COLOR_WHITE);
            drawText(reportTitle, LEFT, PAGE_HEIGHT - 72f, 22f, "F2", COLOR_WHITE);
            drawText(reportSubtitle, LEFT, PAGE_HEIGHT - 92f, 11f, "F1", "0.83 0.95 0.99");
            drawText("Generated " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    PAGE_WIDTH - RIGHT - 144f, PAGE_HEIGHT - 44f, 10f, "F1", COLOR_WHITE);
            cursorY = PAGE_HEIGHT - 170f;
        }

        private void drawTableHeader(List<String> headers, List<Float> widths) {
            ensureSpace(32f);
            float headerHeight = 24f;
            float x = LEFT;
            float y = cursorY - headerHeight;
            fillRect(LEFT, y, CONTENT_WIDTH, headerHeight, COLOR_NAVY);
            for (int i = 0; i < headers.size(); i++) {
                drawClippedTextBlock(List.of(headers.get(i)), x + 1f, y + 1f, widths.get(i) - 2f, headerHeight - 2f, 9f, "F2", COLOR_WHITE);
                x += widths.get(i);
            }
            cursorY = y;
        }

        private void drawTableRow(List<String> row, List<Float> widths, float rowHeight) {
            float y = cursorY - rowHeight;
            fillRect(LEFT, y, CONTENT_WIDTH, rowHeight, COLOR_WHITE);
            strokeRect(LEFT, y, CONTENT_WIDTH, rowHeight, COLOR_LINE, 0.8f);
            float x = LEFT;
            for (int i = 0; i < row.size(); i++) {
                float cellWidth = widths.get(i);
                strokeLine(x, y, x, y + rowHeight, COLOR_LINE, 0.6f);
                List<String> wrapped = wrap(row.get(i), cellWidth - 10f, 9f);
                drawClippedTextBlock(wrapped, x + 1f, y + 1f, cellWidth - 2f, rowHeight - 2f, 9f, "F1", COLOR_TEXT);
                x += cellWidth;
            }
            strokeLine(LEFT + CONTENT_WIDTH, y, LEFT + CONTENT_WIDTH, y + rowHeight, COLOR_LINE, 0.6f);
            cursorY = y;
        }

        private void ensureTableSpace(List<String> headers, List<Float> widths, float rowHeight) {
            if (cursorY - rowHeight < BOTTOM) {
                startPage(reportTitle, reportSubtitle);
                drawSectionTitle("Continued");
                drawTableHeader(headers, widths);
            }
        }

        private float computeRowHeight(List<String> row, List<Float> widths) {
            int lines = 1;
            for (int i = 0; i < row.size(); i++) {
                lines = Math.max(lines, wrap(row.get(i), widths.get(i) - 10f, 9f).size());
            }
            return Math.max(22f, 10f + (lines * 10.5f) + 6f);
        }

        private List<String> wrap(String text, float width, float fontSize) {
            String safe = sanitize(text);
            if (safe.isBlank()) {
                return List.of("");
            }
            int maxChars = Math.max(4, (int) (width / (fontSize * 0.47f)));
            List<String> lines = new ArrayList<>();
            StringBuilder currentLine = new StringBuilder();
            for (String word : safe.split(" ")) {
                if (currentLine.isEmpty()) {
                    appendWithSplit(lines, currentLine, word, maxChars);
                    continue;
                }
                if (currentLine.length() + 1 + word.length() <= maxChars) {
                    currentLine.append(' ').append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                    appendWithSplit(lines, currentLine, word, maxChars);
                }
            }
            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }
            return lines;
        }

        private void appendWithSplit(List<String> lines, StringBuilder currentLine, String word, int maxChars) {
            String remaining = word;
            while (remaining.length() > maxChars) {
                lines.add(remaining.substring(0, maxChars - 1) + "-");
                remaining = remaining.substring(maxChars - 1);
            }
            currentLine.append(remaining);
        }

        private void ensureSpace(float height) {
            if (cursorY - height < BOTTOM) {
                startPage(reportTitle, reportSubtitle);
            }
        }

        private List<Float> normalizeWidths(List<Float> widths) {
            float sum = 0f;
            for (Float width : widths) {
                sum += width;
            }
            float ratio = CONTENT_WIDTH / sum;
            List<Float> normalized = new ArrayList<>(widths.size());
            for (Float width : widths) {
                normalized.add(width * ratio);
            }
            return normalized;
        }

        private void drawText(String text, float x, float y, float size, String font, String color) {
            current.append("BT\n");
            current.append("/").append(font).append(" ").append(format(size)).append(" Tf\n");
            current.append(color).append(" rg\n");
            current.append("1 0 0 1 ").append(format(x)).append(" ").append(format(y)).append(" Tm\n");
            current.append("(").append(escapePdf(sanitize(text))).append(") Tj\n");
            current.append("ET\n");
        }

        private void drawClippedTextBlock(List<String> lines, float x, float y, float width, float height, float size, String font, String color) {
            current.append("q\n");
            current.append(format(x)).append(" ").append(format(y)).append(" ")
                    .append(format(width)).append(" ").append(format(height)).append(" re W n\n");
            float textY = y + height - Math.max(11f, size + 3f);
            for (String line : lines) {
                if (textY < y + 2f) {
                    break;
                }
                drawText(line, x + 4f, textY, size, font, color);
                textY -= size + 1.5f;
            }
            current.append("Q\n");
        }

        private void fillRect(float x, float y, float width, float height, String color) {
            current.append(color).append(" rg\n");
            current.append(format(x)).append(" ").append(format(y)).append(" ")
                    .append(format(width)).append(" ").append(format(height)).append(" re f\n");
        }

        private void strokeRect(float x, float y, float width, float height, String color, float lineWidth) {
            current.append(color).append(" RG\n");
            current.append(format(lineWidth)).append(" w\n");
            current.append(format(x)).append(" ").append(format(y)).append(" ")
                    .append(format(width)).append(" ").append(format(height)).append(" re S\n");
        }

        private void strokeLine(float x1, float y1, float x2, float y2, String color, float lineWidth) {
            current.append(color).append(" RG\n");
            current.append(format(lineWidth)).append(" w\n");
            current.append(format(x1)).append(" ").append(format(y1)).append(" m\n");
            current.append(format(x2)).append(" ").append(format(y2)).append(" l S\n");
        }

        private String format(float value) {
            return String.format(Locale.ROOT, "%.2f", value);
        }

        private String sanitize(String text) {
            if (text == null) {
                return "";
            }
            return text.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        }

        private String escapePdf(String value) {
            return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        }
    }
}
