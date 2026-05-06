package pi.controllers.UserTransactionController;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.converter.DefaultStringConverter;
import pi.controllers.CoursQuizController.AdminCoursesQuizBackOfficeFactory;
import pi.controllers.ExpenseRevenueController.BACK.AdminRevenueExpenseBackOfficeFactory;
import pi.controllers.ImprevusCasreelController.AdminUnexpectedCasesBackOfficeFactory;
import pi.controllers.InvestissementController.AdminController;
import pi.entities.User;
import pi.mains.Main;
import pi.savings.ui.AdminSavingsBackOfficeFactory;
import pi.tools.AdminNavigation;
import pi.tools.FxmlResources;
import pi.tools.ThemeManager;
import pi.tools.UiDialog;

import java.io.IOException;
import java.text.DecimalFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javafx.util.Duration;

public class AdminBackendController {

    @FXML
    private Label headerLabel;

    @FXML
    private Label userLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label headerSubtitle;

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
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, User> avatarColumn;

    @FXML
    private TableColumn<User, String> nameColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, String> balanceColumn;

    @FXML
    private TableColumn<User, User> actionsColumn;

    @FXML
    private VBox menuList;

    @FXML
    private StackPane workspaceHost;

    @FXML
    private VBox usersWorkspace;

    @FXML
    private Button addUserButton;

    @FXML
    private Button myProfileButton;

    @FXML
    private Button editProfileButton;

    @FXML
    private Button applyFiltersButton;

    @FXML
    private Button resetFiltersButton;

    @FXML
    private VBox searchCard;

    @FXML
    private VBox usersCard;

    @FXML
    private Label resultsTextLabel;

    private final UserController userController = new UserController();
    private final ObservableList<User> tableUsers = FXCollections.observableArrayList();
    private final Map<String, Image> avatarImageCache = new HashMap<>();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private User currentUser;
    private String defaultHeaderSubtitle;
    private Parent savingsWorkspace;
    private Parent goalsWorkspace;
    private Parent courseQuizWorkspace;
    private Parent revenueWorkspace;
    private Parent expenseWorkspace;
    private Parent unexpectedWorkspace;
    private Parent realCasesWorkspace;
    private Parent transactionsWorkspace;

    @FXML
    public void initialize() {
        roleFilterCombo.getItems().setAll("All roles", "Admin", "Salary", "Student", "Standard user");
        sortFilterCombo.getItems().setAll("Sort by name", "Sort by email", "Sort by role", "Sort by balance", "Sort by registration date", "Sort by ID");
        orderFilterCombo.getItems().setAll("ASC", "DESC");

        roleFilterCombo.setValue("All roles");
        sortFilterCombo.setValue("Sort by name");
        orderFilterCombo.setValue("ASC");

        defaultHeaderSubtitle = headerSubtitle != null ? headerSubtitle.getText() : "";
        configureUsersTable();
        installButtonEffects();
        playSectionEntrance();
        loadUsers();
    }

    public void setUser(User user) {
        if (user == null) {
            return;
        }
        this.currentUser = user;
        headerLabel.setText("Users");
        userLabel.setText(valueOrDash(user.getNom()));
        roleLabel.setText("Online");
        loadUsers();
    }

    @FXML
    private void handleNavTransactions() {
        showTransactionsWorkspace();
    }

    @FXML
    private void handleSidebarSelection(MouseEvent event) {
        HBox selectedRow = resolveMenuRow(event);
        if (selectedRow == null || menuList == null) {
            return;
        }

        menuList.getChildren().stream()
                .filter(HBox.class::isInstance)
                .map(HBox.class::cast)
                .forEach(row -> row.getStyleClass().remove("menu-row-active"));

        if (!selectedRow.getStyleClass().contains("menu-row-active")) {
            selectedRow.getStyleClass().add("menu-row-active");
        }

        String menuKey = extractMenuKey(selectedRow);
        if (!menuKey.isEmpty()) {
            routeMenuSelection(menuKey);
        }
    }

    @FXML
    private void handleSavingsSidebarClick(MouseEvent event) {
        if (event != null) {
            event.consume();
        }
        activateSidebarRow(event);
        showSavingsWorkspace();
    }

