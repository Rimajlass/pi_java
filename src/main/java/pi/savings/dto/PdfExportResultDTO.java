package pi.savings.dto;

import java.nio.file.Path;

public record PdfExportResultDTO(
        boolean success,
        String userMessage,
        Path savedFile,
        boolean apiGenerated,
        boolean fallbackUsed
) {
    public static PdfExportResultDTO success(String userMessage, Path savedFile, boolean apiGenerated, boolean fallbackUsed) {
        return new PdfExportResultDTO(true, userMessage, savedFile, apiGenerated, fallbackUsed);
    }

    public static PdfExportResultDTO error(String userMessage) {
        return new PdfExportResultDTO(false, userMessage, null, false, false);
    }
}
