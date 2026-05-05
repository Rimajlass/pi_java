package pi.savings.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.geometry.Rectangle2D;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class SavingsGoalsApp extends Application {

    @Override
    public void start(Stage stage) {
        try {
            SavingsUiController controller = new SavingsUiController();
            SavingsGoalsView view = new SavingsGoalsView();
            Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
            double windowWidth = Math.min(1440, Math.max(1200, visualBounds.getWidth() - 40));
            double windowHeight = Math.min(920, Math.max(780, visualBounds.getHeight() - 40));

            StackPane loadingRoot = new StackPane(new Label("Loading Savings & Goals..."));
            loadingRoot.setStyle("-fx-padding: 32; -fx-alignment: center; -fx-background-color: white;");

            Scene scene = new Scene(loadingRoot, windowWidth, windowHeight);
            stage.setTitle("Decide$ - Savings & Goals");
            stage.setScene(scene);
            stage.setMinWidth(Math.min(1200, visualBounds.getWidth() - 20));
            stage.setMinHeight(Math.min(780, visualBounds.getHeight() - 20));
            stage.show();

            Platform.runLater(() -> scene.setRoot(view.build(controller)));
        } catch (RuntimeException exception) {
            System.err.println("JavaFX startup failed in SavingsGoalsApp.start: " + exception.getMessage());
            exception.printStackTrace(System.err);
            throw exception;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
