package pi.services.MailingService;

import javafx.application.Platform;
import javafx.scene.control.Alert;

final class MailDebugPopup {

    private MailDebugPopup() {
    }

    static void showInfo(String message) {
        show(Alert.AlertType.INFORMATION, "Mail", message);
    }

    static void showError(String message) {
        show(Alert.AlertType.ERROR, "Mail", message);
    }

    private static void show(Alert.AlertType type, String title, String message) {
        try {
            Runnable task = () -> {
                Alert alert = new Alert(type);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(message);
                alert.show();
            };

            if (Platform.isFxApplicationThread()) {
                task.run();
            } else {
                Platform.runLater(task);
            }
        } catch (IllegalStateException ignored) {
            // JavaFX toolkit non initialisé (ex: tests/unit); on ignore.
        } catch (Exception e) {
            System.out.println("[MAIL] Popup debug échouée: " + e.getMessage());
        }
    }
}

