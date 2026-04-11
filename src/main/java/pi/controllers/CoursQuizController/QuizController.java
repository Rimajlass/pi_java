package pi.controllers.CoursQuizController;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import pi.entities.Cours;
import pi.entities.Quiz;
import pi.entities.User;
import pi.services.QuizService;

public class QuizController {

    @FXML
    private TableView<Quiz> quizTable;
    @FXML
    private TableColumn<Quiz, Integer> idColumn;
    @FXML
    private TableColumn<Quiz, Integer> coursIdColumn;
    @FXML
    private TableColumn<Quiz, Integer> userIdColumn;
    @FXML
    private TableColumn<Quiz, String> questionColumn;
    @FXML
    private TableColumn<Quiz, Integer> pointsColumn;
    @FXML
    private TextField idField;
    @FXML
    private TextField coursIdField;
    @FXML
    private TextField userIdField;
    @FXML
    private TextArea questionField;
    @FXML
    private TextArea choixReponsesField;
    @FXML
    private TextField reponseCorrecteField;
    @FXML
    private TextField pointsValeurField;

    private final QuizService quizService = new QuizService();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        coursIdColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(extractCoursId(data.getValue())));
        userIdColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(extractUserId(data.getValue())));
        questionColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getQuestion()));
        pointsColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getPointsValeur()));

        quizTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                remplirFormulaire(selected);
            }
        });

        actualiserTable();
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
        coursIdField.clear();
        userIdField.clear();
        questionField.clear();
        choixReponsesField.clear();
        reponseCorrecteField.clear();
        pointsValeurField.clear();
        quizTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void actualiserTable() {
        quizTable.setItems(FXCollections.observableArrayList(quizService.afficher()));
    }

    private Quiz buildQuizFromForm(boolean requireId) {
        String coursIdText = coursIdField.getText().trim();
        String userIdText = userIdField.getText().trim();
        String question = questionField.getText().trim();
        String choixReponses = choixReponsesField.getText().trim();
        String reponseCorrecte = reponseCorrecteField.getText().trim();
        String pointsText = pointsValeurField.getText().trim();

        if (coursIdText.isEmpty() || userIdText.isEmpty() || question.isEmpty() || choixReponses.isEmpty()
                || reponseCorrecte.isEmpty() || pointsText.isEmpty()) {
            throw new IllegalArgumentException("Tous les champs sont obligatoires.");
        }

        Cours cours = new Cours();
        cours.setId(parsePositiveInt(coursIdText, "L'identifiant du cours doit être un entier positif."));

        User user = new User();
        user.setId(parsePositiveInt(userIdText, "L'identifiant utilisateur doit être un entier positif."));

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
        coursIdField.setText(String.valueOf(extractCoursId(quiz)));
        userIdField.setText(String.valueOf(extractUserId(quiz)));
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

    private int extractUserId(Quiz quiz) {
        return quiz.getUser() != null ? quiz.getUser().getId() : 0;
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Validation");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
