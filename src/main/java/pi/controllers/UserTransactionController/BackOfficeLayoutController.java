package pi.controllers.UserTransactionController;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import pi.controllers.AiQuizController.AiQuizGeneratorController;
import pi.controllers.CoursQuizController.CoursQuizDashboardController;
import pi.controllers.ExpenseRevenueController.BACK.AdminRevenueExpenseBackOfficeFactory;
import pi.controllers.InvestissementController.AdminController;
import pi.entities.User;
import pi.mains.Main;
import pi.savings.ui.AdminSavingsBackOfficeFactory;
import pi.tools.FxmlResources;
import pi.tools.AdminNavigation;
import pi.tools.ThemeManager;
import pi.tools.UiDialog;

import java.util.Locale;
import java.util.function.Consumer;

public class BackOfficeLayoutController {

    @FXML
    private VBox menuList;

    @FXML
    private Label userNameLabel;

    @FXML
    private Label roleStatusLabel;

    @FXML
    private AnchorPane contentHost;

    @FXML
    private ToggleButton themeToggleButton;

    private Consumer<String> menuSelectionHandler = key -> { };
    private Runnable logoutHandler = () -> { };

    @FXML
    private void initialize() {
        if (contentHost == null) {
            return;
        }
        contentHost.sceneProperty().addListener((obs, oldScene, newScene) -> refreshThemeToggle(newScene));
    }

    @FXML
    private void handleMenuSelection(MouseEvent event) {
        HBox selectedRow = resolveMenuRow(event);
        if (selectedRow == null) {
            return;
        }
        activateMenuRow(selectedRow);
        String key = extractMenuKey(selectedRow);
        if (key.isEmpty()) {
            return;
        }
        System.out.println("[BackOfficeLayout] menu click: " + key);
        if (!handleCoreNavigation(key)) {
            menuSelectionHandler.accept(key);
        }
    }

    @FXML
    private void handleUsersMenuClick(MouseEvent event) {
        if (event != null) {
            event.consume();
        }
        activateMenuByKey("Users");
        if (!handleCoreNavigation("Users")) {
            menuSelectionHandler.accept("Users");
        }
    }

    @FXML
    private void handleTransactionsMenuClick(MouseEvent event) {
        if (event != null) {
            event.consume();
        }
        activateMenuByKey("Transactions");
        if (!handleCoreNavigation("Transactions")) {
            menuSelectionHandler.accept("Transactions");
        }
    }

    @FXML
    private void handleRevenuesMenuClick(MouseEvent event) {
        if (event != null) {
            event.consume();
        }
        activateMenuByKey("Revenues");
        if (!loadRevenueWorkspaceDirect()) {
            menuSelectionHandler.accept("Revenues");
        }
    }

    @FXML
    private void handleExpensesMenuClick(MouseEvent event) {
        if (event != null) {
            event.consume();
        }
        activateMenuByKey("Expenses");
        if (!loadExpenseWorkspaceDirect()) {
            menuSelectionHandler.accept("Expenses");
        }
    }

    @FXML
    private void handleSavingsMenuClick(MouseEvent event) {
        if (event != null) {
            event.consume();
        }
        activateMenuByKey("Savings");
        if (!loadSavingsWorkspaceDirect()) {
            menuSelectionHandler.accept("Savings");
        }
    }

    @FXML
    private void handleGoalsMenuClick(MouseEvent event) {
        if (event != null) {
            event.consume();
        }
        activateMenuByKey("Goals");
        if (!loadGoalsWorkspaceDirect()) {
            menuSelectionHandler.accept("Goals");
        }
    }

    @FXML
    private void handleInvestmentsMenuClick(MouseEvent event) {
        if (event != null) {
            event.consume();
        }
        activateMenuByKey("Investments");
        if (!loadInvestmentsWorkspaceDirect()) {
            menuSelectionHandler.accept("Investments");
        }
    }

    @FXML
    private void handleLogout() {
        logoutHandler.run();
    }

