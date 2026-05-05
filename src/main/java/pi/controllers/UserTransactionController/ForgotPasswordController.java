package pi.controllers.UserTransactionController;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ForgotPasswordController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final String SUCCESS_MESSAGE =
            "Si cette adresse existe dans notre syst\u00e8me, un lien de r\u00e9initialisation a \u00e9t\u00e9 envoy\u00e9.";
    private static final String ERROR_MESSAGE = "Veuillez entrer une adresse email valide.";
    private static final String DELIVERY_ERROR_MESSAGE =
            "Le service d'envoi est indisponible. V\u00e9rifiez la configuration SMTP.";

    @FXML
    private TextField emailField;

    @FXML
    private Label feedbackLabel;

    private Stage dialogStage;

    // Backend handler injected by LoginController.
    private Consumer<String> sendResetHandler = email -> {
    };

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setSendResetHandler(Consumer<String> sendResetHandler) {
        if (sendResetHandler != null) {
            this.sendResetHandler = sendResetHandler;
        }
    }

    public void setInitialEmail(String email) {
        if (email != null && !email.isBlank()) {
            emailField.setText(email.trim());
        }
    }

    @FXML
    private void handleSend() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        if (email.isEmpty() || !EMAIL_PATTERN.matcher(email).matches()) {
            showFeedback(ERROR_MESSAGE, true);
            return;
        }

        try {
            sendResetHandler.accept(email);
            showFeedback(SUCCESS_MESSAGE, false);
        } catch (RuntimeException ex) {
            Throwable cause = ex;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            String detail = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
            showFeedback(DELIVERY_ERROR_MESSAGE + " (" + detail + ")", true);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("SMTP Error");
            alert.setHeaderText("Password reset email failed");
            alert.setContentText(detail);
            alert.showAndWait();
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void showFeedback(String message, boolean error) {
        feedbackLabel.setVisible(true);
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("error", "success");
        feedbackLabel.getStyleClass().add(error ? "error" : "success");
    }
}
