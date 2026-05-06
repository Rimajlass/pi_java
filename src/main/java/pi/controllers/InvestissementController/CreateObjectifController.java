package pi.controllers.InvestissementController;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pi.entities.Investissement;
import pi.entities.Objectif;
import pi.entities.User;
import pi.services.InvestissementService.InvestissementService;
import pi.services.InvestissementService.ObjectifMetrics;
import pi.services.InvestissementService.ObjectifService;

import java.time.LocalDate;
import java.util.List;

public class CreateObjectifController {

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> prioriteCombo;

    @FXML
    private TextField multiplierField;

    @FXML
    private ListView<Investissement> investList;

    @FXML
    private Label totalLabel;

    @FXML
    private Label targetLabel;

    private final InvestissementService investissementService = new InvestissementService();
    private final ObjectifService objectifService = new ObjectifService();
    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
        refreshUnlinkedInvestments();
    }

    @FXML
    public void initialize() {
        prioriteCombo.setItems(FXCollections.observableArrayList(
                Objectif.P_BASSE,
                Objectif.P_NORMALE,
                Objectif.P_HAUTE,
                Objectif.P_CRITIQUE
        ));
        prioriteCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : ObjectifMetrics.prioriteLabel(item));
            }
        });
        prioriteCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : ObjectifMetrics.prioriteLabel(item));
            }
        });
        prioriteCombo.getSelectionModel().select(Objectif.P_NORMALE);

        investList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        investList.setCellFactory(lv -> new ListCell<Investissement>() {
            @Override
            protected void updateItem(Investissement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCrypto().getName() + " - " + item.getAmountInvested() + " USD");
                }
            }
        });

        refreshUnlinkedInvestments();

        investList.getSelectionModel().getSelectedItems().addListener(
                (javafx.collections.ListChangeListener<Investissement>) change -> updateLabels()
        );

        multiplierField.textProperty().addListener((obs, oldVal, newVal) -> updateLabels());
    }

    private void refreshUnlinkedInvestments() {
        if (investList == null) {
            return;
        }
        try {
            List<Investissement> unlinked =
                    (currentUser != null && currentUser.getId() > 0)
                            ? investissementService.getUnlinkedByUser(currentUser.getId())
                            : List.of();
            investList.getItems().setAll(unlinked);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLabels() {
        double total = investList.getSelectionModel().getSelectedItems()
                .stream().mapToDouble(Investissement::getAmountInvested).sum();

        totalLabel.setText(String.format("Total investi: %.2f USD", total));

        try {
            double multiplier = Double.parseDouble(multiplierField.getText());
            targetLabel.setText(String.format("Objectif: %.2f USD", total * multiplier));
        } catch (NumberFormatException e) {
            targetLabel.setText("Objectif: 0.0 USD");
        }
    }

    @FXML
    public void confirm() {
        try {
            String name = nameField.getText();
            List<Investissement> selected = investList.getSelectionModel().getSelectedItems();

            if (name == null || name.trim().isEmpty()) {
                showAlert("Attention", "Veuillez entrer un nom.");
                return;
            }

            if (selected.isEmpty()) {
                showAlert("Attention", "Veuillez sélectionner au moins un investissement.");
                return;
            }

            double multiplier;
            try {
                multiplier = Double.parseDouble(multiplierField.getText());
                if (multiplier <= 0) {
                    showAlert("Attention", "Le multiplicateur doit être supérieur à 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Erreur", "Multiplicateur invalide.");
                return;
            }

            double initialAmount = selected.stream()
                    .mapToDouble(Investissement::getAmountInvested).sum();
            double targetAmount = initialAmount * multiplier;

            String prio = prioriteCombo.getSelectionModel().getSelectedItem();
            if (prio == null) {
                prio = Objectif.P_NORMALE;
            }

            Objectif objectif = new Objectif(name, multiplier, initialAmount, targetAmount, false, LocalDate.now(), prio);
            objectifService.add(objectif);

            for (Investissement inv : selected) {
                if (currentUser != null && currentUser.getId() > 0) {
                    objectifService.linkInvestissementForUser(objectif.getId(), inv.getId(), currentUser.getId());
                } else {
                    objectifService.linkInvestissement(objectif.getId(), inv.getId());
                }
            }

            showAlert("Succès", "Objectif créé avec succès.");

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.close();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la création : " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
