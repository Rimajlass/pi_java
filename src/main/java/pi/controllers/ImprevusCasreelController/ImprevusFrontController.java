package pi.controllers.ImprevusCasreelController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import pi.entities.CasRelles;
import pi.entities.Imprevus;
import pi.mains.MainFx;
import pi.services.ImprevusCasreelService.CasReelService;
import pi.services.ImprevusCasreelService.ImprevusService;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;

public class ImprevusFrontController {

    @FXML private TextField casTitreField;
    @FXML private ComboBox<String> casTypeComboBox;
    @FXML private TextField casMontantField;
    @FXML private TextField justificatifField;
    @FXML private TextArea casDescriptionField;
    @FXML private TextField searchImprevusField;
    @FXML private TextField searchCasReelsField;
    @FXML private ComboBox<String> triImprevusComboBox;
    @FXML private ComboBox<String> triCasReelsComboBox;
    @FXML private DatePicker dateEffetPicker;
    @FXML private Label statusLabel;
    @FXML private Label selectedCaseTitleLabel;
    @FXML private Label selectedCaseSourceLabel;
    @FXML private Label selectedCaseDescriptionLabel;
    @FXML private Label impactLabel;
    @FXML private Label selectedCaseEtatLabel;
    @FXML private ListView<Imprevus> imprevusListView;
    @FXML private TableView<CasRelles> casReelsTable;
    @FXML private TableColumn<CasRelles, Integer> casIdColumn;
    @FXML private TableColumn<CasRelles, String> casTitreColumn;
    @FXML private TableColumn<CasRelles, String> casTypeColumn;
    @FXML private TableColumn<CasRelles, Double> casMontantColumn;
    @FXML private TableColumn<CasRelles, LocalDate> casDateColumn;
    @FXML private TableColumn<CasRelles, String> casSourceColumn;
    @FXML private TableColumn<CasRelles, String> casEtatColumn;
    @FXML private Button saveCasButton;

    private final ImprevusService imprevusService = new ImprevusService();
    private final CasReelService casReelService = new CasReelService();
    private final ObservableList<Imprevus> imprevusList = FXCollections.observableArrayList();
    private final ObservableList<CasRelles> casReelsList = FXCollections.observableArrayList();
    private final ObservableList<Imprevus> allImprevusList = FXCollections.observableArrayList();
    private final ObservableList<CasRelles> allCasReelsList = FXCollections.observableArrayList();

    private Imprevus imprevuSelectionne;
    private CasRelles casReelSelectionne;