    @FXML
    private void handleToggleTheme() {
        if (contentHost == null || contentHost.getScene() == null) {
            return;
        }
        Scene scene = contentHost.getScene();
        ThemeManager.toggleTheme(scene);
        refreshThemeToggle(scene);
        playThemeTransition(scene.getRoot());
    }

    public void setMenuSelectionHandler(Consumer<String> handler) {
        this.menuSelectionHandler = handler == null ? key -> { } : handler;
    }

    public void setLogoutHandler(Runnable handler) {
        this.logoutHandler = handler == null ? () -> { } : handler;
    }

    public void setActiveMenu(String key) {
        if (menuList == null || key == null) {
            return;
        }
        menuList.getChildren().stream()
                .filter(HBox.class::isInstance)
                .map(HBox.class::cast)
                .forEach(row -> {
                    row.getStyleClass().remove("menu-row-active");
                    if (row.getChildren().size() >= 2 && row.getChildren().get(1) instanceof Label label
                            && key.equalsIgnoreCase(label.getText())) {
                        row.getStyleClass().add("menu-row-active");
                    }
                });
    }

    public void setUser(User user) {
        if (userNameLabel != null && user != null) {
            String display = user.getNom() != null && !user.getNom().isBlank() ? user.getNom() : user.getEmail();
            userNameLabel.setText(display);
        }
        if (roleStatusLabel != null) {
            roleStatusLabel.setText("Online");
        }
    }

    public void setContent(Node content) {
        if (contentHost == null || content == null) {
            return;
        }
        content.setOpacity(0.0);
        content.setTranslateY(8.0);
        contentHost.getChildren().setAll(content);
        AnchorPane.setTopAnchor(content, 0.0);
        AnchorPane.setRightAnchor(content, 0.0);
        AnchorPane.setBottomAnchor(content, 0.0);
        AnchorPane.setLeftAnchor(content, 0.0);
        playContentTransition(content);
    }

    private void refreshThemeToggle(Scene scene) {
        if (themeToggleButton == null || scene == null) {
            return;
        }
        boolean darkMode = ThemeManager.isDarkMode(scene);
        themeToggleButton.setSelected(!darkMode);
        themeToggleButton.setText(darkMode ? "\u263D" : "\u263C");
    }

