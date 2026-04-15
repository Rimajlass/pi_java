package pi.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/pi/mains/login-view.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, 1460, 780);
        scene.getStylesheets().add(Main.class.getResource("/pi/styles/login.css").toExternalForm());
        stage.setMinWidth(1200);
        stage.setMinHeight(720);
        stage.setTitle("User Secure Login");
        stage.setScene(scene);
        stage.show();
    }
}
