package pi.controllers.ExpenseRevenueController.UPDATE;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pi.entities.Revenue;
import pi.services.RevenueExpenseService.RevenueService;

import java.time.LocalDate;
import java.util.Objects;

public class RevenueEditController {

    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<String> typeComboBox;
    @FXML
    private DatePicker receivedAtPicker;
    @FXML
    private TextArea descriptionArea;

    private final RevenueService revenueService = new RevenueService();
    private Revenue revenue;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        typeComboBox.setItems(FXCollections.observableArrayList("FIXE", "BONUS", "FREELANCE", "OTHER"));
    }

    public void setRevenue(Revenue revenue) {
        this.revenue = revenue;
        amountField.setText(String.valueOf(revenue.getAmount()));
        typeComboBox.setValue(revenue.getType());
        receivedAtPicker.setValue(Objects.requireNonNullElse(revenue.getReceivedAt(), LocalDate.now()));
        descriptionArea.setText(revenue.getDescription());
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    private void handleSave() {
        try {
            revenue.setAmount(parseAmount(amountField.getText()));
            revenue.setType(requireValue(typeComboBox.getValue(), "Revenue type"));
            revenue.setReceivedAt(Objects.requireNonNullElse(receivedAtPicker.getValue(), LocalDate.now()));
            revenue.setDescription(normalizeText(descriptionArea.getText()));

            revenueService.update(revenue);
            if (onSaved != null) {
                onSaved.run();
            }
            closeWindow();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private double parseAmount(String value) {
        String normalized = normalizeText(value).replace(',', '.');
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Revenue amount is required.");
        }

        double amount = Double.parseDouble(normalized);
        if (amount <= 0) {
            throw new IllegalArgumentException("Revenue amount must be greater than zero.");
        }
        return amount;
    }

    private String requireValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Update failed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }
}
