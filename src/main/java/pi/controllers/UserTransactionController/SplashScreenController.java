package pi.controllers.UserTransactionController;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SplashScreenController {

    @FXML
    private StackPane splashRoot;

    @FXML
    private Region backgroundPulse;

    @FXML
    private Region ambientOrb;

    @FXML
    private Pane particlesLayer;

    @FXML
    private Region logoGlow;

    @FXML
    private ImageView logoImage;

    @FXML
    private Label titleLabel;

    @FXML
    private Label subtitleLabel;

    @FXML
    private Region loadingLine;

    @FXML
    private StackPane loadingTrack;

    private final List<Timeline> particleAnimations = new ArrayList<>();

    /**
     * Allows easy branding changes without touching FXML/CSS.
     */
    public void setContent(String title, String subtitle, String logoResourcePath) {
        titleLabel.setText(title);
        subtitleLabel.setText(subtitle);
        if (logoResourcePath == null || logoResourcePath.isBlank()) {
            return;
        }
        var stream = getClass().getResourceAsStream(logoResourcePath);
        if (stream != null) {
            Image image = new Image(stream);
            logoImage.setImage(image);
        }
    }

    public void playIntro(Duration totalSplashDuration, Runnable onFinished) {
        initializeVisualState();
        createParticles(20);

        double totalMs = Math.max(2500, totalSplashDuration.toMillis());

        Timeline loadingTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(loadingLine.prefWidthProperty(), 0)),
                new KeyFrame(Duration.millis(totalMs * 0.20), new KeyValue(loadingLine.prefWidthProperty(), 0)),
                new KeyFrame(Duration.millis(totalMs * 0.94), new KeyValue(loadingLine.prefWidthProperty(), 620, Interpolator.EASE_BOTH))
        );

        FadeTransition backgroundFade = new FadeTransition(Duration.millis(totalMs * 0.18), backgroundPulse);
        backgroundFade.setFromValue(0.0);
        backgroundFade.setToValue(1.0);

        FadeTransition ambientFade = new FadeTransition(Duration.millis(totalMs * 0.22), ambientOrb);
        ambientFade.setFromValue(0.0);
        ambientFade.setToValue(1.0);
        ambientFade.setDelay(Duration.millis(totalMs * 0.04));

        ScaleTransition ambientPulse = new ScaleTransition(Duration.millis(totalMs * 0.36), ambientOrb);
        ambientPulse.setFromX(0.92);
        ambientPulse.setFromY(0.92);
        ambientPulse.setToX(1.06);
        ambientPulse.setToY(1.06);
        ambientPulse.setAutoReverse(true);
        ambientPulse.setCycleCount(ScaleTransition.INDEFINITE);
        ambientPulse.setInterpolator(Interpolator.EASE_BOTH);
        ambientPulse.play();

        ScaleTransition logoScale = new ScaleTransition(Duration.millis(totalMs * 0.30), logoImage);
        logoScale.setToX(1.0);
        logoScale.setToY(1.0);
        logoScale.setDelay(Duration.millis(totalMs * 0.08));
        logoScale.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition logoFade = new FadeTransition(Duration.millis(totalMs * 0.21), logoImage);
        logoFade.setFromValue(0.0);
        logoFade.setToValue(1.0);
        logoFade.setDelay(Duration.millis(totalMs * 0.08));

        FadeTransition glowFade = new FadeTransition(Duration.millis(totalMs * 0.18), logoGlow);
        glowFade.setFromValue(0.0);
        glowFade.setToValue(1.0);
        glowFade.setDelay(Duration.millis(totalMs * 0.10));

        ScaleTransition glowPulse = new ScaleTransition(Duration.millis(totalMs * 0.26), logoGlow);
        glowPulse.setFromX(0.90);
        glowPulse.setFromY(0.90);
        glowPulse.setToX(1.10);
        glowPulse.setToY(1.10);
        glowPulse.setAutoReverse(true);
        glowPulse.setCycleCount(ScaleTransition.INDEFINITE);
        glowPulse.setInterpolator(Interpolator.EASE_BOTH);
        glowPulse.play();

        titleLabel.setTranslateY(24);
        FadeTransition titleFade = new FadeTransition(Duration.millis(totalMs * 0.25), titleLabel);
        titleFade.setFromValue(0.0);
        titleFade.setToValue(1.0);
        titleFade.setDelay(Duration.millis(totalMs * 0.34));

        javafx.animation.TranslateTransition titleSlide = new javafx.animation.TranslateTransition(Duration.millis(totalMs * 0.25), titleLabel);
        titleSlide.setFromY(24);
        titleSlide.setToY(0);
        titleSlide.setDelay(Duration.millis(totalMs * 0.34));
        titleSlide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition subtitleFade = new FadeTransition(Duration.millis(totalMs * 0.20), subtitleLabel);
        subtitleFade.setFromValue(0.0);
        subtitleFade.setToValue(1.0);
        subtitleFade.setDelay(Duration.millis(totalMs * 0.54));

        FadeTransition loadingTrackFade = new FadeTransition(Duration.millis(totalMs * 0.16), loadingTrack);
        loadingTrackFade.setFromValue(0.0);
        loadingTrackFade.setToValue(1.0);
        loadingTrackFade.setDelay(Duration.millis(totalMs * 0.20));

        ParallelTransition intro = new ParallelTransition(
                backgroundFade,
                ambientFade,
                logoScale,
                logoFade,
                glowFade,
                titleFade,
                titleSlide,
                subtitleFade,
                loadingTrackFade,
                loadingTimeline
        );

        double introEndMs = totalMs * 0.94;
        PauseTransition hold = new PauseTransition(Duration.millis(Math.max(0, totalMs - introEndMs)));

        SequentialTransition fullSequence = new SequentialTransition(intro, hold);
        fullSequence.setOnFinished(event -> {
            glowPulse.stop();
            ambientPulse.stop();
            stopParticleAnimations();
            onFinished.run();
        });
        fullSequence.play();
    }

    private void initializeVisualState() {
        splashRoot.setOpacity(1.0);
        backgroundPulse.setOpacity(0.0);
        ambientOrb.setOpacity(0.0);
        ambientOrb.setScaleX(1.0);
        ambientOrb.setScaleY(1.0);
        logoImage.setScaleX(0.5);
        logoImage.setScaleY(0.5);
        logoImage.setOpacity(0.0);
        logoGlow.setScaleX(0.9);
        logoGlow.setScaleY(0.9);
        logoGlow.setOpacity(0.0);
        titleLabel.setOpacity(0.0);
        subtitleLabel.setOpacity(0.0);
        loadingTrack.setOpacity(0.0);
        loadingLine.setPrefWidth(0.0);
    }

    private void createParticles(int count) {
        particlesLayer.getChildren().clear();
        stopParticleAnimations();

        double width = splashRoot.getPrefWidth() > 0 ? splashRoot.getPrefWidth() : 1460;
        double height = splashRoot.getPrefHeight() > 0 ? splashRoot.getPrefHeight() : 780;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            Circle particle = new Circle(random.nextDouble(1.2, 2.8));
            particle.setFill(Color.rgb(132, 214, 255, random.nextDouble(0.16, 0.42)));
            particle.setLayoutX(random.nextDouble(0, width));
            particle.setLayoutY(random.nextDouble(0, height));
            particlesLayer.getChildren().add(particle);

            Timeline particleTimeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(particle.opacityProperty(), random.nextDouble(0.14, 0.38)),
                            new KeyValue(particle.translateYProperty(), 0.0)),
                    new KeyFrame(Duration.millis(random.nextDouble(1700, 3200)),
                            new KeyValue(particle.opacityProperty(), random.nextDouble(0.28, 0.58)),
                            new KeyValue(particle.translateYProperty(), random.nextDouble(-24, 24), Interpolator.EASE_BOTH))
            );
            particleTimeline.setCycleCount(Timeline.INDEFINITE);
            particleTimeline.setAutoReverse(true);
            particleTimeline.play();
            particleAnimations.add(particleTimeline);
        }
    }

    private void stopParticleAnimations() {
        for (Timeline animation : particleAnimations) {
            animation.stop();
        }
        particleAnimations.clear();
    }

}
