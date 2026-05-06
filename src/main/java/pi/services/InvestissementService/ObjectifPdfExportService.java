package pi.services.InvestissementService;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import pi.entities.Objectif;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ObjectifPdfExportService {

    private ObjectifPdfExportService() {
    }

    public static void export(File file, List<Objectif> objectifs, Map<Integer, Double> currentValueByObjectifId)
            throws DocumentException, IOException {

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

        document.add(new Paragraph("Decide$ — Pilotage des objectifs d'investissement", titleFont));
        document.add(new Paragraph(
                "Genere le " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(java.time.LocalDateTime.now()),
                normalFont));

        appendExecutiveSummary(document, objectifs, currentValueByObjectifId, boldFont, normalFont);

        document.add(new Paragraph(" ", normalFont));

        PdfPTable table = new PdfPTable(11);
        table.setWidthPercentage(100);

        String[] headers = {
                "Nom", "Priorite", "Initial", "Cible", "Actuelle", "Reste",
                "% prog.", "ROI %", "Alerte metier", "Statut", "Cree le"
        };
        for (String h : headers) {
            table.addCell(h);
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (Objectif o : objectifs) {
            double current = currentValueByObjectifId.getOrDefault(o.getId(), 0.0);
            double target = o.getTargetAmount();
            double remaining = o.isCompleted() ? 0.0 : Math.max(0.0, target - current);
            double pct = target > 0 ? Math.min(100.0, current / target * 100.0) : 0.0;
            double roi = ObjectifMetrics.roiPercent(o, current);

            table.addCell(safe(o.getName()));
            table.addCell(ObjectifMetrics.prioriteLabel(o.getPriorite()));
            table.addCell(fmtUsd(o.getInitialAmount()));
            table.addCell(fmtUsd(target));
            table.addCell(fmtUsd(current));
            table.addCell(fmtUsd(remaining));
            table.addCell(String.format(Locale.US, "%.1f %%", pct));
            table.addCell(String.format(Locale.US, "%+.1f %%", roi));
            table.addCell(ObjectifMetrics.alerteMetier(o, current));
            table.addCell(o.isCompleted() ? "Complete" : "En cours");
            table.addCell(o.getCreatedAt() != null ? o.getCreatedAt().format(df) : "—");
        }

        document.add(table);
        document.close();
    }

    private static void appendExecutiveSummary(
            Document document,
            List<Objectif> objectifs,
            Map<Integer, Double> currentById,
            Font boldFont,
            Font normalFont) throws DocumentException {

        int total = objectifs.size();
        long done = objectifs.stream().filter(Objectif::isCompleted).count();
        double sumCurrent = 0;
        double sumTargetOpen = 0;
        int critiquesRetard = 0;

        for (Objectif o : objectifs) {
            double cur = currentById.getOrDefault(o.getId(), 0.0);
            sumCurrent += cur;
            if (!o.isCompleted()) {
                sumTargetOpen += o.getTargetAmount();
                double ratio = o.getTargetAmount() > 0 ? cur / o.getTargetAmount() : 0;
                if (Objectif.P_CRITIQUE.equals(o.getPriorite()) && ratio < 0.35) {
                    critiquesRetard++;
                }
            }
        }

        document.add(new Paragraph("Synthese executive", boldFont));
        document.add(new Paragraph(
                String.format(Locale.FRENCH,
                        "Nombre d'objectifs : %d — Completes : %d — En cours : %d",
                        total, done, total - done),
                normalFont));
        document.add(new Paragraph(
                String.format(Locale.FRENCH,
                        "Valeur de marche cumulee (tous objectifs) : %.2f USD — Cible cumulee (objectifs non completes) : %.2f USD",
                        sumCurrent, sumTargetOpen),
                normalFont));
        document.add(new Paragraph(
                String.format(Locale.FRENCH,
                        "Objectifs critiques avec retard significatif (< 35 %% de la cible) : %d",
                        critiquesRetard),
                normalFont));
    }

    private static String fmtUsd(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }
}
