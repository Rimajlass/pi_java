package pi.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFx extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(MainFx.class.getResource("/pi/views/dashboard.fxml"));
        Scene scene = new Scene(loader.load(), 1500, 950);
        stage.setTitle("Gestion Cours / Quiz");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(760);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
