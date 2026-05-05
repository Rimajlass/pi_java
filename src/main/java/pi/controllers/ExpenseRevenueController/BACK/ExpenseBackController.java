package pi.controllers.ExpenseRevenueController.BACK;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pi.entities.Expense;
import pi.entities.Revenue;
import pi.entities.User;
import pi.mains.Main;
import pi.services.RevenueExpenseService.ExpenseService;
import pi.services.RevenueExpenseService.RevenueService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExpenseBackController {

    @FXML
    private Label feedbackLabel;
    @FXML
    private TextField expenseAmountField;
    @FXML
    private ComboBox<String> expenseCategoryComboBox;
    @FXML
    private DatePicker expenseDatePicker;
    @FXML
    private TextArea expenseDescriptionArea;
    @FXML
    private Button expenseSubmitButton;
    @FXML
    private ComboBox<Revenue> expenseRevenueComboBox;
    @FXML
    private TextField expenseSearchField;
    @FXML
    private ComboBox<String> expenseSortByComboBox;
    @FXML
    private ComboBox<String> expenseDirectionComboBox;
    @FXML
    private TableView<ExpenseRow> expenseTable;
    @FXML
    private TableColumn<ExpenseRow, Number> expenseIdColumn;
    @FXML
    private TableColumn<ExpenseRow, String> expenseDateColumn;
    @FXML
    private TableColumn<ExpenseRow, Number> expenseAmountColumn;
    @FXML
    private TableColumn<ExpenseRow, String> expenseCategoryColumn;
    @FXML
    private TableColumn<ExpenseRow, String> expenseDescriptionColumn;
    @FXML
    private TableColumn<ExpenseRow, String> expenseRevenueColumn;
    @FXML
    private TableColumn<ExpenseRow, ExpenseRow> expenseActionColumn;
    @FXML
    private VBox menuList;

    private final RevenueService revenueService = new RevenueService();
    private final ExpenseService expenseService = new ExpenseService();
    private final ObservableList<Revenue> revenues = FXCollections.observableArrayList();
    private final ObservableList<ExpenseRow> expenses = FXCollections.observableArrayList();
    private final User currentUser = createCurrentUser();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Expense editingExpense;

    @FXML
    public void initialize() {
        configureFilters();
        configureExpenseTable();
        configureFormDefaults();
        loadData();
    }

    @FXML
    private void handleAddExpense() {
        try {
            Revenue linkedRevenue = expenseRevenueComboBox.getValue();
            if (linkedRevenue == null || linkedRevenue.getId() <= 0) {
                throw new IllegalArgumentException("Select an associated revenue before adding an expense.");
            }

            double expenseAmount = parseAmount(expenseAmountField.getText(), "Expense amount");
            validateExpenseAgainstRevenue(expenseAmount, linkedRevenue);

            boolean updating = editingExpense != null;
            Expense expense;
            if (updating) {
                expense = editingExpense;
                expense.setRevenue(linkedRevenue);
                expense.setUser(currentUser);
                expense.setAmount(expenseAmount);
                expense.setCategory(normalizeExpenseCategory(requireValue(expenseCategoryComboBox.getValue(), "Expense category")));
                expense.setExpenseDate(Objects.requireNonNullElse(expenseDatePicker.getValue(), LocalDate.now()));
                expense.setDescription(normalizeText(expenseDescriptionArea.getText()));
                expenseService.update(expense);
                showInfo("Expense updated successfully.");
            } else {
                expense = new Expense(
                        linkedRevenue,
                        currentUser,
                        expenseAmount,
                        normalizeExpenseCategory(requireValue(expenseCategoryComboBox.getValue(), "Expense category")),
                        Objects.requireNonNullElse(expenseDatePicker.getValue(), LocalDate.now()),
                        normalizeText(expenseDescriptionArea.getText())
                );
                expenseService.add(expense);
                showInfo("Expense added successfully.");
            }
            clearExpenseForm();
            loadData();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleOpenRevenueInterface() {
        openWindow("/Expense/Revenue/BACK/revenue-back-view.fxml", "Revenue Back Office");
    }

    @FXML
    private void handleOpenFrontInterface() {
        openWindow("/Expense/Revenue/FRONT/salary-expense-view.fxml", "Income & Expense Front Office");
    }

    @FXML
    private void handleOpenFrontInterfaceFromSidebar(MouseEvent event) {
        handleOpenFrontInterface();
    }

    @FXML
    private void handleSidebarSelection(MouseEvent event) {
        if (!(event.getSource() instanceof HBox selectedRow) || menuList == null) {
            return;
        }

        menuList.getChildren().stream()
                .filter(HBox.class::isInstance)
                .map(HBox.class::cast)
                .forEach(row -> row.getStyleClass().remove("menu-row-active"));

        if (!selectedRow.getStyleClass().contains("menu-row-active")) {
            selectedRow.getStyleClass().add("menu-row-active");
        }

        if (selectedRow.getChildren().size() >= 2 && selectedRow.getChildren().get(1) instanceof Label menuLabel) {
            String key = menuLabel.getText();
            if ("Users".equalsIgnoreCase(key)) {
                openWindow("/pi/mains/admin-backend-view.fxml", "Admin Backend");
            } else if ("Transactions".equalsIgnoreCase(key)) {
                openWindow("/pi/mains/transactions-management-view.fxml", "Transactions Management");
            } else if ("Unexpected Events".equalsIgnoreCase(key) || "Real Cases".equalsIgnoreCase(key)) {
                openWindow("/back-office-view.fxml", "Unexpected Events & Real Cases");
            } else if ("Revenues".equalsIgnoreCase(key)) {
                handleOpenRevenueInterface();
            }
        }
    }

    @FXML
    private void handleLogout() {
        openWindow("/pi/mains/login-view.fxml", "User Secure Login");
    }

    private void openWindow(String resource, String title) {
        try {
            Parent root = FXMLLoader.load(Main.class.getResource(resource));
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1460, 900));
            if (feedbackLabel != null && feedbackLabel.getScene() != null) {
                stage.initOwner(feedbackLabel.getScene().getWindow());
            }
            stage.show();
        } catch (IOException exception) {
            showError("Unable to open interface: " + exception.getMessage());
        }
    }

    private void configureFilters() {
        expenseCategoryComboBox.setItems(FXCollections.observableArrayList(
                "Food", "Transport", "Rent", "Health", "Education", "Leisure", "Other"
        ));
        if (expenseSortByComboBox != null) {
            expenseSortByComboBox.setItems(FXCollections.observableArrayList("Date", "Amount", "Category", "Id"));
            expenseSortByComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshExpenseTable());
        }
        if (expenseDirectionComboBox != null) {
            expenseDirectionComboBox.setItems(FXCollections.observableArrayList("Desc", "Asc"));
            expenseDirectionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshExpenseTable());
        }
        if (expenseSearchField != null) {
            expenseSearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshExpenseTable());
        }
        expenseAmountField.textProperty().addListener((observable, oldValue, newValue) -> validateExpenseInputHint());
        expenseRevenueComboBox.valueProperty().addListener((observable, oldValue, newValue) -> validateExpenseInputHint());
    }

    private void configureExpenseTable() {
        expenseIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getExpense().getId()));
        expenseDateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getExpense().getExpenseDate())));
        expenseAmountColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getExpense().getAmount()));
        expenseCategoryColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(localizeExpenseCategory(cell.getValue().getExpense().getCategory())));
        expenseDescriptionColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullSafe(cell.getValue().getExpense().getDescription())));
        expenseRevenueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getRevenueLabel()));
        expenseActionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");
            private final HBox actionBox = new HBox(8.0, deleteButton);

            {
                deleteButton.setOnAction(event -> deleteExpense(getTableView().getItems().get(getIndex()).getExpense()));
                deleteButton.getStyleClass().add("table-delete-button");
            }

            @Override
            protected void updateItem(ExpenseRow item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });
        expenseTable.setRowFactory(table -> {
            TableRowWithExpense row = new TableRowWithExpense();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    startExpenseEdit(row.getItem());
                }
            });
            return row;
        });
    }

    private void configureFormDefaults() {
        expenseCategoryComboBox.setValue("Food");
        if (expenseSortByComboBox != null) {
            expenseSortByComboBox.setValue("Date");
        }
        if (expenseDirectionComboBox != null) {
            expenseDirectionComboBox.setValue("Desc");
        }
        expenseDatePicker.setValue(LocalDate.now());
        if (expenseSubmitButton != null) {
            expenseSubmitButton.setText("Add Expense");
        }
        feedbackLabel.setText("Expense interface ready.");
    }

    private void loadData() {
        try {
            List<Revenue> revenueData = revenueService.getAll().stream()
                    .filter(revenue -> revenue.getUser() != null && revenue.getUser().getId() == currentUser.getId())
                    .collect(Collectors.toList());
            List<ExpenseRow> expenseData = buildExpenseRows(revenueData, expenseService.getAll());

            revenues.setAll(revenueData);
            expenses.setAll(expenseData);
            expenseRevenueComboBox.setItems(FXCollections.observableArrayList(revenueData));
            expenseRevenueComboBox.setCellFactory(listView -> new RevenueListCell());
            expenseRevenueComboBox.setButtonCell(new RevenueListCell());
            refreshExpenseTable();
        } catch (SQLException exception) {
            showError("Database error: " + exception.getMessage());
        }
    }

    private List<ExpenseRow> buildExpenseRows(List<Revenue> revenueData, List<Expense> rawExpenses) {
        List<ExpenseRow> rows = new ArrayList<>();
        for (Expense expense : rawExpenses) {
            if (expense.getUser() == null || expense.getUser().getId() != currentUser.getId()) {
                continue;
            }
            Revenue linkedRevenue = revenueData.stream()
                    .filter(revenue -> expense.getRevenue() != null && revenue.getId() == expense.getRevenue().getId())
                    .findFirst()
                    .orElse(null);
            rows.add(new ExpenseRow(expense, linkedRevenue));
        }
        return rows;
    }

    private void refreshExpenseTable() {
        String search = expenseSearchField == null ? "" : normalizeText(expenseSearchField.getText()).toLowerCase(Locale.ROOT);
        Comparator<ExpenseRow> comparator = buildExpenseComparator();

        List<ExpenseRow> filtered = expenses.stream()
                .filter(expense -> matchesExpense(expense, search))
                .sorted(applyDirection(comparator, expenseDirectionComboBox == null ? "Desc" : expenseDirectionComboBox.getValue()))
                .collect(Collectors.toList());

        expenseTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private Comparator<ExpenseRow> buildExpenseComparator() {
        String sortBy = expenseSortByComboBox == null ? "Date" : expenseSortByComboBox.getValue();
        if ("Amount".equals(sortBy)) {
            return Comparator.comparingDouble(row -> row.getExpense().getAmount());
        }
        if ("Category".equals(sortBy)) {
            return Comparator.comparing(row -> localizeExpenseCategory(row.getExpense().getCategory()), String.CASE_INSENSITIVE_ORDER);
        }
        if ("Id".equals(sortBy)) {
            return Comparator.comparingInt(row -> row.getExpense().getId());
        }
        return Comparator.comparing(row -> row.getExpense().getExpenseDate(), Comparator.nullsLast(LocalDate::compareTo));
    }

    private <T> Comparator<T> applyDirection(Comparator<T> comparator, String direction) {
        return "Asc".equalsIgnoreCase(direction) ? comparator : comparator.reversed();
    }

    private boolean matchesExpense(ExpenseRow row, String search) {
        if (search.isBlank()) {
            return true;
        }
        return localizeExpenseCategory(row.getExpense().getCategory()).toLowerCase(Locale.ROOT).contains(search)
                || nullSafe(row.getExpense().getDescription()).toLowerCase(Locale.ROOT).contains(search)
                || row.getRevenueLabel().toLowerCase(Locale.ROOT).contains(search);
    }

    private void deleteExpense(Expense expense) {
        try {
            boolean wasEditingCurrentExpense = editingExpense != null && editingExpense.getId() == expense.getId();
            expenseService.delete(expense.getId());
            if (wasEditingCurrentExpense) {
                clearExpenseForm();
            }
            showInfo("Expense deleted.");
            loadData();
        } catch (SQLException exception) {
            showError("Unable to delete expense: " + exception.getMessage());
        }
    }

    private void validateExpenseAgainstRevenue(double expenseAmount, Revenue linkedRevenue) {
        if (linkedRevenue != null && expenseAmount > linkedRevenue.getAmount()) {
            throw new IllegalArgumentException(
                    String.format(
                            Locale.US,
                            "Expense amount %.2f TND cannot be greater than the selected revenue %.2f TND.",
                            expenseAmount,
                            linkedRevenue.getAmount()
                    )
            );
        }
    }

    private void validateExpenseInputHint() {
        Revenue linkedRevenue = expenseRevenueComboBox.getValue();
        String rawAmount = normalizeText(expenseAmountField.getText());
        if (linkedRevenue == null || rawAmount.isBlank()) {
            return;
        }
        try {
            double expenseAmount = parseAmount(rawAmount, "Expense amount");
            if (expenseAmount > linkedRevenue.getAmount()) {
                showInfo(String.format(
                        Locale.US,
                        "Warning: expense %.2f TND is greater than selected revenue %.2f TND.",
                        expenseAmount,
                        linkedRevenue.getAmount()
                ));
            } else {
                showInfo(String.format(Locale.US, "Selected revenue limit: %.2f TND.", linkedRevenue.getAmount()));
            }
        } catch (IllegalArgumentException ignored) {
            // Keep current feedback message.
        }
    }

    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private String requireValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    private double parseAmount(String value, String fieldName) {
        String normalized = normalizeText(value).replace(',', '.');
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        double amount = Double.parseDouble(normalized);
        if (amount <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
        return amount;
    }

    private void clearExpenseForm() {
        editingExpense = null;
        expenseAmountField.clear();
        expenseDescriptionArea.clear();
        expenseDatePicker.setValue(LocalDate.now());
        expenseCategoryComboBox.setValue("Food");
        expenseRevenueComboBox.getSelectionModel().clearSelection();
        if (expenseTable != null) {
            expenseTable.getSelectionModel().clearSelection();
        }
        if (expenseSubmitButton != null) {
            expenseSubmitButton.setText("Add Expense");
        }
    }

    private void startExpenseEdit(ExpenseRow row) {
        if (row == null || row.getExpense() == null) {
            return;
        }
        editingExpense = row.getExpense();
        expenseAmountField.setText(String.format(Locale.US, "%.2f", editingExpense.getAmount()));
        expenseCategoryComboBox.setValue(localizeExpenseCategory(editingExpense.getCategory()));
        expenseDatePicker.setValue(Objects.requireNonNullElse(editingExpense.getExpenseDate(), LocalDate.now()));
        expenseDescriptionArea.setText(nullSafe(editingExpense.getDescription()));

        Revenue selectedRevenue = revenues.stream()
                .filter(revenue -> editingExpense.getRevenue() != null && revenue.getId() == editingExpense.getRevenue().getId())
                .findFirst()
                .orElse(row.getRevenue());
        expenseRevenueComboBox.setValue(selectedRevenue);

        if (expenseSubmitButton != null) {
            expenseSubmitButton.setText("Update Expense");
        }
        showInfo("Expense loaded. Double-click another row to edit it, then click Update Expense.");
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

    private void showInfo(String message) {
        feedbackLabel.setText(message);
    }

    private void showError(String message) {
        feedbackLabel.setText(message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Operation failed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatDate(LocalDate date) {
        return date == null ? "--/--/----" : DATE_FORMATTER.format(date);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private User createCurrentUser() {
        User user = new User();
        user.setId(1);
        return user;
    }

    public static class ExpenseRow {
        private final Expense expense;
        private final Revenue revenue;

        public ExpenseRow(Expense expense, Revenue revenue) {
            this.expense = expense;
            this.revenue = revenue;
        }

        public Expense getExpense() {
            return expense;
        }

        public Revenue getRevenue() {
            return revenue;
        }

        public String getRevenueLabel() {
            if (revenue == null) {
                return "Revenue #" + (expense.getRevenue() != null ? expense.getRevenue().getId() : "-");
            }
            return "Revenue #" + revenue.getId() + " - " + localizeRevenueTypeStatic(revenue.getType());
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

    private static class RevenueListCell extends javafx.scene.control.ListCell<Revenue> {
        @Override
        protected void updateItem(Revenue item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                return;
            }
            setText("Revenue #" + item.getId() + " - " + localizeRevenueTypeStatic(item.getType()) + " - " + String.format(Locale.US, "%.2f TND", item.getAmount()));
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

    private static final class TableRowWithExpense extends javafx.scene.control.TableRow<ExpenseRow> {
    }
}
