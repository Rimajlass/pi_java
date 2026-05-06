package pi.controllers.ExpenseRevenueController.UPDATE;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pi.entities.Expense;
import pi.entities.Revenue;
import pi.services.RevenueExpenseService.ExpenseService;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ExpenseEditController {

    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private DatePicker expenseDatePicker;
    @FXML
    private ComboBox<Revenue> revenueComboBox;
    @FXML
    private TextArea descriptionArea;

    private final ExpenseService expenseService = new ExpenseService();
    private Expense expense;
    private Runnable onSaved;

    @FXML
    public void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList(
                "Food", "Transport", "Rent", "Health", "Education", "Leisure", "Other"
        ));
        revenueComboBox.setCellFactory(listView -> new RevenueListCell());
        revenueComboBox.setButtonCell(new RevenueListCell());
    }

    public void setExpense(Expense expense) {
        this.expense = expense;
        amountField.setText(String.valueOf(expense.getAmount()));
        categoryComboBox.setValue(localizeExpenseCategory(expense.getCategory()));
        expenseDatePicker.setValue(Objects.requireNonNullElse(expense.getExpenseDate(), LocalDate.now()));
        descriptionArea.setText(expense.getDescription());
    }

    public void setAvailableRevenues(List<Revenue> revenues) {
        revenueComboBox.setItems(FXCollections.observableArrayList(revenues));
        if (expense != null && expense.getRevenue() != null) {
            Revenue selected = revenues.stream()
                    .filter(revenue -> revenue.getId() == expense.getRevenue().getId())
                    .findFirst()
                    .orElse(null);
            revenueComboBox.setValue(selected);
        }
    }

    public void setOnSaved(Runnable onSaved) {
        this.onSaved = onSaved;
    }

    @FXML
    private void handleSave() {
        try {
            Revenue selectedRevenue = revenueComboBox.getValue();
            if (selectedRevenue == null || selectedRevenue.getId() <= 0) {
                throw new IllegalArgumentException("Associated revenue is required.");
            }

            expense.setRevenue(selectedRevenue);
            expense.setAmount(parseAmount(amountField.getText()));
            expense.setCategory(normalizeExpenseCategory(requireValue(categoryComboBox.getValue(), "Expense category")));
            expense.setExpenseDate(Objects.requireNonNullElse(expenseDatePicker.getValue(), LocalDate.now()));
            expense.setDescription(normalizeText(descriptionArea.getText()));

            expenseService.update(expense);
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
            throw new IllegalArgumentException("Expense amount is required.");
        }

        double amount = Double.parseDouble(normalized);
        if (amount <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero.");
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

    private static class RevenueListCell extends ListCell<Revenue> {
        @Override
        protected void updateItem(Revenue item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                return;
            }
            setText("Revenue #" + item.getId() + " - " + localizeRevenueTypeStatic(item.getType()) + " - "
                    + String.format(Locale.US, "%.2f TND", item.getAmount()));
        }

        private static String localizeRevenueTypeStatic(String value) {
            if (value == null) {
                return "";
            }
            return switch (value.trim().toUpperCase(Locale.ROOT)) {
                case "FIXE", "FIXED" -> "FIXED";
                case "BONUS" -> "BONUS";
                case "FREELANCE" -> "FREELANCE";
                default -> "OTHER";
            };
        }
    }

    private String normalizeExpenseCategory(String value) {
        if (value == null) {
            return "";
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "alimentation", "food" -> "Food";
            case "transport" -> "Transport";
            case "loyer", "rent" -> "Rent";
            case "sante", "santé", "health" -> "Health";
            case "education", "éducation" -> "Education";
            case "loisirs", "leisure" -> "Leisure";
            default -> "Other";
        };
    }

    private String localizeExpenseCategory(String value) {
        return normalizeExpenseCategory(value);
    }

    private String localizeRevenueType(String value) {
        if (value == null) {
            return "";
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "FIXE", "FIXED" -> "FIXED";
            case "BONUS" -> "BONUS";
            case "FREELANCE" -> "FREELANCE";
            default -> "OTHER";
        };
    }
}
