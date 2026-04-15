package pi.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import pi.entities.Cours;
import pi.entities.Quiz;
import pi.services.CoursQuizService.CoursService;
import pi.services.CoursQuizService.QuizService;

import java.util.*;
import java.util.stream.Collectors;

public class CoursQuizFrontController {

    private enum State {CATALOG, READER, QUIZ, SCORE}

    @FXML
    private Label breadcrumbLabel;
    @FXML
    private Label kpiCourseCount;
    @FXML
    private Label kpiQuizCount;
    @FXML
    private Label kpiBestScore;

    @FXML
    private VBox catalogPane;
    @FXML
    private VBox readerPane;
    @FXML
    private VBox quizPane;
    @FXML
    private VBox scorePane;

    // Catalog
    @FXML
    private TextField courseSearchField;
    @FXML
    private FlowPane courseCardsPane;

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

    private final CoursService coursService = new CoursService();
    private final QuizService quizService = new QuizService();

    private CoursQuizDashboardController dashboardController;

    private List<Cours> allCourses = new ArrayList<>();
    private Cours selectedCourse;
    private List<Quiz> selectedCourseQuizzes = new ArrayList<>();

    private final Map<Integer, Integer> bestScoreByCourse = new HashMap<>();

    private int quizIndex = 0;
    private ToggleGroup quizToggleGroup;
    private final Map<Integer, String> answersByQuizId = new HashMap<>();

    public void setDashboardController(CoursQuizDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    public void onShow() {
        refreshKpis();
    }

    @FXML
    public void initialize() {
        readerContentArea.setText("");
        setState(State.CATALOG);
        onRefreshCourses();

        Platform.runLater(() -> {
            if (!allCourses.isEmpty()) {
                // keep catalog as default, no auto-open
            }
        });
    }

    @FXML
    private void onOpenBackOffice() {
        if (dashboardController != null) {
            dashboardController.showBackOffice();
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
        Label title = new Label(safe(cours.getTitre()));
        title.getStyleClass().add("course-card-title");
        title.setWrapText(true);

        Label subtitle = new Label(truncate(safe(cours.getContenuTexte()), 120));
        subtitle.getStyleClass().add("course-card-subtitle");
        subtitle.setWrapText(true);

        Label badge = new Label(safe(cours.getTypeMedia()).toUpperCase(Locale.ROOT));
        badge.getStyleClass().add("course-badge");
        if ("video".equalsIgnoreCase(cours.getTypeMedia())) {
            badge.getStyleClass().add("course-badge-video");
        } else if ("pdf".equalsIgnoreCase(cours.getTypeMedia())) {
            badge.getStyleClass().add("course-badge-pdf");
        } else if ("image".equalsIgnoreCase(cours.getTypeMedia())) {
            badge.getStyleClass().add("course-badge-image");
        }

        int bestScore = bestScoreByCourse.getOrDefault(cours.getId(), 0);
        Label best = new Label(bestScore > 0 ? ("Meilleur: " + bestScore + " pts") : "Nouveau");
        best.getStyleClass().add(bestScore > 0 ? "chip-info" : "chip-muted");

        HBox meta = new HBox(10, best);
        HBox.setHgrow(best, Priority.NEVER);

        VBox card = new VBox(10, badge, title, subtitle, meta);
        card.getStyleClass().add("course-card");
        card.setOnMouseClicked(evt -> openReader(cours));
        return card;
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
        if (selectedCourseQuizzes.isEmpty()) {
            return;
        }

        String selected = getSelectedAnswer();
        if (selected == null) {
            showError("Choisissez une reponse avant de valider.");
            return;
        }
        answersByQuizId.put(selectedCourseQuizzes.get(quizIndex).getId(), selected);

        int totalPossible = selectedCourseQuizzes.stream().mapToInt(Quiz::getPointsValeur).sum();
        int earned = 0;
        int correctCount = 0;

        for (Quiz q : selectedCourseQuizzes) {
            String ans = answersByQuizId.get(q.getId());
            if (ans != null && equalsAnswer(ans, q.getReponseCorrecte())) {
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

    private void showScore(int earned, int totalPossible, int correctCount) {
        int totalQuestions = selectedCourseQuizzes.size();
        int percent = totalPossible == 0 ? 0 : (int) Math.round(earned * 100.0 / totalPossible);

        scoreValueLabel.setText(earned + " / " + totalPossible + " pts");
        scoreSubtitleLabel.setText(correctCount + "/" + totalQuestions + " bonnes reponses • " + percent + "%");

        scoreDetailsBox.getChildren().clear();
        for (Quiz q : selectedCourseQuizzes) {
            String ans = answersByQuizId.get(q.getId());
            boolean ok = ans != null && equalsAnswer(ans, q.getReponseCorrecte());

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
        setVisible(catalogPane, state == State.CATALOG);
        setVisible(readerPane, state == State.READER);
        setVisible(quizPane, state == State.QUIZ);
        setVisible(scorePane, state == State.SCORE);

        switch (state) {
            case CATALOG -> breadcrumbLabel.setText("Catalogue des cours");
            case READER -> breadcrumbLabel.setText("Lecture du cours");
            case QUIZ -> breadcrumbLabel.setText("Quiz");
            case SCORE -> breadcrumbLabel.setText("Score");
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
}

