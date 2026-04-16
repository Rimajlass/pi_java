package pi.controllers.ExpenseRevenueController.BACK;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import pi.mains.Main;

import java.io.IOException;

public final class AdminRevenueExpenseBackOfficeFactory {

    private AdminRevenueExpenseBackOfficeFactory() {
    }

    public static Parent buildRevenueWorkspace() {
        return buildEmbeddedWorkspace("/Expense/Revenue/BACK/revenue-back-view.fxml");
    }

    public static Parent buildExpenseWorkspace() {
        return buildEmbeddedWorkspace("/Expense/Revenue/BACK/expense-back-view.fxml");
    }

    private static Parent buildEmbeddedWorkspace(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent root = loader.load();

            if (root instanceof HBox container && container.getChildren().size() > 1) {
                Node content = container.getChildren().get(1);
                container.getChildren().remove(content);

                StackPane wrapper = new StackPane(content);
                wrapper.getStylesheets().addAll(root.getStylesheets());
                wrapper.getStyleClass().add("embedded-admin-workspace");
                return wrapper;
            }

            return root;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load embedded workspace: " + fxmlPath, exception);
        }
    }
}
