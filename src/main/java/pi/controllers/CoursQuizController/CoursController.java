package pi.controllers.CoursQuizController;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import pi.entities.Cours;
import pi.entities.User;
import pi.services.CoursQuizService.CoursService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CoursController {

    @FXML
    private StackPane heroBanner;

    @FXML
    private ImageView heroBannerImage;

    @FXML
    private TableView<Cours> coursTable;
    @FXML
    private TableColumn<Cours, String> titreColumn;
    @FXML
    private TableColumn<Cours, String> typeMediaColumn;
    @FXML
    private TextField idField;
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

    @FXML
    private ComboBox<String> sortCombo;

    private final CoursService coursService = new CoursService();
    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(250));

    @FXML
    public void initialize() {
        titreColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitre()));
        typeMediaColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTypeMedia()));

        coursTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected != null) {
                remplirFormulaire(selected);
            }
        });

        if (searchField != null) {
            searchDebounce.setOnFinished(e -> rechercherCours());
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
        titreField.clear();
        contenuField.clear();
        typeMediaField.clear();
        urlMediaField.clear();
        coursTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void actualiserTable() {
        List<Cours> items = coursService.afficher();
        coursTable.setItems(FXCollections.observableArrayList(sortCours(items)));
    }

    private Cours buildCoursFromForm(boolean requireId) {
        String titre = titreField.getText().trim();
        String contenu = contenuField.getText().trim();
        String typeMedia = typeMediaField.getText().trim();
        String urlMedia = urlMediaField.getText().trim();

        if (titre.isEmpty() || contenu.isEmpty() || typeMedia.isEmpty() || urlMedia.isEmpty()) {
            throw new IllegalArgumentException("Tous les champs sont obligatoires.");
        }

        User user = new User();
        user.setId(resolveAuthenticatedUserId());

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

    private int resolveAuthenticatedUserId() {
        if (titreField == null || titreField.getScene() == null || titreField.getScene().getWindow() == null) {
            throw new IllegalArgumentException("Aucun utilisateur connecté (fenêtre non initialisée).");
        }

        if (!(titreField.getScene().getWindow() instanceof Stage stage)) {
            throw new IllegalArgumentException("Aucun utilisateur connecté (fenêtre invalide).");
        }

        Object data = stage.getUserData();
        if (data instanceof User user && user.getId() > 0) {
            return user.getId();
        }

        throw new IllegalArgumentException("Aucun utilisateur connecté. Veuillez vous authentifier.");
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
            List<Cours> items = coursService.rechercher(critere);
            coursTable.setItems(FXCollections.observableArrayList(sortCours(items)));
        }
    }

    private void configureSortCombo() {
        if (sortCombo == null) {
            return;
        }
        sortCombo.getItems().setAll(
                "Titre (A→Z)",
                "Titre (Z→A)",
                "Type media (A→Z)",
                "Type media (Z→A)"
        );
        sortCombo.setValue("Titre (A→Z)");
        sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> rechercherCours());
    }

    private List<Cours> sortCours(List<Cours> items) {
        if (items == null) {
            return List.of();
        }
        List<Cours> sorted = new ArrayList<>(items);
        String sort = sortCombo != null ? sortCombo.getValue() : null;

        Comparator<Cours> byTitre = Comparator.comparing(c -> safeLower(c != null ? c.getTitre() : null));
        Comparator<Cours> byType = Comparator.comparing(c -> safeLower(c != null ? c.getTypeMedia() : null));

        if ("Titre (Z→A)".equals(sort)) {
            sorted.sort(byTitre.reversed());
        } else if ("Type media (A→Z)".equals(sort)) {
            sorted.sort(byType);
        } else if ("Type media (Z→A)".equals(sort)) {
            sorted.sort(byType.reversed());
        } else {
            sorted.sort(byTitre);
        }

        return sorted;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Validation");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
