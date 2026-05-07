package pi.mains;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import pi.controllers.UserTransactionController.SplashScreenController;
import pi.tools.FxmlResources;
import pi.tools.ThemeManager;

public class UserLoginApp {

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
        public void start(Stage stage) throws Exception {
            ThemeManager.resetToLightMode();
            FXMLLoader splashLoader = FxmlResources.load(UserLoginApp.class, "/pi/mains/splash-view.fxml");
            Parent splashRoot = splashLoader.getRoot();
            SplashScreenController splashController = splashLoader.getController();

            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            double width = Math.min(1100, bounds.getWidth() * 0.9);
            double height = Math.min(720, bounds.getHeight() * 0.9);

            Scene scene = new Scene(splashRoot, width, height);
            FxmlResources.addStylesheet(scene, UserLoginApp.class, "/pi/styles/splash.css");

            stage.setTitle("User Secure Login");
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.setMinWidth(Math.min(900, width));
            stage.setMinHeight(Math.min(600, height));
            stage.setScene(scene);
            ThemeManager.registerStage(stage);
            stage.centerOnScreen();
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
                FXMLLoader loginLoader = FxmlResources.load(UserLoginApp.class, "/pi/mains/login-view.fxml");
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
                FxmlResources.addStylesheet(scene, UserLoginApp.class, "/pi/styles/login.css");
                FxmlResources.addStylesheet(scene, UserLoginApp.class, "/pi/styles/global.css");
                ThemeManager.registerScene(scene);

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
