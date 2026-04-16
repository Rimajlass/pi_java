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
import java.util.regex.Pattern;

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
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-zÀ-ÿ ]{2,60}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private User adminUser;
    private int editingUserId;
    private String pendingImagePath;

    @FXML
    public void initialize() {
        roleCombo.setItems(FXCollections.observableArrayList("Admin", "Salary", "Etudiant", "Utilisateur"));
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
        confirm.setContentText("Supprimer definitivement l'utilisateur #" + editingUserId + " ?");
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
                showError("Enregistrement", "Choisissez un role.");
                return;
            }
            if (!NAME_PATTERN.matcher(nom).matches()) {
                showError("Enregistrement", "Nom invalide (2-60 lettres/espace).");
                return;
            }
            if (!EMAIL_PATTERN.matcher(email).matches() || email.length() > 180) {
                showError("Enregistrement", "Email invalide.");
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
            if (solde < 0 || solde > 10_000_000) {
                showError("Enregistrement", "Solde invalide.");
                return;
            }

            String pwd = passwordField.getText();
            String plain = (pwd != null && !pwd.isBlank()) ? pwd : null;
            if (plain != null && (plain.length() < 8 || !plain.matches(".*[A-Z].*") || !plain.matches(".*[a-z].*") || !plain.matches(".*\\d.*"))) {
                showError("Enregistrement", "Mot de passe faible (min 8, majuscule, minuscule, chiffre).");
                return;
            }

            existing.setNom(nom);
            existing.setEmail(email);
            existing.setRoles(roleJsonFromLabel(roleLabel));
            existing.setSoldeTotal(solde);
            if (pendingImagePath != null && !pendingImagePath.isBlank()) {
                existing.setImage(pendingImagePath);
            }

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
            case "Etudiant" -> "[\"ROLE_ETUDIANT\"]";
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
            return "Etudiant";
        }
        return "Utilisateur";
    }

    private static String valueOrDash(String v) {
        return (v == null || v.isBlank()) ? "" : v;
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}

