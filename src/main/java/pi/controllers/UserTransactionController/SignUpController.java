package pi.controllers.UserTransactionController;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import pi.mains.Main;
import pi.services.UserTransactionService.FacePlusPlusService;
import pi.services.UserTransactionService.WebAuthnDesktopService;
import pi.services.UserTransactionService.WebcamCaptureService;
import pi.tools.QrCodeUtil;
import pi.tools.UiDialog;

import java.io.IOException;
import java.nio.file.Path;

public class SignUpController {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    private TextField balanceField;

    @FXML
    private TextField captchaField;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Hyperlink signInLink;

    @FXML
    private ImageView backgroundImageView;

    @FXML
    private VBox heroContentBox;

    @FXML
    private Label heroAccentLabel;

    @FXML
    private VBox signUpCardBox;

    @FXML
    private Button registerButton;

    @FXML
    private Button faceIdButton;

    @FXML
    private Button passkeyButton;

    @FXML
    private Button openCameraButton;

    @FXML
    private Button captureCameraButton;

    @FXML
    private Button stopCameraButton;

    @FXML
    private Pane cameraPreviewPane;

    private final UserController userController = new UserController();
    private FacePlusPlusService facePlusPlusService;
    private WebAuthnDesktopService webAuthnDesktopService;
    private final WebcamCaptureService webcamCaptureService = new WebcamCaptureService();
    private boolean faceEnrollmentRunning = false;
    private boolean passkeyEnrollmentRunning = false;
    private boolean biometricEnrollmentRequested = false;
    private boolean passkeyEnrollmentRequested = false;
    private String pendingFacePlusToken;
    private String pendingPasskeyCredentialId;
    private ImageView cameraPreviewView;
    private boolean cameraRunning = false;
    private Stage passkeyQrStage;

    @FXML
    public void initialize() {
        roleComboBox.getItems().setAll("ETUDIANT", "SALARY", "ADMIN");
        roleComboBox.setPromptText("Choose a role");
        balanceField.setText("0");
        feedbackLabel.setText("Creez votre compte pour acceder a l'espace securise.");
        signInLink.setOnAction(event -> openLoginScene(null));

        if (cameraPreviewPane != null) {
            cameraPreviewView = new ImageView();
            cameraPreviewView.setPreserveRatio(false);
            cameraPreviewView.fitWidthProperty().bind(cameraPreviewPane.widthProperty());
            cameraPreviewView.fitHeightProperty().bind(cameraPreviewPane.heightProperty());
            cameraPreviewPane.getChildren().setAll(cameraPreviewView);
        }
        updateCameraButtonsState();
        Platform.runLater(this::startPremiumAnimations);
    }

    @FXML
    private void handleEnableFaceCamera() {
        startLiveCameraForEnrollment();
    }

    @FXML
    private void handleEnablePasskeyQr() {
        startPasskeyEnrollment();
    }

    @FXML
    private void handleOpenCamera() {
        startLiveCameraForEnrollment();
    }

    @FXML
    private void handleCaptureAndRegister() {
        captureAndRegisterFace();
    }

    @FXML
    private void handleStopCamera() {
        stopLiveCamera();
    }

