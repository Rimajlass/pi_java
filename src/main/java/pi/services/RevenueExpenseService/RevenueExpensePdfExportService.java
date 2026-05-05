package pi.services.RevenueExpenseService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import pi.entities.Revenue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class RevenueExpensePdfExportService {

    private static final float PAGE_MARGIN = 50f;
    private static final float LINE_HEIGHT = 18f;
    private static final PDType1Font TITLE_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font HEADER_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font BODY_FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void exportRevenues(Path filePath, List<Revenue> revenues) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("filePath cannot be null");
        }
        if (revenues == null || revenues.isEmpty()) {
            throw new IllegalArgumentException("No revenue data available to export");
        }

        try (PDDocument document = new PDDocument()) {
            PdfCanvas canvas = new PdfCanvas(document, "Revenue Report");
            canvas.writeLine("Revenue Report", TITLE_FONT, 18f);
            canvas.writeLine("Generated on: " + formatDate(LocalDate.now()), BODY_FONT, 10f);
            canvas.writeBlankLine();
            canvas.writeLine("Date | Amount (TND) | Type | Description", HEADER_FONT, 12f);
            canvas.writeLine(repeat('-', 110), BODY_FONT, 10f);

            for (Revenue revenue : revenues) {
                canvas.writeWrappedLine(formatRevenueLine(revenue), BODY_FONT, 11f);
            }

            canvas.save(filePath);
        }
    }

    public void exportExpenses(Path filePath, List<pi.entities.Expense> expenses, List<ExpenseRowData> rowData) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("filePath cannot be null");
        }
        if (expenses == null || expenses.isEmpty()) {
            throw new IllegalArgumentException("No expense data available to export");
        }
        if (rowData == null || rowData.size() != expenses.size()) {
            throw new IllegalArgumentException("Expense export metadata is invalid");
        }

        try (PDDocument document = new PDDocument()) {
            PdfCanvas canvas = new PdfCanvas(document, "Expense Report");
            canvas.writeLine("Expense Report", TITLE_FONT, 18f);
            canvas.writeLine("Generated on: " + formatDate(LocalDate.now()), BODY_FONT, 10f);
            canvas.writeBlankLine();
            canvas.writeLine("Date | Amount (TND) | Category | Linked Revenue | Description", HEADER_FONT, 12f);
            canvas.writeLine(repeat('-', 130), BODY_FONT, 10f);

            for (int i = 0; i < expenses.size(); i++) {
                canvas.writeWrappedLine(formatExpenseLine(expenses.get(i), rowData.get(i)), BODY_FONT, 11f);
            }

            canvas.save(filePath);
        }
    }

    private String formatRevenueLine(Revenue revenue) {
        return "%s | %s | %s | %s".formatted(
                formatDate(revenue.getReceivedAt()),
                formatAmount(revenue.getAmount()),
                safe(revenue.getType()),
                safe(revenue.getDescription())
        );
    }

    private String formatExpenseLine(pi.entities.Expense expense, ExpenseRowData rowData) {
        return "%s | %s | %s | %s | %s".formatted(
                formatDate(expense.getExpenseDate()),
                formatAmount(expense.getAmount()),
                safe(expense.getCategory()),
                safe(rowData.linkedRevenueLabel()),
                safe(expense.getDescription())
        );
    }

    private String formatAmount(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "--/--/----" : DATE_FORMATTER.format(date);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String repeat(char character, int count) {
        return String.valueOf(character).repeat(Math.max(0, count));
    }

    public record ExpenseRowData(String linkedRevenueLabel) {
    }

    private static final class PdfCanvas {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream contentStream;
        private float cursorY;

        private PdfCanvas(PDDocument document, String title) throws IOException {
            this.document = document;
            newPage();
            writeLine(title, TITLE_FONT, 18f);
            writeBlankLine();
        }

        private void newPage() throws IOException {
            closeStream();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            cursorY = page.getMediaBox().getHeight() - PAGE_MARGIN;
        }

        private void writeLine(String text, PDType1Font font, float fontSize) throws IOException {
            ensureSpace(1);
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(PAGE_MARGIN, cursorY);
            contentStream.showText(sanitize(text));
            contentStream.endText();
            cursorY -= LINE_HEIGHT;
        }

        private void writeWrappedLine(String text, PDType1Font font, float fontSize) throws IOException {
            List<String> lines = wrapText(text, 98);
            ensureSpace(lines.size());
            for (String line : lines) {
                writeLine(line, font, fontSize);
            }
        }

        private void writeBlankLine() throws IOException {
            ensureSpace(1);
            cursorY -= LINE_HEIGHT * 0.6f;
        }

        private void ensureSpace(int linesNeeded) throws IOException {
            if (cursorY - (linesNeeded * LINE_HEIGHT) < PAGE_MARGIN) {
                newPage();
            }
        }

        private void save(Path filePath) throws IOException {
            closeStream();
            document.save(filePath.toFile());
        }

        private void closeStream() throws IOException {
            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }

        private List<String> wrapText(String text, int maxChars) {
            String normalized = sanitize(text);
            if (normalized.length() <= maxChars) {
                return List.of(normalized);
            }

            java.util.ArrayList<String> lines = new java.util.ArrayList<>();
            String[] words = normalized.split(" ");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                if (current.isEmpty()) {
                    current.append(word);
                    continue;
                }
                if (current.length() + 1 + word.length() <= maxChars) {
                    current.append(' ').append(word);
                } else {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines;
        }

        private String sanitize(String value) {
            if (value == null) {
                return "";
            }
            return value
                    .replace('\u2019', '\'')
                    .replace('\u2013', '-')
                    .replace('\u2014', '-')
                    .replace('\n', ' ')
                    .replace('\r', ' ');
        }
    }
}
