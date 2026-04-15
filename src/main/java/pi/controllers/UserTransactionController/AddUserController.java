package pi.controllers.UserTransactionController;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import pi.entities.User;
import pi.mains.Main;
import pi.tools.FxmlResources;

import java.io.File;

public class AddUserController {

    @FXML
    private Label sidebarNameLabel;
    @FXML
    private TextField nomField;
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<String> roleCombo;
    @FXML
    private TextField soldeField;
    @FXML
    private Label photoPathLabel;

    private final UserController userController = new UserController();

    private User adminUser;
    private String pendingImagePath;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("Admin", "Salary", "Étudiant", "Utilisateur"));
        roleCombo.setValue("Admin");
    }

    public void setContext(User adminUser) {
        this.adminUser = adminUser;
        if (sidebarNameLabel != null && adminUser != null) {
            sidebarNameLabel.setText(valueOrEmpty(adminUser.getNom()));
        }
        resetForm();
    }

    private void resetForm() {
        pendingImagePath = null;
        nomField.clear();
        emailField.clear();
        passwordField.clear();
        soldeField.setText("0");
        roleCombo.setValue("Admin");
        photoPathLabel.setText("Aucun fichier choisi");
    }

    @FXML
    private void handleChoosePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Photo utilisateur");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        Window w = nomField.getScene().getWindow();
        File file = chooser.showOpenDialog(w);
        if (file != null) {
            pendingImagePath = file.getAbsolutePath();
            photoPathLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleBack() {
        navigateToAdminList();
    }

    @FXML
    private void handleSave() {
        try {
            String nom = nomField.getText() != null ? nomField.getText().trim() : "";
            String email = emailField.getText() != null ? emailField.getText().trim() : "";
            String pwd = passwordField.getText() != null ? passwordField.getText() : "";
            String roleLabel = roleCombo.getValue();

            if (roleLabel == null || roleLabel.isBlank()) {
                showError("Création", "Choisissez un rôle.");
                return;
            }
            if (pwd.isBlank()) {
                showError("Création", "Le mot de passe est obligatoire.");
                return;
            }

            double solde;
            try {
                String soldeText = soldeField.getText() != null ? soldeField.getText().trim().replace(',', '.') : "0";
                solde = Double.parseDouble(soldeText);
            } catch (NumberFormatException e) {
                showError("Création", "Solde total invalide.");
                return;
            }

            User user = new User();
            user.setNom(nom);
            user.setEmail(email);
            user.setRoles(roleJsonFromLabel(roleLabel));
            user.setSoldeTotal(solde);
            if (pendingImagePath != null && !pendingImagePath.isBlank()) {
                user.setImage(pendingImagePath);
            }

            userController.create(user, pwd);
            navigateToAdminList();
        } catch (Exception e) {
            showError("Création", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    @FXML
    private void handleSignOut() {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/login-view.fxml");
            Parent root = (Parent) loader.getRoot();
            Stage stage = (Stage) nomField.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/login.css");
            stage.setUserData(null);
            stage.setTitle("User Secure Login");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation", e.getMessage());
        }
    }

    private void navigateToAdminList() {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/admin-backend-view.fxml");
            Parent root = (Parent) loader.getRoot();
            AdminBackendController c = loader.getController();
            if (adminUser != null) {
                c.setUser(adminUser);
            }
            Stage stage = (Stage) nomField.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/admin-backend.css");
            stage.setTitle("Admin Backend");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation", e.getMessage());
        }
    }

    private static String roleJsonFromLabel(String label) {
        return switch (label) {
            case "Admin" -> "[\"ROLE_ADMIN\"]";
            case "Salary" -> "[\"ROLE_SALARY\"]";
            case "Étudiant" -> "[\"ROLE_ETUDIANT\"]";
            default -> "[\"ROLE_USER\"]";
        };
    }

    private static String valueOrEmpty(String v) {
        if (v == null || v.isBlank()) {
            return "";
        }
        return v;
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
