package pi.assistant;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

public class AnimatedRobotPane extends StackPane {

    private final Circle pulseRing;
    private final Circle errorBadge;
    private final Label eyeLeft;
    private final Label eyeRight;
    private final ParallelTransition idleAnimation;

    public AnimatedRobotPane() {
        getStyleClass().add("agent-robot");
        setMinSize(68, 68);
        setPrefSize(68, 68);
        setMaxSize(68, 68);
        setAlignment(Pos.CENTER);

        pulseRing = new Circle(38);
        pulseRing.getStyleClass().add("agent-robot-pulse");
        pulseRing.setOpacity(0);

        Circle body = new Circle(30);
        body.getStyleClass().add("agent-robot-body");

        StackPane facePlate = new StackPane();
        facePlate.getStyleClass().add("agent-robot-faceplate");
        facePlate.setMaxSize(34, 24);
        facePlate.setPrefSize(34, 24);

        HBox eyes = new HBox(8);
        eyes.setAlignment(Pos.CENTER);
        eyeLeft = new Label("•");
        eyeRight = new Label("•");
        eyeLeft.getStyleClass().add("agent-robot-eye");
        eyeRight.getStyleClass().add("agent-robot-eye");
        eyes.getChildren().addAll(eyeLeft, eyeRight);
        facePlate.getChildren().add(eyes);

        errorBadge = new Circle(5);
        errorBadge.getStyleClass().add("agent-robot-error-badge");
        errorBadge.setVisible(false);
        StackPane.setAlignment(errorBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(errorBadge, new javafx.geometry.Insets(7, 8, 0, 0));

        getChildren().addAll(pulseRing, body, facePlate, errorBadge);

        TranslateTransition bob = new TranslateTransition(Duration.seconds(1.6), this);
        bob.setFromY(0);
        bob.setToY(-5);
        bob.setAutoReverse(true);
        bob.setCycleCount(Animation.INDEFINITE);

        ScaleTransition breathe = new ScaleTransition(Duration.seconds(1.8), this);
        breathe.setFromX(1.0);
        breathe.setToX(1.05);
        breathe.setFromY(1.0);
        breathe.setToY(1.05);
        breathe.setAutoReverse(true);
        breathe.setCycleCount(Animation.INDEFINITE);

        idleAnimation = new ParallelTransition(bob, breathe);
        idleAnimation.play();
    }

    public void setState(AgentUiState state) {
        errorBadge.setVisible(state == AgentUiState.ERROR);
        eyeLeft.setText(state == AgentUiState.TRANSCRIBING ? "·" : "•");
        eyeRight.setText(state == AgentUiState.TRANSCRIBING ? "·" : "•");
        if (state == AgentUiState.LISTENING || state == AgentUiState.TRANSCRIBING || state == AgentUiState.UNDERSTANDING || state == AgentUiState.EXECUTING) {
            playPulse();
        } else if (state == AgentUiState.ERROR) {
            playPulse();
        } else {
            pulseRing.setOpacity(0);
        }
    }

    private void playPulse() {
        pulseRing.setOpacity(0.65);
        pulseRing.setScaleX(1);
        pulseRing.setScaleY(1);
        FadeTransition fade = new FadeTransition(Duration.millis(650), pulseRing);
        fade.setFromValue(0.65);
        fade.setToValue(0.0);
        ScaleTransition scale = new ScaleTransition(Duration.millis(650), pulseRing);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.22);
        scale.setToY(1.22);
        new ParallelTransition(fade, scale).play();
    }

}
