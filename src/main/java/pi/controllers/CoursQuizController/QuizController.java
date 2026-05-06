package pi.controllers.CoursQuizController;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import pi.entities.Cours;
import pi.entities.Quiz;
import pi.entities.User;
import pi.services.CoursQuizService.CoursService;
import pi.services.CoursQuizService.QuizService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class QuizController {

    @FXML
    private StackPane heroBanner;

    @FXML
    private ImageView heroBannerImage;

    @FXML
    private TableView<Quiz> quizTable;
    @FXML
    private TableColumn<Quiz, String> coursTitreColumn;
    @FXML
    private TableColumn<Quiz, String> questionColumn;
    @FXML
    private TableColumn<Quiz, Integer> pointsColumn;
    @FXML
    private TextField idField;
    @FXML
    private ComboBox<Cours> coursCombo;
    @FXML
    private TextArea questionField;
    @FXML
    private TextArea choixReponsesField;
    @FXML
    private TextField reponseCorrecteField;
    @FXML
    private TextField pointsValeurField;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> sortCombo;

    private final QuizService quizService = new QuizService();
    private final CoursService coursService = new CoursService();
    private final Map<Integer, String> coursTitreById = new HashMap<>();
    private final Map<Integer, Cours> coursById = new HashMap<>();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(250));

    @FXML
    public void initialize() {
        configureCoursCombo();
        refreshCoursCache();
        coursTitreColumn.setCellValueFactory(data -> new SimpleStringProperty(resolveCoursTitre(data.getValue())));
        questionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getQuestion()));
        pointsColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getPointsValeur()));

        quizTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                remplirFormulaire(selected);
            }
        });

        if (searchField != null) {
            searchDebounce.setOnFinished(e -> rechercherQuiz());
            searchField.textProperty().addListener((obs, oldValue, newValue) -> searchDebounce.playFromStart());
        }

        configureSortCombo();
        actualiserTable();

        javafx.application.Platform.runLater(this::bindHeroBannerImage);
    }

    private void bindHeroBannerImage() {
        if (heroBanner == null || heroBannerImage == null) {
            return;
        }
        heroBannerImage.fitWidthProperty().bind(heroBanner.widthProperty());
        heroBannerImage.fitHeightProperty().bind(heroBanner.heightProperty());
    }

    @FXML
    private void ajouterQuiz() {
        try {
            Quiz quiz = buildQuizFromForm(false);
            quizService.ajouter(quiz);
            actualiserTable();
            viderFormulaire();
            showInfo("Quiz ajouté avec succès.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void modifierQuiz() {
        try {
            Quiz quiz = buildQuizFromForm(true);
            quizService.modifier(quiz);
            actualiserTable();
            showInfo("Quiz modifié avec succès.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void supprimerQuiz() {
        Quiz selected = quizTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionne un quiz à supprimer.");
            return;
        }

        quizService.supprimer(selected.getId());
        actualiserTable();
        viderFormulaire();
        showInfo("Quiz supprimé avec succès.");
    }

    @FXML
    private void viderFormulaire() {
        idField.clear();
        if (coursCombo != null) {
            coursCombo.getSelectionModel().clearSelection();
            coursCombo.setValue(null);
        }
        questionField.clear();
        choixReponsesField.clear();
        reponseCorrecteField.clear();
        pointsValeurField.clear();
        quizTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void actualiserTable() {
        refreshCoursTitreCache();
        List<Quiz> items = quizService.afficher();
        quizTable.setItems(FXCollections.observableArrayList(sortQuiz(items)));
    }

    private Quiz buildQuizFromForm(boolean requireId) {
        Cours selectedCours = coursCombo != null ? coursCombo.getValue() : null;
        String question = questionField.getText().trim();
        if (!question.endsWith("?")) {
            throw new IllegalArgumentException("La question doit se terminer par un '?'.");
        }
        String choixReponses = choixReponsesField.getText().trim();
        String reponseCorrecte = reponseCorrecteField.getText().trim();
        String pointsText = pointsValeurField.getText().trim();

        if (selectedCours == null || selectedCours.getId() <= 0 || question.isEmpty() || choixReponses.isEmpty() || reponseCorrecte.isEmpty() || pointsText.isEmpty()) {
            throw new IllegalArgumentException("Tous les champs sont obligatoires.");
        }

        Cours cours = new Cours();
        cours.setId(selectedCours.getId());

        User user = new User();
        user.setId(resolveAuthenticatedUserId());

        Quiz quiz = new Quiz(
                cours,
                user,
                question,
                choixReponses,
                reponseCorrecte,
                parsePositiveInt(pointsText, "Les points doivent être un entier positif.")
        );

        if (requireId) {
            String idText = idField.getText().trim();
            if (idText.isEmpty()) {
                throw new IllegalArgumentException("Sélectionne un quiz à modifier.");
            }
            quiz.setId(parsePositiveInt(idText, "L'identifiant du quiz doit être un entier positif."));
        }

        return quiz;
    }

    private void remplirFormulaire(Quiz quiz) {
        idField.setText(String.valueOf(quiz.getId()));
        int coursId = extractCoursId(quiz);
        if (coursCombo != null) {
            Cours cours = coursById.get(coursId);
            coursCombo.setValue(cours);
        }
        questionField.setText(quiz.getQuestion());
        choixReponsesField.setText(quiz.getChoixReponses());
        reponseCorrecteField.setText(quiz.getReponseCorrecte());
        pointsValeurField.setText(String.valueOf(quiz.getPointsValeur()));
    }

    private int parsePositiveInt(String value, String message) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) {
                throw new IllegalArgumentException(message);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
    }

    private int extractCoursId(Quiz quiz) {
        return quiz.getCours() != null ? quiz.getCours().getId() : 0;
    }

    private void refreshCoursTitreCache() {
        refreshCoursCache();
    }

    private void refreshCoursCache() {
        coursTitreById.clear();
        coursById.clear();

        List<Cours> coursList = coursService.afficher();
        if (coursList == null) {
            if (coursCombo != null) {
                coursCombo.getItems().clear();
            }
            return;
        }

        List<Cours> sanitized = coursList.stream()
                .filter(Objects::nonNull)
                .toList();

        for (Cours cours : sanitized) {
            coursTitreById.put(cours.getId(), cours.getTitre());
            coursById.put(cours.getId(), cours);
        }

        if (coursCombo != null) {
            Cours selected = coursCombo.getValue();
            coursCombo.setItems(FXCollections.observableArrayList(sanitized));
            if (selected != null) {
                coursCombo.setValue(coursById.get(selected.getId()));
            }
        }
    }

    private String resolveCoursTitre(Quiz quiz) {
        int coursId = extractCoursId(quiz);
        String titre = coursTitreById.get(coursId);
        if (titre == null || titre.isBlank()) {
            return coursId > 0 ? ("Cours #" + coursId) : "";
        }
        return titre;
    }

    private void configureCoursCombo() {
        if (coursCombo == null) {
            return;
        }

        coursCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Cours cours) {
                return cours == null ? "" : safe(cours.getTitre());
            }

            @Override
            public Cours fromString(String string) {
                return null;
            }
        });

        coursCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Cours item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : safe(item.getTitre()));
            }
        });

        coursCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Cours item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : safe(item.getTitre()));
            }
        });
    }

    private int resolveAuthenticatedUserId() {
        if (questionField == null || questionField.getScene() == null || questionField.getScene().getWindow() == null) {
            throw new IllegalArgumentException("Aucun utilisateur connecté (fenêtre non initialisée).");
        }

        if (!(questionField.getScene().getWindow() instanceof Stage stage)) {
            throw new IllegalArgumentException("Aucun utilisateur connecté (fenêtre invalide).");
        }

        Object data = stage.getUserData();
        if (data instanceof User user && user.getId() > 0) {
            return user.getId();
        }

        throw new IllegalArgumentException("Aucun utilisateur connecté. Veuillez vous authentifier.");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void rechercherQuiz() {
        String critere = searchField.getText() != null ? searchField.getText().trim() : "";
        refreshCoursTitreCache();
        if (critere.isEmpty()) {
            actualiserTable();
        } else {
            List<Quiz> items = quizService.rechercher(critere);
            quizTable.setItems(FXCollections.observableArrayList(sortQuiz(items)));
        }
    }

    private void configureSortCombo() {
        if (sortCombo == null) {
            return;
        }
        sortCombo.getItems().setAll(
                "Cours (A→Z)",
                "Cours (Z→A)",
                "Question (A→Z)",
                "Question (Z→A)",
                "Points (↑)",
                "Points (↓)"
        );
        sortCombo.setValue("Question (A→Z)");
        sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> rechercherQuiz());
    }

    private List<Quiz> sortQuiz(List<Quiz> items) {
        if (items == null) {
            return List.of();
        }
        List<Quiz> sorted = new ArrayList<>(items);
        String sort = sortCombo != null ? sortCombo.getValue() : null;

        Comparator<Quiz> byCours = Comparator.comparing(q -> safeLower(resolveCoursTitre(q)));
        Comparator<Quiz> byQuestion = Comparator.comparing(q -> safeLower(q != null ? q.getQuestion() : null));
        Comparator<Quiz> byPoints = Comparator.comparingInt(q -> q != null ? q.getPointsValeur() : 0);

        if ("Cours (Z→A)".equals(sort)) {
            sorted.sort(byCours.reversed());
        } else if ("Question (A→Z)".equals(sort)) {
            sorted.sort(byQuestion);
        } else if ("Question (Z→A)".equals(sort)) {
            sorted.sort(byQuestion.reversed());
        } else if ("Points (↑)".equals(sort)) {
            sorted.sort(byPoints);
        } else if ("Points (↓)".equals(sort)) {
            sorted.sort(byPoints.reversed());
        } else {
            sorted.sort(byCours);
        }

        return sorted;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Validation");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
