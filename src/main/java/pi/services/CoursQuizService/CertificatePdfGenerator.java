package pi.services.CoursQuizService;

import pi.entities.Cours;
import pi.entities.User;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class CertificatePdfGenerator {

    private CertificatePdfGenerator() {
    }

    public static void generateTo(Path outputFile, User user, Cours cours, int percentage, String certificateCode) throws IOException {
        if (outputFile == null) {
            throw new IllegalArgumentException("Fichier de sortie invalide.");
        }
        Files.createDirectories(outputFile.toAbsolutePath().getParent());

        PDRectangle pageSize = PDRectangle.A4;
        PDPage page = new PDPage(pageSize);
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float w = pageSize.getWidth();
                float h = pageSize.getHeight();

                // Background
                cs.setNonStrokingColor(new Color(246, 250, 255));
                cs.addRect(0, 0, w, h);
                cs.fill();

                // Decorative header bar
                cs.setNonStrokingColor(new Color(15, 106, 166));
                cs.addRect(0, h - 70, w, 70);
                cs.fill();

                cs.setNonStrokingColor(Color.WHITE);
                drawCentered(cs, PDType1Font.HELVETICA_BOLD, 18, w / 2, h - 45, "Decide$ Learning");

                // Border
                cs.setStrokingColor(new Color(15, 106, 166));
                cs.setLineWidth(2);
                cs.addRect(36, 36, w - 72, h - 110);
                cs.stroke();

                // Title
                cs.setNonStrokingColor(new Color(16, 38, 58));
                drawCentered(cs, PDType1Font.HELVETICA_BOLD, 28, w / 2, h - 140, "CERTIFICAT DE RÉUSSITE");

                // Subtitle
                cs.setNonStrokingColor(new Color(79, 96, 115));
                drawCentered(cs, PDType1Font.HELVETICA, 12, w / 2, h - 165, "Ce certificat atteste la réussite du quiz avec un score de performance.");

                String userName = safe(user == null ? "" : user.getNom());
                String userEmail = safe(user == null ? "" : user.getEmail());
                String coursTitle = safe(cours == null ? "" : cours.getTitre());
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

                // Recipient
                cs.setNonStrokingColor(new Color(16, 38, 58));
                drawCentered(cs, PDType1Font.HELVETICA_BOLD, 20, w / 2, h - 220, userName.isBlank() ? "Étudiant" : userName);
                if (!userEmail.isBlank()) {
                    cs.setNonStrokingColor(new Color(79, 96, 115));
                    drawCentered(cs, PDType1Font.HELVETICA, 12, w / 2, h - 242, userEmail);
                }

                // Course box
                float boxX = 70;
                float boxW = w - 140;
                float boxY = h - 370;
                float boxH = 110;
                cs.setNonStrokingColor(Color.WHITE);
                cs.addRect(boxX, boxY, boxW, boxH);
                cs.fill();
                cs.setStrokingColor(new Color(219, 229, 238));
                cs.setLineWidth(1.2f);
                cs.addRect(boxX, boxY, boxW, boxH);
                cs.stroke();

                cs.setNonStrokingColor(new Color(79, 96, 115));
                drawText(cs, PDType1Font.HELVETICA_BOLD, 12, boxX + 18, boxY + boxH - 28, "Cours");
                cs.setNonStrokingColor(new Color(16, 38, 58));
                drawText(cs, PDType1Font.HELVETICA_BOLD, 16, boxX + 18, boxY + boxH - 52, truncate(coursTitle, 70));

                cs.setNonStrokingColor(new Color(79, 96, 115));
                drawText(cs, PDType1Font.HELVETICA, 12, boxX + 18, boxY + boxH - 78,
                        "Score: " + percentage + "%  •  Seuil: " + LearningCertificationService.PASS_THRESHOLD_PERCENT + "%");

                // Footer
                cs.setNonStrokingColor(new Color(79, 96, 115));
                drawText(cs, PDType1Font.HELVETICA, 11, 70, 90, "Date: " + now);
                drawText(cs, PDType1Font.HELVETICA, 11, 70, 72, "Code de vérification: " + safe(certificateCode));
                drawText(cs, PDType1Font.HELVETICA, 10, 70, 52, "Généré automatiquement par Decide$.");
            }

            doc.save(outputFile.toFile());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static void drawCentered(PDPageContentStream cs, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float centerX, float y, String text) throws IOException {
        String v = safe(text);
        float textWidth = font.getStringWidth(v) / 1000f * fontSize;
        float x = centerX - textWidth / 2f;
        drawText(cs, font, fontSize, x, y, v);
    }

    private static void drawText(PDPageContentStream cs, org.apache.pdfbox.pdmodel.font.PDFont font, float fontSize, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(safe(text));
        cs.endText();
    }

    private static String truncate(String value, int max) {
        String v = safe(value).trim();
        if (v.length() <= max) {
            return v;
        }
        return v.substring(0, Math.max(0, max - 1)) + "…";
    }
}
