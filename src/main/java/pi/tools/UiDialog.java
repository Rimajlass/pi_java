package pi.tools;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import pi.mains.Main;

import java.util.Optional;

public final class UiDialog {
    private static final boolean ENABLE_BACKGROUND_BLUR = false;
    private static final double BACKGROUND_BLUR_RADIUS = 7.0;

    public enum Type {
        INFO("i", "dialog-icon-info"),
        ERROR("!", "dialog-icon-error"),
        SUCCESS("\u2713", "dialog-icon-success"),
        WARNING("!", "dialog-icon-warning"),
        DELETE("D", "dialog-icon-delete"),
        CONFIRM("?", "dialog-icon-confirm");

        final String iconText;
        final String iconClass;

        Type(String iconText, String iconClass) {
            this.iconText = iconText;
            this.iconClass = iconClass;
        }
    }

    private UiDialog() {
    }

    public static void show(Stage owner, Type type, String title, String heading, String message) {
        showDialog(owner, type, title, heading, message, null, false);
    }

    public static void showCustomMessage(Stage owner, String title, String heading, String message, Type type) {
        showDialog(owner, type, title, heading, message, null, false);
    }

    public static void showCustomMessage(Stage owner, String title, String heading, String message, String hint, Type type) {
        showDialog(owner, type, title, heading, message, hint, false);
    }

    public static boolean showConfirm(Stage owner, String title, String heading, String message) {
        return showDialog(owner, Type.CONFIRM, title, heading, message, null, true)
                .orElse(false);
    }

    public static void success(Stage owner, String title, String message) {
        show(owner, Type.SUCCESS, title, title, message);
    }

    public static void error(Stage owner, String title, String message) {
        show(owner, Type.ERROR, title, title, message);
    }

    public static void info(Stage owner, String title, String message) {
        show(owner, Type.INFO, title, title, message);
    }

    public static void deleted(Stage owner, String title, String message) {
        show(owner, Type.DELETE, title, title, message);
    }

