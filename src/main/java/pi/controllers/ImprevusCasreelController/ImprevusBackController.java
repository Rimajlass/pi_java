package pi.controllers.ImprevusCasreelController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import pi.entities.CasRelles;
import pi.entities.Imprevus;
import pi.mains.MainFx;
import pi.services.ImprevusCasreelService.CasReelService;
import pi.services.ImprevusCasreelService.ImprevusService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Locale;

public class ImprevusBackController {

    @FXML private TextField imprevuTitreField;
    @FXML private ComboBox<String> imprevuTypeComboBox;
    @FXML private TextField imprevuBudgetField;
    @FXML private TextField searchImprevuField;
    @FXML private TableView<Imprevus> imprevusTable;
    @FXML private TableColumn<Imprevus, Integer> imprevuIdColumn;
    @FXML private TableColumn<Imprevus, String> imprevuTitreColumn;
    @FXML private TableColumn<Imprevus, String> imprevuTypeColumn;
    @FXML private TableColumn<Imprevus, Double> imprevuBudgetColumn;

    @FXML private ComboBox<String> casImprevuComboBox;
    @FXML private TextField casTitreField;
    @FXML private ComboBox<String> casTypeComboBox;
    @FXML private TextField casMontantField;
    @FXML private TextField casDateField;
    @FXML private TextArea casDescriptionField;
    @FXML private TextField casJustificatifField;
    @FXML private TextField searchCasField;
    @FXML private TableView<CasRelles> casTable;
    @FXML private TableColumn<CasRelles, Integer> casIdColumn;
    @FXML private TableColumn<CasRelles, String> casTitreColumn;
    @FXML private TableColumn<CasRelles, String> casTypeColumn;
    @FXML private TableColumn<CasRelles, Double> casMontantColumn;
    @FXML private TableColumn<CasRelles, String> casImprevuColumn;
    @FXML private TableColumn<CasRelles, String> casStatutColumn;
    @FXML private TextField raisonRefusField;

    private final ImprevusService imprevusService = new ImprevusService();
    private final CasReelService casReelService = new CasReelService();

    private final ObservableList<Imprevus> allImprevus = FXCollections.observableArrayList();
    private final ObservableList<Imprevus> displayedImprevus = FXCollections.observableArrayList();
    private final ObservableList<CasRelles> allCas = FXCollections.observableArrayList();
    private final ObservableList<CasRelles> displayedCas = FXCollections.observableArrayList();

    private Imprevus selectedImprevu;
    private CasRelles selectedCas;