    @FXML
    private void handleGoalsSidebarClick(MouseEvent event) {
        if (event != null) {
            event.consume();
        }
        activateSidebarRow(event);
        showGoalsWorkspace();
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
        if (currentUser == null || headerLabel == null || headerLabel.getScene() == null) {
            return;
        }
        Stage stage = (Stage) headerLabel.getScene().getWindow();
        AdminNavigation.showUserCreate(stage, currentUser);
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

    private void configureUsersTable() {
        if (usersTable == null) {
            return;
        }

        usersTable.setEditable(true);
        usersTable.setItems(tableUsers);

        avatarColumn.setEditable(false);
        avatarColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        avatarColumn.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            private final Label fallbackLabel = new Label();
            private final StackPane avatarShell = new StackPane();

            {
                imageView.setFitWidth(34);
                imageView.setFitHeight(34);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.setClip(new Circle(17, 17, 17));

                fallbackLabel.getStyleClass().add("avatar-fallback-text");
                avatarShell.getStyleClass().add("avatar-shell-cell");
                avatarShell.getChildren().addAll(imageView, fallbackLabel);

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (!getStyleClass().contains("avatar-table-cell")) {
                    getStyleClass().add("avatar-table-cell");
                }
                if (empty || user == null) {
                    setGraphic(null);
                    return;
                }

                Image avatarImage = loadUserAvatarImage(user.getImage());
                if (avatarImage != null) {
                    imageView.setImage(avatarImage);
                    imageView.setVisible(true);
                    fallbackLabel.setVisible(false);
                } else {
                    imageView.setImage(null);
                    imageView.setVisible(false);
                    fallbackLabel.setText(getInitial(user.getNom()));
                    fallbackLabel.setVisible(true);
                }
                setGraphic(avatarShell);
            }
        });

