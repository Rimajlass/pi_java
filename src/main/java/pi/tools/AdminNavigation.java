package pi.tools;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pi.controllers.CoursQuizController.AdminCoursesQuizBackOfficeFactory;
import pi.controllers.ExpenseRevenueController.BACK.AdminRevenueExpenseBackOfficeFactory;
import pi.controllers.ImprevusCasreelController.AdminUnexpectedCasesBackOfficeFactory;
import pi.controllers.UserTransactionController.UsersManagementController;
import pi.controllers.UserTransactionController.AddUserController;
import pi.controllers.UserTransactionController.BackOfficeLayoutController;
import pi.controllers.UserTransactionController.EditUserController;
import pi.controllers.UserTransactionController.TransactionsManagementController;
import pi.controllers.UserTransactionController.UserShowController;
import pi.entities.User;
import pi.mains.Main;
import pi.savings.ui.AdminSavingsBackOfficeFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Navigation entre écrans admin (Users, Transactions) avec le même {@link User} connecté.
 */
public final class AdminNavigation {
    private static final String LAYOUT_CONTROLLER_KEY = "backoffice.layout.controller";
    private static final String LAYOUT_SCENE_KEY = "backoffice.layout.scene";
    private static final Map<String, Node> WORKSPACE_CACHE = new HashMap<>();

    private AdminNavigation() {
    }