    @FXML
    public void initialize() {
        imprevuTypeComboBox.setItems(FXCollections.observableArrayList("Depense", "Gain"));
        casTypeComboBox.setItems(FXCollections.observableArrayList("Depense", "Gain"));

        imprevuIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        imprevuTitreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        imprevuTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        imprevuBudgetColumn.setCellValueFactory(new PropertyValueFactory<>("budget"));

        casIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        casTitreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        casTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        casMontantColumn.setCellValueFactory(new PropertyValueFactory<>("montant"));
        casImprevuColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(getImprevuLabel(cell.getValue())));
        casStatutColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getResultat()));

        imprevusTable.setItems(displayedImprevus);
        casTable.setItems(displayedCas);

        imprevusTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedImprevu = newValue;
            if (newValue != null) {
                imprevuTitreField.setText(newValue.getTitre());
                imprevuTypeComboBox.setValue(newValue.getType());
                imprevuBudgetField.setText(String.valueOf(newValue.getBudget()));
            }
        });

        casTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedCas = newValue;
            if (newValue != null) {
                fillCasForm(newValue);
            }
        });

        if (searchImprevuField != null) {
            searchImprevuField.textProperty().addListener((obs, oldValue, newValue) -> filterImprevus(newValue));
        }
        if (searchCasField != null) {
            searchCasField.textProperty().addListener((obs, oldValue, newValue) -> filterCas(newValue));
        }

        refreshImprevus();
        refreshCas();
    }

    @FXML
    private void handleSaveImprevu() {
        Imprevus imprevu = readImprevuForm();
        if (imprevu == null) {
            return;
        }
        if (isDuplicateImprevu(imprevu)) {
            showError("Cet imprevu existe deja.");
            return;
        }

        if (selectedImprevu != null) {
            imprevu.setId(selectedImprevu.getId());
            imprevusService.modifier(imprevu);
        } else {
            imprevusService.ajouter(imprevu);
        }
        clearImprevuForm();
        refreshImprevus();
        refreshCasImprevuOptions();
    }

    @FXML
    private void handleDeleteImprevu() {
        if (selectedImprevu == null) {
            showError("Selectionne un imprevu a supprimer.");
            return;
        }
        imprevusService.supprimer(selectedImprevu.getId());
        clearImprevuForm();
        refreshImprevus();
        refreshCas();
        refreshCasImprevuOptions();
    }

    @FXML
    private void handleClearImprevu() {
        clearImprevuForm();
    }

    @FXML
    private void handleSaveCas() {
        CasRelles cas = readCasForm();
        if (cas == null) {
            return;
        }
        if (isDuplicateCas(cas)) {
            showError("Ce cas reel existe deja.");
            return;
        }

        if (selectedCas != null) {
            cas.setId(selectedCas.getId());
            cas.setResultat(selectedCas.getResultat());
            cas.setRaisonRefus(selectedCas.getRaisonRefus());
            cas.setConfirmedAt(selectedCas.getConfirmedAt());
            casReelService.modifier(cas);
        } else {
            cas.setResultat(CasReelService.STATUT_EN_ATTENTE);
            casReelService.ajouter(cas);
        }

        clearCasForm();
        refreshCas();
    }

    @FXML
    private void handleDeleteCas() {
        if (selectedCas == null) {
            showError("Selectionne un cas reel a supprimer.");
            return;
        }
        casReelService.supprimer(selectedCas.getId());
        clearCasForm();
        refreshCas();
    }

    @FXML
    private void handleAcceptCas() {
        if (selectedCas == null) {
            showError("Selectionne un cas reel a accepter.");
            return;
        }
        casReelService.changerStatut(selectedCas.getId(), CasReelService.STATUT_ACCEPTE, null);
        refreshCas();
        clearCasForm();
    }

    @FXML
    private void handleRefuseCas() {
        if (selectedCas == null) {
            showError("Selectionne un cas reel a refuser.");
            return;
        }
        String raison = safe(raisonRefusField);
        if (raison.length() < 3) {
            showError("Ajoute une raison de refus de 3 caracteres minimum.");
            return;
        }
        casReelService.changerStatut(selectedCas.getId(), CasReelService.STATUT_REFUSE, raison);
        refreshCas();
        clearCasForm();
    }

    @FXML
    private void handleClearCas() {
        clearCasForm();
    }

    @FXML
    private void handleGoFront() {
        switchScene("/imprevus-view.fxml", "Unexpected Events & Real Cases");
    }

    private void refreshImprevus() {
        allImprevus.setAll(imprevusService.afficher());
        filterImprevus(searchImprevuField == null ? "" : searchImprevuField.getText());
        refreshCasImprevuOptions();
    }

    private void refreshCas() {
        allCas.setAll(casReelService.afficher());
        filterCas(searchCasField == null ? "" : searchCasField.getText());
    }

    private void refreshCasImprevuOptions() {
        ObservableList<String> options = FXCollections.observableArrayList("Aucun imprevu");
        for (Imprevus imprevu : allImprevus) {
            options.add(formatImprevuOption(imprevu));
        }
        casImprevuComboBox.setItems(options);
        if (casImprevuComboBox.getValue() == null) {
            casImprevuComboBox.setValue("Aucun imprevu");
        }
    }

    private void filterImprevus(String query) {
        String value = safe(query).toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            displayedImprevus.setAll(allImprevus);
            return;
        }
        displayedImprevus.setAll(allImprevus.filtered(imprevu ->
                contains(imprevu.getTitre(), value)
                        || contains(imprevu.getType(), value)
                        || String.valueOf(imprevu.getBudget()).contains(value)
        ));
    }

    private void filterCas(String query) {
        String value = safe(query).toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            displayedCas.setAll(allCas);
            return;
        }
        displayedCas.setAll(allCas.filtered(cas ->
                contains(cas.getTitre(), value)
                        || contains(cas.getType(), value)
                        || contains(cas.getDescription(), value)
                        || contains(getImprevuLabel(cas), value)
                        || contains(cas.getResultat(), value)
        ));
    }

    private Imprevus readImprevuForm() {
        String titre = safe(imprevuTitreField);
        String type = imprevuTypeComboBox.getValue();
        String budgetText = safe(imprevuBudgetField);

        if (titre.length() < 3 || type == null || budgetText.isBlank()) {
            showError("Titre, type et budget de l'imprevu sont obligatoires.");
            return null;
        }
        if (!titre.matches("[\\p{L}0-9 .,'()\\-]{3,}")) {
            showError("Titre d'imprevu invalide.");
            return null;
        }

        double budget = parsePositiveDouble(budgetText, "budget de l'imprevu");
        if (budget <= 0) {
            return null;
        }

        return new Imprevus(titre, type, budget);
    }

    private CasRelles readCasForm() {
        String titre = safe(casTitreField);
        String type = casTypeComboBox.getValue();
        String montantText = safe(casMontantField);
        String dateText = safe(casDateField);
        String description = safe(casDescriptionField);
        String justificatif = safe(casJustificatifField);

        if (titre.length() < 3 || type == null || montantText.isBlank() || dateText.isBlank()) {
            showError("Titre, type, montant et date du cas reel sont obligatoires.");
            return null;
        }
        if (!titre.matches("[\\p{L}0-9 .,'()\\-]{3,}")) {
            showError("Titre de cas reel invalide.");
            return null;
        }
        if (!description.isEmpty() && !description.matches("[\\p{L}0-9 .,!?:;'()\\-\\r\\n]+")) {
            showError("Description de cas reel invalide.");
            return null;
        }

        double montant = parsePositiveDouble(montantText, "montant du cas reel");
        if (montant <= 0) {
            return null;
        }

        LocalDate dateEffet;
        try {
            dateEffet = LocalDate.parse(dateText);
        } catch (Exception e) {
            showError("La date doit etre au format YYYY-MM-DD.");
            return null;
        }
        if (dateEffet.isAfter(LocalDate.now())) {
            showError("La date du cas reel ne peut pas etre dans le futur.");
            return null;
        }

        CasRelles cas = new CasRelles();
        cas.setImprevus(resolveSelectedImprevu());
        cas.setTitre(titre);
        cas.setType(type);
        cas.setMontant(montant);
        cas.setDateEffet(dateEffet);
        cas.setDescription(description);
        cas.setJustificatifFileName(justificatif);
        cas.setCategorie(cas.getImprevus() == null ? "Independant" : "Associe a un imprevu");
        cas.setSolution(selectedCas == null ? null : selectedCas.getSolution());
        return cas;
    }

    private double parsePositiveDouble(String rawValue, String fieldLabel) {
        try {
            double value = Double.parseDouble(rawValue.replace(',', '.'));
            if (value <= 0) {
                showError("Le " + fieldLabel + " doit etre strictement positif.");
                return -1;
            }
            return value;
        } catch (NumberFormatException e) {
            showError("Le " + fieldLabel + " doit etre numerique.");
            return -1;
        }
    }

    private boolean isDuplicateImprevu(Imprevus imprevu) {
        return allImprevus.stream()
                .filter(existing -> selectedImprevu == null || existing.getId() != selectedImprevu.getId())
                .anyMatch(existing ->
                        normalize(existing.getTitre()).equals(normalize(imprevu.getTitre()))
                                && normalize(existing.getType()).equals(normalize(imprevu.getType()))
                                && Double.compare(existing.getBudget(), imprevu.getBudget()) == 0
                );
    }

    private boolean isDuplicateCas(CasRelles cas) {
        return allCas.stream()
                .filter(existing -> selectedCas == null || existing.getId() != selectedCas.getId())
                .anyMatch(existing ->
                        normalize(existing.getTitre()).equals(normalize(cas.getTitre()))
                                && normalize(existing.getType()).equals(normalize(cas.getType()))
                                && Double.compare(existing.getMontant(), cas.getMontant()) == 0
                                && existing.getDateEffet() != null
                                && existing.getDateEffet().equals(cas.getDateEffet())
                                && sameImprevu(existing.getImprevus(), cas.getImprevus())
                );
    }

    private void fillCasForm(CasRelles cas) {
        casTitreField.setText(cas.getTitre());
        casTypeComboBox.setValue(cas.getType());
        casMontantField.setText(String.valueOf(cas.getMontant()));
        casDateField.setText(cas.getDateEffet() == null ? "" : cas.getDateEffet().toString());
        casDescriptionField.setText(cas.getDescription());
        casJustificatifField.setText(cas.getJustificatifFileName());
        raisonRefusField.setText(cas.getRaisonRefus());
        casImprevuComboBox.setValue(cas.getImprevus() == null ? "Aucun imprevu" : formatImprevuOption(cas.getImprevus()));
    }

    private void clearImprevuForm() {
        selectedImprevu = null;
        imprevusTable.getSelectionModel().clearSelection();
        imprevuTitreField.clear();
        imprevuTypeComboBox.getSelectionModel().clearSelection();
        imprevuBudgetField.clear();
    }

    private void clearCasForm() {
        selectedCas = null;
        casTable.getSelectionModel().clearSelection();
        casImprevuComboBox.setValue("Aucun imprevu");
        casTitreField.clear();
        casTypeComboBox.getSelectionModel().clearSelection();
        casMontantField.clear();
        casDateField.clear();
        casDescriptionField.clear();
        casJustificatifField.clear();
        raisonRefusField.clear();
    }

    private Imprevus resolveSelectedImprevu() {
        String value = casImprevuComboBox.getValue();
        if (value == null || value.equals("Aucun imprevu")) {
            return null;
        }
        String idPart = value.split(" - ")[0];
        int id = Integer.parseInt(idPart.replace("#", ""));
        return allImprevus.stream().filter(imprevu -> imprevu.getId() == id).findFirst().orElse(null);
    }

    private String formatImprevuOption(Imprevus imprevu) {
        return "#" + imprevu.getId() + " - " + imprevu.getTitre();
    }

    private String getImprevuLabel(CasRelles cas) {
        return cas.getImprevus() == null ? "Independant" : formatImprevuOption(cas.getImprevus());
    }

    private boolean sameImprevu(Imprevus first, Imprevus second) {
        if (first == null || second == null) {
            return first == null && second == null;
        }
        return first.getId() == second.getId();
    }

    private boolean contains(String source, String value) {
        return source != null && source.toLowerCase(Locale.ROOT).contains(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String safe(TextArea field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void switchScene(String resource, String title) {
        try {
            Stage stage = (Stage) imprevusTable.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(MainFx.class.getResource(resource));
            stage.setScene(new Scene(loader.load(), 1400, 900));
            stage.setTitle(title);
        } catch (IOException e) {
            throw new RuntimeException("Erreur navigation : " + e.getMessage(), e);
        }
    }
}
