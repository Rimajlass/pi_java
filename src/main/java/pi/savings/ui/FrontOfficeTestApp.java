package pi.savings.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class FrontOfficeTestApp extends Application {

    @Override
    public void start(Stage stage) {
        VBox card = new VBox(16);
        card.getStyleClass().add("front-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(520);

        Label title = new Label("Decide$ Front Office Test");
        title.getStyleClass().add("front-title");

        Label subtitle = new Label("Use this temporary front-office entry point to validate the Goals back-office before integrating it into the final user flow.");
        subtitle.getStyleClass().add("front-subtitle");
        subtitle.setWrapText(true);

        Button openUserButton = new Button("Open User Workspace");
        openUserButton.getStyleClass().add("front-primary-btn");
        openUserButton.setOnAction(event -> new SavingsGoalsApp().start(new Stage()));

        Button openBackOfficeButton = new Button("Tester Back Office");
        openBackOfficeButton.getStyleClass().add("front-secondary-btn");
        openBackOfficeButton.setOnAction(event -> new GoalsBackOfficeApp().start(new Stage()));

        Region spacer = new Region();
        spacer.setPrefHeight(4);

        card.getChildren().addAll(title, subtitle, spacer, openUserButton, openBackOfficeButton);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("front-root");
        root.setPadding(new Insets(32));

        Scene scene = new Scene(root, 980, 620);
        scene.getStylesheets().add(
                FrontOfficeTestApp.class.getResource("/pi/savings/ui/front-office-test.css").toExternalForm()
        );

        stage.setTitle("Decide$ - Front Office Test");
        stage.setScene(scene);
        stage.setMinWidth(860);
        stage.setMinHeight(560);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
