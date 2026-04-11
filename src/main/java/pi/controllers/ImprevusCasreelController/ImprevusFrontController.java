package pi.controllers.ImprevusCasreelController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import pi.entities.CasRelles;
import pi.entities.Imprevus;
import pi.services.ImprevusCasreelService.CasReelService;
import pi.services.ImprevusCasreelService.ImprevusService;

import java.io.File;
import java.time.LocalDate;

public class ImprevusFrontController {

    @FXML private TextField casTitreField;
    @FXML private ComboBox<String> casTypeComboBox;
    @FXML private TextField casMontantField;
    @FXML private TextField justificatifField;
    @FXML private TextArea casDescriptionField;
    @FXML private TextField searchImprevusField;
    @FXML private TextField searchCasReelsField;
    @FXML private DatePicker dateEffetPicker;
    @FXML private Label statusLabel;
    @FXML private Label selectedCaseTitleLabel;
    @FXML private Label selectedCaseSourceLabel;
    @FXML private Label selectedCaseDescriptionLabel;
    @FXML private Label impactLabel;
    @FXML private ListView<Imprevus> imprevusListView;
    @FXML private TableView<CasRelles> casReelsTable;
    @FXML private TableColumn<CasRelles, Integer> casIdColumn;
    @FXML private TableColumn<CasRelles, String> casTitreColumn;
    @FXML private TableColumn<CasRelles, String> casTypeColumn;
    @FXML private TableColumn<CasRelles, Double> casMontantColumn;
    @FXML private TableColumn<CasRelles, LocalDate> casDateColumn;
    @FXML private TableColumn<CasRelles, String> casSourceColumn;
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
        dateEffetPicker.setValue(LocalDate.now());

        casIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        casTitreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        casTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        casMontantColumn.setCellValueFactory(new PropertyValueFactory<>("montant"));
        casDateColumn.setCellValueFactory(new PropertyValueFactory<>("dateEffet"));
        casSourceColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(getSourceLabel(cellData.getValue()))
        );

        casReelsTable.setItems(casReelsList);
        casReelsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            casReelSelectionne = selected;
            if (selected != null) {
                remplirFormulaireDepuisCasReel(selected);
                afficherDetails(selected.getTitre(), selected.getDescription(), selected.getMontant(), getSourceLabel(selected));
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
                afficherDetails(selected.getTitre(), "Cas charge depuis la liste des imprevus.", selected.getBudget(), "Imprevu #" + selected.getId());
                saveCasButton.setText("Ajouter manuellement");
                statusLabel.setText("Imprevu selectionne. Tu peux le transformer en cas reel.");
            }
        });

        if (searchImprevusField != null) {
            searchImprevusField.textProperty().addListener((obs, oldValue, newValue) -> filtrerImprevus(newValue));
        }
        if (searchCasReelsField != null) {
            searchCasReelsField.textProperty().addListener((obs, oldValue, newValue) -> filtrerCasReels(newValue));
        }

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

        try {
            if (casReelSelectionne != null) {
                casReel.setId(casReelSelectionne.getId());
                casReelService.modifier(casReel);
                statusLabel.setText("Cas reel mis a jour en base.");
                afficherInfo("Modification", "Cas reel modifie avec succes.");
            } else {
                casReelService.ajouter(casReel);
                statusLabel.setText("Cas reel ajoute en base.");
                afficherInfo("Ajout", "Cas reel ajoute avec succes.");
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

        try {
            if (casReelSelectionne != null) {
                casReel.setId(casReelSelectionne.getId());
                casReelService.modifier(casReel);
                statusLabel.setText("Cas reel lie a l'imprevu et mis a jour en base.");
                afficherInfo("Modification", "Cas reel mis a jour avec succes.");
            } else {
                casReelService.ajouter(casReel);
                statusLabel.setText("Imprevu transforme en cas reel et enregistre en base.");
                afficherInfo("Ajout", "Imprevu transforme en cas reel avec succes.");
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
            statusLabel.setText("Cas reel supprime de la base.");
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

    private void appliquerSolution(String solution) {
        if (casReelSelectionne == null) {
            statusLabel.setText("Selectionne un cas reel d'abord.");
            return;
        }

        casReelSelectionne.setSolution(solution);
        try {
            casReelService.modifier(casReelSelectionne);
            refreshCasReels();
            statusLabel.setText("Solution enregistree en base : " + solution);
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

        double montant;
        try {
            montant = Double.parseDouble(montantText);
        } catch (NumberFormatException e) {
            afficherErreur("Le montant doit etre un nombre valide.");
            return null;
        }

        return new CasRelles(
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
    }

    private String getSourceLabel(CasRelles casReel) {
        return casReel.getImprevus() == null ? "Manuel" : "Imprevu #" + casReel.getImprevus().getId();
    }

    private void filtrerImprevus(String query) {
        String value = query == null ? "" : query.trim().toLowerCase();
        if (value.isEmpty()) {
            imprevusList.setAll(allImprevusList);
            return;
        }

        imprevusList.setAll(allImprevusList.filtered(imprevu ->
                contains(imprevu.getTitre(), value)
                        || contains(imprevu.getType(), value)
                        || String.valueOf(imprevu.getBudget()).contains(value)
        ));
    }

    private void filtrerCasReels(String query) {
        String value = query == null ? "" : query.trim().toLowerCase();
        if (value.isEmpty()) {
            casReelsList.setAll(allCasReelsList);
            return;
        }

        casReelsList.setAll(allCasReelsList.filtered(casReel ->
                contains(casReel.getTitre(), value)
                        || contains(casReel.getType(), value)
                        || contains(casReel.getDescription(), value)
                        || contains(getSourceLabel(casReel), value)
                        || String.valueOf(casReel.getMontant()).contains(value)
        ));
    }

    private boolean contains(String source, String query) {
        return source != null && source.toLowerCase().contains(query);
    }

    private void afficherDetails(String titre, String description, double montant, String source) {
        selectedCaseTitleLabel.setText(titre);
        selectedCaseSourceLabel.setText(source);
        selectedCaseDescriptionLabel.setText(description == null || description.isBlank() ? "Aucune description." : description);
        String signe = montant >= 0 ? "+ " : "- ";
        impactLabel.setText(signe + String.format("%.2f DT", Math.abs(montant)));
    }

    private void resetDetails() {
        selectedCaseTitleLabel.setText("Aucun cas selectionne");
        selectedCaseSourceLabel.setText("Selectionne un imprevu ou ajoute un cas reel");
        selectedCaseDescriptionLabel.setText("Les details du cas apparaissent ici.");
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
}
