package pi.services.ImprevusCasreelService;

import pi.entities.CasRelles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CaseReportExporter {

    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private CaseReportExporter() {
    }

    public record CaseStats(
            int totalCases,
            int gainCases,
            int depenseCases,
            int pendingCases,
            int acceptedCases,
            double totalGainAmount,
            double totalExpenseAmount,
            String dominantRisk
    ) {
    }

    public static Path writePdf(List<CasRelles> cases, CaseStats stats, Path exportDirectory) throws IOException {
        Files.createDirectories(exportDirectory);
        Path file = exportDirectory.resolve("cas-reels-bilan-" + LocalDateTime.now().format(FILE_STAMP) + ".pdf");

        PdfDocument pdf = new PdfDocument();
        pdf.startPage("Bilan Cas Reels", "Export Decide$ du module imprevus / cas reels");
        pdf.drawMetricCards(List.of(
                new MetricCard("Cas", String.valueOf(stats.totalCases())),
                new MetricCard("En attente", String.valueOf(stats.pendingCases())),
                new MetricCard("Acceptes", String.valueOf(stats.acceptedCases())),
                new MetricCard("Risque dominant", stats.dominantRisk())
        ));
        pdf.drawSectionTitle("Resume");
        pdf.drawKeyValueGrid(List.of(
                "Gains", money(stats.totalGainAmount()),
                "Depenses", money(stats.totalExpenseAmount()),
                "Nombre gains", String.valueOf(stats.gainCases()),
                "Nombre depenses", String.valueOf(stats.depenseCases())
        ));
        pdf.drawSectionTitle("Historique");
        pdf.drawTable(
                List.of("Date", "Titre", "Type", "Montant", "Statut", "Affectation"),
                List.of(68f, 176f, 52f, 60f, 86f, 92f),
                toRows(cases)
        );

        Files.write(file, pdf.build().getBytes(StandardCharsets.ISO_8859_1));
        return file;
    }

    private static List<List<String>> toRows(List<CasRelles> cases) {
        List<List<String>> rows = new ArrayList<>();
        for (CasRelles cas : cases) {
            rows.add(List.of(
                    cas.getDateEffet() == null ? "-" : cas.getDateEffet().toString(),
                    safe(cas.getTitre()),
                    safe(cas.getType()),
                    money(cas.getMontant()),
                    safe(cas.getResultat()),
                    safe(cas.getPaymentMethod())
            ));
        }
        return rows;
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String money(double value) {
        return String.format(Locale.US, "%.2f TND", value);
    }

    private record MetricCard(String label, String value) {
    }

    private static final class PdfDocument {
        private static final float PAGE_WIDTH = 595f;
        private static final float PAGE_HEIGHT = 842f;
        private static final float LEFT = 34f;
        private static final float RIGHT = 34f;
        private static final float TOP = 34f;
        private static final float BOTTOM = 40f;
        private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT - RIGHT;
        private static final String COLOR_NAVY = "0.08 0.17 0.33";
        private static final String COLOR_GOLD = "0.84 0.63 0.18";
        private static final String COLOR_SOFT = "0.96 0.98 1.00";
        private static final String COLOR_TEXT = "0.15 0.22 0.31";
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
                fillRect(x, y, cardWidth, 64f, COLOR_SOFT);
                strokeRect(x, y, cardWidth, 64f, COLOR_LINE, 1f);
                drawText(card.label(), x + 12f, y + 42f, 10f, "F1", COLOR_MUTED);
                drawText(card.value(), x + 12f, y + 22f, 15f, "F2", COLOR_NAVY);
                x += cardWidth + 8f;
            }
            cursorY = y - 18f;
        }

        void drawSectionTitle(String title) {
            ensureSpace(26f);
            drawText(title, LEFT, cursorY - 12f, 16f, "F2", COLOR_NAVY);
            fillRect(LEFT, cursorY - 20f, 60f, 3f, COLOR_GOLD);
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
                fillRect(LEFT, cursorY - 34f, CONTENT_WIDTH, 28f, COLOR_SOFT);
                drawText("Aucun cas a exporter.", LEFT + 12f, cursorY - 18f, 11f, "F1", COLOR_TEXT);
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
                pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
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
            fillRect(0f, PAGE_HEIGHT - 112f, PAGE_WIDTH, 4f, COLOR_GOLD);
            drawText("Decide$", LEFT, PAGE_HEIGHT - 44f, 24f, "F2", COLOR_WHITE);
            drawText(reportTitle, LEFT, PAGE_HEIGHT - 72f, 22f, "F2", COLOR_WHITE);
            drawText(reportSubtitle, LEFT, PAGE_HEIGHT - 92f, 11f, "F1", "0.88 0.92 0.98");
            cursorY = PAGE_HEIGHT - 150f;
        }

        private void drawTableHeader(List<String> headers, List<Float> widths) {
            ensureSpace(32f);
            float h = 24f;
            float x = LEFT;
            float y = cursorY - h;
            fillRect(LEFT, y, CONTENT_WIDTH, h, COLOR_NAVY);
            for (int i = 0; i < headers.size(); i++) {
                drawClippedTextBlock(List.of(headers.get(i)), x + 1f, y + 1f, widths.get(i) - 2f, h - 2f, 9f, "F2", COLOR_WHITE);
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
                float w = widths.get(i);
                strokeLine(x, y, x, y + rowHeight, COLOR_LINE, 0.6f);
                drawClippedTextBlock(wrap(row.get(i), w - 10f, 9f), x + 1f, y + 1f, w - 2f, rowHeight - 2f, 9f, "F1", COLOR_TEXT);
                x += w;
            }
            strokeLine(LEFT + CONTENT_WIDTH, y, LEFT + CONTENT_WIDTH, y + rowHeight, COLOR_LINE, 0.6f);
            cursorY = y;
        }

        private void ensureTableSpace(List<String> headers, List<Float> widths, float rowHeight) {
            if (cursorY - rowHeight < BOTTOM) {
                startPage(reportTitle, reportSubtitle);
                drawSectionTitle("Suite");
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
            if (safe.isBlank()) return List.of("");
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
            if (!currentLine.isEmpty()) lines.add(currentLine.toString());
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
            if (cursorY - height < BOTTOM) startPage(reportTitle, reportSubtitle);
        }

        private List<Float> normalizeWidths(List<Float> widths) {
            float sum = 0f;
            for (Float width : widths) sum += width;
            float ratio = CONTENT_WIDTH / sum;
            List<Float> normalized = new ArrayList<>();
            for (Float width : widths) normalized.add(width * ratio);
            return normalized;
        }

        private void drawText(String text, float x, float y, float size, String font, String color) {
            current.append("BT\n/");
            current.append(font).append(" ").append(format(size)).append(" Tf\n");
            current.append(color).append(" rg\n");
            current.append("1 0 0 1 ").append(format(x)).append(" ").append(format(y)).append(" Tm\n");
            current.append("(").append(escapePdf(sanitize(text))).append(") Tj\nET\n");
        }

        private void drawClippedTextBlock(List<String> lines, float x, float y, float width, float height, float size, String font, String color) {
            current.append("q\n");
            current.append(format(x)).append(" ").append(format(y)).append(" ")
                    .append(format(width)).append(" ").append(format(height)).append(" re W n\n");
            float textY = y + height - Math.max(11f, size + 3f);
            for (String line : lines) {
                if (textY < y + 2f) break;
                drawText(line, x + 4f, textY, size, font, color);
                textY -= size + 1.5f;
            }
            current.append("Q\n");
        }

        private void fillRect(float x, float y, float width, float height, String color) {
            current.append(color).append(" rg\n");
            current.append(format(x)).append(" ").append(format(y)).append(" ").append(format(width)).append(" ").append(format(height)).append(" re f\n");
        }

        private void strokeRect(float x, float y, float width, float height, String color, float lineWidth) {
            current.append(color).append(" RG\n").append(format(lineWidth)).append(" w\n");
            current.append(format(x)).append(" ").append(format(y)).append(" ").append(format(width)).append(" ").append(format(height)).append(" re S\n");
        }

        private void strokeLine(float x1, float y1, float x2, float y2, String color, float lineWidth) {
            current.append(color).append(" RG\n").append(format(lineWidth)).append(" w\n");
            current.append(format(x1)).append(" ").append(format(y1)).append(" m\n");
            current.append(format(x2)).append(" ").append(format(y2)).append(" l S\n");
        }

        private String format(float value) {
            return String.format(Locale.ROOT, "%.2f", value);
        }

        private String sanitize(String text) {
            if (text == null) return "";
            return text.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
        }

        private String escapePdf(String value) {
            return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        }
    }
}
