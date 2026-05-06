package pi.services.MailingService;

import javafx.application.Platform;
import javafx.scene.control.Alert;

public final class MailFxPopup {

    private MailFxPopup() {
    }

    public static void showResult(MailSendResult result) {
        if (result == null) {
            return;
        }

        try {
            Runnable task = () -> {
                Alert.AlertType type = result.isSent() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING;
                Alert alert = new Alert(type);
                alert.setHeaderText(null);
                alert.setTitle("Email");

                String content;
                if (result.isSent()) {
                    content = "Email envoyé.\n" + safe(result.getMessage());
                } else {
                    content = "Email non envoyé.\n" + safe(result.getMessage());
                }

                alert.setContentText(content.trim());
                alert.showAndWait();
            };

            if (Platform.isFxApplicationThread()) {
                task.run();
            } else {
                Platform.runLater(task);
            }
        } catch (IllegalStateException e) {
            // JavaFX toolkit non initialisé (ex: tests unitaires, exécution headless).
            System.out.println("[MAIL] Popup indisponible: " + e.getMessage());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
