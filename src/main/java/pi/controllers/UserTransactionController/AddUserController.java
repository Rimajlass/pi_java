package pi.controllers.UserTransactionController;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import pi.entities.User;
import pi.mains.Main;
import pi.tools.AdminNavigation;
import pi.tools.FxmlResources;
import pi.tools.UiDialog;

import java.io.File;
import java.util.regex.Pattern;

public class AddUserController {

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
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zÀ-ÿ ]{2,60}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private User adminUser;
    private String pendingImagePath;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("Admin", "Salary", "Etudiant", "Utilisateur"));
        roleCombo.setValue("Admin");
    }

    public void setContext(User adminUser) {
        this.adminUser = adminUser;
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
                showError("Creation", "Choisissez un role.");
                return;
            }
            if (!NAME_PATTERN.matcher(nom).matches()) {
                showError("Creation", "Nom invalide (2-60 lettres/espace).");
                return;
            }
            if (!EMAIL_PATTERN.matcher(email).matches() || email.length() > 180) {
                showError("Creation", "Email invalide.");
                return;
            }
            if (pwd.isBlank()) {
                showError("Creation", "Le mot de passe est obligatoire.");
                return;
            }
            if (pwd.length() < 8 || !pwd.matches(".*[A-Z].*") || !pwd.matches(".*[a-z].*") || !pwd.matches(".*\\d.*")) {
                showError("Creation", "Mot de passe faible (min 8, majuscule, minuscule, chiffre).");
                return;
            }

            double solde;
            try {
                String soldeText = soldeField.getText() != null ? soldeField.getText().trim().replace(',', '.') : "0";
                solde = Double.parseDouble(soldeText);
            } catch (NumberFormatException e) {
                showError("Creation", "Solde total invalide.");
                return;
            }
            if (solde < 0 || solde > 10_000_000) {
                showError("Creation", "Solde invalide.");
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
            try {
                showSuccess("Utilisateur", "Utilisateur ajoute avec succes.");
            } catch (Exception popupError) {
                System.err.println("[AddUser] Success popup failed: " + popupError.getMessage());
            }
            navigateToAdminList();
        } catch (Exception e) {
            showError("Creation", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
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
        Stage stage = (Stage) nomField.getScene().getWindow();
        AdminNavigation.showUsersManagement(stage, adminUser);
    }

    private static String roleJsonFromLabel(String label) {
        return switch (label) {
            case "Admin" -> "[\"ROLE_ADMIN\"]";
            case "Salary" -> "[\"ROLE_SALARY\"]";
            case "Etudiant" -> "[\"ROLE_ETUDIANT\"]";
            default -> "[\"ROLE_USER\"]";
        };
    }

    private void showError(String title, String message) {
        if (nomField != null && nomField.getScene() != null && nomField.getScene().getWindow() instanceof Stage stage) {
            UiDialog.error(stage, title, message);
        }
    }

    private void showSuccess(String title, String message) {
        if (nomField != null && nomField.getScene() != null && nomField.getScene().getWindow() instanceof Stage stage) {
            UiDialog.success(stage, title, message);
        }
    }
}