    private static Optional<Boolean> showDialog(
            Stage owner,
            Type type,
            String title,
            String heading,
            String message,
            String hint,
            boolean confirmMode
    ) {
        Stage resolvedOwner = resolveOwner(owner);
        javafx.scene.Node ownerRoot = resolvedOwner != null && resolvedOwner.getScene() != null
                ? resolvedOwner.getScene().getRoot()
                : null;
        Effect previousEffect = applyOwnerBlur(ownerRoot);
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/ui-dialog-view.fxml");
            Parent root = loader.getRoot();
            UiDialogController controller = loader.getController();

            Stage dialog = new Stage(StageStyle.TRANSPARENT);
            if (resolvedOwner != null) {
                dialog.initOwner(resolvedOwner);
            }
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setResizable(false);
            dialog.setTitle(title == null ? "Message" : title);

            controller.getIconLabel().setText(type.iconText);
            controller.getIconLabel().getStyleClass().removeIf(style -> style.startsWith("dialog-icon-"));
            controller.getIconLabel().getStyleClass().add(type.iconClass);
            controller.getDialogCard().getStyleClass().removeIf(style -> style.startsWith("dialog-type-"));
            controller.getDialogCard().getStyleClass().add("dialog-type-" + type.name().toLowerCase());
            controller.getOkButton().getStyleClass().removeIf(style -> style.startsWith("dialog-ok-"));
            controller.getOkButton().getStyleClass().add("dialog-ok-" + type.name().toLowerCase());
            controller.getHeadingLabel().setText(heading == null ? "" : heading);
            controller.getMessageLabel().setText(message == null ? "" : message);
            if (hint != null && !hint.isBlank()) {
                controller.getHintLabel().setText(hint);
                controller.getHintBox().setVisible(true);
                controller.getHintBox().setManaged(true);
            } else {
                controller.getHintBox().setVisible(false);
                controller.getHintBox().setManaged(false);
            }

            final boolean[] confirmed = new boolean[]{false};
            controller.getCloseButton().setOnAction(e -> closeWithAnimation(dialog, controller, false, confirmed));
            controller.getOkButton().setOnAction(e -> {
                closeWithAnimation(dialog, controller, true, confirmed);
            });
            controller.getCancelButton().setOnAction(e -> {
                closeWithAnimation(dialog, controller, false, confirmed);
            });

            controller.getCancelButton().setVisible(confirmMode);
            controller.getCancelButton().setManaged(confirmMode);
            controller.getOkButton().setText(confirmMode ? "Confirm" : "OK");

            Scene scene = new Scene(root);
            scene.setFill(null);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/ui-dialog.css");
            ThemeManager.registerScene(scene);
            dialog.setScene(scene);

            playOpenAnimation(controller);
            dialog.showAndWait();
            return Optional.of(confirmed[0]);
        } catch (Exception e) {
            System.err.println("[UiDialog] Failed to display dialog: " + e.getMessage());
            e.printStackTrace();
            return transparentFallback(resolvedOwner, title, heading, message, confirmMode);
        } finally {
            restoreOwnerEffect(ownerRoot, previousEffect);
        }
    }

    private static Effect applyOwnerBlur(javafx.scene.Node ownerRoot) {
        if (!ENABLE_BACKGROUND_BLUR || ownerRoot == null) {
            return null;
        }
        Effect previous = ownerRoot.getEffect();
        ownerRoot.setEffect(new GaussianBlur(BACKGROUND_BLUR_RADIUS));
        return previous;
    }

    private static void restoreOwnerEffect(javafx.scene.Node ownerRoot, Effect previousEffect) {
        if (ownerRoot == null) {
            return;
        }
        ownerRoot.setEffect(previousEffect);
    }

    private static void playOpenAnimation(UiDialogController controller) {
        if (controller == null || controller.getDialogCard() == null || controller.getOverlayRoot() == null) {
            return;
        }
        controller.getOverlayRoot().setOpacity(0.0);
        controller.getDialogCard().setScaleX(0.96);
        controller.getDialogCard().setScaleY(0.96);

        FadeTransition fade = new FadeTransition(Duration.millis(160), controller.getOverlayRoot());
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(180), controller.getDialogCard());
        scale.setFromX(0.96);
        scale.setFromY(0.96);
        scale.setToX(1.0);
        scale.setToY(1.0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(180), controller.getDialogCard());
        slide.setFromY(8);
        slide.setToY(0);

        new ParallelTransition(fade, scale, slide).play();
    }

    private static Stage resolveOwner(Stage owner) {
        if (owner != null) {
            return owner;
        }
        for (Window window : Window.getWindows()) {
            if (window instanceof Stage stage && stage.isShowing()) {
                return stage;
            }
        }
        return null;
    }

    private static void closeWithAnimation(Stage dialog, UiDialogController controller, boolean confirmValue, boolean[] output) {
        if (dialog == null || controller == null || controller.getOverlayRoot() == null || controller.getDialogCard() == null) {
            if (output != null && output.length > 0) {
                output[0] = confirmValue;
            }
            if (dialog != null) {
                dialog.close();
            }
            return;
        }
        if (output != null && output.length > 0) {
            output[0] = confirmValue;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(130), controller.getOverlayRoot());
        fade.setFromValue(controller.getOverlayRoot().getOpacity() <= 0 ? 1.0 : controller.getOverlayRoot().getOpacity());
        fade.setToValue(0.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(130), controller.getDialogCard());
        scale.setFromX(controller.getDialogCard().getScaleX() <= 0 ? 1.0 : controller.getDialogCard().getScaleX());
        scale.setFromY(controller.getDialogCard().getScaleY() <= 0 ? 1.0 : controller.getDialogCard().getScaleY());
        scale.setToX(0.97);
        scale.setToY(0.97);

        ParallelTransition closeAnim = new ParallelTransition(fade, scale);
        closeAnim.setOnFinished(e -> dialog.close());
        closeAnim.play();
    }

    private static Optional<Boolean> transparentFallback(
            Stage owner,
            String title,
            String heading,
            String message,
            boolean confirmMode
    ) {
        try {
            Stage dialog = new Stage(StageStyle.TRANSPARENT);
            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setResizable(false);
            dialog.setTitle(title == null ? "Message" : title);

            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-background-color: transparent;");

            VBox card = new VBox(12);
            card.setMaxWidth(420);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-radius: 20; -fx-padding: 16; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.24), 28, 0.2, 0, 8);");

            Label headingLabel = new Label(heading == null ? "" : heading);
            headingLabel.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #0F172A;");
            Label bodyLabel = new Label(message == null ? "" : message);
            bodyLabel.setWrapText(true);
            bodyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748B;");

            HBox actions = new HBox(8);
            actions.setAlignment(Pos.CENTER_RIGHT);
            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            Button cancel = new Button("Cancel");
            Button ok = new Button(confirmMode ? "Confirm" : "OK");
            cancel.setStyle("-fx-background-color: transparent; -fx-border-color: #CBD5E1; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 8 14;");
            ok.setStyle("-fx-background-color: linear-gradient(to right, #3B82F6, #2563EB); -fx-text-fill: white; -fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 8 14;");
            final boolean[] confirmed = new boolean[]{false};
            cancel.setOnAction(e -> {
                confirmed[0] = false;
                dialog.close();
            });
            ok.setOnAction(e -> {
                confirmed[0] = true;
                dialog.close();
            });

            actions.getChildren().add(spacer);
            if (confirmMode) {
                actions.getChildren().add(cancel);
            }
            actions.getChildren().add(ok);

            card.getChildren().addAll(headingLabel, bodyLabel, actions);
            StackPane.setMargin(card, new Insets(20));
            overlay.getChildren().add(card);

            Scene scene = new Scene(overlay, javafx.scene.paint.Color.TRANSPARENT);
            dialog.setScene(scene);
            dialog.showAndWait();
            return Optional.of(confirmed[0]);
        } catch (Exception ex) {
            return Optional.of(!confirmMode);
        }
    }
}
