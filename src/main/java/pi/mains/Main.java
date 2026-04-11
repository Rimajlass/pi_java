package pi.mains;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/Expense/Revenue/salary-expense-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1400, 900);
        stage.setTitle("Income & Expense Management");
        stage.setScene(scene);
        stage.show();
    }
}