    private void playThemeTransition(Node root) {
        if (root == null) {
            return;
        }
        FadeTransition transition = new FadeTransition(Duration.millis(180), root);
        transition.setFromValue(0.92);
        transition.setToValue(1.0);
        transition.play();
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

    private void playContentTransition(Node content) {
        FadeTransition fade = new FadeTransition(Duration.millis(190), content);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        TranslateTransition rise = new TranslateTransition(Duration.millis(190), content);
        rise.setFromY(8.0);
        rise.setToY(0.0);

        new ParallelTransition(fade, rise).play();
    }

    private void activateMenuRow(HBox selectedRow) {
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

    private void activateMenuByKey(String key) {
        if (menuList == null || key == null) {
            return;
        }
        menuList.getChildren().stream()
                .filter(HBox.class::isInstance)
                .map(HBox.class::cast)
                .forEach(row -> {
                    row.getStyleClass().remove("menu-row-active");
                    String rowKey = extractMenuKey(row);
                    if (key.equalsIgnoreCase(rowKey) && !row.getStyleClass().contains("menu-row-active")) {
                        row.getStyleClass().add("menu-row-active");
                    }
                });
    }

    private String extractMenuKey(HBox row) {
        if (row == null) {
            return "";
        }
        for (Node child : row.getChildren()) {
            if (child instanceof Label label) {
                return label.getText() == null ? "" : label.getText().trim();
            }
        }
        return "";
    }

    private boolean handleCoreNavigation(String key) {
        if (contentHost == null || contentHost.getScene() == null || !(contentHost.getScene().getWindow() instanceof Stage stage)) {
            return false;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "users" -> {
                if (loadUsersContentDirect()) {
                    yield true;
                }
                if (stage.getUserData() instanceof User currentUser) {
                    AdminNavigation.showUsersManagement(stage, currentUser);
                    yield true;
                }
                yield false;
            }
            case "transactions" -> {
                if (loadTransactionsContentDirect()) {
                    yield true;
                }
                if (stage.getUserData() instanceof User currentUser) {
                    AdminNavigation.showTransactionsManagement(stage, currentUser);
                    yield true;
                }
                yield false;
            }
            case "revenues" -> loadRevenueWorkspaceDirect();
            case "expenses" -> loadExpenseWorkspaceDirect();
            case "savings" -> loadSavingsWorkspaceDirect();
            case "goals" -> loadGoalsWorkspaceDirect();
            case "investments" -> loadInvestmentsWorkspaceDirect();
            case "course & quiz", "cours & quiz" -> {
                if (loadContentFromFxml("/pi/views/dashboard.fxml", CoursQuizDashboardController.class, null)) {
                    if (contentHost.getScene().getWindow() instanceof Stage currentStage) {
                        currentStage.setTitle("Course & Quiz | Decide$");
                    }
                    yield true;
                }
                yield false;
            }
            case "ai quiz generator" -> {
                User user = resolveCurrentUser();
                if (loadContentFromFxml(
                        "/pi/views/ai-quiz-generator.fxml",
                        AiQuizGeneratorController.class,
                        controller -> controller.setContext(user)
                )) {
                    if (contentHost.getScene().getWindow() instanceof Stage currentStage) {
                        currentStage.setTitle("AI Quiz Generator | Decide$");
                    }
                    yield true;
                }
                yield false;
            }
            default -> false;
        };
    }

    private boolean loadUsersContentDirect() {
        return loadContentFromFxml(
                "/pi/mains/admin-backend-view.fxml",
                AdminBackendController.class,
                controller -> {
                    User user = resolveCurrentUser();
                    if (user != null) {
                        controller.setUser(user);
                    }
                }
        );
    }

    private boolean loadTransactionsContentDirect() {
        return loadContentFromFxml(
                "/pi/mains/transactions-management-view.fxml",
                TransactionsManagementController.class,
                controller -> {
                    User user = resolveCurrentUser();
                    if (user != null) {
                        controller.setContext(user);
                    }
                    ensureStyles("/pi/styles/transactions-management.css");
                }
        );
    }

    private boolean loadRevenueWorkspaceDirect() {
        try {
            setContent(AdminRevenueExpenseBackOfficeFactory.buildRevenueWorkspace());
            if (contentHost.getScene().getWindow() instanceof Stage stage) {
                stage.setTitle("Revenues | Decide$");
            }
            return true;
        } catch (Exception e) {
            System.err.println("[BackOfficeLayout] Failed to load revenue workspace");
            e.printStackTrace();
            return false;
        }
    }

    private boolean loadExpenseWorkspaceDirect() {
        try {
            setContent(AdminRevenueExpenseBackOfficeFactory.buildExpenseWorkspace());
            if (contentHost.getScene().getWindow() instanceof Stage stage) {
                stage.setTitle("Expenses | Decide$");
            }
            return true;
        } catch (Exception e) {
            System.err.println("[BackOfficeLayout] Failed to load expense workspace");
            e.printStackTrace();
            return false;
        }
    }

    private boolean loadSavingsWorkspaceDirect() {
        try {
            setContent(AdminSavingsBackOfficeFactory.buildSavingsWorkspace());
            if (contentHost.getScene().getWindow() instanceof Stage stage) {
                stage.setTitle("Savings | Decide$");
            }
            return true;
        } catch (Exception e) {
            System.err.println("[BackOfficeLayout] Failed to load savings workspace");
            e.printStackTrace();
            return false;
        }
    }

    private boolean loadGoalsWorkspaceDirect() {
        try {
            setContent(AdminSavingsBackOfficeFactory.buildGoalsWorkspace());
            if (contentHost.getScene().getWindow() instanceof Stage stage) {
                stage.setTitle("Goals | Decide$");
            }
            return true;
        } catch (Exception e) {
            System.err.println("[BackOfficeLayout] Failed to load goals workspace");
            e.printStackTrace();
            return false;
        }
    }

    private boolean loadInvestmentsWorkspaceDirect() {
        boolean loaded = loadContentFromFxml(
                "/Invest/admin.fxml",
                AdminController.class,
                controller -> {
                    User user = resolveCurrentUser();
                    if (user != null) {
                        controller.setUser(user);
                    }
                }
        );
        if (loaded) {
            return true;
        }

        loaded = loadContentFromFxml(
                "/invest/admin.fxml",
                AdminController.class,
                controller -> {
                    User user = resolveCurrentUser();
                    if (user != null) {
                        controller.setUser(user);
                    }
                }
        );
        if (!loaded && roleStatusLabel != null) {
            roleStatusLabel.setText("Investments view load failed");
        }
        return loaded;
    }

    private <T> boolean loadContentFromFxml(String fxmlPath, Class<T> controllerClass, Consumer<T> initializer) {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, fxmlPath);
            Parent root = loader.getRoot();
            Object rawController = loader.getController();
            if (!controllerClass.isInstance(rawController)) {
                System.err.println("[BackOfficeLayout] Unexpected controller for " + fxmlPath + ": " +
                        (rawController == null ? "null" : rawController.getClass().getName()));
                showLoadError("Navigation", "Controller inattendu pour " + fxmlPath);
                return false;
            }
            T controller = controllerClass.cast(rawController);
            if (initializer != null) {
                initializer.accept(controller);
            }
            Node contentNode = detachContentWrapper(root);
            setContent(contentNode);
            return true;
        } catch (Exception e) {
            System.err.println("[BackOfficeLayout] Failed to load content: " + fxmlPath);
            e.printStackTrace();
            showLoadError("Navigation", "Impossible de charger " + fxmlPath + ":\n" + chainMessages(e));
            return false;
        }
    }

