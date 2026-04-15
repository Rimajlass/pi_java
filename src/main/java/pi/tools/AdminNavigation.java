package pi.tools;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pi.controllers.UserTransactionController.AdminBackendController;
import pi.controllers.UserTransactionController.TransactionsManagementController;
import pi.entities.User;
import pi.mains.Main;

/**
 * Navigation entre écrans admin (Users, Transactions) avec le même {@link User} connecté.
 */
public final class AdminNavigation {

    private AdminNavigation() {
    }

    public static void showUsersManagement(Stage stage, User currentUser) {
        if (currentUser == null) {
            return;
        }
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/admin-backend-view.fxml");
            Parent root = (Parent) loader.getRoot();
            AdminBackendController controller = loader.getController();
            Scene scene = new Scene(root, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/admin-backend.css");
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/user-show.css");
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/edit-user.css");
            stage.setTitle("Users | Decide$");
            stage.setScene(scene);
            stage.show();
            controller.setUser(currentUser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void showTransactionsManagement(Stage stage, User currentUser) {
        if (currentUser == null) {
            return;
        }
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/transactions-management-view.fxml");
            Parent root = (Parent) loader.getRoot();
            TransactionsManagementController controller = loader.getController();
            Scene scene = new Scene(root, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/admin-backend.css");
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/transactions-management.css");
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/user-show.css");
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/edit-user.css");
            stage.setTitle("Transactions Management | Decide$");
            stage.setScene(scene);
            stage.show();
            controller.setContext(currentUser);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