    public static void showUsersManagement(Stage stage, User currentUser) {
        try {
            LayoutContext layout = ensureLayout(stage);

            ContentBundle<UsersManagementController> bundle = loadContentBundle(
                    "/pi/mains/users-management-view.fxml",
                    UsersManagementController.class
            );
            layout.controller().setContent(bundle.contentNode());
            layout.controller().setActiveMenu("Users");
            layout.controller().setMenuSelectionHandler(key -> routeMenuSelection(stage, currentUser, key));
            layout.controller().setLogoutHandler(() -> showLogin(stage));

            stage.setTitle("Users | Decide$");
            stage.setScene(layout.scene());
            configureResponsiveStage(stage);
            stage.show();
            bundle.controller().setUser(currentUser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void showTransactionsManagement(Stage stage, User currentUser) {
        try {
            LayoutContext layout = ensureLayout(stage);

            ContentBundle<TransactionsManagementController> bundle = loadContentBundle(
                    "/pi/mains/transactions-management-view.fxml",
                    TransactionsManagementController.class
            );
            layout.controller().setContent(bundle.contentNode());
            layout.controller().setUser(currentUser);
            layout.controller().setActiveMenu("Transactions");
            layout.controller().setMenuSelectionHandler(key -> routeMenuSelection(stage, currentUser, key));
            layout.controller().setLogoutHandler(() -> showLogin(stage));
            ensureStyles(layout.scene(), "/pi/styles/transactions-management.css");
            stage.setTitle("Transactions Management | Decide$");
            stage.setScene(layout.scene());
            configureResponsiveStage(stage);
            stage.show();
            if (currentUser != null) {
                bundle.controller().setContext(currentUser);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void showUserProfile(Stage stage, User currentUser, User viewedUser) {
        if (currentUser == null || viewedUser == null) {
            return;
        }
        try {
            LayoutContext layout = ensureLayout(stage);

            ContentBundle<UserShowController> bundle = loadContentBundle(
                    "/pi/mains/user-show-view.fxml",
                    UserShowController.class
            );
            layout.controller().setContent(bundle.contentNode());
            layout.controller().setUser(currentUser);
            layout.controller().setActiveMenu("Users");
            layout.controller().setMenuSelectionHandler(key -> routeMenuSelection(stage, currentUser, key));
            layout.controller().setLogoutHandler(() -> showLogin(stage));

            stage.setTitle("User Profile");
            stage.setScene(layout.scene());
            configureResponsiveStage(stage);
            stage.show();
            bundle.controller().setContext(currentUser, viewedUser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void showUserEdit(Stage stage, User currentUser, User editedUser) {
        if (currentUser == null || editedUser == null) {
            return;
        }
        try {
            LayoutContext layout = ensureLayout(stage);

            ContentBundle<EditUserController> bundle = loadContentBundle(
                    "/pi/mains/edit-user-view.fxml",
                    EditUserController.class
            );
            layout.controller().setContent(bundle.contentNode());
            layout.controller().setUser(currentUser);
            layout.controller().setActiveMenu("Users");
            layout.controller().setMenuSelectionHandler(key -> routeMenuSelection(stage, currentUser, key));
            layout.controller().setLogoutHandler(() -> showLogin(stage));

            stage.setTitle("Edit utilisateur");
            stage.setScene(layout.scene());
            configureResponsiveStage(stage);
            stage.show();
            bundle.controller().setContext(currentUser, editedUser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void showUserCreate(Stage stage, User currentUser) {
        if (currentUser == null) {
            return;
        }
        try {
            LayoutContext layout = ensureLayout(stage);

            ContentBundle<AddUserController> bundle = loadContentBundle(
                    "/pi/mains/add-user-view.fxml",
                    AddUserController.class
            );
            layout.controller().setContent(bundle.contentNode());
            layout.controller().setUser(currentUser);
            layout.controller().setActiveMenu("Users");
            layout.controller().setMenuSelectionHandler(key -> routeMenuSelection(stage, currentUser, key));
            layout.controller().setLogoutHandler(() -> showLogin(stage));
            ensureStyles(layout.scene(), "/pi/styles/add-user.css");

            stage.setTitle("Create User");
            stage.setScene(layout.scene());
            configureResponsiveStage(stage);
            stage.show();
            bundle.controller().setContext(currentUser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void routeMenuSelection(Stage stage, User currentUser, String key) {
        if (key == null) {
            return;
        }
        switch (key.trim().toLowerCase()) {
            case "users" -> showUsersManagement(stage, currentUser);
            case "transactions" -> showTransactionsManagement(stage, currentUser);
            case "revenues" -> showWorkspace(stage, currentUser, key, () -> AdminRevenueExpenseBackOfficeFactory.buildRevenueWorkspace());
            case "expenses" -> showWorkspace(stage, currentUser, key, () -> AdminRevenueExpenseBackOfficeFactory.buildExpenseWorkspace());
            case "course & quiz" -> showWorkspace(stage, currentUser, key, AdminCoursesQuizBackOfficeFactory::buildWorkspace);
            case "unexpected events", "real cases" -> showWorkspace(stage, currentUser, key, AdminUnexpectedCasesBackOfficeFactory::buildWorkspace);
            case "savings" -> showWorkspace(stage, currentUser, key, AdminSavingsBackOfficeFactory::buildSavingsWorkspace);
            case "goals" -> showWorkspace(stage, currentUser, key, AdminSavingsBackOfficeFactory::buildGoalsWorkspace);
            case "investments" -> showWorkspace(stage, currentUser, key, AdminNavigation::buildInvestmentsWorkspace);
            case "reports", "objectives", "reclamations", "statistics", "ai quiz generator" -> showPlaceholderModule(stage, currentUser, key);
            default -> showPlaceholderModule(stage, currentUser, key);
        }
    }

    private static Node buildInvestmentsWorkspace() {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/Invest/admin.fxml");
            Parent root = (Parent) loader.getRoot();
            return detachContentWrapper(root);
        } catch (Exception e) {
            throw new RuntimeException("Unable to load investments workspace", e);
        }
    }

    private static void showWorkspace(Stage stage, User currentUser, String key, WorkspaceProvider provider) {
        try {
            LayoutContext layout = ensureLayout(stage);
            layout.controller().setUser(currentUser);
            layout.controller().setActiveMenu(key);
            layout.controller().setMenuSelectionHandler(next -> routeMenuSelection(stage, currentUser, next));
            layout.controller().setLogoutHandler(() -> showLogin(stage));

            Node content = WORKSPACE_CACHE.computeIfAbsent(
                    normalizeKey(key),
                    ignored -> provider.create()
            );

            layout.controller().setContent(content);
            stage.setScene(layout.scene());
            stage.setTitle(key + " | Decide$");
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void showPlaceholderModule(Stage stage, User currentUser, String key) {
        try {
            LayoutContext layout = ensureLayout(stage);
            layout.controller().setUser(currentUser);
            layout.controller().setActiveMenu(key);
            layout.controller().setMenuSelectionHandler(next -> routeMenuSelection(stage, currentUser, next));
            layout.controller().setLogoutHandler(() -> showLogin(stage));

            VBox card = new VBox(10);
            card.getStyleClass().add("card");
            Label title = new Label(key + " module");
            title.getStyleClass().add("card-title");
            Label subtitle = new Label("This workspace will be connected here while keeping the same sidebar and theme.");
            subtitle.getStyleClass().add("admin-subtitle");
            subtitle.setWrapText(true);
            card.getChildren().addAll(title, subtitle);

            VBox wrapper = new VBox(12, card);
            wrapper.getStyleClass().add("content-root");
            VBox.setVgrow(card, Priority.NEVER);

            HBox line = new HBox();
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            line.getChildren().add(spacer);
            wrapper.getChildren().add(line);

            layout.controller().setContent(wrapper);
            stage.setScene(layout.scene());
            stage.setTitle(key + " | Decide$");
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void showLogin(Stage stage) {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/login-view.fxml");
            Parent root = (Parent) loader.getRoot();
            Scene scene = new Scene(root, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/login.css");
            ThemeManager.registerScene(scene);
            stage.setUserData(null);
            stage.setTitle("User Secure Login");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> ContentBundle<T> loadContentBundle(String fxmlPath, Class<T> controllerClass) throws Exception {
        FXMLLoader loader = FxmlResources.load(Main.class, fxmlPath);
        Parent fullRoot = (Parent) loader.getRoot();
        Object rawController = loader.getController();
        if (!controllerClass.isInstance(rawController)) {
            throw new IllegalStateException("Unexpected controller for " + fxmlPath);
        }
        Node contentNode = detachContentWrapper(fullRoot);
        return new ContentBundle<>(controllerClass.cast(rawController), contentNode);
    }

    private static Node detachContentWrapper(Parent root) {
        Node contentNode = findByStyleClass(root, "content-wrapper");
        if (contentNode == null) {
            throw new IllegalStateException("content-wrapper not found");
        }
        if (!(contentNode.getParent() instanceof Pane parentPane)) {
            throw new IllegalStateException("Unsupported parent for content-wrapper");
        }
        parentPane.getChildren().remove(contentNode);
        AnchorPane.setTopAnchor(contentNode, 0.0);
        AnchorPane.setRightAnchor(contentNode, 0.0);
        AnchorPane.setBottomAnchor(contentNode, 0.0);
        AnchorPane.setLeftAnchor(contentNode, 0.0);
        return contentNode;
    }

    private static Node findByStyleClass(Parent root, String styleClass) {
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

    private record ContentBundle<T>(T controller, Node contentNode) {}

    private record LayoutContext(BackOfficeLayoutController controller, Scene scene) {}

    private static LayoutContext ensureLayout(Stage stage) throws Exception {
        Object existingController = stage.getProperties().get(LAYOUT_CONTROLLER_KEY);
        Object existingScene = stage.getProperties().get(LAYOUT_SCENE_KEY);
        if (existingController instanceof BackOfficeLayoutController layoutController && existingScene instanceof Scene scene) {
            ThemeManager.registerScene(scene);
            ensureStyles(scene,
                    "/pi/styles/admin-backend.css",
                    "/pi/styles/user-show.css",
                    "/pi/styles/edit-user.css",
                    "/pi/styles/transactions-management.css"
            );
            return new LayoutContext(layoutController, scene);
        }
        FXMLLoader layoutLoader = FxmlResources.load(Main.class, "/pi/mains/back-office-layout-view.fxml");
        Parent layoutRoot = (Parent) layoutLoader.getRoot();
        BackOfficeLayoutController layoutController = layoutLoader.getController();
        Scene scene = new Scene(layoutRoot, 1460, 780);
        ensureStyles(scene,
                "/pi/styles/admin-backend.css",
                "/pi/styles/user-show.css",
                "/pi/styles/edit-user.css",
                "/pi/styles/transactions-management.css"
        );
        ThemeManager.registerScene(scene);
        ThemeManager.debugSceneStylesheets(scene, "ensureLayout");
        stage.getProperties().put(LAYOUT_CONTROLLER_KEY, layoutController);
        stage.getProperties().put(LAYOUT_SCENE_KEY, scene);
        return new LayoutContext(layoutController, scene);
    }

    private static void ensureStyles(Scene scene, String... styles) {
        if (scene == null || styles == null) {
            return;
        }
        for (String style : styles) {
            if (style == null || style.isBlank()) {
                continue;
            }
            String url = Main.class.getResource(style).toExternalForm();
            if (!scene.getStylesheets().contains(url)) {
                scene.getStylesheets().add(url);
            }
        }
    }

    private static String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replace('\u00A0', ' ')
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase();
    }

    private static void configureResponsiveStage(Stage stage) {
        stage.setMinWidth(1180);
        stage.setMinHeight(700);
        stage.setMaximized(true);
    }

    @FunctionalInterface
    private interface WorkspaceProvider {
        Node create();
    }
}
