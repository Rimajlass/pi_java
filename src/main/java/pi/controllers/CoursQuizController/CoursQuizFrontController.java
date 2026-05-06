package pi.controllers.CoursQuizController;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import pi.entities.Cours;
import pi.entities.Quiz;
import pi.entities.User;
import pi.mains.Main;
import pi.services.CoursQuizService.CoursService;
import pi.services.CoursQuizService.CertificatePdfGenerator;
import pi.services.CoursQuizService.LearningCertificationService;
import pi.services.CoursQuizService.QuizService;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.prefs.Preferences;

public class CoursQuizFrontController {

    private enum State {CATALOG, READER, QUIZ, SCORE, GAMES}

    @FXML
    private Label breadcrumbLabel;
    @FXML
    private Label kpiCourseCount;
    @FXML
    private Label kpiQuizCount;
    @FXML
    private Label kpiBestScore;
    @FXML
    private Button backOfficeButton;
    @FXML
    private Button gamesButton;
    @FXML
    private Button logoutButton;

    @FXML
    private VBox catalogPane;
    @FXML
    private VBox readerPane;
    @FXML
    private VBox quizPane;
    @FXML
    private VBox scorePane;
    @FXML
    private VBox gamesPane;

    // Catalog
    @FXML
    private TextField courseSearchField;
    @FXML
    private FlowPane courseCardsPane;

    @FXML
    private ScrollPane rootScroll;

    @FXML
    private StackPane heroBanner;

    @FXML
    private ImageView heroBannerImage;

    // Reader
    @FXML
    private Label readerTitleLabel;
    @FXML
    private Label readerMetaLabel;
    @FXML
    private TextArea readerContentArea;
    @FXML
    private Hyperlink readerMediaLink;
    @FXML
    private Label readerQuizCountLabel;
    @FXML
    private Label readerBestScoreLabel;

    // Quiz
    @FXML
    private Label quizTitleLabel;
    @FXML
    private Label quizSubtitleLabel;
    @FXML
    private Label quizProgressLabel;
    @FXML
    private ProgressBar quizProgressBar;
    @FXML
    private Label quizTimerLabel;
    @FXML
    private Label quizQuestionLabel;
    @FXML
    private VBox quizChoicesBox;
    @FXML
    private Button quizPrevButton;
    @FXML
    private Button quizNextButton;
    @FXML
    private Button quizSubmitButton;

    // Score
    @FXML
    private Label scoreSubtitleLabel;
    @FXML
    private Label scoreValueLabel;
    @FXML
    private VBox scoreDetailsBox;
    @FXML
    private FlowPane scoreBadgesPane;
    @FXML
    private Button downloadCertificateButton;
    @FXML
    private Button copyCertificateCodeButton;
    @FXML
    private Label certificateCodeLabel;

    // Games: Puzzle
    // Games: Crossword
    @FXML
    private GridPane crosswordGrid;
    @FXML
    private Label crosswordStatusLabel;
    @FXML
    private Label gamesProgressLabel;

    private final CoursService coursService = new CoursService();
    private final QuizService quizService = new QuizService();
    private final LearningCertificationService certificationService = new LearningCertificationService();

    private LearningCertificationService.CertificateInfo lastCertificate;

    private CoursQuizDashboardController dashboardController;
    private boolean backOfficeAccessVisible = true;

    private List<Cours> allCourses = new ArrayList<>();
    private Cours selectedCourse;
    private List<Quiz> selectedCourseQuizzes = new ArrayList<>();

    private final Map<Integer, Integer> bestScoreByCourse = new HashMap<>();

    private int quizIndex = 0;
    private ToggleGroup quizToggleGroup;
    private final Map<Integer, String> answersByQuizId = new HashMap<>();

    private Timeline quizTimer;
    private int quizSecondsRemaining = 0;
    private Process ttsHost;
    private BufferedWriter ttsHostStdin;

    private static final int CROSS_ROWS = 10;
    private static final int CROSS_COLS = 11;
    private char[][] crosswordSolution;
    private TextField[][] crosswordInputs;
    private boolean crosswordAlreadySolved = false;

    private final Preferences gamesPrefs = Preferences.userNodeForPackage(CoursQuizFrontController.class);
    private int crosswordSolvedCount = 0;

