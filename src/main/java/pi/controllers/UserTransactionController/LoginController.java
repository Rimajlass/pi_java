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
import javafx.scene.control.CheckBox;
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
import pi.controllers.CoursQuizController.CoursQuizFrontController;
import pi.entities.User;
import pi.mains.Main;
import pi.services.UserTransactionService.FacePlusPlusService;
import pi.services.UserTransactionService.PasswordResetService;
import pi.services.UserTransactionService.SocialAuthService;
import pi.services.UserTransactionService.WebAuthnDesktopService;
import pi.services.UserTransactionService.WebcamCaptureService;
import pi.tools.AdminNavigation;
import pi.tools.FxmlResources;
import pi.tools.QrCodeUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.prefs.Preferences;
import java.util.function.Supplier;

public class LoginController {
    private static final String PREF_NODE = "pi.decides.login";
    private static final String PREF_REMEMBER_ME = "remember_me";
    private static final String PREF_REMEMBERED_EMAIL = "remembered_email";
    private static final String PREF_REMEMBERED_PASSWORD = "remembered_password";

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label statusPill;

    @FXML
    private CheckBox rememberMeCheckBox;

    @FXML
    private Hyperlink biometricLink;

    @FXML
    private Hyperlink passkeyLink;

    @FXML
    private ImageView backgroundImageView;

    @FXML
    private ImageView heroLogoImage;

    @FXML
    private Label heroSecurelyLabel;

    @FXML
    private VBox heroContentBox;

    @FXML
    private VBox loginCardBox;

    @FXML
    private Button signInButton;

    @FXML
    private Button googleButton;

    @FXML
    private Button facebookButton;

    @FXML
    private Pane cameraPreviewPane;

    @FXML
    private Button openCameraButton;

    @FXML
    private Button captureLoginButton;

    @FXML
    private Button stopCameraButton;

    private UserController userController;
    private PasswordResetService passwordResetService;
    private SocialAuthService socialAuthService;
    private WebAuthnDesktopService webAuthnDesktopService;
    private final WebcamCaptureService webcamCaptureService = new WebcamCaptureService();
    private FacePlusPlusService facePlusPlusService;
    private ImageView cameraPreviewView;
    private boolean cameraRunning = false;
    private boolean biometricLoginRunning = false;
    private boolean socialAuthReady = true;
    private Stage passkeyQrStage;
    private final Preferences preferences = Preferences.userRoot().node(PREF_NODE);

    @FXML
    public void initialize() {
        restoreRememberedCredentials();
        passwordField.setText("");
        feedbackLabel.setText("Saisissez vos identifiants pour acceder a votre espace.");
        welcomeLabel.setText("Bienvenue dans votre espace securise");
        statusPill.setText("Acces au compte");
        biometricLink.setOnAction(event -> handleBiometricSignIn());
        passkeyLink.setOnAction(event -> handlePasskeySignIn());
        openCameraButton.setOnAction(event -> startLiveCameraForLogin());
        captureLoginButton.setOnAction(event -> handleBiometricSignIn());
        stopCameraButton.setOnAction(event -> stopLiveCamera());

        if (cameraPreviewPane != null) {
            cameraPreviewView = new ImageView();
            cameraPreviewView.setPreserveRatio(false);
            cameraPreviewView.fitWidthProperty().bind(cameraPreviewPane.widthProperty());
            cameraPreviewView.fitHeightProperty().bind(cameraPreviewPane.heightProperty());
            cameraPreviewPane.getChildren().setAll(cameraPreviewView);
        }

        updateCameraButtonsState();
        validateOAuthConfigurationOnStartup();
        Platform.runLater(this::startPremiumAnimations);
    }

    private void restoreRememberedCredentials() {
        boolean remember = preferences.getBoolean(PREF_REMEMBER_ME, false);
        String rememberedEmail = preferences.get(PREF_REMEMBERED_EMAIL, "");
        String rememberedPassword = preferences.get(PREF_REMEMBERED_PASSWORD, "");

        rememberMeCheckBox.setSelected(remember);
        if (remember && rememberedEmail != null && !rememberedEmail.isBlank()) {
            emailField.setText(rememberedEmail);
            passwordField.setText(decodePreferenceValue(rememberedPassword));
        } else {
            emailField.clear();
            passwordField.clear();
        }
    }

