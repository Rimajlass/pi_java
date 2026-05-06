package pi.controllers.CoursQuizController;

import pi.entities.Cours;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CoursePdfExporter {

    private static final DateTimeFormatter EXPORT_STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private CoursePdfExporter() {
    }

    static String suggestFileName(Cours cours) {
        String base = cours == null ? null : cours.getTitre();
        String safe = toSafeFileName(base);
        if (safe.isBlank()) {
            safe = "cours";
        }
        return safe + ".pdf";
    }

    static void exportCourse(Cours cours, Path outputFile) throws Exception {
        if (cours == null) {
            throw new IllegalArgumentException("Cours introuvable.");
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Chemin de sortie invalide.");
        }

        Path parent = outputFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        LocalDateTime now = LocalDateTime.now();
        CoursePdfDocument pdf = new CoursePdfDocument(
                "Fiche de cours",
                "Decide$ Learning - Export du cours",
                ""
        );

        pdf.drawHeroCourseTitle(valueOrDash(cours.getTitre()));

        pdf.drawSectionTitle("Contenu du cours");
        String content = normalizeContent(cours.getContenuTexte());
        if (content.isBlank()) {
            pdf.drawParagraph("Aucun contenu texte n'est disponible pour ce cours.", 11f);
        } else {
            pdf.drawParagraphs(content, 11f);
        }

        byte[] bytes = pdf.build();
        Files.write(outputFile.toAbsolutePath(), bytes);
    }

    private static String valueOrDash(String value) {
        String safe = value == null ? "" : value.trim();
        return safe.isEmpty() ? "--" : safe;
    }

    private static String normalizeContent(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r\n", "\n").trim();
    }

    private static String toSafeFileName(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String normalized = trimmed
                .replaceAll("[\\\\/:*?\"<>|]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .replace(' ', '-');
        normalized = normalized.replaceAll("[-]+", "-");
        if (normalized.length() > 80) {
            normalized = normalized.substring(0, 80);
        }
        return normalized;
    }

    private static final class CoursePdfDocument {
        private static final float PAGE_WIDTH = 595f;
        private static final float PAGE_HEIGHT = 842f;
        private static final float LEFT = 40f;
        private static final float RIGHT = 40f;
        private static final float TOP = 34f;
        private static final float BOTTOM = 44f;
        private static final float CONTENT_WIDTH = PAGE_WIDTH - LEFT - RIGHT;

        private static final String COLOR_NAVY = "0.04 0.11 0.25";
        private static final String COLOR_CYAN = "0.00 0.75 0.90";
        private static final String COLOR_CYAN_SOFT = "0.91 0.98 1.00";
        private static final String COLOR_TEXT = "0.13 0.22 0.33";
        private static final String COLOR_MUTED = "0.43 0.50 0.56";
        private static final String COLOR_LINE = "0.84 0.91 0.95";
        private static final String COLOR_WHITE = "1.00 1.00 1.00";

        private final String headerTitle;
        private final String headerSubtitle;
        private final String footerRight;

        private final List<StringBuilder> pageStreams = new ArrayList<>();
        private StringBuilder current;
        private float cursorY;
        private int pageNumber = 0;

        private CoursePdfDocument(String headerTitle, String headerSubtitle, String footerRight) {
            this.headerTitle = safe(headerTitle);
            this.headerSubtitle = safe(headerSubtitle);
            this.footerRight = safe(footerRight);
            startPage();
        }

        void drawSectionTitle(String title) {
            ensureSpace(34f);
            drawText(title, LEFT, cursorY - 14f, 16f, "F2", COLOR_NAVY);
            fillRect(LEFT, cursorY - 22f, 70f, 3f, COLOR_CYAN);
            cursorY -= 38f;
        }

        void drawHeroCourseTitle(String title) {
            ensureSpace(110f);
            float cardHeight = 86f;
            float y = cursorY - cardHeight;

            fillRect(LEFT, y, CONTENT_WIDTH, cardHeight, COLOR_CYAN_SOFT);
            strokeRect(LEFT, y, CONTENT_WIDTH, cardHeight, COLOR_LINE, 1f);

            drawText("Titre du cours", LEFT + 14f, y + cardHeight - 26f, 10f, "F1", COLOR_MUTED);

            List<String> lines = wrap(title, CONTENT_WIDTH - 28f, 18f);
            float titleY = y + cardHeight - 48f;
            for (int i = 0; i < Math.min(lines.size(), 2); i++) {
                drawText(lines.get(i), LEFT + 14f, titleY, 18f, "F2", COLOR_NAVY);
                titleY -= 20f;
            }

            cursorY = y - 22f;
        }

        void drawKeyValueGrid(List<String> values) {
            if (values == null || values.isEmpty()) {
                return;
            }
            ensureSpace(90f);

            float rowHeight = 28f;
            int rows = (int) Math.ceil((values.size() / 2f) / 2f);
            float boxHeight = (rows * rowHeight) + 18f;

            float y = cursorY - boxHeight;
            fillRect(LEFT, y, CONTENT_WIDTH, boxHeight, COLOR_WHITE);
            strokeRect(LEFT, y, CONTENT_WIDTH, boxHeight, COLOR_LINE, 1f);

            float colWidth = CONTENT_WIDTH / 2f;
            float textY = y + boxHeight - 28f;
            for (int i = 0; i < values.size(); i += 2) {
                float x = LEFT + ((i / 2) % 2) * colWidth + 14f;
                drawText(values.get(i), x, textY, 10f, "F1", COLOR_MUTED);
                drawText(values.get(i + 1), x, textY - 14f, 12.5f, "F2", COLOR_TEXT);
                if ((i / 2) % 2 == 1) {
                    textY -= rowHeight;
                }
            }

            cursorY = y - 18f;
        }

        void drawParagraphs(String text, float fontSize) {
            String normalized = safe(text).replace("\r\n", "\n").trim();
            if (normalized.isEmpty()) {
                return;
            }

            String[] paragraphs = normalized.split("\\n\\s*\\n");
            for (String paragraph : paragraphs) {
                drawParagraph(paragraph, fontSize);
                drawSpacer(8f);
            }
        }

        void drawParagraph(String text, float fontSize) {
            List<String> lines = wrap(text, CONTENT_WIDTH, fontSize);
            float lineHeight = fontSize + 3.5f;
            for (String line : lines) {
                ensureSpace(lineHeight + 4f);
                drawText(line, LEFT, cursorY - lineHeight, fontSize, "F1", COLOR_TEXT);
                cursorY -= lineHeight;
            }
        }

        void drawMutedNote(String text) {
            ensureSpace(22f);
            fillRect(LEFT, cursorY - 22f, CONTENT_WIDTH, 20f, COLOR_CYAN_SOFT);
            strokeRect(LEFT, cursorY - 22f, CONTENT_WIDTH, 20f, COLOR_LINE, 1f);
            drawText(text, LEFT + 12f, cursorY - 16f, 10.5f, "F1", COLOR_MUTED);
            cursorY -= 32f;
        }

        void drawSpacer(float height) {
            cursorY -= Math.max(0f, height);
        }

        byte[] build() {
            List<String> objects = new ArrayList<>();

            // 1) Catalog
            objects.add("<< /Type /Catalog /Pages 2 0 R >>");

            // 2) Pages
            StringBuilder kids = new StringBuilder();
            int pageCount = pageStreams.size();
            int firstPageObject = 3;
            int firstContentObject = firstPageObject + pageCount;
            for (int i = 0; i < pageCount; i++) {
                kids.append(firstPageObject + i).append(" 0 R ");
            }
            objects.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>");

            // 3..N) Page objects
            for (int i = 0; i < pageCount; i++) {
                int contentId = firstContentObject + i;
                StringBuilder page = new StringBuilder();
                page.append("<< /Type /Page ");
                page.append("/Parent 2 0 R ");
                page.append("/MediaBox [0 0 595 842] ");
                page.append("/Resources << /Font << ");
                page.append("/F1 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >> ");
                page.append("/F2 << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >> ");
                page.append(">> >> ");
                page.append("/Contents ").append(contentId).append(" 0 R ");
                page.append(">>");
                objects.add(page.toString());
            }

            // Content streams
            for (StringBuilder stream : pageStreams) {
                String content = stream.toString();
                byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);
                StringBuilder obj = new StringBuilder();
                obj.append("<< /Length ").append(contentBytes.length).append(" >>\n");
                obj.append("stream\n");
                obj.append(content);
                if (!content.endsWith("\n")) {
                    obj.append("\n");
                }
                obj.append("endstream");
                objects.add(obj.toString());
            }

            // Assemble PDF with xref
            StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
            List<Integer> offsets = new ArrayList<>();
            offsets.add(0);
            for (int i = 0; i < objects.size(); i++) {
                offsets.add(pdf.length());
                pdf.append(i + 1).append(" 0 obj\n");
                pdf.append(objects.get(i)).append("\n");
                pdf.append("endobj\n");
            }

            int xrefOffset = pdf.length();
            pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
            pdf.append("0000000000 65535 f \n");
            for (int i = 1; i < offsets.size(); i++) {
                pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i)));
            }
            pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
            pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF");

            return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
        }

        private void startPage() {
            pageNumber++;
            current = new StringBuilder();
            pageStreams.add(current);
            cursorY = PAGE_HEIGHT - TOP;
            drawPageChrome();
        }

        private void drawPageChrome() {
            // Background frame
            fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, COLOR_WHITE);
            strokeRect(18f, 18f, PAGE_WIDTH - 36f, PAGE_HEIGHT - 36f, COLOR_LINE, 1f);

            // Header banner
            fillRect(0, PAGE_HEIGHT - 132f, PAGE_WIDTH, 132f, COLOR_NAVY);
            fillRect(0, PAGE_HEIGHT - 132f, PAGE_WIDTH, 6f, COLOR_CYAN);

            drawText("Decide$", LEFT, PAGE_HEIGHT - 52f, 24f, "F2", COLOR_WHITE);
            drawText(headerTitle, LEFT, PAGE_HEIGHT - 80f, 20f, "F2", COLOR_WHITE);
            drawText(headerSubtitle, LEFT, PAGE_HEIGHT - 102f, 11f, "F1", "0.83 0.95 0.99");

            // Footer
            drawText("Page " + pageNumber, LEFT, 26f, 9.5f, "F1", COLOR_MUTED);
            drawText(footerRight, PAGE_WIDTH - RIGHT - 170f, 26f, 9.5f, "F1", COLOR_MUTED);

            cursorY = PAGE_HEIGHT - 156f;
        }

        private void ensureSpace(float height) {
            if (cursorY - height < BOTTOM) {
                startPage();
            }
        }

        private List<String> wrap(String text, float width, float fontSize) {
            String safe = normalizeForPdf(text);
            if (safe.isBlank()) {
                return List.of("");
            }

            int maxChars = Math.max(8, (int) (width / (fontSize * 0.47f)));
            List<String> lines = new ArrayList<>();
            StringBuilder currentLine = new StringBuilder();
            for (String rawWord : safe.split("\\s+")) {
                String word = rawWord.trim();
                if (word.isEmpty()) {
                    continue;
                }
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

        private void drawText(String text, float x, float y, float size, String font, String color) {
            current.append("BT\n");
            current.append("/").append(font).append(" ").append(format(size)).append(" Tf\n");
            current.append(color).append(" rg\n");
            current.append("1 0 0 1 ").append(format(x)).append(" ").append(format(y)).append(" Tm\n");
            current.append("(").append(escapePdf(normalizeForPdf(text))).append(") Tj\n");
            current.append("ET\n");
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

        @SuppressWarnings("SameParameterValue")
        private void strokeLine(float x1, float y1, float x2, float y2, String color, float lineWidth) {
            current.append(color).append(" RG\n");
            current.append(format(lineWidth)).append(" w\n");
            current.append(format(x1)).append(" ").append(format(y1)).append(" m\n");
            current.append(format(x2)).append(" ").append(format(y2)).append(" l S\n");
        }

        private String format(float value) {
            return String.format(Locale.ROOT, "%.2f", value);
        }

        private String normalizeForPdf(String text) {
            String base = safe(text)
                    .replace('\r', ' ')
                    .replace('\n', ' ')
                    .replace('\t', ' ')
                    .replace('•', '-')
                    .replace('’', '\'')
                    .replace('“', '"')
                    .replace('”', '"');

            // Keep PDF stream ISO-8859-1 friendly
            StringBuilder out = new StringBuilder(base.length());
            for (int i = 0; i < base.length(); i++) {
                char ch = base.charAt(i);
                if (ch <= 255) {
                    out.append(ch);
                } else {
                    out.append('?');
                }
            }
            return out.toString();
        }

        private String escapePdf(String value) {
            return value
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)");
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }
}
