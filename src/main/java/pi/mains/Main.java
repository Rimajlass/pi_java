package pi.mains;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import pi.controllers.UserTransactionController.SplashScreenController;
import pi.tools.FxmlResources;
import pi.tools.ThemeManager;

import java.io.IOException;

public class Main {

    private static final long TOTAL_SPLASH_FLOW_MS = 6000;
    private static final long SCENE_TRANSITION_MS = 600;
    private static final String SPLASH_TITLE = "Access your finance workspace securely";
    private static final String SPLASH_SUBTITLE = "Encrypted access | Smart insights | Trusted financial control";
    private static final String SPLASH_LOGO = "/pi/images/chat.png";

    public static void main(String[] args) {
        Application.launch(FxApp.class, args);
    }

    public static class FxApp extends Application {

        @Override
        public void start(Stage stage) throws IOException {
            FXMLLoader splashLoader = FxmlResources.load(Main.class, "/pi/mains/splash-view.fxml");
            Parent splashRoot = splashLoader.getRoot();
            SplashScreenController splashController = splashLoader.getController();

            Scene scene = new Scene(splashRoot, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/splash.css");

            stage.setMinWidth(1200);
            stage.setMinHeight(720);
            stage.setTitle("User Secure Login");
            stage.setScene(scene);
            ThemeManager.registerStage(stage);
            stage.setMaximized(true);
            stage.show();

            splashController.setContent(SPLASH_TITLE, SPLASH_SUBTITLE, SPLASH_LOGO);
            splashController.playIntro(Duration.millis(TOTAL_SPLASH_FLOW_MS - SCENE_TRANSITION_MS), () -> switchToLogin(scene));
        }

        private void switchToLogin(Scene scene) {
            Parent loginRoot = loadLoginRoot();
            if (loginRoot == null) {
                return;
            }

            FadeTransition splashFadeOut = new FadeTransition(Duration.millis(SCENE_TRANSITION_MS / 2.0), scene.getRoot());
            splashFadeOut.setFromValue(1.0);
            splashFadeOut.setToValue(0.0);
            splashFadeOut.setInterpolator(Interpolator.EASE_BOTH);
            splashFadeOut.setOnFinished(event -> showLoginWithFadeIn(scene, loginRoot));
            splashFadeOut.play();
        }

        private Parent loadLoginRoot() {
            try {
                FXMLLoader loginLoader = FxmlResources.load(Main.class, "/pi/mains/login-view.fxml");
                return loginLoader.getRoot();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private void showLoginWithFadeIn(Scene scene, Parent loginRoot) {
            try {
                loginRoot.setOpacity(0.0);
                scene.setRoot(loginRoot);
                scene.getStylesheets().clear();
                FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/login.css");

                FadeTransition loginFadeIn = new FadeTransition(Duration.millis(SCENE_TRANSITION_MS / 2.0), loginRoot);
                loginFadeIn.setFromValue(0.0);
                loginFadeIn.setToValue(1.0);
                loginFadeIn.setInterpolator(Interpolator.EASE_BOTH);
                loginFadeIn.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
