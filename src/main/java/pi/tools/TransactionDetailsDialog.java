package pi.tools;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import pi.entities.Transaction;
import pi.entities.User;
import pi.mains.Main;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TransactionDetailsDialog {

    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final boolean ENABLE_BACKGROUND_BLUR = true;
    private static final double BACKGROUND_BLUR_RADIUS = 7.0;

    private TransactionDetailsDialog() {
    }

    public static void show(Stage owner, Transaction transaction) {
        if (owner == null || transaction == null) {
            return;
        }
        javafx.scene.Node ownerRoot = owner.getScene() != null ? owner.getScene().getRoot() : null;
        Effect previousEffect = applyOwnerBlur(ownerRoot);
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/transaction-details-dialog-view.fxml");
            Parent root = loader.getRoot();
            TransactionDetailsDialogController controller = loader.getController();

            bindTransaction(controller, transaction);

            Stage dialog = new Stage(StageStyle.TRANSPARENT);
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setResizable(false);
            dialog.setTitle("Transaction #" + transaction.getId());

            controller.getCloseButton().setOnAction(e -> closeWithAnimation(dialog, controller));
            controller.getOkButton().setOnAction(e -> closeWithAnimation(dialog, controller));

            Scene scene = new Scene(root);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/transaction-details-dialog.css");
            ThemeManager.registerScene(scene);
            dialog.setScene(scene);

            playOpenAnimation(controller);
            dialog.showAndWait();
        } catch (Exception e) {
            System.err.println("[TransactionDetailsDialog] Failed to display details: " + e.getMessage());
            e.printStackTrace();
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

    private static void bindTransaction(TransactionDetailsDialogController controller, Transaction transaction) {
        controller.getTitleLabel().setText("Transaction #" + transaction.getId());

        String type = normalize(transaction.getType());
        controller.getTypeBadge().setText(type.isEmpty() ? "-" : type);
        controller.getTypeBadge().getStyleClass().removeIf(style -> style.startsWith("tx-detail-badge-"));
        controller.getTypeBadge().getStyleClass().add(mapBadgeStyle(type));

        controller.getAmountValueLabel().setText(AMOUNT_FORMAT.format(transaction.getMontant()) + " DT");
        controller.getDateValueLabel().setText(transaction.getDate() == null ? "-" : transaction.getDate().format(DATE_FORMAT));

        User user = transaction.getUser();
        String userLine;
        if (user == null) {
            userLine = "-";
        } else {
            String userName = user.getNom() != null && !user.getNom().isBlank() ? user.getNom() : "-";
            String userEmail = user.getEmail() != null && !user.getEmail().isBlank() ? user.getEmail() : "-";
            userLine = userName + " / " + userEmail;
        }
        controller.getUserValueLabel().setText(userLine);
        controller.getDescriptionValueLabel().setText(normalizeOrDash(transaction.getDescription()));
        controller.getSourceValueLabel().setText(normalizeOrDash(transaction.getModuleSource()));
    }

    private static String mapBadgeStyle(String type) {
        return switch (type) {
            case "EXPENSE" -> "tx-detail-badge-expense";
            case "SAVING" -> "tx-detail-badge-saving";
            case "INVESTMENT" -> "tx-detail-badge-investment";
            default -> "tx-detail-badge-default";
        };
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeOrDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value.trim();
    }

    private static void playOpenAnimation(TransactionDetailsDialogController controller) {
        if (controller == null || controller.getOverlayRoot() == null || controller.getModalCard() == null) {
            return;
        }
        controller.getOverlayRoot().setOpacity(0.0);
        controller.getModalCard().setScaleX(0.96);
        controller.getModalCard().setScaleY(0.96);
        controller.getModalCard().setTranslateY(8);

        FadeTransition fade = new FadeTransition(Duration.millis(170), controller.getOverlayRoot());
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(180), controller.getModalCard());
        scale.setFromX(0.96);
        scale.setFromY(0.96);
        scale.setToX(1.0);
        scale.setToY(1.0);

        TranslateTransition rise = new TranslateTransition(Duration.millis(180), controller.getModalCard());
        rise.setFromY(8);
        rise.setToY(0);

        new ParallelTransition(fade, scale, rise).play();
    }

    private static void closeWithAnimation(Stage dialog, TransactionDetailsDialogController controller) {
        if (dialog == null || controller == null || controller.getOverlayRoot() == null || controller.getModalCard() == null) {
            if (dialog != null) {
                dialog.close();
            }
            return;
        }

        FadeTransition fade = new FadeTransition(Duration.millis(130), controller.getOverlayRoot());
        fade.setFromValue(controller.getOverlayRoot().getOpacity() <= 0 ? 1.0 : controller.getOverlayRoot().getOpacity());
        fade.setToValue(0.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(130), controller.getModalCard());
        scale.setFromX(controller.getModalCard().getScaleX() <= 0 ? 1.0 : controller.getModalCard().getScaleX());
        scale.setFromY(controller.getModalCard().getScaleY() <= 0 ? 1.0 : controller.getModalCard().getScaleY());
        scale.setToX(0.97);
        scale.setToY(0.97);

        ParallelTransition closeAnim = new ParallelTransition(fade, scale);
        closeAnim.setOnFinished(e -> dialog.close());
        closeAnim.play();
    }
}
