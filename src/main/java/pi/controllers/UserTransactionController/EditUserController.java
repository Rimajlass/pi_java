package pi.controllers.UserTransactionController;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

public class EditUserController {

    @FXML
    private Label sidebarNameLabel;
    @FXML
    private Label avatarLetterLabel;
    @FXML
    private Label cardUserNameLabel;
    @FXML
    private Label cardUserIdLabel;
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
    private final DecimalFormat moneyFormat = new DecimalFormat("#0.##", DecimalFormatSymbols.getInstance(Locale.FRANCE));

    private User adminUser;
    private int editingUserId;
    private String pendingImagePath;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("Admin", "Salary", "Étudiant", "Utilisateur"));
    }

    public void setContext(User adminUser, User userToEdit) {
        this.adminUser = adminUser;
        this.editingUserId = userToEdit != null ? userToEdit.getId() : 0;
        this.pendingImagePath = null;

        if (sidebarNameLabel != null && adminUser != null) {
            sidebarNameLabel.setText(valueOrDash(adminUser.getNom()));
        }

        User fresh = userToEdit != null ? userController.show(userToEdit.getId()) : null;
        if (fresh == null) {
            showError("Utilisateur", "Impossible de charger cet utilisateur.");
            navigateToAdminList();
            return;
        }

        cardUserNameLabel.setText(valueOrDash(fresh.getNom()));
        cardUserIdLabel.setText("ID #" + fresh.getId());
        if (avatarLetterLabel != null) {
            String n = fresh.getNom();
            avatarLetterLabel.setText((n == null || n.isBlank()) ? "?" : n.substring(0, 1).toUpperCase(Locale.ROOT));
        }
        nomField.setText(valueOrDash(fresh.getNom()));
        emailField.setText(valueOrDash(fresh.getEmail()));
        passwordField.clear();
        soldeField.setText(moneyFormat.format(fresh.getSoldeTotal()));
        roleCombo.setValue(resolveRoleLabel(fresh));

        String img = fresh.getImage();
        if (img != null && !img.isBlank()) {
            photoPathLabel.setText(new File(img).getName());
            pendingImagePath = img;
        } else {
            photoPathLabel.setText("Aucun fichier choisi");
            pendingImagePath = null;
        }
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
    private void handleDeleteUser() {
        if (adminUser != null && editingUserId == adminUser.getId()) {
            showError("Suppression", "Vous ne pouvez pas supprimer votre propre compte administrateur.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer l'utilisateur");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer définitivement l'utilisateur #" + editingUserId + " ?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            userController.delete(editingUserId);
            navigateToAdminList();
        } catch (Exception e) {
            showError("Suppression", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    @FXML
    private void handleSave() {
        try {
            User existing = userController.show(editingUserId);
            if (existing == null) {
                showError("Enregistrement", "Utilisateur introuvable.");
                return;
            }

            String nom = nomField.getText() != null ? nomField.getText().trim() : "";
            String email = emailField.getText() != null ? emailField.getText().trim() : "";
            String roleLabel = roleCombo.getValue();
            if (roleLabel == null || roleLabel.isBlank()) {
                showError("Enregistrement", "Choisissez un rôle.");
                return;
            }

            double solde;
            try {
                String soldeText = soldeField.getText() != null ? soldeField.getText().trim().replace(',', '.') : "0";
                solde = Double.parseDouble(soldeText);
            } catch (NumberFormatException e) {
                showError("Enregistrement", "Solde total invalide.");
                return;
            }

            existing.setNom(nom);
            existing.setEmail(email);
            existing.setRoles(roleJsonFromLabel(roleLabel));
            existing.setSoldeTotal(solde);
            if (pendingImagePath != null && !pendingImagePath.isBlank()) {
                existing.setImage(pendingImagePath);
            }

            String pwd = passwordField.getText();
            String plain = pwd != null && !pwd.isBlank() ? pwd : null;

            userController.edit(existing, plain);
            navigateToAdminList();
        } catch (Exception e) {
            showError("Enregistrement", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
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

    private static String resolveRoleLabel(User user) {
        if (user.hasRole("ROLE_ADMIN")) {
            return "Admin";
        }
        if (user.hasRole("ROLE_SALARY")) {
            return "Salary";
        }
        if (user.hasRole("ROLE_ETUDIANT")) {
            return "Étudiant";
        }
        return "Utilisateur";
    }

    private static String valueOrDash(String v) {
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
