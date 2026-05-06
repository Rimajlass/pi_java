package pi.controllers.InvestissementController;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pi.entities.Crypto;
import pi.entities.Investissement;
import pi.services.InvestissementService.CryptoService;
import pi.services.InvestissementService.InvestissementService;

import java.time.LocalDate;
import java.util.List;

public class InvestissementController {

    @FXML
    private ComboBox<Crypto> cryptoBox;

    @FXML
    private TextField amountField;

    private final CryptoService cryptoService = new CryptoService();
    private final InvestissementService investissementService = new InvestissementService();

    @FXML
    public void initialize() {
        try {
            List<Crypto> cryptos = cryptoService.getAll();
            cryptoBox.getItems().addAll(cryptos);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les cryptos : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    public void invest() {
        try {
            Crypto selectedCrypto = cryptoBox.getValue();

            if (selectedCrypto == null) {
                showAlert("Attention", "Veuillez choisir une crypto.", Alert.AlertType.WARNING);
                return;
            }

            String amountText = amountField.getText();

            if (amountText == null || amountText.trim().isEmpty()) {
                showAlert("Attention", "Veuillez saisir un montant.", Alert.AlertType.WARNING);
                return;
            }

            double amountInvested = Double.parseDouble(amountText);

            if (amountInvested <= 0) {
                showAlert("Attention", "Le montant doit être supérieur à 0.", Alert.AlertType.WARNING);
                return;
            }

            double buyPrice = selectedCrypto.getCurrentprice();
            double quantity = amountInvested / buyPrice;

            Investissement investissement = new Investissement(
                    selectedCrypto,
                    null,
                    null,
                    amountInvested,
                    buyPrice,
                    quantity,
                    LocalDate.now()
            );

            investissementService.add(investissement);

            showAlert("Succès", "Investissement ajouté avec succès.", Alert.AlertType.INFORMATION);

            amountField.clear();
            cryptoBox.setValue(null);

            Stage stage = (Stage) amountField.getScene().getWindow();
            stage.close();

        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez entrer un montant valide.", Alert.AlertType.ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'ajout : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}