package pi.controllers.CoursQuizController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import pi.mains.Main;

import java.io.IOException;

public final class AdminCoursesQuizBackOfficeFactory {

    private AdminCoursesQuizBackOfficeFactory() {
    }

    public static Parent buildWorkspace() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/views/cours-quiz-backoffice.fxml"));
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger le back office Cours & Quiz.", e);
        }
    }
}