        nameColumn.setEditable(true);
        nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(valueOrDash(cell.getValue().getNom())));
        nameColumn.setCellFactory(col -> createEditableTextCell());
        nameColumn.setOnEditCommit(this::handleNameEditCommit);

        emailColumn.setEditable(true);
        emailColumn.setCellValueFactory(cell -> new SimpleStringProperty(valueOrDash(cell.getValue().getEmail())));
        emailColumn.setCellFactory(col -> createEditableTextCell());
        emailColumn.setOnEditCommit(this::handleEmailEditCommit);

        roleColumn.setEditable(false);
        roleColumn.setCellValueFactory(cell -> new SimpleStringProperty(resolveRoleLabel(cell.getValue())));
        roleColumn.setCellFactory(col -> new TableCell<>() {
            private final Label roleTag = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                User rowUser = getTableRow() != null ? getTableRow().getItem() : null;
                roleTag.setText(item);
                roleTag.getStyleClass().setAll("tag", rowUser == null ? "tag-user" : resolveRoleStyle(rowUser));
                setGraphic(roleTag);
            }
        });

        balanceColumn.setEditable(false);
        balanceColumn.setCellValueFactory(cell -> new SimpleStringProperty(moneyFormat.format(cell.getValue().getSoldeTotal()) + " TND"));
        balanceColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("col-balance");
                    return;
                }
                setText(item);
                if (!getStyleClass().contains("col-balance")) {
                    getStyleClass().add("col-balance");
                }
            }
        });

        actionsColumn.setEditable(false);
        actionsColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button showButton = new Button("Show");
            private final Button editButton = new Button("Edit");
            private final HBox box = new HBox(6, showButton, editButton);

            {
                showButton.getStyleClass().add("action-outline");
                editButton.getStyleClass().add("action-solid");
                installRowButtonEffect(showButton, false);
                installRowButtonEffect(editButton, true);
                showButton.setOnAction(event -> {
                    User user = getItem();
                    if (user != null) {
                        openUserShow(user);
                    }
                });
                editButton.setOnAction(event -> {
                    User user = getItem();
                    if (user != null) {
                        openUserEdit(user);
                    }
                });
            }

            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : box);
            }
        });
    }

    private TableCell<User, String> createEditableTextCell() {
        TextFieldTableCell<User, String> cell = new TextFieldTableCell<>(new DefaultStringConverter());
        cell.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY || event.getClickCount() != 2 || cell.isEmpty()) {
                return;
            }
            TableView<User> table = cell.getTableView();
            TableColumn<User, String> column = cell.getTableColumn();
            if (table != null && column != null) {
                table.getSelectionModel().clearAndSelect(cell.getIndex(), column);
                table.edit(cell.getIndex(), column);
                event.consume();
            }
        });
        return cell;
    }

    private void handleNameEditCommit(TableColumn.CellEditEvent<User, String> event) {
        User user = event.getRowValue();
        if (user == null) {
            return;
        }

        String previousValue = valueOrEmpty(user.getNom());
        String newValue = clean(event.getNewValue());

        if (newValue.equals(previousValue)) {
            return;
        }
        if (newValue.length() < 2) {
            showError("Validation", "Le nom doit contenir au moins 2 caractères.");
            usersTable.refresh();
            return;
        }

        user.setNom(newValue);
        persistInlineUserUpdate(
                user,
                () -> user.setNom(previousValue),
                "Nom mis à jour."
        );
    }

    private void handleEmailEditCommit(TableColumn.CellEditEvent<User, String> event) {
        User user = event.getRowValue();
        if (user == null) {
            return;
        }

        String previousValue = valueOrEmpty(user.getEmail());
        String newValue = clean(event.getNewValue()).toLowerCase(Locale.ROOT);

        if (newValue.equals(previousValue)) {
            return;
        }
        if (!isValidEmail(newValue)) {
            showError("Email invalide", "Veuillez saisir une adresse email valide avant la sauvegarde.");
            usersTable.refresh();
            return;
        }

        user.setEmail(newValue);
        persistInlineUserUpdate(
                user,
                () -> user.setEmail(previousValue),
                "Email mis à jour."
        );
    }

    private void persistInlineUserUpdate(User user, Runnable rollbackAction, String successMessage) {
        try {
            userController.edit(user, null);
            usersTable.refresh();
            showInlineSuccess(successMessage);
        } catch (Exception e) {
            rollbackAction.run();
            usersTable.refresh();
            showError("Mise à jour", e.getMessage());
        }
    }

    private void showInlineSuccess(String message) {
        if (headerSubtitle == null) {
            return;
        }
        headerSubtitle.setText(message);
        PauseTransition resetMessageTimer = new PauseTransition(Duration.seconds(2.4));
        resetMessageTimer.setOnFinished(event -> headerSubtitle.setText(defaultHeaderSubtitle));
        resetMessageTimer.play();
    }

    private void loadUsers() {
        try {
            String search = clean(searchField.getText());
            if (search.length() > 80) {
                showError("Filtre", "Le texte de recherche est trop long (max 80).");
                return;
            }
            String role = mapRole(roleFilterCombo.getValue());
            String sortBy = mapSort(sortFilterCombo.getValue());
            String order = "DESC".equalsIgnoreCase(orderFilterCombo.getValue()) ? "DESC" : "ASC";

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

    private void showUsersWorkspace() {
        headerLabel.setText("Users");
        headerSubtitle.setText("Manage admin and user accounts from one workspace.");
        defaultHeaderSubtitle = headerSubtitle.getText();
        if (addUserButton != null) {
            addUserButton.setManaged(true);
            addUserButton.setVisible(true);
        }
        if (workspaceHost != null && usersWorkspace != null) {
            workspaceHost.getChildren().setAll(usersWorkspace);
        }
        loadUsers();
    }

    private void showSavingsWorkspace() {
        try {
            headerLabel.setText("Savings");
            headerSubtitle.setText("Search, sort and monitor savings transactions while keeping the admin sidebar visible.");
            if (addUserButton != null) {
                addUserButton.setManaged(false);
                addUserButton.setVisible(false);
            }
            if (savingsWorkspace == null) {
                savingsWorkspace = AdminSavingsBackOfficeFactory.buildSavingsWorkspace();
            }
            replaceWorkspace(savingsWorkspace);
        } catch (RuntimeException e) {
            showError("Savings navigation", chainMessages(e));
        }
    }

    private void showCourseQuizWorkspace() {
        headerLabel.setText("Course & Quiz");
        headerSubtitle.setText("Manage courses and quizzes in the shared admin workspace while keeping the same sidebar visible.");
        if (addUserButton != null) {
            addUserButton.setManaged(false);
            addUserButton.setVisible(false);
        }
        if (courseQuizWorkspace == null) {
            courseQuizWorkspace = AdminCoursesQuizBackOfficeFactory.buildWorkspace();
        }
        replaceWorkspace(courseQuizWorkspace);
    }

    private void showRevenueWorkspace() {
        headerLabel.setText("Revenues");
        headerSubtitle.setText("Manage revenues in the shared admin workspace while keeping the blue sidebar visible.");
        if (addUserButton != null) {
            addUserButton.setManaged(false);
            addUserButton.setVisible(false);
        }
        if (revenueWorkspace == null) {
            revenueWorkspace = AdminRevenueExpenseBackOfficeFactory.buildRevenueWorkspace();
        }
        replaceWorkspace(revenueWorkspace);
    }

    private void showExpenseWorkspace() {
        headerLabel.setText("Expenses");
        headerSubtitle.setText("Manage expenses in the shared admin workspace while keeping the blue sidebar visible.");
        if (addUserButton != null) {
            addUserButton.setManaged(false);
            addUserButton.setVisible(false);
        }
        if (expenseWorkspace == null) {
            expenseWorkspace = AdminRevenueExpenseBackOfficeFactory.buildExpenseWorkspace();
        }
        replaceWorkspace(expenseWorkspace);
    }

    private void showUnexpectedWorkspace() {
        headerLabel.setText("Unexpected Events");
        headerSubtitle.setText("Manage unexpected events while keeping the same admin sidebar visible.");
        if (addUserButton != null) {
            addUserButton.setManaged(false);
            addUserButton.setVisible(false);
        }
        if (unexpectedWorkspace == null) {
            unexpectedWorkspace = AdminUnexpectedCasesBackOfficeFactory.buildWorkspace();
        }
        replaceWorkspace(unexpectedWorkspace);
    }

    private void showRealCasesWorkspace() {
        headerLabel.setText("Real Cases");
        headerSubtitle.setText("Manage real cases while keeping the same admin sidebar visible.");
        if (addUserButton != null) {
            addUserButton.setManaged(false);
            addUserButton.setVisible(false);
        }
        if (realCasesWorkspace == null) {
            realCasesWorkspace = AdminUnexpectedCasesBackOfficeFactory.buildWorkspace();
        }
        replaceWorkspace(realCasesWorkspace);
    }

    private void showGoalsWorkspace() {
        try {
            headerLabel.setText("Goals");
            headerSubtitle.setText("Search, sort and monitor financial goals while navigating from the same admin sidebar.");
            if (addUserButton != null) {
                addUserButton.setManaged(false);
                addUserButton.setVisible(false);
            }
            if (goalsWorkspace == null) {
                goalsWorkspace = AdminSavingsBackOfficeFactory.buildGoalsWorkspace();
            }
            replaceWorkspace(goalsWorkspace);
        } catch (RuntimeException e) {
            showError("Goals navigation", chainMessages(e));
        }
    }

    private void showInvestmentsWorkspace() {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/Invest/admin.fxml");
            Parent root = (Parent) loader.getRoot();
            Object raw = loader.getController();
            if (raw instanceof AdminController controller && currentUser != null) {
                controller.setUser(currentUser);
            }
            Stage stage = (Stage) headerLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            stage.setTitle("Investments | Decide$");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation", chainMessages(e));
        }
    }

    private void routeMenuSelection(String menuKey) {
        String normalized = normalizeMenuKey(menuKey);
        switch (normalized) {
            case "users" -> showUsersWorkspace();
            case "transactions" -> showTransactionsWorkspace();
            case "course & quiz" -> showCourseQuizWorkspace();
            case "unexpected events" -> showUnexpectedWorkspace();
            case "real cases" -> showRealCasesWorkspace();
            case "revenues" -> showRevenueWorkspace();
            case "expenses" -> showExpenseWorkspace();
            case "savings" -> showSavingsWorkspace();
            case "goals" -> showGoalsWorkspace();
            case "investments" -> showInvestmentsWorkspace();
            case "reports", "objectives", "reclamations", "statistics", "ai quiz generator" -> showPlaceholderWorkspace(menuKey);
            default -> showPlaceholderWorkspace(menuKey);
        }
    }

    private HBox resolveMenuRow(MouseEvent event) {
        if (event == null) {
            return null;
        }
        Object source = event.getSource();
        if (source instanceof HBox row && row.getStyleClass().contains("menu-row")) {
            return row;
        }
        if (!(event.getTarget() instanceof Node node)) {
            return null;
        }
        Node current = node;
        while (current != null) {
            if (current instanceof HBox row && row.getStyleClass().contains("menu-row")) {
                return row;
            }
            current = current.getParent();
        }
        return null;
    }

    private void activateSidebarRow(MouseEvent event) {
        HBox selectedRow = resolveMenuRow(event);
        if (selectedRow == null || menuList == null) {
            return;
        }
        menuList.getChildren().stream()
                .filter(HBox.class::isInstance)
                .map(HBox.class::cast)
                .forEach(row -> row.getStyleClass().remove("menu-row-active"));
        if (!selectedRow.getStyleClass().contains("menu-row-active")) {
            selectedRow.getStyleClass().add("menu-row-active");
        }
    }

    private String extractMenuKey(HBox row) {
        if (row == null) {
            return "";
        }
        for (Node child : row.getChildren()) {
            if (child instanceof Label label) {
                return label.getText() == null ? "" : label.getText();
            }
        }
        return "";
    }

    private String normalizeMenuKey(String key) {
        if (key == null) {
            return "";
        }
        return key
                .replace('\u00A0', ' ')
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private void showTransactionsWorkspace() {
        if (headerLabel != null && headerLabel.getScene() != null && headerLabel.getScene().getWindow() instanceof Stage stage && currentUser != null) {
            AdminNavigation.showTransactionsManagement(stage, currentUser);
            return;
        }
        loadContent(
                "/pi/mains/transactions-management-view.fxml",
                "Transactions",
                "Track, filter and create transactions in a structured workspace.",
                false,
                TransactionsManagementController.class,
                controller -> {
                    if (currentUser != null) {
                        controller.setContext(currentUser);
                    }
                }
        );
        ensureTransactionsStylesheet();
    }

    private void showPlaceholderWorkspace(String key) {
        headerLabel.setText(key);
        headerSubtitle.setText("This module is not connected yet. The admin sidebar remains available for navigation.");
        if (addUserButton != null) {
            addUserButton.setManaged(false);
            addUserButton.setVisible(false);
        }

        VBox placeholder = new VBox(14);
        placeholder.getStyleClass().add("card");
        Label title = new Label(key + " module");
        title.getStyleClass().add("card-title");
        Label subtitle = new Label("This area is reserved for the future " + key + " back-office workspace.");
        subtitle.getStyleClass().add("admin-subtitle");
        subtitle.setWrapText(true);
        placeholder.getChildren().addAll(title, subtitle);
        replaceWorkspace(placeholder);
    }

    private void replaceWorkspace(Node content) {
        if (workspaceHost != null && content != null) {
            workspaceHost.getChildren().setAll(content);
        }
    }

    private <T> void loadContent(
            String fxmlPath,
            String title,
            String subtitle,
            boolean showAddUser,
            Class<T> controllerClass,
            Consumer<T> controllerInitializer
    ) {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, fxmlPath);
            Parent root = (Parent) loader.getRoot();
            Object rawController = loader.getController();
            if (controllerClass != null && !controllerClass.isInstance(rawController)) {
                throw new IllegalStateException("Unexpected controller for " + fxmlPath);
            }
            if (controllerClass != null && controllerInitializer != null) {
                controllerInitializer.accept(controllerClass.cast(rawController));
            }
            headerLabel.setText(title);
            headerSubtitle.setText(subtitle);
            if (addUserButton != null) {
                addUserButton.setManaged(showAddUser);
                addUserButton.setVisible(showAddUser);
            }
            replaceWorkspace(root);
        } catch (Exception e) {
            showError("Navigation", chainMessages(e));
        }
    }

    private void ensureTransactionsStylesheet() {
        if (headerLabel == null || headerLabel.getScene() == null) {
            return;
        }
        Scene scene = headerLabel.getScene();
        String txCss = Main.class.getResource("/pi/styles/transactions-management.css").toExternalForm();
        if (!scene.getStylesheets().contains(txCss)) {
            scene.getStylesheets().add(txCss);
        }
        ThemeManager.registerScene(scene);
    }

    private void updateMetrics(List<User> allUsers) {
        int adminsCount = (int) allUsers.stream()
                .filter(user -> user.hasRole("ROLE_ADMIN"))
                .count();
        int activeToday = (int) allUsers.stream()
                .filter(user -> user.getDateInscription() != null && user.getDateInscription().isEqual(LocalDate.now()))
                .count();

        totalUsersMetric.setText(String.valueOf(allUsers.size()));
        adminsMetric.setText(String.valueOf(adminsCount));
        activeTodayMetric.setText(String.valueOf(activeToday));
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
        lift.setDelay(Duration.millis(delayMs));
        fade.play();
        lift.play();
    }

    private void installButtonEffects() {
        installStaticButtonEffect(addUserButton, true);
        installStaticButtonEffect(applyFiltersButton, true);
        installStaticButtonEffect(myProfileButton, false);
        installStaticButtonEffect(editProfileButton, false);
        installStaticButtonEffect(resetFiltersButton, false);
    }

    private void installStaticButtonEffect(Button button, boolean primary) {
        if (button == null) {
            return;
        }
        DropShadow shadow = new DropShadow();
        shadow.setRadius(primary ? 16 : 10);
        shadow.setSpread(primary ? 0.24 : 0.16);
        shadow.setOffsetY(2);
        shadow.setColor(primary ? Color.rgb(76, 185, 255, 0.46) : Color.rgb(98, 142, 206, 0.24));
        button.setEffect(shadow);

        button.setOnMouseEntered(event -> animateButtonScale(button, 1.05));
        button.setOnMouseExited(event -> animateButtonScale(button, 1.0));
    }

    private void installRowButtonEffect(Button button, boolean primary) {
        DropShadow shadow = new DropShadow();
        shadow.setRadius(primary ? 12 : 8);
        shadow.setSpread(primary ? 0.22 : 0.12);
        shadow.setOffsetY(1);
        shadow.setColor(primary ? Color.rgb(66, 186, 255, 0.35) : Color.rgb(121, 181, 243, 0.24));
        button.setEffect(shadow);

        button.setOnMouseEntered(event -> animateButtonScale(button, primary ? 1.05 : 1.03));
        button.setOnMouseExited(event -> animateButtonScale(button, 1.0));
    }

    private void animateButtonScale(Button button, double targetScale) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(170), button);
        transition.setToX(targetScale);
        transition.setToY(targetScale);
        transition.play();
    }

    private void openUserEdit(User user) {
        if (user == null || currentUser == null || headerLabel == null || headerLabel.getScene() == null) {
            return;
        }
        Stage stage = (Stage) headerLabel.getScene().getWindow();
        AdminNavigation.showUserEdit(stage, currentUser, user);
    }

    private void openUserShow(User user) {
        if (user == null || currentUser == null || headerLabel == null || headerLabel.getScene() == null) {
            return;
        }
        Stage stage = (Stage) headerLabel.getScene().getWindow();
        AdminNavigation.showUserProfile(stage, currentUser, user);
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

    private void updateResultsFooter(int usersCount) {
        if (resultsTextLabel == null) {
            return;
        }
        resultsTextLabel.setText("Showing " + usersCount + " user(s)");
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
            case "Sort by role" -> "role";
            case "Sort by balance" -> "solde";
            case "Sort by registration date" -> "date";
            case "Sort by ID" -> "id";
            default -> "nom";
        };
    }

    private String resolveRoleLabel(User user) {
        if (user.hasRole("ROLE_ADMIN")) {
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

    private String resolveRoleStyle(User user) {
        if (user.hasRole("ROLE_ADMIN")) {
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

    private Image loadUserAvatarImage(String imageRef) {
        if (imageRef == null || imageRef.isBlank()) {
            return null;
        }

        String cacheKey = imageRef.trim();
        if (avatarImageCache.containsKey(cacheKey)) {
            return avatarImageCache.get(cacheKey);
        }

        Image loaded = tryLoadAvatarImage(cacheKey);
        if (loaded != null && !loaded.isError()) {
            avatarImageCache.put(cacheKey, loaded);
            return loaded;
        }
        return null;
    }

    private Image tryLoadAvatarImage(String imageRef) {
        try {
            Path path = Path.of(imageRef);
            if (Files.exists(path)) {
                return new Image(path.toUri().toString(), 34, 34, true, true, true);
            }
        } catch (Exception ignored) {
            // Not a local path; fallback below.
        }

        try {
            if (imageRef.startsWith("/") && Main.class.getResource(imageRef) != null) {
                return new Image(Main.class.getResource(imageRef).toExternalForm(), 34, 34, true, true, true);
            }
            return new Image(imageRef, 34, 34, true, true, true);
        } catch (Exception e) {
            return null;
        }
    }

    private String valueOrDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private String valueOrEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private boolean isValidEmail(String email) {
        return email != null && email.length() <= 180 && EMAIL_PATTERN.matcher(email).matches();
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
        if (headerLabel != null && headerLabel.getScene() != null && headerLabel.getScene().getWindow() instanceof Stage stage) {
            UiDialog.show(stage, UiDialog.Type.INFO, title, title, content);
        }
    }

    private void showError(String title, String content) {
        if (headerLabel != null && headerLabel.getScene() != null && headerLabel.getScene().getWindow() instanceof Stage stage) {
            UiDialog.show(stage, UiDialog.Type.ERROR, title, title, content);
        }
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
