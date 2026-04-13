package pi.controllers.UserTransactionController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pi.entities.User;
import pi.mains.Main;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label statusPill;

    @FXML
    private CheckBox rememberMeCheckBox;

    @FXML
    private Hyperlink biometricLink;

    private final UserController userController = new UserController();

    @FXML
    public void initialize() {
        emailField.setText("rima.jlassi@esprit.tn");
        passwordField.setText("");
        feedbackLabel.setText("Enter your credentials to access your workspace.");
        welcomeLabel.setText("Welcome to your secure space");
        statusPill.setText("Account access");
        biometricLink.setOnAction(event ->
                feedbackLabel.setText("Biometric login can be linked after the classic login flow."));
    }

    public void showRegistrationSuccess(String email) {
        feedbackLabel.getStyleClass().remove("feedback-error");
        if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
            feedbackLabel.getStyleClass().add("feedback-success");
        }
        feedbackLabel.setText("Account created in database for " + email + ". You can sign in now.");
        emailField.setText(email);
        passwordField.clear();
    }

    @FXML
    private void handleSignIn() {
        try {
            User user = userController.login(emailField.getText(), passwordField.getText());

            if (user == null) {
                feedbackLabel.setText("Invalid credentials.");
                feedbackLabel.getStyleClass().remove("feedback-success");
                if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                    feedbackLabel.getStyleClass().add("feedback-error");
                }
                return;
            }

            feedbackLabel.getStyleClass().remove("feedback-error");
            if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
                feedbackLabel.getStyleClass().add("feedback-success");
            }

            openSalaryHome(user);
        } catch (Exception e) {
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText(e.getMessage());
        }
    }

    @FXML
    private void handleGoogle() {
        feedbackLabel.getStyleClass().remove("feedback-success");
        if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
            feedbackLabel.getStyleClass().add("feedback-error");
        }
        feedbackLabel.setText("Google sign-in UI is not connected yet in the Java version.");
    }

    @FXML
    private void handleFacebook() {
        feedbackLabel.getStyleClass().remove("feedback-success");
        if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
            feedbackLabel.getStyleClass().add("feedback-error");
        }
        feedbackLabel.setText("Facebook sign-in UI is not connected yet in the Java version.");
    }

    @FXML
    private void handleCreateAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/sign-up-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) feedbackLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/sign-up.css").toExternalForm());

            stage.setTitle("User Registration");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText("Impossible d'ouvrir la page d'inscription.");
        }
    }

    private void openSalaryHome(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/salary-home-view.fxml"));
        Parent root = loader.load();
        SalaryHomeController controller = loader.getController();
        controller.setUser(user);

        Stage stage = (Stage) feedbackLabel.getScene().getWindow();
        stage.setUserData(user);
        Scene scene = new Scene(root, 1460, 780);
        scene.getStylesheets().add(Main.class.getResource("/pi/styles/salary-home.css").toExternalForm());

        stage.setTitle("Salary Home");
        stage.setScene(scene);
        stage.show();
    }
}
