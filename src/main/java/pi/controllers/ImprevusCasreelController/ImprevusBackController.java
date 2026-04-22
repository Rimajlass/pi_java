package pi.controllers.ImprevusCasreelController;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
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
    @FXML private ListView<Imprevus> imprevusListView;
    @FXML private ListView<CasRelles> casListView;
    @FXML private Label casTitreLabel;
    @FXML private Label casMetaLabel;
    @FXML private Label casEtatLabel;
    @FXML private TextArea casDescriptionArea;
    @FXML private TextField raisonRefusField;

    private final ImprevusService imprevusService = new ImprevusService();
    private final CasReelService casReelService = new CasReelService();
    private final ObservableList<Imprevus> imprevus = FXCollections.observableArrayList();
    private final ObservableList<CasRelles> casReels = FXCollections.observableArrayList();
    private Imprevus selectedImprevu;
    private CasRelles selectedCas;

    @FXML
    public void initialize() {
        imprevuTypeComboBox.setItems(FXCollections.observableArrayList("Depense", "Gain"));
        imprevusListView.setItems(imprevus);
        casListView.setItems(casReels);

        imprevusListView.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Imprevus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitre());
            }
        });
        casListView.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(CasRelles item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label title = new Label(item.getTitre());
                title.getStyleClass().add("list-card-title");
                Label meta = new Label((item.getImprevus() == null ? "Manual case" : item.getImprevus().getTitre()) + " - " + item.getType() + " - " + String.format(Locale.US, "%.2f DT", item.getMontant()));
                meta.getStyleClass().add("list-card-meta");
                Label status = new Label(item.getResultat() == null ? CasReelService.STATUT_EN_ATTENTE : item.getResultat());
                status.getStyleClass().add(resolveStatusStyle(item.getResultat()));
                VBox card = new VBox(6, title, meta, status);
                card.getStyleClass().add("list-card-box");
                setText(null);
                setGraphic(card);
            }
        });

        imprevusListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            selectedImprevu = selected;
            if (selected != null) {
                imprevuTitreField.setText(selected.getTitre());
                imprevuTypeComboBox.setValue(selected.getType());
            }
        });
        casListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            selectedCas = selected;
            showSelectedCase(selected);
        });

        refreshImprevus();
        refreshCas();
    }

    @FXML
    private void handleSaveImprevu() {
        String titre = safe(imprevuTitreField);
        String type = imprevuTypeComboBox.getValue();
        if (titre.length() < 3 || type == null) {
            showError("Titre et type de l'imprevu sont obligatoires.");
            return;
        }
        Imprevus imprevu = new Imprevus(titre, type, 0);
        if (selectedImprevu != null) {
            imprevu.setId(selectedImprevu.getId());
            imprevusService.modifier(imprevu);
        } else {
            imprevusService.ajouter(imprevu);
        }
        clearImprevuForm();
        refreshImprevus();
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
    }

    @FXML
    private void handleClearImprevu() {
        clearImprevuForm();
    }

    @FXML
    private void handleAcceptCas() {
        if (selectedCas == null) {
            showError("Selectionne un cas reel a valider.");
            return;
        }
        casReelService.changerStatut(selectedCas.getId(), CasReelService.STATUT_ACCEPTE, null);
        refreshCas();
        reselectCase(selectedCas.getId());
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
        reselectCase(selectedCas.getId());
    }

    @FXML
    private void handleGoFront() {
        switchScene("/imprevus-view.fxml", "Unexpected Events & Real Cases");
    }

    private void refreshImprevus() {
        imprevus.setAll(imprevusService.afficher());
        imprevusListView.refresh();
    }

    private void refreshCas() {
        casReels.setAll(casReelService.afficher());
        casListView.refresh();
        showSelectedCase(selectedCas == null ? null : casReels.stream().filter(c -> c.getId() == selectedCas.getId()).findFirst().orElse(null));
    }

    private double calculateLinkedBudget(Imprevus imprevu) {
        return casReels.stream()
                .filter(cas -> cas.getImprevus() != null && cas.getImprevus().getId() == imprevu.getId())
                .mapToDouble(CasRelles::getMontant)
                .sum();
    }

    private void reselectCase(int id) {
        casReels.stream()
                .filter(cas -> cas.getId() == id)
                .findFirst()
                .ifPresent(cas -> casListView.getSelectionModel().select(cas));
    }

    private void showSelectedCase(CasRelles cas) {
        selectedCas = cas;
        if (cas == null) {
            casTitreLabel.setText("Select a case");
            casMetaLabel.setText("No case selected");
            casEtatLabel.setText("-");
            casDescriptionArea.clear();
            raisonRefusField.clear();
            return;
        }
        casTitreLabel.setText(cas.getTitre());
        casMetaLabel.setText((cas.getImprevus() == null ? "Manual case" : cas.getImprevus().getTitre()) + " - " + cas.getType() + " - " + String.format(Locale.US, "%.2f DT", cas.getMontant()));
        casEtatLabel.setText(cas.getResultat() == null ? CasReelService.STATUT_EN_ATTENTE : cas.getResultat());
        casDescriptionArea.setText(cas.getDescription());
        raisonRefusField.setText(cas.getRaisonRefus() == null ? "" : cas.getRaisonRefus());
    }

    private String resolveStatusStyle(String statut) {
        if (CasReelService.STATUT_ACCEPTE.equals(statut)) {
            return "status-accepted";
        }
        if (CasReelService.STATUT_REFUSE.equals(statut)) {
            return "status-refused";
        }
        return "status-pending";
    }

    private void clearImprevuForm() {
        selectedImprevu = null;
        imprevusListView.getSelectionModel().clearSelection();
        imprevuTitreField.clear();
        imprevuTypeComboBox.getSelectionModel().clearSelection();
    }

    private String safe(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Erreur");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void switchScene(String resource, String title) {
        try {
            Stage stage = (Stage) imprevusListView.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(MainFx.class.getResource(resource));
            stage.setScene(new Scene(loader.load(), 1400, 900));
            stage.setTitle(title);
        } catch (IOException e) {
            throw new RuntimeException("Erreur navigation : " + e.getMessage(), e);
        }
    }
}