    @FXML
    private void handleRegister() {
        try {
            validateCaptcha();
            validateBiometricBeforeRegister();

            double balance = parseBalance();
            String email = emailField.getText() == null ? "" : emailField.getText().trim().toLowerCase();
            userController.register(
                    fullNameField.getText(),
                    email,
                    passwordField.getText(),
                    confirmPasswordField.getText(),
                    validateRole(),
                    balance,
                    pendingPasskeyCredentialId,
                    pendingFacePlusToken
            );

            feedbackLabel.getStyleClass().remove("feedback-error");
            if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
                feedbackLabel.getStyleClass().add("feedback-success");
            }
            feedbackLabel.setText("Compte cree avec succes en base de donnees.");
            UiDialog.success(resolveStage(), "Registration", "The account " + email + " was created successfully.");
            openLoginScene(email);
        } catch (Exception e) {
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText(e.getMessage());
            UiDialog.error(resolveStage(), "Registration", e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin() {
        openLoginScene(null);
    }

    private void validateCaptcha() {
        String captcha = captchaField.getText() == null ? "" : captchaField.getText().trim();
        if (!"QFDG".equalsIgnoreCase(captcha)) {
            throw new IllegalArgumentException("Captcha invalide. Tape QFDG.");
        }
    }

    private double parseBalance() {
        try {
            return Double.parseDouble(balanceField.getText().trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Initial balance invalide.");
        }
    }

    private String validateRole() {
        String role = roleComboBox.getValue();
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("Choisis un role: ETUDIANT, SALARY ou ADMIN.");
        }
        return role;
    }

    private void startLiveCameraForEnrollment() {
        biometricEnrollmentRequested = true;
        if (cameraRunning) {
            return;
        }
        if (cameraPreviewView == null) {
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText("Zone de preview camera non initialisee.");
            return;
        }
        try {
            webcamCaptureService.startPreview(cameraPreviewView);
            cameraRunning = true;
            updateCameraButtonsState();
            feedbackLabel.getStyleClass().remove("feedback-error");
            if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
                feedbackLabel.getStyleClass().add("feedback-success");
            }
            feedbackLabel.setText("Camera live activee. Cliquez sur 'Capture and register'.");
        } catch (Exception e) {
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText("Impossible de demarrer la camera: " + e.getMessage());
        }
    }

    private void captureAndRegisterFace() {
        if (faceEnrollmentRunning) {
            return;
        }
        if (!cameraRunning) {
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText("Ouvrez d'abord la camera.");
            return;
        }

        faceEnrollmentRunning = true;
        setFaceIdButtonsDisabled(true);
        feedbackLabel.getStyleClass().remove("feedback-success");
        if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
            feedbackLabel.getStyleClass().add("feedback-error");
        }
        feedbackLabel.setText("Enregistrement du visage via Face++...");

        Path imagePath;
        try {
            imagePath = webcamCaptureService.captureFrameToTempJpg();
        } catch (Exception e) {
            faceEnrollmentRunning = false;
            setFaceIdButtonsDisabled(false);
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText("Capture impossible: " + e.getMessage());
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                String faceToken = facePlusPlusService().extractFaceToken(imagePath);
                Platform.runLater(() -> {
                    pendingFacePlusToken = faceToken;
                    faceIdButton.setText("Face ID enabled on account");
                    feedbackLabel.getStyleClass().remove("feedback-error");
                    if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
                        feedbackLabel.getStyleClass().add("feedback-success");
                    }
                    feedbackLabel.setText("Visage enregistre. Cliquez maintenant sur CREATE ACCOUNT.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    feedbackLabel.getStyleClass().remove("feedback-success");
                    if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                        feedbackLabel.getStyleClass().add("feedback-error");
                    }
                    feedbackLabel.setText("Echec d'enregistrement du visage: " + e.getMessage());
                });
            } finally {
                Platform.runLater(() -> {
                    faceEnrollmentRunning = false;
                    setFaceIdButtonsDisabled(false);
                });
            }
        }, "facepp-enroll");
        worker.setDaemon(true);
        worker.start();
    }

    private void startPasskeyEnrollment() {
        if (passkeyEnrollmentRunning) {
            return;
        }
        String email = emailField.getText() == null ? null : emailField.getText().trim().toLowerCase();
        if (email == null || email.isBlank()) {
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText("Saisissez d'abord votre email, puis cliquez sur Enable Passkey (QR).");
            return;
        }

        passkeyEnrollmentRequested = true;
        passkeyEnrollmentRunning = true;
        setFaceIdButtonsDisabled(true);
        feedbackLabel.getStyleClass().remove("feedback-success");
        if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
            feedbackLabel.getStyleClass().add("feedback-error");
        }
        feedbackLabel.setText("Ouverture de l'enregistrement Passkey (QR)...");

        WebAuthnDesktopService.PendingPasskeyFlow flow;
        try {
            flow = webAuthnDesktopService().startEnrollFlow(email);
            showPasskeyQrDialog(flow, "Scannez ce QR pour enregistrer la cle d'acces");
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText("QR affiche. Scannez avec votre telephone pour terminer l'enregistrement.");
        } catch (Exception e) {
            passkeyEnrollmentRunning = false;
            setFaceIdButtonsDisabled(false);
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText("Echec d'ouverture du QR Passkey: " + e.getMessage());
            UiDialog.error(resolveStage(), "Passkey", "Echec d'ouverture du QR Passkey: " + e.getMessage());
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                String credentialId = flow.awaitCredential(java.time.Duration.ofSeconds(120));
                Platform.runLater(() -> {
                    pendingPasskeyCredentialId = credentialId;
                    passkeyButton.setText("Passkey enabled on this account");
                    closePasskeyQrDialog();
                    feedbackLabel.getStyleClass().remove("feedback-error");
                    if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
                        feedbackLabel.getStyleClass().add("feedback-success");
                    }
                    feedbackLabel.setText("Passkey enregistre. Vous pouvez creer le compte.");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    closePasskeyQrDialog();
                    feedbackLabel.getStyleClass().remove("feedback-success");
                    if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                        feedbackLabel.getStyleClass().add("feedback-error");
                    }
                    feedbackLabel.setText("Echec d'enregistrement Passkey: " + e.getMessage());
                });
            } finally {
                flow.close();
                Platform.runLater(() -> {
                    passkeyEnrollmentRunning = false;
                    setFaceIdButtonsDisabled(false);
                });
            }
        }, "passkey-enroll");
        worker.setDaemon(true);
        worker.start();
    }

    private void showPasskeyQrDialog(WebAuthnDesktopService.PendingPasskeyFlow flow, String title) {
        closePasskeyQrDialog();
        Stage owner = resolveStage();
        Stage dialog = new Stage();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.initModality(Modality.NONE);
        dialog.setTitle("Passkey QR");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700;");

        VBox qrBox = new VBox(6);
        try {
            ImageView qrImage = new ImageView(QrCodeUtil.renderQr(flow.qrUrl(), 260));
            qrImage.setFitWidth(260);
            qrImage.setFitHeight(260);
            qrBox.getChildren().add(qrImage);
        } catch (Throwable t) {
            Label fallback = new Label("QR indisponible. Utilisez l'URL ci-dessous.");
            fallback.setWrapText(true);
            qrBox.getChildren().add(fallback);
        }

        Label hintLabel = new Label("Scannez avec votre telephone (meme reseau Wi-Fi) puis validez la biometrie.");
        hintLabel.setWrapText(true);

        TextField urlField = new TextField(flow.qrUrl());
        urlField.setEditable(false);

        Button openLocalButton = new Button("Open on this PC");
        openLocalButton.setOnAction(e -> flow.openLocalBrowser());
        Button closeButton = new Button("Close");
        closeButton.setOnAction(e -> dialog.close());
        HBox actions = new HBox(8, openLocalButton, closeButton);

        VBox root = new VBox(10, titleLabel, qrBox, hintLabel, urlField, actions);
        root.setStyle("-fx-padding: 14;");

        dialog.setScene(new Scene(root, 360, 430));
        dialog.show();
        dialog.toFront();
        passkeyQrStage = dialog;
    }

    private void closePasskeyQrDialog() {
        if (passkeyQrStage != null) {
            passkeyQrStage.close();
            passkeyQrStage = null;
        }
    }

    private void stopLiveCamera() {
        webcamCaptureService.stop();
        cameraRunning = false;
        updateCameraButtonsState();
        faceIdButton.setText("Enable Face ID on this device");
        if (pendingFacePlusToken != null && !pendingFacePlusToken.isBlank()) {
            feedbackLabel.getStyleClass().remove("feedback-error");
            if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
                feedbackLabel.getStyleClass().add("feedback-success");
            }
            feedbackLabel.setText("Camera arretee. Le token visage est conserve; vous pouvez creer le compte.");
            return;
        }
        feedbackLabel.getStyleClass().remove("feedback-success");
        if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
            feedbackLabel.getStyleClass().add("feedback-error");
        }
        feedbackLabel.setText("Camera arretee.");
    }

    private void validateBiometricBeforeRegister() {
        if (!biometricEnrollmentRequested) {
            if (!passkeyEnrollmentRequested) {
                return;
            }
        }
        if (pendingFacePlusToken == null || pendingFacePlusToken.isBlank()) {
            if (biometricEnrollmentRequested) {
                throw new IllegalStateException("Biometric enrollment not finished. Click Open camera, then Capture and register.");
            }
        }
        if (passkeyEnrollmentRequested && (pendingPasskeyCredentialId == null || pendingPasskeyCredentialId.isBlank())) {
            throw new IllegalStateException("Passkey enrollment not finished. Click Enable Passkey (QR) and complete the browser step.");
        }
    }

    private FacePlusPlusService facePlusPlusService() {
        if (facePlusPlusService == null) {
            facePlusPlusService = new FacePlusPlusService();
        }
        return facePlusPlusService;
    }

    private WebAuthnDesktopService webAuthnDesktopService() {
        if (webAuthnDesktopService == null) {
            webAuthnDesktopService = new WebAuthnDesktopService();
        }
        return webAuthnDesktopService;
    }

    private void setFaceIdButtonsDisabled(boolean disabled) {
        faceIdButton.setDisable(disabled || cameraRunning || passkeyEnrollmentRunning);
        passkeyButton.setDisable(disabled || passkeyEnrollmentRunning || faceEnrollmentRunning);
        openCameraButton.setDisable(disabled || cameraRunning || passkeyEnrollmentRunning);
        captureCameraButton.setDisable(disabled || !cameraRunning || passkeyEnrollmentRunning);
        stopCameraButton.setDisable(disabled || !cameraRunning || passkeyEnrollmentRunning);
    }

    private void updateCameraButtonsState() {
        faceIdButton.setDisable(cameraRunning || faceEnrollmentRunning || passkeyEnrollmentRunning);
        passkeyButton.setDisable(faceEnrollmentRunning || passkeyEnrollmentRunning);
        openCameraButton.setDisable(cameraRunning || faceEnrollmentRunning || passkeyEnrollmentRunning);
        captureCameraButton.setDisable(!cameraRunning || faceEnrollmentRunning || passkeyEnrollmentRunning);
        stopCameraButton.setDisable(!cameraRunning || faceEnrollmentRunning || passkeyEnrollmentRunning);
    }

    private void openLoginScene(String registeredEmail) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/login-view.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            if (registeredEmail != null && !registeredEmail.isBlank()) {
                controller.showRegistrationSuccess(registeredEmail);
            }

            Stage stage = (Stage) signInLink.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/login.css").toExternalForm());
            stage.setTitle("User Secure Login");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page demandee.", e);
        }
    }

    private Stage resolveStage() {
        if (signInLink != null && signInLink.getScene() != null && signInLink.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    private void startPremiumAnimations() {
        playIntroAnimations();
        playContinuousAnimations();
        installHoverAnimations();
        installFocusAnimations();
    }

    private void playIntroAnimations() {
        if (heroContentBox != null) {
            heroContentBox.setOpacity(0.0);
            heroContentBox.setTranslateY(heroContentBox.getTranslateY() + 12);
            FadeTransition fade = new FadeTransition(Duration.millis(560), heroContentBox);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            TranslateTransition slide = new TranslateTransition(Duration.millis(560), heroContentBox);
            slide.setFromY(heroContentBox.getTranslateY());
            slide.setToY(heroContentBox.getTranslateY() - 12);
            new ParallelTransition(fade, slide).play();
        }

        if (signUpCardBox != null) {
            signUpCardBox.setOpacity(0.0);
            signUpCardBox.setTranslateY(signUpCardBox.getTranslateY() + 16);
            FadeTransition fade = new FadeTransition(Duration.millis(620), signUpCardBox);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            TranslateTransition slide = new TranslateTransition(Duration.millis(620), signUpCardBox);
            slide.setFromY(signUpCardBox.getTranslateY());
            slide.setToY(signUpCardBox.getTranslateY() - 16);
            new ParallelTransition(fade, slide).play();
        }
    }

    private void playContinuousAnimations() {
        if (backgroundImageView != null) {
            ScaleTransition zoom = new ScaleTransition(Duration.seconds(30), backgroundImageView);
            zoom.setFromX(1.0);
            zoom.setFromY(1.0);
            zoom.setToX(1.04);
            zoom.setToY(1.04);
            zoom.setAutoReverse(true);
            zoom.setCycleCount(Timeline.INDEFINITE);
            zoom.setInterpolator(Interpolator.EASE_BOTH);
            zoom.play();
        }

        if (heroAccentLabel != null) {
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(heroAccentLabel.opacityProperty(), 1.0, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(2.2), new KeyValue(heroAccentLabel.opacityProperty(), 0.76, Interpolator.EASE_BOTH))
            );
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();
        }

        if (signUpCardBox != null) {
            DropShadow cardGlow = new DropShadow(28, Color.rgb(58, 141, 255, 0.18));
            cardGlow.setOffsetY(8);
            cardGlow.setSpread(0.08);
            signUpCardBox.setEffect(cardGlow);

            Timeline breath = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(signUpCardBox.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                            new KeyValue(signUpCardBox.scaleYProperty(), 1.0, Interpolator.EASE_BOTH),
                            new KeyValue(cardGlow.radiusProperty(), 24, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(3.2),
                            new KeyValue(signUpCardBox.scaleXProperty(), 1.01, Interpolator.EASE_BOTH),
                            new KeyValue(signUpCardBox.scaleYProperty(), 1.01, Interpolator.EASE_BOTH),
                            new KeyValue(cardGlow.radiusProperty(), 32, Interpolator.EASE_BOTH))
            );
            breath.setAutoReverse(true);
            breath.setCycleCount(Timeline.INDEFINITE);
            breath.play();
        }

        if (registerButton != null) {
            DropShadow btnGlow = new DropShadow(14, Color.rgb(58, 141, 255, 0.28));
            registerButton.setEffect(btnGlow);
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(btnGlow.radiusProperty(), 11, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(1.8), new KeyValue(btnGlow.radiusProperty(), 18, Interpolator.EASE_BOTH))
            );
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.play();
        }
    }

    private void installHoverAnimations() {
        installHoverLift(registerButton, 1.03, -1.4);
        installHoverLift(faceIdButton, 1.02, -1.0);
        installHoverLift(passkeyButton, 1.02, -1.0);
        installHoverLift(openCameraButton, 1.02, -0.8);
        installHoverLift(captureCameraButton, 1.02, -0.8);
        installHoverLift(stopCameraButton, 1.02, -0.8);
    }

    private void installFocusAnimations() {
        installFocusScale(fullNameField);
        installFocusScale(emailField);
        installFocusScale(passwordField);
        installFocusScale(confirmPasswordField);
        installFocusScale(balanceField);
        installFocusScale(captchaField);
    }

    private void installHoverLift(Node node, double scaleValue, double translateY) {
        if (node == null) {
            return;
        }
        node.setOnMouseEntered(event -> animateNode(node, scaleValue, translateY, 140));
        node.setOnMouseExited(event -> animateNode(node, 1.0, 0.0, 140));
    }

    private void installFocusScale(TextField field) {
        if (field == null) {
            return;
        }
        field.focusedProperty().addListener((obs, oldValue, focused) -> animateNode(field, focused ? 1.01 : 1.0, 0.0, 110));
    }

    private void animateNode(Node node, double scale, double translateY, int durationMs) {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(durationMs), node);
        scaleTransition.setToX(scale);
        scaleTransition.setToY(scale);
        scaleTransition.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition liftTransition = new TranslateTransition(Duration.millis(durationMs), node);
        liftTransition.setToY(translateY);
        liftTransition.setInterpolator(Interpolator.EASE_BOTH);

        new ParallelTransition(scaleTransition, liftTransition).play();
    }
}
