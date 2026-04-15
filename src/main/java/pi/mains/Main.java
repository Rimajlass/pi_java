package pi.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main {

    public static void main(String[] args) {
        Application.launch(FxApp.class, args);
    }

    public static class FxApp extends Application {

        @Override
        public void start(Stage stage) throws Exception {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/login-view.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/login.css").toExternalForm());

            stage.setTitle("User Secure Login");
            stage.setMinWidth(1200);
            stage.setMinHeight(720);
            stage.setScene(scene);
            stage.show();
        }
    }
}
