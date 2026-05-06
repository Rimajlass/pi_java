package pi.controllers.AiQuizController;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import pi.entities.Cours;
import pi.entities.Quiz;
import pi.entities.User;
import pi.services.AiQuizService.AiQuizGeneratorService;
import pi.services.AiQuizService.AiSettings;
import pi.services.AiQuizService.AiSettingsLoader;
import pi.services.AiQuizService.InsufficientQuotaException;
import pi.services.AiQuizService.QuizGenerationModels.GeneratedQuizBundle;
import pi.services.AiQuizService.QuizGenerationModels.GeneratedQuestion;
import pi.services.CoursQuizService.CoursService;
import pi.services.CoursQuizService.QuizService;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class AiQuizGeneratorController {

    @FXML
    private ComboBox<Cours> courseCombo;
    @FXML
    private TextField topicField;
    @FXML
    private Spinner<Integer> countSpinner;
    @FXML
    private ComboBox<String> difficultyCombo;
    @FXML
    private ComboBox<String> modelCombo;
    @FXML
    private VBox previewBox;
    @FXML
    private Label statusLabel;
    @FXML
    private Button generateButton;
    @FXML
    private Button saveButton;

    private final CoursService coursService = new CoursService();
    private final QuizService quizService = new QuizService();
    private final AiQuizGeneratorService aiService = new AiQuizGeneratorService();

    private User currentUser;
    private GeneratedQuizBundle lastBundle;

    public void setContext(User currentUser) {
        this.currentUser = currentUser;
    }

    @FXML
    public void initialize() {
        countSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 8));
        difficultyCombo.getItems().setAll("Facile", "Moyen", "Difficile");
        difficultyCombo.setValue("Moyen");

        modelCombo.getItems().setAll("gemini-2.5-flash", "gemini-2.5-pro");
        modelCombo.setValue("gemini-2.5-flash");

        if (previewBox != null) {
            previewBox.getChildren().clear();
        }
        statusLabel.setText("");
        saveButton.setDisable(true);

        onRefreshCourses(null);
    }

    @FXML
    public void onRefreshCourses(ActionEvent event) {
        List<Cours> all = coursService.afficher();
        if (all == null) {
            all = List.of();
        }
        List<Cours> sorted = all.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(c -> safe(c.getTitre()).toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        courseCombo.getItems().setAll(sorted);
        if (!sorted.isEmpty() && courseCombo.getValue() == null) {
            courseCombo.setValue(sorted.get(0));
        }
    }

    @FXML
    public void onGenerate(ActionEvent event) {
        Cours cours = courseCombo.getValue();
        String topic = safe(topicField.getText()).trim();
        String difficulty = difficultyCombo.getValue();
        int count = countSpinner.getValue() != null ? countSpinner.getValue() : 8;
        String model = modelCombo.getValue();

        String apiKey = resolveApiKey();
        setBusy(true);
        statusLabel.setText("Generation en cours...");
        clearPreview();
        lastBundle = null;
        saveButton.setDisable(true);

        Task<GeneratedQuizBundle> task = new Task<>() {
            @Override
            protected GeneratedQuizBundle call() throws Exception {
                // Reload cours to ensure we have text content
                if (cours != null && safe(cours.getContenuTexte()).isBlank()) {
                    // best-effort: leave as is if service doesn't hydrate content
                }
                try {
                    return aiService.generateQuiz(apiKey, model, cours, topic, difficulty, count);
                } catch (InsufficientQuotaException quota) {
                    return aiService.generateDemoQuiz(cours, topic, difficulty, count);
                }
            }
        };

        task.setOnSucceeded(e -> {
            GeneratedQuizBundle bundle = task.getValue();
            lastBundle = bundle;
            renderPreview(bundle);
            boolean demo = safe(bundle.title).toLowerCase(Locale.ROOT).contains("(demo)");
            statusLabel.setText((demo ? "Mode DEMO (quota insuffisant). " : "") + "Quiz genere: " + bundle.questions.size() + " questions.");
            saveButton.setDisable(false);
            setBusy(false);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "generation echouee";
            statusLabel.setText("Erreur: " + msg);
            setBusy(false);
        });

        Thread t = new Thread(task, "ai-quiz-generator");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void onSave(ActionEvent event) {
        if (lastBundle == null) {
            statusLabel.setText("Rien a enregistrer.");
            return;
        }
        if (currentUser == null) {
            statusLabel.setText("Utilisateur admin introuvable dans le contexte.");
            return;
        }
        Cours cours = courseCombo.getValue();
        if (cours == null) {
            statusLabel.setText("Veuillez selectionner un cours.");
            return;
        }

        int inserted = 0;
        try {
            for (GeneratedQuestion q : lastBundle.questions) {
                String choices = toJsonArrayString(q.choices);
                String correct = pickCorrectChoice(q);
                Quiz quiz = new Quiz(
                        cours,
                        currentUser,
                        safe(q.question).trim(),
                        choices,
                        correct,
                        Math.max(1, q.points)
                );
                boolean ok = quizService.ajouterWithResult(quiz);
                if (ok) {
                    inserted++;
                }
            }
            if (inserted == 0) {
                statusLabel.setText("Aucun quiz n'a ete enregistre (verifiez la base / contraintes).");
            } else {
                statusLabel.setText("Enregistre: " + inserted + " quiz.");
            }
        } catch (Exception e) {
            statusLabel.setText("Erreur BD: " + (e.getMessage() != null ? e.getMessage() : String.valueOf(e)));
        }
    }

    private void setBusy(boolean busy) {
        if (generateButton != null) {
            generateButton.setDisable(busy);
        }
        if (courseCombo != null) {
            courseCombo.setDisable(busy);
        }
        if (topicField != null) {
            topicField.setDisable(busy);
        }
        if (countSpinner != null) {
            countSpinner.setDisable(busy);
        }
        if (difficultyCombo != null) {
            difficultyCombo.setDisable(busy);
        }
        if (modelCombo != null) {
            modelCombo.setDisable(busy);
        }
    }

    private String resolveApiKey() {
        AiSettings settings = AiSettingsLoader.load();
        return settings.getApiKey() == null ? "" : settings.getApiKey().trim();
    }

    private String pickCorrectChoice(GeneratedQuestion q) {
        if (q == null || q.choices == null || q.choices.isEmpty()) {
            return "";
        }
        String correct = safe(q.correct).trim();
        for (String c : q.choices) {
            if (safe(c).trim().equalsIgnoreCase(correct)) {
                return safe(c).trim();
            }
        }
        return safe(q.choices.get(0)).trim();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private String toJsonArrayString(List<String> items) {
        List<String> list = items != null ? items : List.of();
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (String item : list) {
            String v = safe(item).trim();
            if (v.isBlank()) {
                continue;
            }
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(jsonEscape(v)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String jsonEscape(String s) {
        String v = safe(s);
        return v.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private void clearPreview() {
        if (previewBox == null) {
            return;
        }
        previewBox.getChildren().clear();
    }

    private void renderPreview(GeneratedQuizBundle bundle) {
        clearPreview();
        if (previewBox == null || bundle == null || bundle.questions == null) {
            return;
        }

        Label title = new Label(safe(bundle.title).isBlank() ? "Quiz" : safe(bundle.title));
        title.getStyleClass().add("card-title");
        previewBox.getChildren().add(title);

        int i = 1;
        for (GeneratedQuestion q : bundle.questions) {
            VBox card = new VBox(6);
            card.getStyleClass().add("card");

            Label qTitle = new Label("Q" + i + " (" + Math.max(1, q.points) + " pts)");
            qTitle.getStyleClass().add("filter-label");
            Label qText = new Label(safe(q.question));
            qText.getStyleClass().add("admin-subtitle");
            qText.setWrapText(true);

            VBox choicesBox = new VBox(4);
            List<String> choices = q.choices != null ? q.choices : List.of();
            String correct = pickCorrectChoice(q);
            for (int c = 0; c < choices.size(); c++) {
                String choice = safe(choices.get(c)).trim();
                Label l = new Label((char) ('A' + c) + ") " + choice + (choice.equalsIgnoreCase(correct) ? "  ✓" : ""));
                l.getStyleClass().add("admin-subtitle");
                l.setWrapText(true);
                choicesBox.getChildren().add(l);
            }

            if (!safe(q.explanation).isBlank()) {
                Label exp = new Label("Explication: " + safe(q.explanation).trim());
                exp.getStyleClass().add("admin-subtitle");
                exp.setWrapText(true);
                card.getChildren().addAll(qTitle, qText, choicesBox, exp);
            } else {
                card.getChildren().addAll(qTitle, qText, choicesBox);
            }

            previewBox.getChildren().add(card);
            i++;
        }
    }
}