    private void showLoadError(String title, String details) {
        try {
            if (contentHost != null && contentHost.getScene() != null && contentHost.getScene().getWindow() instanceof Stage stage) {
                UiDialog.error(stage, title, details);
            }
        } catch (Exception ignored) {
        }
    }

    private String chainMessages(Throwable error) {
        if (error == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Throwable current = error;
        int hops = 0;
        while (current != null && hops < 8) {
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append("\n");
                }
                sb.append(message.trim());
            }
            current = current.getCause();
            hops++;
        }
        return sb.isEmpty() ? error.getClass().getSimpleName() : sb.toString();
    }

    private Node detachContentWrapper(Parent root) {
        Node contentNode = findByStyleClass(root, "content-wrapper");
        if (contentNode == null) {
            return root;
        }
        if (contentNode.getParent() instanceof Pane parent) {
            parent.getChildren().remove(contentNode);
        }
        AnchorPane.setTopAnchor(contentNode, 0.0);
        AnchorPane.setRightAnchor(contentNode, 0.0);
        AnchorPane.setBottomAnchor(contentNode, 0.0);
        AnchorPane.setLeftAnchor(contentNode, 0.0);
        return contentNode;
    }

    private Node findByStyleClass(Parent root, String styleClass) {
        if (root.getStyleClass().contains(styleClass)) {
            return root;
        }
        for (Node child : root.getChildrenUnmodifiable()) {
            if (child.getStyleClass().contains(styleClass)) {
                return child;
            }
            if (child instanceof Parent parentChild) {
                Node nested = findByStyleClass(parentChild, styleClass);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private User resolveCurrentUser() {
        if (contentHost != null && contentHost.getScene() != null && contentHost.getScene().getWindow() instanceof Stage stage
                && stage.getUserData() instanceof User user) {
            return user;
        }
        return null;
    }

    private void ensureStyles(String... classpathStyles) {
        if (contentHost == null || contentHost.getScene() == null || classpathStyles == null) {
            return;
        }
        Scene scene = contentHost.getScene();
        for (String style : classpathStyles) {
            if (style == null || style.isBlank() || Main.class.getResource(style) == null) {
                continue;
            }
            String url = Main.class.getResource(style).toExternalForm();
            if (!scene.getStylesheets().contains(url)) {
                scene.getStylesheets().add(url);
            }
        }
        ThemeManager.registerScene(scene);
    }
}
