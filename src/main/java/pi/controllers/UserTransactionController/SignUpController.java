package pi.controllers.UserTransactionController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pi.mains.Main;

import java.io.IOException;

public class SignUpController {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private TextField balanceField;

    @FXML
    private TextField captchaField;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Hyperlink signInLink;

    private final UserController userController = new UserController();

    @FXML
    public void initialize() {
        roleComboBox.getItems().setAll("ETUDIANT", "SALARY", "ADMIN");
        roleComboBox.setPromptText("Choose a role");
        balanceField.setText("0");
        feedbackLabel.setText("Create your account to access the secure workspace.");
        signInLink.setOnAction(event -> openLoginScene(null));
    }

    @FXML
    private void handleRegister() {
        try {
            validateCaptcha();

            double balance = parseBalance();
            String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
            userController.register(
                    fullNameField.getText(),
                    email,
                    passwordField.getText(),
                    confirmPasswordField.getText(),
                    validateRole(),
                    balance,
                    null,
                    null
            );

            feedbackLabel.getStyleClass().remove("feedback-error");
            if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
                feedbackLabel.getStyleClass().add("feedback-success");
            }
            feedbackLabel.setText("Account created successfully in database.");
            showSuccessAlert(email);
            openLoginScene(email);
        } catch (Exception e) {
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin() {
        openLoginScene(null);
    }

    private void validateCaptcha() {
        String captcha = captchaField.getText() == null ? "" : captchaField.getText().trim();
        if (!"QFDG".equalsIgnoreCase(captcha)) {
            throw new IllegalArgumentException("Captcha invalide. Tape QFDG.");
        }
    }

    private double parseBalance() {
        try {
            return Double.parseDouble(balanceField.getText().trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Initial balance invalide.");
        }
    }

    private String validateRole() {
        String role = roleComboBox.getValue();
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Choisis un role: ETUDIANT, SALARY ou ADMIN.");
        }
        return role;
    }

    private void openLoginScene(String registeredEmail) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/login-view.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            if (registeredEmail != null && !registeredEmail.isBlank()) {
                controller.showRegistrationSuccess(registeredEmail);
            }

            Stage stage = (Stage) signInLink.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/login.css").toExternalForm());
            stage.setTitle("User Secure Login");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page demandee.", e);
        }
    }

    private void showSuccessAlert(String email) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Registration");
        alert.setHeaderText("User inserted into database");
        alert.setContentText("The account " + email + " was created successfully.");
        alert.showAndWait();
    }
}
