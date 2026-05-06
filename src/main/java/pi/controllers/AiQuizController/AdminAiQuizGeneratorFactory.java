package pi.controllers.AiQuizController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import pi.mains.Main;

import java.io.IOException;

public final class AdminAiQuizGeneratorFactory {

    private AdminAiQuizGeneratorFactory() {
    }

    public static FXMLLoader createLoader() {
        return new FXMLLoader(Main.class.getResource("/pi/views/ai-quiz-generator.fxml"));
    }

    public static Parent buildWorkspace() {
        try {
            return createLoader().load();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de charger AI Quiz Generator.", e);
        }
    }
}