    public void setDashboardController(CoursQuizDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public void setBackOfficeAccessVisible(boolean visible) {
        this.backOfficeAccessVisible = visible;
        applyBackOfficeVisibility();
    }

    public void onShow() {
        refreshKpis();
    }

    @FXML
    public void initialize() {
        readerContentArea.setText("");
        applyBackOfficeVisibility();
        setState(State.CATALOG);
        onRefreshCourses();
        bindResponsiveLayout();
        initGames();

        Platform.runLater(() -> {
            if (!allCourses.isEmpty()) {
                // keep catalog as default, no auto-open
            }
        });
    }

    @FXML
    private void onOpenBackOffice() {
        if (backOfficeAccessVisible && dashboardController != null) {
            dashboardController.showBackOffice();
        }
    }

    private void bindResponsiveLayout() {
        // Ensure banner image never forces horizontal scrolling.
        if (heroBanner != null && heroBannerImage != null) {
            heroBannerImage.fitWidthProperty().bind(heroBanner.widthProperty());
            heroBannerImage.fitHeightProperty().bind(heroBanner.heightProperty());
        }

        // Make cards wrap to the viewport width so they don't overflow horizontally.
        if (rootScroll != null && courseCardsPane != null) {
            rootScroll.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
                if (newBounds != null) {
                    courseCardsPane.setPrefWrapLength(Math.max(320, newBounds.getWidth()));
                }
            });

            if (rootScroll.getViewportBounds() != null) {
                courseCardsPane.setPrefWrapLength(Math.max(320, rootScroll.getViewportBounds().getWidth()));
            }
        }
    }

    @FXML
    public void onLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/login-view.fxml"));
            Parent root = loader.load();

            Node source = event != null ? (Node) event.getSource() : null;
            Stage stage = source != null ? (Stage) source.getScene().getWindow() : null;
            if (stage == null) {
                return;
            }

            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/login.css").toExternalForm());
            stage.setUserData(null);
            stage.setTitle("User Secure Login");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page Login.", e);
        }
    }

    @FXML
    private void onRefreshCourses() {
        List<Cours> courses = coursService.afficher();
        allCourses = courses == null ? new ArrayList<>() : courses.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Cours::getTitre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
        renderCourseCards(allCourses);
        refreshKpis();
    }

    @FXML
    private void onSearchCourses() {
        String query = courseSearchField.getText() != null ? courseSearchField.getText().trim() : "";
        if (query.isEmpty()) {
            renderCourseCards(allCourses);
            return;
        }

        String needle = query.toLowerCase(Locale.ROOT);
        List<Cours> filtered = allCourses.stream()
                .filter(c -> safe(c.getTitre()).toLowerCase(Locale.ROOT).contains(needle)
                        || safe(c.getContenuTexte()).toLowerCase(Locale.ROOT).contains(needle))
                .collect(Collectors.toList());
        renderCourseCards(filtered);
    }

    private void renderCourseCards(List<Cours> courses) {
        courseCardsPane.getChildren().clear();

        if (courses == null || courses.isEmpty()) {
            Label empty = new Label("Aucun cours trouve.");
            empty.getStyleClass().add("section-subtitle");
            courseCardsPane.getChildren().add(empty);
            return;
        }

        for (Cours course : courses) {
            courseCardsPane.getChildren().add(createCourseCard(course));
        }
    }

    private Node createCourseCard(Cours cours) {
        String mediaType = resolveMediaType(safe(cours.getTypeMedia()));

        StackPane header = new StackPane();
        header.getStyleClass().addAll("course-media-header", "course-media-" + mediaType);
        header.setMinHeight(140);

        Label icon = new Label(resolveMediaIcon(mediaType));
        icon.getStyleClass().add("course-media-icon");

        Label badge = new Label(resolveMediaBadgeText(mediaType));
        badge.getStyleClass().addAll("course-media-badge", "course-media-badge-" + mediaType);
        StackPane.setAlignment(badge, javafx.geometry.Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new javafx.geometry.Insets(12, 12, 0, 0));

        header.getChildren().addAll(icon, badge);

        Label title = new Label(safe(cours.getTitre()));
        title.getStyleClass().add("course-card-title");
        title.setWrapText(true);

        Label desc = new Label(truncate(safe(cours.getContenuTexte()), 110));
        desc.getStyleClass().add("course-card-desc");
        desc.setWrapText(true);

        HBox chips = new HBox(8);
        chips.getStyleClass().add("course-chips");
        chips.getChildren().addAll(
                createChip(resolvePrimaryChip(mediaType), true),
                createChip(resolveSecondaryChip(mediaType), false)
        );

        Button start = new Button("Start course");
        start.getStyleClass().addAll("action-button", "course-start-button", "course-start-" + mediaType);
        start.setMaxWidth(Double.MAX_VALUE);
        start.setOnAction(evt -> openReader(cours));

        VBox content = new VBox(8, title, desc, chips, start);
        content.getStyleClass().add("course-card-content");

        VBox card = new VBox(0, header, content);
        card.getStyleClass().addAll("course-card", "course-card-v2");
        card.setOnMouseClicked(evt -> openReader(cours));

        return card;
    }

    @FXML
    private void onOpenGames() {
        setState(State.GAMES);
        if (crosswordInputs == null) {
            buildCrossword();
        }
    }

    @FXML
    private void onCrosswordReset() {
        if (crosswordInputs == null) {
            return;
        }
        crosswordAlreadySolved = false;
        for (int r = 0; r < CROSS_ROWS; r++) {
            for (int c = 0; c < CROSS_COLS; c++) {
                TextField tf = crosswordInputs[r][c];
                if (tf != null) {
                    tf.clear();
                }
            }
        }
        if (crosswordStatusLabel != null) {
            crosswordStatusLabel.setText("");
        }
    }

    @FXML
    private void onCrosswordCheck() {
        if (crosswordInputs == null || crosswordSolution == null) {
            return;
        }
        boolean anyEmpty = false;
        boolean ok = true;
        for (int r = 0; r < CROSS_ROWS; r++) {
            for (int c = 0; c < CROSS_COLS; c++) {
                char sol = crosswordSolution[r][c];
                TextField tf = crosswordInputs[r][c];
                if (sol == '\0') {
                    continue;
                }
                String text = tf != null ? safe(tf.getText()).trim() : "";
                if (text.isEmpty()) {
                    anyEmpty = true;
                    ok = false;
                    continue;
                }
                char ch = Character.toUpperCase(text.charAt(0));
                if (ch != sol) {
                    ok = false;
                }
            }
        }

        if (crosswordStatusLabel == null) {
            return;
        }
        if (ok) {
            if (!crosswordAlreadySolved) {
                crosswordSolvedCount++;
                saveGamesProgress();
                crosswordAlreadySolved = true;
            }
            crosswordStatusLabel.setText("Bravo ! Grille correcte.");
            updateGamesProgressLabel();
        } else if (anyEmpty) {
            crosswordStatusLabel.setText("Il manque des lettres.");
        } else {
            crosswordStatusLabel.setText("Quelques lettres sont incorrectes.");
        }
    }

    private void initGames() {
        loadGamesProgress();
        updateGamesProgressLabel();
        if (crosswordGrid != null) {
            buildCrossword();
        }
    }

    private void loadGamesProgress() {
        crosswordSolvedCount = Math.max(0, gamesPrefs.getInt("games.crosswordSolvedCount", 0));
    }

    private void saveGamesProgress() {
        gamesPrefs.putInt("games.crosswordSolvedCount", crosswordSolvedCount);
    }

    private void updateGamesProgressLabel() {
        if (gamesProgressLabel == null) {
            return;
        }
        gamesProgressLabel.setText("Progression: Mots croises " + crosswordSolvedCount);
    }

    private void buildCrossword() {
        if (crosswordGrid == null) {
            return;
        }
        crosswordAlreadySolved = false;
        crosswordSolution = new char[CROSS_ROWS][CROSS_COLS];
        crosswordInputs = new TextField[CROSS_ROWS][CROSS_COLS];

        placeWordAcross("BUDGET", 0, 0);
        placeWordDown("DETTE", 0, 2);
        placeWordAcross("INTERET", 2, 0);
        placeWordDown("RISQUE", 2, 4);
        placeWordAcross("EPARGNE", 8, 0);
        placeWordDown("ACTION", 3, 7);
        placeWordDown("CAPITAL", 2, 9);

        crosswordGrid.getChildren().clear();

        for (int r = 0; r < CROSS_ROWS; r++) {
            for (int c = 0; c < CROSS_COLS; c++) {
                char sol = crosswordSolution[r][c];
                if (sol == '\0') {
                    Region block = new Region();
                    block.getStyleClass().add("crossword-block");
                    block.setMinSize(46, 46);
                    block.setPrefSize(46, 46);
                    block.setMaxSize(46, 46);
                    crosswordGrid.add(block, c, r);
                    continue;
                }

                TextField tf = new TextField();
                tf.setAlignment(Pos.CENTER);
                tf.getStyleClass().add("crossword-cell");
                tf.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 15));
                tf.setStyle("-fx-text-fill: #0b375a; -fx-text-inner-color: #0b375a;"
                        + " -fx-control-inner-background: white; -fx-background-color: white;");
                tf.setMinSize(46, 46);
                tf.setPrefSize(46, 46);
                tf.setMaxSize(46, 46);
                tf.textProperty().addListener((obs, oldValue, newValue) -> {
                    String raw = newValue == null ? "" : newValue;
                    String lettersOnly = raw.replaceAll("[^A-Za-z]", "");
                    if (lettersOnly.isEmpty()) {
                        if (!raw.isEmpty()) {
                            tf.setText("");
                        }
                        return;
                    }
                    String one = String.valueOf(lettersOnly.charAt(lettersOnly.length() - 1)).toUpperCase(Locale.ROOT);
                    if (!one.equals(raw)) {
                        tf.setText(one);
                    }
                });
                crosswordInputs[r][c] = tf;
                crosswordGrid.add(tf, c, r);
            }
        }
    }

    private void placeWordAcross(String word, int row, int col) {
        if (word == null) {
            return;
        }
        String w = word.trim().toUpperCase(Locale.ROOT);
        for (int i = 0; i < w.length(); i++) {
            int r = row;
            int c = col + i;
            if (r < 0 || r >= CROSS_ROWS || c < 0 || c >= CROSS_COLS) {
                return;
            }
            char ch = w.charAt(i);
            if (!Character.isLetter(ch)) {
                continue;
            }
            char existing = crosswordSolution[r][c];
            if (existing != '\0' && existing != ch) {
                return;
            }
            crosswordSolution[r][c] = ch;
        }
    }

    private void placeWordDown(String word, int row, int col) {
        if (word == null) {
            return;
        }
        String w = word.trim().toUpperCase(Locale.ROOT);
        for (int i = 0; i < w.length(); i++) {
            int r = row + i;
            int c = col;
            if (r < 0 || r >= CROSS_ROWS || c < 0 || c >= CROSS_COLS) {
                return;
            }
            char ch = w.charAt(i);
            if (!Character.isLetter(ch)) {
                continue;
            }
            char existing = crosswordSolution[r][c];
            if (existing != '\0' && existing != ch) {
                return;
            }
            crosswordSolution[r][c] = ch;
        }
    }

    private String normalizeSentence(String sentence) {
        String s = safe(sentence).trim().replaceAll("\\s+", " ");
        return s;
    }

    private Label createChip(String text, boolean primary) {
        Label chip = new Label(text);
        chip.getStyleClass().addAll("course-chip", primary ? "course-chip-primary" : "course-chip-secondary");
        return chip;
    }

    private String resolveMediaType(String rawType) {
        String t = safe(rawType).trim().toLowerCase(Locale.ROOT);
        if (t.contains("video")) {
            return "video";
        }
        if (t.contains("pdf")) {
            return "pdf";
        }
        if (t.contains("image")) {
            return "image";
        }
        return "default";
    }

    private String resolveMediaIcon(String mediaType) {
        return switch (mediaType) {
            case "video" -> "▶";
            case "pdf" -> "⧉";
            case "image" -> "▦";
            default -> "▶";
        };
    }

    private String resolveMediaBadgeText(String mediaType) {
        return switch (mediaType) {
            case "video" -> "Video";
            case "pdf" -> "PDF";
            case "image" -> "Image";
            default -> "Cours";
        };
    }

    private String resolvePrimaryChip(String mediaType) {
        return switch (mediaType) {
            case "video" -> "Interactive video";
            case "pdf" -> "Guided reading";
            case "image" -> "Visual lesson";
            default -> "Learning";
        };
    }

    private String resolveSecondaryChip(String mediaType) {
        return switch (mediaType) {
            case "video" -> "Quick practice";
            case "pdf" -> "Printable";
            case "image" -> "Illustrated";
            default -> "Module";
        };
    }

    private void openReader(Cours cours) {
        selectedCourse = cours;

        readerTitleLabel.setText(safe(cours.getTitre()));
        readerMetaLabel.setText("Type: " + safe(cours.getTypeMedia()) + " • Cours ID: " + cours.getId());
        readerContentArea.setText(safe(cours.getContenuTexte()));
        readerMediaLink.setText(safe(cours.getUrlMedia()));

        selectedCourseQuizzes = loadQuizzesForCourse(cours.getId());
        readerQuizCountLabel.setText(String.valueOf(selectedCourseQuizzes.size()));

        int best = bestScoreByCourse.getOrDefault(cours.getId(), 0);
        readerBestScoreLabel.setText(best > 0 ? ("Meilleur score: " + best + " pts") : "");

        setState(State.READER);
    }

    @FXML
    private void onBackToCatalog() {
        setState(State.CATALOG);
    }

    @FXML
    private void onBackToReader() {
        if (selectedCourse != null) {
            setState(State.READER);
        } else {
            setState(State.CATALOG);
        }
    }

    @FXML
    private void onGoToQuiz() {
        if (selectedCourse == null) {
            return;
        }
        selectedCourseQuizzes = loadQuizzesForCourse(selectedCourse.getId());
        if (selectedCourseQuizzes.isEmpty()) {
            showInfo("Aucun quiz associe a ce cours.");
            return;
        }
        startQuiz();
    }

    private void startQuiz() {
        answersByQuizId.clear();
        quizIndex = 0;
        quizTitleLabel.setText("Quiz");
        quizSubtitleLabel.setText(safe(selectedCourse.getTitre()));
        setState(State.QUIZ);
        startQuizTimer(30);
        renderQuizQuestion();
    }

    @FXML
    private void onQuizPrev() {
        if (quizIndex <= 0) {
            return;
        }
        saveCurrentAnswerIfAny();
        quizIndex--;
        renderQuizQuestion();
    }

    @FXML
    private void onQuizNext() {
        if (selectedCourseQuizzes.isEmpty()) {
            return;
        }
        String selected = getSelectedAnswer();
        if (selected == null) {
            showError("Choisissez une reponse avant de continuer.");
            return;
        }
        answersByQuizId.put(selectedCourseQuizzes.get(quizIndex).getId(), selected);

        if (quizIndex >= selectedCourseQuizzes.size() - 1) {
            onQuizSubmit();
            return;
        }

        quizIndex++;
        renderQuizQuestion();
    }

    @FXML
    private void onQuizSubmit() {
        submitQuiz(true);
    }

    @FXML
    private void onRetryQuiz() {
        if (selectedCourse == null) {
            setState(State.CATALOG);
            return;
        }
        if (selectedCourseQuizzes.isEmpty()) {
            selectedCourseQuizzes = loadQuizzesForCourse(selectedCourse.getId());
        }
        if (selectedCourseQuizzes.isEmpty()) {
            setState(State.READER);
            return;
        }
        startQuiz();
    }

    private void submitQuiz(boolean requireCurrentAnswer) {
        if (selectedCourseQuizzes.isEmpty()) {
            return;
        }

        if (requireCurrentAnswer) {
            String selected = getSelectedAnswer();
            if (selected == null) {
                showError("Choisissez une reponse avant de valider.");
                return;
            }
            answersByQuizId.put(selectedCourseQuizzes.get(quizIndex).getId(), selected);
        } else {
            saveCurrentAnswerIfAny();
        }

        stopQuizTimer();

        int totalPossible = selectedCourseQuizzes.stream().mapToInt(Quiz::getPointsValeur).sum();
        int earned = 0;
        int correctCount = 0;

        for (Quiz q : selectedCourseQuizzes) {
            String ans = answersByQuizId.get(q.getId());
            if (ans != null && isCorrectAnswer(q, ans)) {
                earned += q.getPointsValeur();
                correctCount++;
            }
        }

        if (selectedCourse != null) {
            int courseId = selectedCourse.getId();
            int prevBest = bestScoreByCourse.getOrDefault(courseId, 0);
            if (earned > prevBest) {
                bestScoreByCourse.put(courseId, earned);
            }
        }

        showScore(earned, totalPossible, correctCount);
        refreshKpis();
        renderCourseCards(allCourses);
    }

    private void showScore(int earned, int totalPossible, int correctCount) {
        stopQuizTimer();
        stopTts();
        int totalQuestions = selectedCourseQuizzes.size();
        int percent = totalPossible == 0 ? 0 : (int) Math.round(earned * 100.0 / totalPossible);

        scoreValueLabel.setText(earned + " / " + totalPossible + " pts");
        scoreSubtitleLabel.setText(correctCount + "/" + totalQuestions + " bonnes reponses • " + percent + "%");

        if (scoreBadgesPane != null) {
            scoreBadgesPane.getChildren().clear();
        }
        if (certificateCodeLabel != null) {
            certificateCodeLabel.setText("");
        }
        if (downloadCertificateButton != null) {
            downloadCertificateButton.setDisable(true);
        }
        if (copyCertificateCodeButton != null) {
            copyCertificateCodeButton.setDisable(true);
        }
        lastCertificate = null;

        scoreDetailsBox.getChildren().clear();
        for (Quiz q : selectedCourseQuizzes) {
            String ans = answersByQuizId.get(q.getId());
            boolean ok = ans != null && isCorrectAnswer(q, ans);

            Label qLabel = new Label(safe(q.getQuestion()));
            qLabel.getStyleClass().add("score-q");
            qLabel.setWrapText(true);

            String status = ok ? "Correct" : "Incorrect";
            Label meta = new Label(status + " • Votre reponse: " + safe(ans) + " • Correct: " + safe(q.getReponseCorrecte()));
            meta.getStyleClass().add(ok ? "score-ok" : "score-ko");
            meta.setWrapText(true);

            VBox row = new VBox(4, qLabel, meta);
            row.getStyleClass().add("score-row");
            scoreDetailsBox.getChildren().add(row);
        }

        setState(State.SCORE);
        awardBadgesAndMaybeCertificateAsync(earned, totalPossible, percent);
    }

    private void awardBadgesAndMaybeCertificateAsync(int score, int total, int percentage) {
        User user;
        try {
            user = resolveAuthenticatedUser();
        } catch (Exception e) {
            System.out.println("[CERT] User not found in stage: " + e.getMessage());
            return;
        }
        Cours cours = selectedCourse;
        if (cours == null) {
            return;
        }

        new Thread(() -> {
            try {
                LearningCertificationService.CertificationOutcome outcome =
                        certificationService.recordAttemptAndAward(user, cours, score, total, percentage);
                Platform.runLater(() -> applyCertificationOutcome(outcome));
            } catch (Exception e) {
                System.out.println("[CERT] Award failed: " + e.getMessage());
            }
        }, "certification-awarder").start();
    }

    private void applyCertificationOutcome(LearningCertificationService.CertificationOutcome outcome) {
        if (outcome == null) {
            return;
        }

        if (scoreBadgesPane != null) {
            scoreBadgesPane.getChildren().clear();
            List<LearningCertificationService.UserBadge> badges = outcome.badgesAwarded() != null ? outcome.badgesAwarded() : List.of();
            for (LearningCertificationService.UserBadge badge : badges) {
                Label l = new Label(badge.title());
                l.getStyleClass().addAll("course-badge", "course-badge-" + badge.level().toLowerCase(Locale.ROOT));
                scoreBadgesPane.getChildren().add(l);
            }
            if (badges.isEmpty()) {
                Label l = new Label(outcome.passed() ? "Aucun nouveau badge" : "Validez le quiz (>= 80%) pour obtenir des badges.");
                l.getStyleClass().add("detail-meta");
                scoreBadgesPane.getChildren().add(l);
            }
        }

        lastCertificate = outcome.certificate();
        if (lastCertificate != null && certificateCodeLabel != null) {
            certificateCodeLabel.setText("Code certificat: " + safe(lastCertificate.code()));
        }
        boolean available = lastCertificate != null && safe(lastCertificate.code()).length() > 0;
        if (downloadCertificateButton != null) {
            downloadCertificateButton.setDisable(!available);
        }
        if (copyCertificateCodeButton != null) {
            copyCertificateCodeButton.setDisable(lastCertificate == null || safe(lastCertificate.code()).isBlank());
        }
    }

    @FXML
    private void onDownloadCertificate() {
        if (lastCertificate == null || safe(lastCertificate.code()).isBlank()) {
            showError("Aucun certificat disponible. Validez le quiz (>= 80%) pour obtenir un certificat.");
            return;
        }

        User user;
        try {
            user = resolveAuthenticatedUser();
        } catch (Exception e) {
            showError("Aucun utilisateur connecte.");
            return;
        }
        if (selectedCourse == null) {
            showError("Cours introuvable.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le certificat");
        chooser.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        String suggested = "certificat-" + resolveCourseTitle(selectedCourse.getId()).replaceAll("[\\\\/:*?\"<>|]+", "-") + "-" + lastCertificate.code() + ".pdf";
        chooser.setInitialFileName(suggested);

        File dest = chooser.showSaveDialog(rootScroll.getScene().getWindow());
        if (dest == null) {
            return;
        }

        try {
            CertificatePdfGenerator.generateTo(dest.toPath(), user, selectedCourse, lastCertificate.percentage(), lastCertificate.code());
            showInfo("Certificat enregistre:\n" + dest.getAbsolutePath());
        } catch (Exception e) {
            showError("Erreur generation certificat: " + (e.getMessage() != null ? e.getMessage() : String.valueOf(e)));
        }
    }

    @FXML
    private void onCopyCertificateCode() {
        if (lastCertificate == null || safe(lastCertificate.code()).isBlank()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(lastCertificate.code());
        Clipboard.getSystemClipboard().setContent(content);
        showInfo("Code certificat copie: " + lastCertificate.code());
    }

    private User resolveAuthenticatedUser() {
        if (rootScroll == null || rootScroll.getScene() == null || rootScroll.getScene().getWindow() == null) {
            throw new IllegalStateException("Fenetre non initialisee.");
        }
        if (!(rootScroll.getScene().getWindow() instanceof Stage stage)) {
            throw new IllegalStateException("Fenetre invalide.");
        }
        Object data = stage.getUserData();
        if (data instanceof User user && user.getId() > 0) {
            return user;
        }
        throw new IllegalStateException("Aucun utilisateur connecte.");
    }

    @FXML
    private void onShowLearningHistory() {
        User user;
        try {
            user = resolveAuthenticatedUser();
        } catch (Exception e) {
            showError("Aucun utilisateur connecte.");
            return;
        }

        List<LearningCertificationService.CertificateInfoWithCourse> certs = certificationService.listCertificates(user.getId(), 20);
        List<LearningCertificationService.AttemptInfo> attempts = certificationService.listAttempts(user.getId(), 20);

        StringBuilder sb = new StringBuilder();
        sb.append("Certificats (").append(certs.size()).append("):\n");
        if (certs.isEmpty()) {
            sb.append("- Aucun\n");
        } else {
            for (var c : certs) {
                sb.append("- ")
                        .append(resolveCourseTitle(c.coursId()))
                        .append(" • ")
                        .append(c.percentage()).append("% • code=").append(c.code())
                        .append(" • ").append(c.issuedAt())
                        .append("\n");
            }
        }

        sb.append("\nDernieres tentatives (").append(attempts.size()).append("):\n");
        if (attempts.isEmpty()) {
            sb.append("- Aucune\n");
        } else {
            for (var a : attempts) {
                sb.append("- ")
                        .append(resolveCourseTitle(a.coursId()))
                        .append(" • ").append(a.score()).append("/").append(a.total())
                        .append(" (").append(a.percentage()).append("%)")
                        .append(a.passed() ? " • VALIDÉ\n" : " • non validé\n");
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText("Historique (certification & quiz)");
        alert.setTitle("Historique");
        alert.setContentText(sb.toString());
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    private String resolveCourseTitle(int coursId) {
        if (coursId <= 0) {
            return "Cours";
        }
        for (Cours c : allCourses) {
            if (c != null && c.getId() == coursId) {
                String title = safe(c.getTitre()).trim();
                return title.isBlank() ? ("Cours #" + coursId) : title;
            }
        }
        return "Cours #" + coursId;
    }

    @FXML
    private void onCopyMediaLink() {
        String url = readerMediaLink.getText();
        if (url == null || url.isBlank()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(url);
        Clipboard.getSystemClipboard().setContent(content);
        showInfo("Lien copie dans le presse-papiers.");
    }

    private void startQuizTimer(int seconds) {
        stopQuizTimer();

        quizSecondsRemaining = Math.max(0, seconds);
        updateQuizTimerLabel();

        if (quizSecondsRemaining <= 0) {
            return;
        }

        quizTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            quizSecondsRemaining--;
            updateQuizTimerLabel();
            if (quizSecondsRemaining <= 0) {
                stopQuizTimer();
                submitQuiz(false);
            }
        }));
        quizTimer.setCycleCount(seconds);
        quizTimer.playFromStart();
    }

    private void stopQuizTimer() {
        if (quizTimer != null) {
            quizTimer.stop();
            quizTimer = null;
        }
    }

    private void updateQuizTimerLabel() {
        if (quizTimerLabel == null) {
            return;
        }
        int value = Math.max(0, quizSecondsRemaining);
        quizTimerLabel.setText("Temps: " + value + "s");
    }

    @FXML
    private void onSpeakQuestion() {
        String question = quizQuestionLabel != null ? quizQuestionLabel.getText() : null;
        String text = safe(question).trim();
        if (text.isEmpty()) {
            return;
        }

        // Minimal, front-only TTS: uses Windows built-in System.Speech via PowerShell.
        String os = System.getProperty("os.name", "");
        if (os.toLowerCase(Locale.ROOT).contains("windows")) {
            speakWithWindowsTtsHost(text);
        } else {
            showInfo("Text-to-speech disponible uniquement sur Windows (System.Speech).");
        }
    }

    private void speakWithWindowsTtsHost(String text) {
        String normalized = text
                .replace("\r\n", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ")
                .trim();
        if (normalized.isEmpty()) {
            return;
        }

        try {
            ensureWindowsTtsHost();
            String textB64 = Base64.getEncoder().encodeToString(normalized.getBytes(StandardCharsets.UTF_8));
            ttsHostStdin.write(textB64);
            ttsHostStdin.newLine();
            ttsHostStdin.flush();
        } catch (Exception ex) {
            // If the host died for any reason, reset and let the next click re-create it.
            shutdownTtsHost();
            showInfo("Impossible de demarrer la lecture audio. Reessayez.");
        }
    }

    private void stopTts() {
        if (ttsHostStdin == null) {
            return;
        }
        try {
            ttsHostStdin.write("__STOP__");
            ttsHostStdin.newLine();
            ttsHostStdin.flush();
        } catch (Exception ignored) {
        }
    }

    private void ensureWindowsTtsHost() throws Exception {
        if (ttsHost != null && ttsHost.isAlive() && ttsHostStdin != null) {
            return;
        }

        shutdownTtsHost();

        String script =
                "Add-Type -AssemblyName System.Speech; " +
                "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                "$s.SetOutputToDefaultAudioDevice(); " +
                "$s.Volume = 100; " +
                "$s.Rate = -1; " +
                "$ci = New-Object System.Globalization.CultureInfo('fr-FR'); " +
                "try { $s.SelectVoiceByHints([System.Speech.Synthesis.VoiceGender]::NotSet, [System.Speech.Synthesis.VoiceAge]::NotSet, 0, $ci) } catch { } " +
                "while (($line = [Console]::In.ReadLine()) -ne $null) { " +
                "  if ($line -eq '__QUIT__') { break } " +
                "  if ($line -eq '__STOP__') { try { $s.SpeakAsyncCancelAll() } catch { } ; continue } " +
                "  try { " +
                "    $bytes = [System.Convert]::FromBase64String($line); " +
                "    $txt = [System.Text.Encoding]::UTF8.GetString($bytes); " +
                "    try { $s.SpeakAsyncCancelAll() } catch { } " +
                "    $s.Speak($txt); " +
                "  } catch { } " +
                "}";

        String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));

        ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-Sta",
                "-ExecutionPolicy", "Bypass",
                "-EncodedCommand",
                encoded
        );
        pb.redirectErrorStream(true);
        ttsHost = pb.start();
        ttsHostStdin = new BufferedWriter(new OutputStreamWriter(ttsHost.getOutputStream(), StandardCharsets.UTF_8));

        Thread drain = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(ttsHost.getInputStream(), StandardCharsets.UTF_8))) {
                while (reader.readLine() != null) {
                    // keep stream drained
                }
            } catch (Exception ignored) {
            }
        }, "tts-host-drain");
        drain.setDaemon(true);
        drain.start();
    }

    private void shutdownTtsHost() {
        if (ttsHostStdin != null) {
            try {
                ttsHostStdin.write("__QUIT__");
                ttsHostStdin.newLine();
                ttsHostStdin.flush();
            } catch (Exception ignored) {
            }
        }

        if (ttsHost != null) {
            try {
                ttsHost.destroy();
                if (ttsHost.isAlive()) {
                    ttsHost.destroyForcibly();
                }
            } catch (Exception ignored) {
            }
        }

        ttsHost = null;
        ttsHostStdin = null;
    }

    @FXML
    private void onExportCoursePdf() {
        if (selectedCourse == null) {
            showInfo("Veuillez d'abord ouvrir un cours (catalogue -> lecture) pour l'exporter en PDF.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le cours en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        chooser.setInitialFileName(CoursePdfExporter.suggestFileName(selectedCourse));

        File file = chooser.showSaveDialog(readerPane != null && readerPane.getScene() != null ? readerPane.getScene().getWindow() : null);
        if (file == null) {
            return;
        }

        try {
            CoursePdfExporter.exportCourse(selectedCourse, file.toPath());
            showInfo("PDF exporte: " + file.getAbsolutePath());
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Export PDF");
            alert.setContentText(ex.getMessage() == null ? "Erreur lors de l'export PDF." : ex.getMessage());
            alert.showAndWait();
        }
    }

    private void renderQuizQuestion() {
        Quiz quiz = selectedCourseQuizzes.get(quizIndex);

        quizProgressLabel.setText((quizIndex + 1) + "/" + selectedCourseQuizzes.size());
        quizProgressBar.setProgress((quizIndex + 1) / (double) selectedCourseQuizzes.size());

        quizPrevButton.setDisable(quizIndex == 0);

        boolean isLast = quizIndex == selectedCourseQuizzes.size() - 1;
        quizNextButton.setVisible(!isLast);
        quizNextButton.setManaged(!isLast);
        quizSubmitButton.setVisible(isLast);
        quizSubmitButton.setManaged(isLast);

        quizQuestionLabel.setText(safe(quiz.getQuestion()));

        quizChoicesBox.getChildren().clear();
        quizToggleGroup = new ToggleGroup();

        List<String> choices = parseChoices(quiz.getChoixReponses());
        if (choices.isEmpty()) {
            choices = List.of("A", "B", "C", "D");
        }

        String existing = answersByQuizId.get(quiz.getId());
        for (String choice : choices) {
            RadioButton rb = new RadioButton(choice);
            rb.setToggleGroup(quizToggleGroup);
            rb.getStyleClass().add("runner-choice");
            quizChoicesBox.getChildren().add(rb);
            if (existing != null && equalsAnswer(existing, choice)) {
                rb.setSelected(true);
            }
        }
    }

    private void saveCurrentAnswerIfAny() {
        if (selectedCourseQuizzes.isEmpty()) {
            return;
        }
        String selected = getSelectedAnswer();
        if (selected == null) {
            return;
        }
        answersByQuizId.put(selectedCourseQuizzes.get(quizIndex).getId(), selected);
    }

    private String getSelectedAnswer() {
        if (quizToggleGroup == null) {
            return null;
        }
        Toggle selected = quizToggleGroup.getSelectedToggle();
        if (selected instanceof RadioButton rb) {
            return rb.getText();
        }
        return null;
    }

    private List<Quiz> loadQuizzesForCourse(int courseId) {
        List<Quiz> all = quizService.afficher();
        if (all == null) {
            return List.of();
        }
        return all.stream()
                .filter(Objects::nonNull)
                .filter(q -> q.getCours() != null && q.getCours().getId() == courseId)
                .sorted(Comparator.comparingInt(Quiz::getId))
                .collect(Collectors.toList());
    }

    private void refreshKpis() {
        int coursesCount = allCourses != null ? allCourses.size() : 0;
        List<Quiz> allQuizzes = quizService.afficher();
        int quizCount = allQuizzes != null ? allQuizzes.size() : 0;
        int globalBest = bestScoreByCourse.values().stream().max(Integer::compareTo).orElse(0);

        kpiCourseCount.setText(String.valueOf(coursesCount));
        kpiQuizCount.setText(String.valueOf(quizCount));
        kpiBestScore.setText(globalBest + " pts");
    }

    private void setState(State state) {
        if (state != State.QUIZ) {
            stopQuizTimer();
            stopTts();
        }
        setVisible(catalogPane, state == State.CATALOG);
        setVisible(readerPane, state == State.READER);
        setVisible(quizPane, state == State.QUIZ);
        setVisible(scorePane, state == State.SCORE);
        setVisible(gamesPane, state == State.GAMES);

        switch (state) {
            case CATALOG -> breadcrumbLabel.setText("Catalogue des cours");
            case READER -> breadcrumbLabel.setText("Lecture du cours");
            case QUIZ -> breadcrumbLabel.setText("Quiz");
            case SCORE -> breadcrumbLabel.setText("Score");
            case GAMES -> breadcrumbLabel.setText("Jeux");
        }
    }

    private void setVisible(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private List<String> parseChoices(String raw) {
        if (raw == null) {
            return List.of();
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String inner = trimmed.substring(1, trimmed.length() - 1).trim();
            if (inner.isEmpty()) {
                return List.of();
            }
            List<String> out = new ArrayList<>();
            for (String part : inner.split(",")) {
                String item = part.trim();
                if ((item.startsWith("\"") && item.endsWith("\"")) || (item.startsWith("'") && item.endsWith("'"))) {
                    item = item.substring(1, item.length() - 1);
                }
                if (!item.isBlank()) {
                    out.add(item);
                }
            }
            return out;
        }

        if (trimmed.contains(",")) {
            return Arrays.stream(trimmed.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        }

        if (trimmed.contains("\n")) {
            return Arrays.stream(trimmed.split("\\R"))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
        }

        return List.of(trimmed);
    }

    private boolean equalsAnswer(String selected, String correct) {
        return safe(selected).trim().equalsIgnoreCase(safe(correct).trim());
    }

    private boolean isCorrectAnswer(Quiz quiz, String selectedAnswer) {
        if (quiz == null) {
            return false;
        }

        String selected = safe(selectedAnswer);
        String correctRaw = safe(quiz.getReponseCorrecte());
        if (selected.isBlank() || correctRaw.isBlank()) {
            return false;
        }

        if (equalsAnswer(selected, correctRaw)) {
            return true;
        }

        // Compare by leading option label (A/B/C...) when possible.
        Character selectedLabel = extractLeadingOptionLabel(selected);
        Character correctLabel = extractLeadingOptionLabel(correctRaw);
        if (selectedLabel != null && correctLabel != null
                && Character.toUpperCase(selectedLabel) == Character.toUpperCase(correctLabel)) {
            return true;
        }

        // Compare by stripping prefixes like "A)", "A.", "A -", "1)" from both.
        if (equalsAnswer(stripOptionPrefix(selected), stripOptionPrefix(correctRaw))) {
            return true;
        }

        // Compare by index / letter mapped to choice list.
        List<String> choices = parseChoices(quiz.getChoixReponses());
        if (choices.isEmpty()) {
            return false;
        }

        Integer correctIndex = resolveCorrectChoiceIndex(correctRaw, choices.size());
        if (correctIndex != null) {
            String correctChoice = choices.get(correctIndex);
            if (equalsAnswer(selected, correctChoice)) {
                return true;
            }
            if (equalsAnswer(stripOptionPrefix(selected), stripOptionPrefix(correctChoice))) {
                return true;
            }
        }

        // Also allow match if stored correct text equals the selected text without its prefix.
        return equalsAnswer(stripOptionPrefix(selected), correctRaw);
    }

    private Integer resolveCorrectChoiceIndex(String correctRaw, int choiceCount) {
        if (choiceCount <= 0) {
            return null;
        }

        String trimmed = safe(correctRaw).trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        // 1-based numeric index: "1", "2", ...
        if (trimmed.matches("\\d+")) {
            try {
                int idx1 = Integer.parseInt(trimmed);
                int idx0 = idx1 - 1;
                if (idx0 >= 0 && idx0 < choiceCount) {
                    return idx0;
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        // Letter label: "A", "B", ...
        Character label = extractLeadingOptionLabel(trimmed);
        if (label != null) {
            int idx0 = Character.toUpperCase(label) - 'A';
            if (idx0 >= 0 && idx0 < choiceCount) {
                return idx0;
            }
        }

        return null;
    }

    private Character extractLeadingOptionLabel(String value) {
        String s = safe(value).trim();
        if (s.isEmpty()) {
            return null;
        }

        char first = s.charAt(0);
        if (!Character.isLetter(first)) {
            return null;
        }

        if (s.length() == 1) {
            return first;
        }

        char second = s.charAt(1);
        if (Character.isWhitespace(second) || second == ')' || second == '.' || second == ':' || second == '-' || second == ']') {
            return first;
        }

        return null;
    }

    private String stripOptionPrefix(String value) {
        String s = safe(value).trim();
        if (s.isEmpty()) {
            return "";
        }

        // Leading letter label: "A) text", "B - text", "C. text"
        Character label = extractLeadingOptionLabel(s);
        if (label != null && s.length() >= 2) {
            int i = 1;
            while (i < s.length() && (s.charAt(i) == ')' || s.charAt(i) == '.' || s.charAt(i) == ':' || s.charAt(i) == '-' || Character.isWhitespace(s.charAt(i)))) {
                i++;
            }
            return s.substring(i).trim();
        }

        // Leading numeric label: "1) text", "2 - text"
        if (s.matches("\\d+.*")) {
            int i = 0;
            while (i < s.length() && Character.isDigit(s.charAt(i))) {
                i++;
            }
            while (i < s.length() && (s.charAt(i) == ')' || s.charAt(i) == '.' || s.charAt(i) == ':' || s.charAt(i) == '-' || Character.isWhitespace(s.charAt(i)))) {
                i++;
            }
            return s.substring(i).trim();
        }

        return s;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= maxChars) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxChars - 1)) + "…";
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Quiz");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void applyBackOfficeVisibility() {
        if (backOfficeButton == null) {
            return;
        }
        backOfficeButton.setVisible(backOfficeAccessVisible);
        backOfficeButton.setManaged(backOfficeAccessVisible);
    }
}

