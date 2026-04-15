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
import pi.entities.User;
import pi.services.CoursQuizService.CoursService;

public class CoursController {

    @FXML
    private TableView<Cours> coursTable;
    @FXML
    private TableColumn<Cours, Integer> idColumn;
    @FXML
    private TableColumn<Cours, Integer> userIdColumn;
    @FXML
    private TableColumn<Cours, String> titreColumn;
    @FXML
    private TableColumn<Cours, String> typeMediaColumn;
    @FXML
    private TextField idField;
    @FXML
    private TextField userIdField;
    @FXML
    private TextField titreField;
    @FXML
    private TextArea contenuField;
    @FXML
    private TextField typeMediaField;
    @FXML
    private TextField urlMediaField;

    @FXML
    private TextField searchField;
    
    private final CoursService coursService = new CoursService();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().getId()));
        userIdColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(extractUserId(data.getValue())));
        titreColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitre()));
        typeMediaColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTypeMedia()));

        coursTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                remplirFormulaire(selected);
            }
        });

        actualiserTable();
    }

    @FXML
    private void ajouterCours() {
        try {
            Cours cours = buildCoursFromForm(false);
            coursService.ajouter(cours);
            actualiserTable();
            viderFormulaire();
            showInfo("Cours ajouté avec succès.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void modifierCours() {
        try {
            Cours cours = buildCoursFromForm(true);
            coursService.modifier(cours);
            actualiserTable();
            showInfo("Cours modifié avec succès.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    @FXML
    private void supprimerCours() {
        Cours selected = coursTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Sélectionne un cours à supprimer.");
            return;
        }

        coursService.supprimer(selected.getId());
        actualiserTable();
        viderFormulaire();
        showInfo("Cours supprimé avec succès.");
    }

    @FXML
    private void viderFormulaire() {
        idField.clear();
        userIdField.clear();
        titreField.clear();
        contenuField.clear();
        typeMediaField.clear();
        urlMediaField.clear();
        coursTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void actualiserTable() {
        coursTable.setItems(FXCollections.observableArrayList(coursService.afficher()));
    }

    private Cours buildCoursFromForm(boolean requireId) {
        String userIdText = userIdField.getText().trim();
        String titre = titreField.getText().trim();
        String contenu = contenuField.getText().trim();
        String typeMedia = typeMediaField.getText().trim();
        String urlMedia = urlMediaField.getText().trim();

        if (userIdText.isEmpty() || titre.isEmpty() || contenu.isEmpty() || typeMedia.isEmpty() || urlMedia.isEmpty()) {
            throw new IllegalArgumentException("Tous les champs sont obligatoires.");
        }

        User user = new User();
        user.setId(parsePositiveInt(userIdText, "L'identifiant utilisateur doit être un entier positif."));

        Cours cours = new Cours();
        cours.setUser(user);
        cours.setTitre(titre);
        cours.setContenuTexte(contenu);
        cours.setTypeMedia(typeMedia);
        cours.setUrlMedia(urlMedia);

        cours.validate();

        if (requireId) {
            String idText = idField.getText().trim();
            if (idText.isEmpty()) {
                throw new IllegalArgumentException("Sélectionne un cours à modifier.");
            }
            cours.setId(parsePositiveInt(idText, "L'identifiant du cours doit être un entier positif."));
        }

        return cours;
    }

    private void remplirFormulaire(Cours cours) {
        idField.setText(String.valueOf(cours.getId()));
        userIdField.setText(String.valueOf(extractUserId(cours)));
        titreField.setText(cours.getTitre());
        contenuField.setText(cours.getContenuTexte());
        typeMediaField.setText(cours.getTypeMedia());
        urlMediaField.setText(cours.getUrlMedia());
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

    private int extractUserId(Cours cours) {
        return cours.getUser() != null ? cours.getUser().getId() : 0;
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void rechercherCours() {
        String critere = searchField.getText() != null ? searchField.getText().trim() : "";
        if (critere.isEmpty()) {
            actualiserTable();
        } else {
            coursTable.setItems(FXCollections.observableArrayList(coursService.rechercher(critere)));
        }
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Validation");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
