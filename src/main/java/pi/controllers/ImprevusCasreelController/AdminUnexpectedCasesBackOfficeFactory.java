package pi.controllers.ImprevusCasreelController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import pi.mains.Main;

import java.io.IOException;

public final class AdminUnexpectedCasesBackOfficeFactory {

    private AdminUnexpectedCasesBackOfficeFactory() {
    }

    public static Parent buildWorkspace() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/back-office-view.fxml"));
            Parent root = loader.load();

            if (root instanceof BorderPane borderPane && borderPane.getCenter() instanceof Parent center) {
                center.getStylesheets().addAll(root.getStylesheets());
                return center;
            }
            return root;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load Unexpected Events & Real Cases back office.", exception);
        }
    }
}