    @FXML
    public void initialize() {
        casTypeComboBox.setItems(FXCollections.observableArrayList("Depense", "Gain"));
        triImprevusComboBox.setItems(FXCollections.observableArrayList(
                "Titre A-Z",
                "Titre Z-A",
                "Montant croissant",
                "Montant decroissant"
        ));
        triCasReelsComboBox.setItems(FXCollections.observableArrayList(
                "Plus recent",
                "Plus ancien",
                "Montant croissant",
                "Montant decroissant",
                "Titre A-Z",
                "Titre Z-A"
        ));
        triImprevusComboBox.setValue("Titre A-Z");
        triCasReelsComboBox.setValue("Plus recent");
        dateEffetPicker.setValue(LocalDate.now());
        appliquerControleSaisie();

        casIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        casTitreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        casTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        casMontantColumn.setCellValueFactory(new PropertyValueFactory<>("montant"));
        casDateColumn.setCellValueFactory(new PropertyValueFactory<>("dateEffet"));
        casSourceColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(getSourceLabel(cellData.getValue()))
        );
        casEtatColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getResultat())
        );

        casReelsTable.setItems(casReelsList);
        casReelsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            casReelSelectionne = selected;
            if (selected != null) {
                remplirFormulaireDepuisCasReel(selected);
                afficherDetails(selected);
                saveCasButton.setText("Mettre a jour");
                statusLabel.setText("Cas reel selectionne. Toute sauvegarde mettra a jour la base.");
            }
        });

        imprevusListView.setItems(imprevusList);
        imprevusListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Imprevus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitre() + " | " + item.getType() + " | " + String.format("%.2f DT", item.getBudget()));
                }
            }
        });

        imprevusListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            imprevuSelectionne = selected;
            if (selected != null) {
                casReelSelectionne = null;
                chargerDepuisImprevu(selected);
                afficherDetailsDepuisImprevu(selected);
                saveCasButton.setText("Ajouter manuellement");
                statusLabel.setText("Imprevu selectionne. Tu peux le transformer en cas reel.");
            }
        });

        searchImprevusField.textProperty().addListener((obs, oldValue, newValue) -> filtrerImprevus(newValue));
        searchCasReelsField.textProperty().addListener((obs, oldValue, newValue) -> filtrerCasReels(newValue));
        triImprevusComboBox.valueProperty().addListener((obs, oldValue, newValue) ->
                filtrerImprevus(searchImprevusField.getText()));
        triCasReelsComboBox.valueProperty().addListener((obs, oldValue, newValue) ->
                filtrerCasReels(searchCasReelsField.getText()));

        refreshImprevus();
        refreshCasReels();
        resetDetails();
    }

    @FXML
    private void handleChoisirFichier() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir un justificatif");
        File file = fileChooser.showOpenDialog(justificatifField.getScene().getWindow());
        if (file != null) {
            justificatifField.setText(file.getName());
        }
    }

    @FXML
    private void handleCreerDepuisFormulaire() {
        CasRelles casReel = lireFormulaire();
        if (casReel == null) {
            return;
        }
        if (existeDeja(casReel)) {
            afficherErreur("Ce cas reel existe deja.");
            return;
        }

        try {
            if (casReelSelectionne != null) {
                casReel.setId(casReelSelectionne.getId());
                casReel.setResultat(casReelSelectionne.getResultat());
                casReel.setRaisonRefus(casReelSelectionne.getRaisonRefus());
                casReel.setConfirmedAt(casReelSelectionne.getConfirmedAt());
                casReelService.modifier(casReel);
                statusLabel.setText("Cas reel mis a jour.");
                afficherInfo("Modification", "Cas reel modifie avec succes.");
            } else {
                casReel.setResultat(CasReelService.STATUT_EN_ATTENTE);
                casReelService.ajouter(casReel);
                statusLabel.setText("Cas reel ajoute en attente de validation.");
                afficherInfo("Ajout", "Cas reel ajoute avec etat EN_ATTENTE.");
            }
            refreshCasReels();
            viderSelectionsEtFormulaire();
        } catch (RuntimeException e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void handleCreerDepuisImprevu() {
        if (imprevuSelectionne == null) {
            afficherErreur("Selectionne d'abord un imprevu dans la liste.");
            return;
        }

        CasRelles casReel = lireFormulaire();
        if (casReel == null) {
            return;
        }

        casReel.setImprevus(imprevuSelectionne);
        casReel.setCategorie("Depuis imprevu");
        if (existeDeja(casReel)) {
            afficherErreur("Ce cas reel existe deja pour cet imprevu.");
            return;
        }

        try {
            if (casReelSelectionne != null) {
                casReel.setId(casReelSelectionne.getId());
                casReel.setResultat(casReelSelectionne.getResultat());
                casReel.setRaisonRefus(casReelSelectionne.getRaisonRefus());
                casReel.setConfirmedAt(casReelSelectionne.getConfirmedAt());
                casReelService.modifier(casReel);
                statusLabel.setText("Cas reel associe mis a jour.");
                afficherInfo("Modification", "Cas reel mis a jour avec succes.");
            } else {
                casReel.setResultat(CasReelService.STATUT_EN_ATTENTE);
                casReelService.ajouter(casReel);
                statusLabel.setText("Cas reel associe ajoute en attente.");
                afficherInfo("Ajout", "Cas reel ajoute avec etat EN_ATTENTE.");
            }
            refreshCasReels();
            viderSelectionsEtFormulaire();
        } catch (RuntimeException e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void handleActualiserImprevus() {
        refreshImprevus();
        refreshCasReels();
        statusLabel.setText("Donnees actualisees depuis la base.");
    }

    @FXML
    private void handleVider() {
        viderSelectionsEtFormulaire();
        statusLabel.setText("Formulaire vide.");
    }

    @FXML
    private void handleSupprimerCasReel() {
        if (casReelSelectionne == null) {
            afficherErreur("Selectionne un cas reel a supprimer.");
            return;
        }

        try {
            casReelService.supprimer(casReelSelectionne.getId());
            refreshCasReels();
            viderSelectionsEtFormulaire();
            statusLabel.setText("Cas reel supprime.");
            afficherInfo("Suppression", "Cas reel supprime avec succes.");
        } catch (RuntimeException e) {
            afficherErreur(e.getMessage());
        }
    }

    @FXML
    private void handleUseEmergencyFund() {
        appliquerSolution("Emergency Fund");
    }

    @FXML
    private void handleUseObjectif() {
        appliquerSolution("Objectif");
    }

    @FXML
    private void handleAskFamily() {
        appliquerSolution("Famille");
    }

    @FXML
    private void handleGoBack() {
        changerScene("/back-office-view.fxml", "Back Office - Imprevus et Cas reels");
    }

    private void appliquerSolution(String solution) {
        if (casReelSelectionne == null) {
            statusLabel.setText("Selectionne un cas reel d'abord.");
            return;
        }

        casReelSelectionne.setSolution(solution);
        try {
            casReelService.modifier(casReelSelectionne);
            refreshCasReels();
            statusLabel.setText("Solution enregistree : " + solution);
            afficherInfo("Mise a jour", "Solution mise a jour avec succes.");
        } catch (RuntimeException e) {
            afficherErreur(e.getMessage());
        }
    }

    private void refreshImprevus() {
        allImprevusList.setAll(imprevusService.afficher());
        filtrerImprevus(searchImprevusField == null ? "" : searchImprevusField.getText());
    }

    private void refreshCasReels() {
        allCasReelsList.setAll(casReelService.afficher());
        filtrerCasReels(searchCasReelsField == null ? "" : searchCasReelsField.getText());
    }

    private void chargerDepuisImprevu(Imprevus imprevu) {
        casTitreField.setText(imprevu.getTitre());
        casTypeComboBox.setValue("Depense");
        casMontantField.setText(String.valueOf(imprevu.getBudget()));
        casDescriptionField.clear();
        justificatifField.clear();
        dateEffetPicker.setValue(LocalDate.now());
    }

    private void remplirFormulaireDepuisCasReel(CasRelles casReel) {
        casTitreField.setText(casReel.getTitre());
        casTypeComboBox.setValue(casReel.getType());
        casMontantField.setText(String.valueOf(casReel.getMontant()));
        casDescriptionField.setText(casReel.getDescription());
        justificatifField.setText(casReel.getJustificatifFileName());
        dateEffetPicker.setValue(casReel.getDateEffet());
        imprevuSelectionne = casReel.getImprevus();
    }

    private CasRelles lireFormulaire() {
        String titre = safeText(casTitreField);
        String type = casTypeComboBox.getValue();
        String montantText = safeText(casMontantField);
        String description = safeText(casDescriptionField);
        String justificatif = safeText(justificatifField);
        LocalDate dateEffet = dateEffetPicker.getValue();

        if (titre.isEmpty() || type == null || montantText.isEmpty() || dateEffet == null) {
            afficherErreur("Titre, type, montant et date sont obligatoires.");
            return null;
        }
        if (titre.length() < 3) {
            afficherErreur("Le titre doit contenir au moins 3 caracteres.");
            return null;
        }
        if (!titre.matches("[\\p{L}0-9 .,'()\\-]{3,}")) {
            afficherErreur("Le titre contient des caracteres non autorises.");
            return null;
        }
        if (description.length() > 250) {
            afficherErreur("La description ne doit pas depasser 250 caracteres.");
            return null;
        }
        if (!description.isEmpty() && !description.matches("[\\p{L}0-9 .,!?:;'()\\-\\r\\n]+")) {
            afficherErreur("La description contient des caracteres non autorises.");
            return null;
        }
        if (dateEffet.isAfter(LocalDate.now())) {
            afficherErreur("La date d'effet ne peut pas etre dans le futur.");
            return null;
        }

        double montant;
        try {
            montant = Double.parseDouble(montantText.replace(',', '.'));
        } catch (NumberFormatException e) {
            afficherErreur("Le montant doit etre un nombre valide.");
            return null;
        }
        if (montant <= 0) {
            afficherErreur("Le montant doit etre strictement superieur a 0.");
            return null;
        }

        CasRelles cas = new CasRelles(
                imprevuSelectionne,
                titre,
                description,
                type,
                imprevuSelectionne == null ? "Manuel" : "Depuis imprevu",
                montant,
                casReelSelectionne == null ? null : casReelSelectionne.getSolution(),
                dateEffet,
                justificatif
        );
        cas.setResultat(casReelSelectionne == null ? CasReelService.STATUT_EN_ATTENTE : casReelSelectionne.getResultat());
        return cas;
    }

    private boolean existeDeja(CasRelles nouveauCas) {
        return allCasReelsList.stream()
                .filter(casExistant -> casReelSelectionne == null || casExistant.getId() != casReelSelectionne.getId())
                .anyMatch(casExistant ->
                        memesSources(casExistant, nouveauCas)
                                && normaliser(casExistant.getTitre()).equals(normaliser(nouveauCas.getTitre()))
                                && normaliser(casExistant.getType()).equals(normaliser(nouveauCas.getType()))
                                && Double.compare(casExistant.getMontant(), nouveauCas.getMontant()) == 0
                                && casExistant.getDateEffet() != null
                                && casExistant.getDateEffet().equals(nouveauCas.getDateEffet())
                );
    }

    private boolean memesSources(CasRelles premierCas, CasRelles deuxiemeCas) {
        Integer premierImprevuId = premierCas.getImprevus() == null ? null : premierCas.getImprevus().getId();
        Integer deuxiemeImprevuId = deuxiemeCas.getImprevus() == null ? null : deuxiemeCas.getImprevus().getId();
        return premierImprevuId == null ? deuxiemeImprevuId == null : premierImprevuId.equals(deuxiemeImprevuId);
    }

    private void filtrerImprevus(String query) {
        String value = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            imprevusList.setAll(allImprevusList);
            appliquerTriImprevus();
            return;
        }

        imprevusList.setAll(allImprevusList.filtered(imprevu ->
                contains(imprevu.getTitre(), value)
                        || contains(imprevu.getType(), value)
                        || String.valueOf(imprevu.getBudget()).contains(value)
        ));
        appliquerTriImprevus();
    }

    private void filtrerCasReels(String query) {
        String value = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            casReelsList.setAll(allCasReelsList);
            appliquerTriCasReels();
            return;
        }

        casReelsList.setAll(allCasReelsList.filtered(casReel ->
                contains(casReel.getTitre(), value)
                        || contains(casReel.getType(), value)
                        || contains(casReel.getDescription(), value)
                        || contains(getSourceLabel(casReel), value)
                        || contains(casReel.getResultat(), value)
                        || String.valueOf(casReel.getMontant()).contains(value)
        ));
        appliquerTriCasReels();
    }

    private void appliquerControleSaisie() {
        casMontantField.setTextFormatter(new TextFormatter<>(change -> {
            String nouveauTexte = change.getControlNewText();
            return nouveauTexte.matches("\\d{0,7}([\\.,]\\d{0,2})?") ? change : null;
        }));

        casTitreField.setTextFormatter(new TextFormatter<>(change -> {
            String nouveauTexte = change.getControlNewText();
            return nouveauTexte.length() <= 60 ? change : null;
        }));

        casDescriptionField.setTextFormatter(new TextFormatter<>(change -> {
            String nouveauTexte = change.getControlNewText();
            return nouveauTexte.length() <= 250 ? change : null;
        }));
    }

    private void appliquerTriImprevus() {
        String tri = triImprevusComboBox.getValue();
        Comparator<Imprevus> comparator;

        if ("Titre Z-A".equals(tri)) {
            comparator = Comparator.comparing(this::normaliserTitre).reversed();
        } else if ("Montant croissant".equals(tri)) {
            comparator = Comparator.comparingDouble(Imprevus::getBudget);
        } else if ("Montant decroissant".equals(tri)) {
            comparator = Comparator.comparingDouble(Imprevus::getBudget).reversed();
        } else {
            comparator = Comparator.comparing(this::normaliserTitre);
        }

        FXCollections.sort(imprevusList, comparator);
    }

    private void appliquerTriCasReels() {
        String tri = triCasReelsComboBox.getValue();
        Comparator<CasRelles> comparator;

        if ("Plus ancien".equals(tri)) {
            comparator = Comparator.comparing(CasRelles::getDateEffet, Comparator.nullsLast(LocalDate::compareTo))
                    .thenComparing(CasRelles::getId);
        } else if ("Montant croissant".equals(tri)) {
            comparator = Comparator.comparingDouble(CasRelles::getMontant)
                    .thenComparing(this::normaliserTitreCasReel);
        } else if ("Montant decroissant".equals(tri)) {
            comparator = Comparator.comparingDouble(CasRelles::getMontant).reversed()
                    .thenComparing(this::normaliserTitreCasReel);
        } else if ("Titre A-Z".equals(tri)) {
            comparator = Comparator.comparing(this::normaliserTitreCasReel)
                    .thenComparing(CasRelles::getId);
        } else if ("Titre Z-A".equals(tri)) {
            comparator = Comparator.comparing(this::normaliserTitreCasReel, Comparator.reverseOrder())
                    .thenComparing(CasRelles::getId);
        } else {
            comparator = Comparator.comparing(CasRelles::getDateEffet, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(CasRelles::getId, Comparator.reverseOrder());
        }

        FXCollections.sort(casReelsList, comparator);
    }

    private void afficherDetails(CasRelles casReel) {
        selectedCaseTitleLabel.setText(casReel.getTitre());
        selectedCaseSourceLabel.setText(getSourceLabel(casReel));
        selectedCaseDescriptionLabel.setText(casReel.getDescription() == null || casReel.getDescription().isBlank() ? "Aucune description." : casReel.getDescription());
        selectedCaseEtatLabel.setText(casReel.getResultat());
        String signe = casReel.getMontant() >= 0 ? "+ " : "- ";
        impactLabel.setText(signe + String.format("%.2f DT", Math.abs(casReel.getMontant())));
    }

    private void afficherDetailsDepuisImprevu(Imprevus imprevu) {
        selectedCaseTitleLabel.setText(imprevu.getTitre());
        selectedCaseSourceLabel.setText("Imprevu #" + imprevu.getId());
        selectedCaseDescriptionLabel.setText("Cas charge depuis la liste des imprevus.");
        selectedCaseEtatLabel.setText(CasReelService.STATUT_EN_ATTENTE);
        impactLabel.setText("+ " + String.format("%.2f DT", Math.abs(imprevu.getBudget())));
    }

    private void resetDetails() {
        selectedCaseTitleLabel.setText("Aucun cas selectionne");
        selectedCaseSourceLabel.setText("Selectionne un imprevu ou ajoute un cas reel");
        selectedCaseDescriptionLabel.setText("Les details du cas apparaissent ici.");
        selectedCaseEtatLabel.setText("-");
        impactLabel.setText("0.00 DT");
    }

    private void viderSelectionsEtFormulaire() {
        casReelSelectionne = null;
        imprevuSelectionne = null;
        casReelsTable.getSelectionModel().clearSelection();
        imprevusListView.getSelectionModel().clearSelection();
        casTitreField.clear();
        casTypeComboBox.getSelectionModel().clearSelection();
        casMontantField.clear();
        justificatifField.clear();
        casDescriptionField.clear();
        dateEffetPicker.setValue(LocalDate.now());
        saveCasButton.setText("Ajouter manuellement");
        resetDetails();
    }

    private String getSourceLabel(CasRelles casReel) {
        return casReel.getImprevus() == null ? "Manuel" : "Imprevu #" + casReel.getImprevus().getId();
    }

    private String normaliserTitre(Imprevus imprevu) {
        return imprevu == null || imprevu.getTitre() == null ? "" : imprevu.getTitre().toLowerCase(Locale.ROOT);
    }

    private String normaliserTitreCasReel(CasRelles casReel) {
        return casReel == null || casReel.getTitre() == null ? "" : casReel.getTitre().toLowerCase(Locale.ROOT);
    }

    private String normaliser(String valeur) {
        return valeur == null ? "" : valeur.trim().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String source, String query) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(query);
    }

    private String safeText(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String safeText(TextArea area) {
        return area.getText() == null ? "" : area.getText().trim();
    }

    private void afficherErreur(String message) {
        statusLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void afficherInfo(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(titre);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void changerScene(String resource, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
            Scene scene = new Scene(loader.load(), 1500, 950);

            Stage stage = (Stage) casReelsTable.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Impossible d'ouvrir le back office : " + e.getMessage());
        }
    }
}
