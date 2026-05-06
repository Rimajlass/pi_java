package pi.controllers.UserTransactionController;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.DefaultStringConverter;
import pi.entities.User;
import pi.tools.AdminNavigation;
import pi.tools.UiDialog;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class UsersManagementController {

    @FXML private Label headerLabel;
    @FXML private Label headerSubtitle;

    @FXML private Label totalUsersMetric;
    @FXML private Label adminsMetric;
    @FXML private Label activeTodayMetric;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private ComboBox<String> sortFilterCombo;
    @FXML private ComboBox<String> orderFilterCombo;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, User> avatarColumn;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> balanceColumn;
    @FXML private TableColumn<User, User> actionsColumn;

    @FXML private Button addUserButton;
    @FXML private Button myProfileButton;
    @FXML private Button editProfileButton;
    @FXML private Button applyFiltersButton;
    @FXML private Button resetFiltersButton;

    @FXML private VBox searchCard;
    @FXML private VBox usersCard;
    @FXML private Label resultsTextLabel;

    private final UserController userController = new UserController();
    private final ObservableList<User> tableUsers = FXCollections.observableArrayList();
    private final Map<String, Image> avatarImageCache = new HashMap<>();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        loadUsers();
    }

    @FXML
    public void initialize() {
        if (roleFilterCombo != null) {
            roleFilterCombo.getItems().setAll("All roles", "Admin", "Salary", "Student", "Standard user");
            roleFilterCombo.setValue("All roles");
        }
        if (sortFilterCombo != null) {
            sortFilterCombo.getItems().setAll(
                    "Sort by name",
                    "Sort by email",
                    "Sort by role",
                    "Sort by balance",
                    "Sort by registration date",
                    "Sort by ID"
            );
            sortFilterCombo.setValue("Sort by name");
        }
        if (orderFilterCombo != null) {
            orderFilterCombo.getItems().setAll("ASC", "DESC");
            orderFilterCombo.setValue("ASC");
        }

        configureUsersTable();
        playSectionEntrance();
        loadUsers();
    }

    @FXML
    private void handleApplyFilters() {
        loadUsers();
    }

    @FXML
    private void handleResetFilters() {
        if (searchField != null) {
            searchField.clear();
        }
        if (roleFilterCombo != null) {
            roleFilterCombo.setValue("All roles");
        }
        if (sortFilterCombo != null) {
            sortFilterCombo.setValue("Sort by name");
        }
        if (orderFilterCombo != null) {
            orderFilterCombo.setValue("ASC");
        }
        loadUsers();
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
        Stage stage = resolveStage();
        if (stage == null) {
            return;
        }
        AdminNavigation.showUserProfile(stage, currentUser, self);
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
        Stage stage = resolveStage();
        if (stage == null) {
            return;
        }
        AdminNavigation.showUserEdit(stage, currentUser, self);
    }

    @FXML
    private void handleAddUser() {
        Stage stage = resolveStage();
        if (stage == null) {
            return;
        }
        AdminNavigation.showUserCreate(stage, currentUser);
    }

    private void loadUsers() {
        try {
            String search = searchField == null ? "" : safe(searchField.getText()).trim();
            if (search.length() > 80) {
                showError("Filtre", "Le texte de recherche est trop long (max 80)." );
                return;
            }
            String role = mapRole(roleFilterCombo == null ? null : roleFilterCombo.getValue());
            String sortBy = mapSort(sortFilterCombo == null ? null : sortFilterCombo.getValue());
            String order = orderFilterCombo != null && "DESC".equalsIgnoreCase(orderFilterCombo.getValue()) ? "DESC" : "ASC";

            List<User> allUsers = userController.index(search, role, sortBy, order);
            avatarImageCache.clear();
            tableUsers.setAll(allUsers);
            updateMetrics(allUsers);
            updateResultsFooter(allUsers.size());
            if (usersTable != null) {
                usersTable.refresh();
            }
        } catch (Exception e) {
            showError("Erreur de chargement", e.getMessage());
        }
    }

    private void configureUsersTable() {
        if (usersTable == null) {
            return;
        }
        usersTable.setEditable(true);
        usersTable.setItems(tableUsers);

        if (avatarColumn != null) {
            avatarColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
            avatarColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setGraphic(null);
                        return;
                    }
                    Circle circle = new Circle(16, Color.web("#0f6aa6"));
                    Label initial = new Label(initials(user));
                    initial.getStyleClass().add("avatar-initial");
                    VBox box = new VBox();
                    box.getChildren().add(new javafx.scene.layout.StackPane(circle, initial));
                    setGraphic(box.getChildren().get(0));
                }
            });
        }

        if (nameColumn != null) {
            nameColumn.setCellValueFactory(param -> new SimpleStringProperty(safe(param.getValue() == null ? null : param.getValue().getNom())));
            nameColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        }

        if (emailColumn != null) {
            emailColumn.setCellValueFactory(param -> new SimpleStringProperty(safe(param.getValue() == null ? null : param.getValue().getEmail())));
            emailColumn.setCellFactory(TextFieldTableCell.forTableColumn(new DefaultStringConverter()));
        }

        if (roleColumn != null) {
            roleColumn.setCellValueFactory(param -> new SimpleStringProperty(renderRole(param.getValue())));
        }

        if (balanceColumn != null) {
            balanceColumn.setCellValueFactory(param -> new SimpleStringProperty(renderBalance(param.getValue())));
        }

        if (actionsColumn != null) {
            actionsColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
            actionsColumn.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setGraphic(null);
                        return;
                    }
                    Button viewBtn = new Button("View");
                    viewBtn.getStyleClass().add("secondary-button");
                    viewBtn.setOnAction(e -> openUserShow(user));
                    Button editBtn = new Button("Edit");
                    editBtn.getStyleClass().add("header-button");
                    editBtn.setOnAction(e -> openUserEdit(user));
                    HBox wrap = new HBox(8, viewBtn, editBtn);
                    setGraphic(wrap);
                }
            });
        }

        usersTable.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2) {
                return;
            }
            User selected = usersTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openUserShow(selected);
            }
        });
    }

    private void openUserShow(User viewedUser) {
        if (viewedUser == null) {
            return;
        }
        Stage stage = resolveStage();
        if (stage == null) {
            return;
        }
        AdminNavigation.showUserProfile(stage, currentUser, viewedUser);
    }

    private void openUserEdit(User viewedUser) {
        if (viewedUser == null) {
            return;
        }
        Stage stage = resolveStage();
        if (stage == null) {
            return;
        }
        AdminNavigation.showUserEdit(stage, currentUser, viewedUser);
    }

    private void updateMetrics(List<User> allUsers) {
        if (allUsers == null) {
            return;
        }
        int adminsCount = (int) allUsers.stream()
                .filter(user -> user != null && user.hasRole("ROLE_ADMIN"))
                .count();
        int activeToday = (int) allUsers.stream()
                .filter(user -> user != null && user.getDateInscription() != null && user.getDateInscription().isEqual(LocalDate.now()))
                .count();

        if (totalUsersMetric != null) {
            totalUsersMetric.setText(String.valueOf(allUsers.size()));
        }
        if (adminsMetric != null) {
            adminsMetric.setText(String.valueOf(adminsCount));
        }
        if (activeTodayMetric != null) {
            activeTodayMetric.setText(String.valueOf(activeToday));
        }
    }

    private void updateResultsFooter(int count) {
        if (resultsTextLabel != null) {
            resultsTextLabel.setText("Showing " + count + " user(s)");
        }
    }

    private void playSectionEntrance() {
        playCardFade(searchCard, 0);
        playCardFade(usersCard, 90);
    }

    private void playCardFade(Node node, int delayMs) {
        if (node == null) {
            return;
        }
        node.setOpacity(0.0);
        node.setTranslateY(8);
        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setDelay(Duration.millis(delayMs));
        TranslateTransition lift = new TranslateTransition(Duration.millis(260), node);
        lift.setFromY(8);
        lift.setToY(0);
        fade.play();
        lift.play();
    }

    private void showError(String title, String content) {
        Stage stage = resolveStage();
        if (stage != null) {
            UiDialog.error(stage, title, content);
        }
    }

    private Stage resolveStage() {
        if (headerLabel != null && headerLabel.getScene() != null && headerLabel.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String initials(User user) {
        if (user == null) {
            return "";
        }
        String name = safe(user.getNom()).trim();
        if (name.isBlank()) {
            String email = safe(user.getEmail()).trim();
            return email.isBlank() ? "U" : email.substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return name.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String mapRole(String roleSelection) {
        if (roleSelection == null || roleSelection.isBlank() || "All roles".equalsIgnoreCase(roleSelection)) {
            return "";
        }
        return roleSelection;
    }

    private String mapSort(String sortSelection) {
        if (sortSelection == null) {
            return "name";
        }
        String normalized = sortSelection.toLowerCase(Locale.ROOT);
        if (normalized.contains("email")) {
            return "email";
        }
        if (normalized.contains("role")) {
            return "role";
        }
        if (normalized.contains("balance")) {
            return "solde";
        }
        if (normalized.contains("registration")) {
            return "date";
        }
        if (normalized.contains("id")) {
            return "id";
        }
        return "name";
    }

    private String renderRole(User user) {
        if (user == null) {
            return "";
        }
        String role = safe(user.getRoles());
        return role.isBlank() ? "-" : role;
    }

    private String renderBalance(User user) {
        if (user == null) {
            return "";
        }
        return moneyFormat.format(user.getSoldeTotal()) + " TND";
    }
}