    private void persistRememberMeChoice() {
        boolean remember = rememberMeCheckBox != null && rememberMeCheckBox.isSelected();
        if (remember) {
            preferences.putBoolean(PREF_REMEMBER_ME, true);
            preferences.put(PREF_REMEMBERED_EMAIL, emailField.getText() == null ? "" : emailField.getText().trim());
            preferences.put(PREF_REMEMBERED_PASSWORD, encodePreferenceValue(passwordField.getText() == null ? "" : passwordField.getText()));
        } else {
            preferences.putBoolean(PREF_REMEMBER_ME, false);
            preferences.remove(PREF_REMEMBERED_EMAIL);
            preferences.remove(PREF_REMEMBERED_PASSWORD);
        }
    }

    private String encodePreferenceValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decodePreferenceValue(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return "";
        }
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return "";
        }
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
            FadeTransition fade = new FadeTransition(Duration.millis(580), heroContentBox);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            TranslateTransition slide = new TranslateTransition(Duration.millis(580), heroContentBox);
            slide.setFromY(heroContentBox.getTranslateY());
            slide.setToY(heroContentBox.getTranslateY() - 12);
            new ParallelTransition(fade, slide).play();
        }

        if (loginCardBox != null) {
            loginCardBox.setOpacity(0.0);
            loginCardBox.setTranslateY(loginCardBox.getTranslateY() + 16);
            FadeTransition fade = new FadeTransition(Duration.millis(620), loginCardBox);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            TranslateTransition slide = new TranslateTransition(Duration.millis(620), loginCardBox);
            slide.setFromY(loginCardBox.getTranslateY());
            slide.setToY(loginCardBox.getTranslateY() - 16);
            new ParallelTransition(fade, slide).play();
        }
    }

    private void playContinuousAnimations() {
        if (backgroundImageView != null) {
            ScaleTransition zoom = new ScaleTransition(Duration.seconds(28), backgroundImageView);
            zoom.setFromX(1.0);
            zoom.setFromY(1.0);
            zoom.setToX(1.045);
            zoom.setToY(1.045);
            zoom.setAutoReverse(true);
            zoom.setCycleCount(Timeline.INDEFINITE);
            zoom.setInterpolator(Interpolator.EASE_BOTH);
            zoom.play();

            TranslateTransition drift = new TranslateTransition(Duration.seconds(24), backgroundImageView);
            drift.setFromY(0.0);
            drift.setToY(-10.0);
            drift.setAutoReverse(true);
            drift.setCycleCount(Timeline.INDEFINITE);
            drift.setInterpolator(Interpolator.EASE_BOTH);
            drift.play();
        }

        if (heroLogoImage != null) {
            TranslateTransition floatLogo = new TranslateTransition(Duration.seconds(4.6), heroLogoImage);
            floatLogo.setFromY(0.0);
            floatLogo.setToY(-8.0);
            floatLogo.setAutoReverse(true);
            floatLogo.setCycleCount(Timeline.INDEFINITE);
            floatLogo.setInterpolator(Interpolator.EASE_BOTH);
            floatLogo.play();

            DropShadow logoGlow = new DropShadow(22, Color.rgb(80, 192, 255, 0.36));
            logoGlow.setSpread(0.18);
            heroLogoImage.setEffect(logoGlow);
            Timeline logoPulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(logoGlow.radiusProperty(), 18, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(2.4), new KeyValue(logoGlow.radiusProperty(), 28, Interpolator.EASE_BOTH))
            );
            logoPulse.setAutoReverse(true);
            logoPulse.setCycleCount(Timeline.INDEFINITE);
            logoPulse.play();
        }

        if (heroSecurelyLabel != null) {
            Timeline securePulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(heroSecurelyLabel.opacityProperty(), 1.0, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(2.0), new KeyValue(heroSecurelyLabel.opacityProperty(), 0.78, Interpolator.EASE_BOTH))
            );
            securePulse.setCycleCount(Timeline.INDEFINITE);
            securePulse.setAutoReverse(true);
            securePulse.play();
        }

        if (loginCardBox != null) {
            DropShadow cardGlow = new DropShadow(30, Color.rgb(56, 147, 255, 0.22));
            cardGlow.setOffsetY(8);
            cardGlow.setSpread(0.10);
            loginCardBox.setEffect(cardGlow);

            Timeline cardBreath = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(loginCardBox.scaleXProperty(), 1.0, Interpolator.EASE_BOTH),
                            new KeyValue(loginCardBox.scaleYProperty(), 1.0, Interpolator.EASE_BOTH),
                            new KeyValue(cardGlow.radiusProperty(), 24, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(3.0),
                            new KeyValue(loginCardBox.scaleXProperty(), 1.01, Interpolator.EASE_BOTH),
                            new KeyValue(loginCardBox.scaleYProperty(), 1.01, Interpolator.EASE_BOTH),
                            new KeyValue(cardGlow.radiusProperty(), 34, Interpolator.EASE_BOTH))
            );
            cardBreath.setAutoReverse(true);
            cardBreath.setCycleCount(Timeline.INDEFINITE);
            cardBreath.play();
        }

        if (signInButton != null) {
            DropShadow buttonGlow = new DropShadow(14, Color.rgb(68, 190, 255, 0.32));
            buttonGlow.setSpread(0.18);
            signInButton.setEffect(buttonGlow);
            Timeline pulse = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(buttonGlow.radiusProperty(), 12, Interpolator.EASE_BOTH)),
                    new KeyFrame(Duration.seconds(1.9), new KeyValue(buttonGlow.radiusProperty(), 20, Interpolator.EASE_BOTH))
            );
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Timeline.INDEFINITE);
            pulse.play();
        }
    }

    private void installHoverAnimations() {
        installHoverLift(signInButton, 1.035, -1.5);
        installHoverLift(googleButton, 1.025, -1.2);
        installHoverLift(facebookButton, 1.025, -1.2);
        installHoverLift(biometricLink, 1.02, -1.0);
        installHoverLift(passkeyLink, 1.02, -1.0);
    }

    private void installFocusAnimations() {
        installFocusScale(emailField);
        installFocusScale(passwordField);
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
        field.focusedProperty().addListener((obs, oldValue, focused) -> animateNode(field, focused ? 1.01 : 1.0, 0.0, 120));
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

    public void showRegistrationSuccess(String email) {
        feedbackLabel.getStyleClass().remove("feedback-error");
        if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
            feedbackLabel.getStyleClass().add("feedback-success");
        }
        feedbackLabel.setText("Account created in database for " + email + ". You can sign in now.");
        emailField.setText(email);
        passwordField.clear();
    }

    @FXML
    private void handleSignIn() {
        try {
            User user = userController().login(emailField.getText(), passwordField.getText());

            if (user == null) {
                feedbackLabel.setText("Identifiants invalides.");
                feedbackLabel.getStyleClass().remove("feedback-success");
                if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                    feedbackLabel.getStyleClass().add("feedback-error");
                }
                return;
            }

            feedbackLabel.getStyleClass().remove("feedback-error");
            if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
                feedbackLabel.getStyleClass().add("feedback-success");
            }
            persistRememberMeChoice();
            openHomeForUser(user);
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void handleGoogle() {
        runSocialLogin("Google", () -> socialAuthService().authenticateWithGoogle());
    }

    @FXML
    private void handleFacebook() {
        runSocialLogin("Facebook", () -> socialAuthService().authenticateWithFacebook());
    }

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/forgot-password-view.fxml");
            Parent root = loader.getRoot();
            ForgotPasswordController controller = loader.getController();

            Stage owner = (Stage) feedbackLabel.getScene().getWindow();
            Stage dialog = new Stage();
            dialog.initOwner(owner);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Mot de passe oublie");
            dialog.setResizable(false);

            Scene scene = new Scene(root, 500, 280);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/forgot-password.css");
            dialog.setScene(scene);

            controller.setDialogStage(dialog);
            controller.setInitialEmail(emailField.getText());
            controller.setSendResetHandler(email -> passwordResetService().requestReset(email));

            dialog.showAndWait();
        } catch (IOException e) {
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText("Impossible d'ouvrir la fenetre de reinitialisation.");
        }
    }

    @FXML
    private void handleCreateAccount() {
        try {
            webcamCaptureService.stop();
            cameraRunning = false;
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/sign-up-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) feedbackLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/sign-up.css").toExternalForm());

            stage.setTitle("User Registration");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            String rootMessage = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
            feedbackLabel.setText("Impossible d'ouvrir la page d'inscription: " + rootMessage);
            e.printStackTrace();
        }
    }

    private void openSalaryHome(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/salary-home-view.fxml"));
        Parent root = loader.load();
        SalaryHomeController controller = loader.getController();
        controller.setUser(user);

        Stage stage = (Stage) feedbackLabel.getScene().getWindow();
        stage.setUserData(user);
        Scene scene = new Scene(root, 1460, 780);
        scene.getStylesheets().add(Main.class.getResource("/pi/styles/salary-home.css").toExternalForm());

        stage.setTitle("Salary Home");
        stage.setScene(scene);
        stage.show();
    }

    private void openAdminBackend(User user) throws IOException {
        Stage stage = (Stage) feedbackLabel.getScene().getWindow();
        stage.setUserData(user);
        AdminNavigation.showUsersManagement(stage, user);
    }

    private void openStudentLearningFront(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/views/cours-quiz-front.fxml"));
        Parent root = loader.load();
        CoursQuizFrontController controller = loader.getController();
        controller.setBackOfficeAccessVisible(false);

        Stage stage = (Stage) feedbackLabel.getScene().getWindow();
        stage.setUserData(user);
        Scene scene = new Scene(root, 1460, 900);

        stage.setTitle("Cours & Quiz");
        stage.setScene(scene);
        stage.show();
    }

    private void openHomeForUser(User user) throws IOException {
        webcamCaptureService.stop();
        cameraRunning = false;
        refreshGeoLocationAsync(user);
        if (user.hasRole("ROLE_ADMIN")) {
            openAdminBackend(user);
        } else if (user.hasRole("ROLE_ETUDIANT")) {
            openStudentLearningFront(user);
        } else {
            openSalaryHome(user);
        }
    }

    private void refreshGeoLocationAsync(User user) {
        if (user == null || user.getId() <= 0) {
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                userController().refreshGeoLocationForUser(user);
            } catch (Exception ignored) {
                // Geolocation update must never block user login.
            }
        }, "geo-login-" + user.getId());
        worker.setDaemon(true);
        worker.start();
    }

    private void runSocialLogin(String providerName, Supplier<User> authCall) {
        if (!socialAuthReady) {
            showError("OAuth desactive au demarrage: verifiez GOOGLE_REDIRECT_URI et FACEBOOK_REDIRECT_URI.");
            return;
        }
        setLoadingState(true, "Ouverture de la connexion " + providerName + "...");
        Thread worker = new Thread(() -> {
            try {
                User user = authCall.get();
                Platform.runLater(() -> {
                    feedbackLabel.getStyleClass().remove("feedback-error");
                    if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
                        feedbackLabel.getStyleClass().add("feedback-success");
                    }
                    feedbackLabel.setText("Connexion " + providerName + " reussie.");
                    try {
                        openHomeForUser(user);
                    } catch (IOException e) {
                        showError("Connexion reussie, mais navigation impossible.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            } finally {
                Platform.runLater(() -> setLoadingState(false, null));
            }
        }, "oauth-" + providerName.toLowerCase());
        worker.setDaemon(true);
        worker.start();
    }

    private void setLoadingState(boolean loading, String message) {
        signInButton.setDisable(loading);
        googleButton.setDisable(loading || !socialAuthReady);
        facebookButton.setDisable(loading || !socialAuthReady);
        biometricLink.setDisable(loading);
        passkeyLink.setDisable(loading);
        openCameraButton.setDisable(loading);
        captureLoginButton.setDisable(loading || !cameraRunning);
        stopCameraButton.setDisable(loading || !cameraRunning);
        if (loading && message != null) {
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText(message);
        } else if (!loading) {
            updateCameraButtonsState();
        }
    }

    private void handleBiometricSignIn() {
        if (biometricLoginRunning) {
            return;
        }
        String email = emailField.getText() == null ? null : emailField.getText().trim();
        if (email == null || email.isBlank()) {
            showError("Saisissez d'abord votre email pour la connexion biométrique.");
            return;
        }

        if (!cameraRunning) {
            startLiveCameraForLogin();
            if (cameraRunning) {
                feedbackLabel.getStyleClass().remove("feedback-error");
                if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
                    feedbackLabel.getStyleClass().add("feedback-success");
                }
                feedbackLabel.setText("Camera ouverte. Cliquez sur 'Capture and sign in'.");
            }
            return;
        }

        setLoadingState(true, "Verification du visage avec Face++...");
        biometricLoginRunning = true;
        Path imagePath;
        try {
            imagePath = webcamCaptureService.captureFrameToTempJpg();
        } catch (Exception e) {
            biometricLoginRunning = false;
            setLoadingState(false, null);
            showError("Capture impossible: " + e.getMessage());
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                User existing = userController().findByEmail(email);
                if (existing == null) {
                    throw new IllegalStateException("Aucun compte trouve pour cet email.");
                }
                if (!existing.isFacePlusEnabled() || existing.getFacePlusToken() == null || existing.getFacePlusToken().isBlank()) {
                    throw new IllegalStateException("Aucun token biométrique enregistre pour ce compte.");
                }

                boolean matched = facePlusPlusService().verifyFaceAgainstToken(imagePath, existing.getFacePlusToken());
                if (!matched) {
                    throw new IllegalStateException("Verification biométrique echouee.");
                }

                Platform.runLater(() -> {
                    feedbackLabel.getStyleClass().remove("feedback-error");
                    if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
                        feedbackLabel.getStyleClass().add("feedback-success");
                    }
                    feedbackLabel.setText("Connexion biométrique reussie.");
                    try {
                        openHomeForUser(existing);
                    } catch (IOException e) {
                        showError("Connexion reussie, mais navigation impossible.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            } finally {
                Platform.runLater(() -> {
                    biometricLoginRunning = false;
                    setLoadingState(false, null);
                });
            }
        }, "webauthn-login");
        worker.setDaemon(true);
        worker.start();
    }

    private void handlePasskeySignIn() {
        if (biometricLoginRunning) {
            return;
        }
        String email = emailField.getText() == null ? null : emailField.getText().trim().toLowerCase();
        if (email == null || email.isBlank()) {
            showError("Saisissez d'abord votre email pour la connexion Passkey.");
            return;
        }

        setLoadingState(true, "Ouverture de la connexion Passkey (QR)...");
        biometricLoginRunning = true;
        User existing;
        try {
            existing = userController().findByEmail(email);
            if (existing == null) {
                throw new IllegalStateException("Aucun compte trouve pour cet email.");
            }
            if (!existing.isFaceIdEnabled() || existing.getFaceIdCredentialId() == null || existing.getFaceIdCredentialId().isBlank()) {
                throw new IllegalStateException("Aucune passkey enregistree pour ce compte. Activez-la lors de l'inscription.");
            }
        } catch (Exception e) {
            biometricLoginRunning = false;
            setLoadingState(false, null);
            showError(e.getMessage());
            return;
        }

        WebAuthnDesktopService.PendingPasskeyFlow flow;
        try {
            flow = webAuthnDesktopService().startLoginFlow(email, existing.getFaceIdCredentialId());
            showPasskeyQrDialog(flow, "Scannez ce QR pour vous connecter");
            feedbackLabel.getStyleClass().remove("feedback-success");
            if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
                feedbackLabel.getStyleClass().add("feedback-error");
            }
            feedbackLabel.setText("QR affiche. Scannez avec votre telephone pour terminer la connexion.");
        } catch (Exception e) {
            biometricLoginRunning = false;
            setLoadingState(false, null);
            closePasskeyQrDialog();
            showError("Echec d'ouverture du QR Passkey: " + e.getMessage());
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                String credentialId = flow.awaitCredential(java.time.Duration.ofSeconds(120));
                User authenticated = userController().loginWithBiometric(email, credentialId);
                if (authenticated == null) {
                    throw new IllegalStateException("Passkey invalide pour ce compte.");
                }

                Platform.runLater(() -> {
                    closePasskeyQrDialog();
                    feedbackLabel.getStyleClass().remove("feedback-error");
                    if (!feedbackLabel.getStyleClass().contains("feedback-success")) {
                        feedbackLabel.getStyleClass().add("feedback-success");
                    }
                    feedbackLabel.setText("Connexion Passkey reussie.");
                    try {
                        openHomeForUser(authenticated);
                    } catch (IOException e) {
                        showError("Connexion reussie, mais navigation impossible.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    closePasskeyQrDialog();
                    showError(e.getMessage());
                });
            } finally {
                flow.close();
                Platform.runLater(() -> {
                    biometricLoginRunning = false;
                    setLoadingState(false, null);
                });
            }
        }, "passkey-login");
        worker.setDaemon(true);
        worker.start();
    }

    private void showPasskeyQrDialog(WebAuthnDesktopService.PendingPasskeyFlow flow, String title) {
        closePasskeyQrDialog();
        Stage owner = feedbackLabel != null && feedbackLabel.getScene() != null && feedbackLabel.getScene().getWindow() instanceof Stage s ? s : null;
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

        Label hintLabel = new Label("Scannez (meme reseau Wi-Fi) puis validez Face ID/empreinte sur le telephone.");
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

    private void startLiveCameraForLogin() {
        if (cameraRunning) {
            return;
        }
        if (cameraPreviewView == null) {
            showError("Zone de preview camera non initialisee.");
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
            feedbackLabel.setText("Camera live activee. Cliquez sur 'Capture and sign in'.");
        } catch (Exception e) {
            showError("Impossible de demarrer la camera: " + e.getMessage());
        }
    }

    private void stopLiveCamera() {
        webcamCaptureService.stop();
        cameraRunning = false;
        updateCameraButtonsState();
        feedbackLabel.getStyleClass().remove("feedback-success");
        if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
            feedbackLabel.getStyleClass().add("feedback-error");
        }
        feedbackLabel.setText("Camera arretee.");
    }

    private FacePlusPlusService facePlusPlusService() {
        if (facePlusPlusService == null) {
            facePlusPlusService = new FacePlusPlusService();
        }
        return facePlusPlusService;
    }

    private UserController userController() {
        if (userController == null) {
            userController = new UserController();
        }
        return userController;
    }

    private PasswordResetService passwordResetService() {
        if (passwordResetService == null) {
            passwordResetService = new PasswordResetService();
        }
        return passwordResetService;
    }

    private SocialAuthService socialAuthService() {
        if (socialAuthService == null) {
            socialAuthService = new SocialAuthService();
        }
        return socialAuthService;
    }

    private WebAuthnDesktopService webAuthnDesktopService() {
        if (webAuthnDesktopService == null) {
            webAuthnDesktopService = new WebAuthnDesktopService();
        }
        return webAuthnDesktopService;
    }

    private void showError(String message) {
        feedbackLabel.getStyleClass().remove("feedback-success");
        if (!feedbackLabel.getStyleClass().contains("feedback-error")) {
            feedbackLabel.getStyleClass().add("feedback-error");
        }
        feedbackLabel.setText(message);
    }

    private void validateOAuthConfigurationOnStartup() {
        try {
            socialAuthService().validateStartupConfiguration();
            socialAuthReady = true;
            googleButton.setDisable(false);
            facebookButton.setDisable(false);
        } catch (Exception e) {
            socialAuthReady = false;
            googleButton.setDisable(true);
            facebookButton.setDisable(true);
        }
    }

    private void updateCameraButtonsState() {
        if (openCameraButton != null) {
            openCameraButton.setDisable(cameraRunning);
        }
        if (captureLoginButton != null) {
            captureLoginButton.setDisable(!cameraRunning || biometricLoginRunning);
        }
        if (stopCameraButton != null) {
            stopCameraButton.setDisable(!cameraRunning);
        }
    }
}
