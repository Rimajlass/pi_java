package pi.controllers.UserTransactionController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pi.entities.User;
import pi.mains.Main;
import pi.tools.AdminNavigation;
import pi.tools.FxmlResources;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AdminBackendController {

    @FXML
    private Label headerLabel;

    @FXML
    private Label userLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label totalUsersMetric;

    @FXML
    private Label adminsMetric;

    @FXML
    private Label activeTodayMetric;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> roleFilterCombo;

    @FXML
    private ComboBox<String> sortFilterCombo;

    @FXML
    private ComboBox<String> orderFilterCombo;

    @FXML
    private VBox adminsRowsBox;

    @FXML
    private VBox usersRowsBox;

    private final UserController userController = new UserController();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");
    private User currentUser;

    @FXML
    public void initialize() {
        roleFilterCombo.getItems().setAll("All roles", "Admin", "Salary", "Student", "Standard user");
        sortFilterCombo.getItems().setAll("Sort by name", "Sort by email", "Sort by balance", "Sort by registration date", "Sort by ID");
        orderFilterCombo.getItems().setAll("ASC", "DESC");

        roleFilterCombo.setValue("All roles");
        sortFilterCombo.setValue("Sort by name");
        orderFilterCombo.setValue("ASC");

        loadUsers();
    }

    public void setUser(User user) {
        if (user == null) {
            return;
        }
        this.currentUser = user;
        headerLabel.setText("Users");
        userLabel.setText(valueOrDash(user.getNom()));
        roleLabel.setText("Admin Panel");
        loadUsers();
    }

    @FXML
    private void handleNavTransactions() {
        if (currentUser == null) {
            return;
        }
        try {
            AdminNavigation.showTransactionsManagement((Stage) headerLabel.getScene().getWindow(), currentUser);
        } catch (Exception e) {
            showError("Navigation", e.getMessage() != null ? e.getMessage() : String.valueOf(e));
        }
    }

    @FXML
    private void handleMyProfile() {
        if (currentUser == null) {
            return;
        }
        User self = userController.show(currentUser.getId());
        if (self == null) {
            showError("Profil", "Impossible de charger votre profil.");
            return;
        }
        openUserShow(self);
    }

    @FXML
    private void handleEditMyProfile() {
        if (currentUser == null) {
            return;
        }
        User self = userController.show(currentUser.getId());
        if (self == null) {
            showError("Profil", "Impossible de charger votre profil.");
            return;
        }
        openUserEdit(self);
    }

    @FXML
    private void handleAddUser() {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/add-user-view.fxml");
            Parent root = (Parent) loader.getRoot();
            AddUserController controller = loader.getController();

            Stage stage = (Stage) headerLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/user-show.css");
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/edit-user.css");

            stage.setTitle("Create User");
            stage.setScene(scene);
            stage.show();

            controller.setContext(currentUser);
        } catch (Exception e) {
            showError("Navigation error", "Unable to open create user form:\n" + chainMessages(e));
        }
    }

    @FXML
    private void handleApplyFilters() {
        loadUsers();
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        roleFilterCombo.setValue("All roles");
        sortFilterCombo.setValue("Sort by name");
        orderFilterCombo.setValue("ASC");
        loadUsers();
    }

    private void loadUsers() {
        try {
            String search = clean(searchField.getText());
            String role = mapRole(roleFilterCombo.getValue());
            String sortBy = mapSort(sortFilterCombo.getValue());
            String order = "DESC".equalsIgnoreCase(orderFilterCombo.getValue()) ? "DESC" : "ASC";

            List<User> allUsers = userController.index(search, role, sortBy, order);
            List<User> admins = new ArrayList<>();
            List<User> regularUsers = new ArrayList<>();

            for (User user : allUsers) {
                boolean isCurrent = currentUser != null && currentUser.getId() == user.getId();
                if (user.hasRole("ROLE_ADMIN") || isCurrent) {
                    admins.add(user);
                } else {
                    regularUsers.add(user);
                }
            }

            renderRows(adminsRowsBox, admins, true);
            renderRows(usersRowsBox, regularUsers, false);
            updateMetrics(allUsers, admins);
        } catch (Exception e) {
            showError("Erreur de chargement", e.getMessage());
        }
    }

    private void updateMetrics(List<User> allUsers, List<User> admins) {
        int activeToday = (int) allUsers.stream()
                .filter(user -> user.getDateInscription() != null && user.getDateInscription().isEqual(LocalDate.now()))
                .count();

        totalUsersMetric.setText(String.valueOf(allUsers.size()));
        adminsMetric.setText(String.valueOf(admins.size()));
        activeTodayMetric.setText(String.valueOf(activeToday));
    }

    private void renderRows(VBox container, List<User> users, boolean adminSection) {
        container.getChildren().clear();

        if (users.isEmpty()) {
            HBox emptyRow = new HBox();
            emptyRow.getStyleClass().add("table-row");
            Label emptyText = new Label(adminSection ? "No administrator found." : "No user found.");
            emptyText.getStyleClass().add("cell-text");
            emptyRow.getChildren().add(emptyText);
            container.getChildren().add(emptyRow);
            return;
        }

        for (User user : users) {
            container.getChildren().add(createUserRow(user, adminSection));
        }
    }

    private HBox createUserRow(User user, boolean adminSection) {
        HBox row = new HBox();
        row.getStyleClass().add("table-row");

        HBox nameCell = new HBox(8);
        nameCell.setAlignment(Pos.CENTER_LEFT);
        nameCell.getStyleClass().add("col-name");

        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("user-avatar");
        Label avatarText = new Label(getInitial(user.getNom()));
        avatarText.getStyleClass().add("user-avatar-text");
        avatar.getChildren().add(avatarText);

        Label nameLabel = new Label(valueOrDash(user.getNom()));
        nameLabel.getStyleClass().add("cell-text");
        nameCell.getChildren().addAll(avatar, nameLabel);

        Label emailLabel = new Label(valueOrDash(user.getEmail()));
        emailLabel.getStyleClass().addAll("col-email", "cell-text");

        StackPane roleCell = new StackPane();
        roleCell.getStyleClass().add("col-role");
        Label roleTag = new Label(resolveRoleLabel(user, adminSection));
        roleTag.getStyleClass().addAll("tag", resolveRoleStyle(user, adminSection));
        roleCell.getChildren().add(roleTag);

        Label balanceLabel = new Label(moneyFormat.format(user.getSoldeTotal()) + " TND");
        balanceLabel.getStyleClass().addAll("col-balance", "cell-text");

        HBox actionsCell = new HBox(6);
        actionsCell.setAlignment(Pos.CENTER_LEFT);
        actionsCell.getStyleClass().add("col-actions");

        Button showButton = new Button("Show");
        showButton.getStyleClass().add("action-outline");
        showButton.setOnAction(event -> openUserShow(user));

        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("action-solid");
        editButton.setOnAction(event -> openUserEdit(user));

        actionsCell.getChildren().addAll(showButton, editButton);
        row.getChildren().addAll(nameCell, emailLabel, roleCell, balanceLabel, actionsCell);

        return row;
    }

    private void openUserEdit(User user) {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/edit-user-view.fxml");
            Parent root = (Parent) loader.getRoot();

            EditUserController controller = loader.getController();

            Stage stage = (Stage) headerLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/user-show.css");
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/edit-user.css");

            stage.setTitle("Edit utilisateur");
            stage.setScene(scene);
            stage.show();

            controller.setContext(currentUser, user);
        } catch (Exception e) {
            showError("Navigation error", "Unable to open edit form:\n" + chainMessages(e));
        }
    }

    private void openUserShow(User user) {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/user-show-view.fxml");
            Parent root = (Parent) loader.getRoot();

            UserShowController controller = loader.getController();

            Stage stage = (Stage) headerLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/user-show.css");

            stage.setTitle("User Profile");
            stage.setScene(scene);
            stage.show();

            controller.setContext(currentUser, user);
        } catch (Exception e) {
            showError("Navigation error", "Unable to open user details:\n" + chainMessages(e));
        }
    }

    private static String chainMessages(Throwable e) {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (Throwable x = e; x != null && depth < 8; x = x.getCause(), depth++) {
            if (depth > 0) {
                sb.append("\n");
            }
            sb.append(x.getClass().getSimpleName()).append(": ").append(String.valueOf(x.getMessage()));
        }
        return sb.toString();
    }

    private void showUserSummary(User user) {
        String message = "ID: " + user.getId() +
                "\nName: " + valueOrDash(user.getNom()) +
                "\nEmail: " + valueOrDash(user.getEmail()) +
                "\nRoles: " + valueOrDash(user.getRoles()) +
                "\nBalance: " + moneyFormat.format(user.getSoldeTotal()) + " TND";
        showInfo("User details", message);
    }

    private String mapRole(String displayRole) {
        if (displayRole == null) {
            return "";
        }
        return switch (displayRole) {
            case "Admin" -> "ROLE_ADMIN";
            case "Salary" -> "ROLE_SALARY";
            case "Student" -> "ROLE_ETUDIANT";
            case "Standard user" -> "ROLE_USER_ONLY";
            default -> "";
        };
    }

    private String mapSort(String displaySort) {
        if (displaySort == null) {
            return "nom";
        }
        return switch (displaySort) {
            case "Sort by email" -> "email";
            case "Sort by balance" -> "solde";
            case "Sort by registration date" -> "date";
            case "Sort by ID" -> "id";
            default -> "nom";
        };
    }

    private String resolveRoleLabel(User user, boolean adminSection) {
        if (adminSection || user.hasRole("ROLE_ADMIN")) {
            return "Admin";
        }
        if (user.hasRole("ROLE_SALARY")) {
            return "Salary";
        }
        if (user.hasRole("ROLE_ETUDIANT")) {
            return "Student";
        }
        return "User";
    }

    private String resolveRoleStyle(User user, boolean adminSection) {
        if (adminSection || user.hasRole("ROLE_ADMIN")) {
            return "tag-admin";
        }
        if (user.hasRole("ROLE_SALARY")) {
            return "tag-salary";
        }
        if (user.hasRole("ROLE_ETUDIANT")) {
            return "tag-student";
        }
        return "tag-user";
    }

    private String valueOrDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private String getInitial(String name) {
        String safeName = valueOrDash(name);
        if ("-".equals(safeName)) {
            return "U";
        }
        return safeName.substring(0, 1).toUpperCase();
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/login-view.fxml");
            Parent root = (Parent) loader.getRoot();

            Stage stage = (Stage) headerLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/login.css");

            stage.setUserData(null);
            stage.setTitle("User Secure Login");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de revenir vers la page login.", e);
        }
    }
}
