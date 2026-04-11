package pi.controllers.ExpenseRevenueController;

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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pi.entities.Expense;
import pi.entities.Revenue;
import pi.entities.User;
import pi.services.RevenueExpenseService.ExpenseService;
import pi.services.RevenueExpenseService.RevenueService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class SalaryExpenseController {

    @FXML
    private Label totalIncomeLabel;
    @FXML
    private Label totalExpensesLabel;
    @FXML
    private Label netBalanceLabel;
    @FXML
    private Label lastTransactionLabel;
    @FXML
    private Label feedbackLabel;

    @FXML
    private TextField revenueAmountField;
    @FXML
    private ComboBox<String> revenueTypeComboBox;
    @FXML
    private DatePicker revenueDatePicker;
    @FXML
    private TextArea revenueDescriptionArea;
    @FXML
    private TextField revenueSearchField;
    @FXML
    private ComboBox<String> revenueSortByComboBox;
    @FXML
    private ComboBox<String> revenueDirectionComboBox;
    @FXML
    private TableView<Revenue> revenueTable;
    @FXML
    private TableColumn<Revenue, Number> revenueIdColumn;
    @FXML
    private TableColumn<Revenue, String> revenueDateColumn;
    @FXML
    private TableColumn<Revenue, Number> revenueAmountColumn;
    @FXML
    private TableColumn<Revenue, String> revenueTypeColumn;
    @FXML
    private TableColumn<Revenue, String> revenueDescriptionColumn;
    @FXML
    private TableColumn<Revenue, Revenue> revenueActionColumn;

    @FXML
    private TextField expenseAmountField;
    @FXML
    private ComboBox<String> expenseCategoryComboBox;
    @FXML
    private DatePicker expenseDatePicker;
    @FXML
    private TextArea expenseDescriptionArea;
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

    private final RevenueService revenueService = new RevenueService();
    private final ExpenseService expenseService = new ExpenseService();
    private final ObservableList<Revenue> revenues = FXCollections.observableArrayList();
    private final ObservableList<ExpenseRow> expenses = FXCollections.observableArrayList();
    private final User currentUser = createCurrentUser();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configureFilters();
        configureRevenueTable();
        configureExpenseTable();
        configureFormDefaults();
        loadData();
    }

    @FXML
    private void handleAddRevenue() {
        try {
            Revenue revenue = new Revenue();
            revenue.setUser(currentUser);
            revenue.setAmount(parseAmount(revenueAmountField.getText(), "Revenue amount"));
            revenue.setType(requireValue(revenueTypeComboBox.getValue(), "Revenue type"));
            revenue.setReceivedAt(Objects.requireNonNullElse(revenueDatePicker.getValue(), LocalDate.now()));
            revenue.setDescription(normalizeText(revenueDescriptionArea.getText()));
            revenue.setCreatedAt(LocalDateTime.now());

            revenueService.add(revenue);
            showInfo("Revenue added successfully.");
            clearRevenueForm();
            loadData();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleAddExpense() {
        try {
            Revenue linkedRevenue = expenseRevenueComboBox.getValue();
            if (linkedRevenue == null || linkedRevenue.getId() <= 0) {
                throw new IllegalArgumentException("Select an associated revenue before adding an expense.");
            }

            Expense expense = new Expense(
                    linkedRevenue,
                    currentUser,
                    parseAmount(expenseAmountField.getText(), "Expense amount"),
                    requireValue(expenseCategoryComboBox.getValue(), "Expense category"),
                    Objects.requireNonNullElse(expenseDatePicker.getValue(), LocalDate.now()),
                    normalizeText(expenseDescriptionArea.getText())
            );

            expenseService.add(expense);
            showInfo("Expense added successfully.");
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

    private void configureFilters() {
        revenueTypeComboBox.setItems(FXCollections.observableArrayList("FIXE", "BONUS", "FREELANCE", "OTHER"));
        expenseCategoryComboBox.setItems(FXCollections.observableArrayList(
                "Alimentation", "Transport", "Loyer", "Sante", "Education", "Loisirs", "Other"
        ));
        revenueSortByComboBox.setItems(FXCollections.observableArrayList("Date", "Amount", "Type", "Id"));
        expenseSortByComboBox.setItems(FXCollections.observableArrayList("Date", "Amount", "Category", "Id"));
        revenueDirectionComboBox.setItems(FXCollections.observableArrayList("Desc", "Asc"));
        expenseDirectionComboBox.setItems(FXCollections.observableArrayList("Desc", "Asc"));

        revenueSearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshRevenueTable());
        expenseSearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshExpenseTable());
        revenueSortByComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshRevenueTable());
        expenseSortByComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshExpenseTable());
        revenueDirectionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshRevenueTable());
        expenseDirectionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshExpenseTable());
    }

    private void configureRevenueTable() {
        revenueIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        revenueDateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getReceivedAt())));
        revenueAmountColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getAmount()));
        revenueTypeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullSafe(cell.getValue().getType())));
        revenueDescriptionColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullSafe(cell.getValue().getDescription())));
        revenueActionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button editButton = createActionButton("Modify", "table-edit-button");
            private final Button deleteButton = new Button("Delete");
            private final HBox actionBox = new HBox(8.0, editButton, deleteButton);

            {
                editButton.setOnAction(event -> {
                    Revenue revenue = getTableView().getItems().get(getIndex());
                    openRevenueEditDialog(revenue);
                });
                deleteButton.setOnAction(event -> {
                    Revenue revenue = getTableView().getItems().get(getIndex());
                    deleteRevenue(revenue);
                });
                deleteButton.getStyleClass().add("table-delete-button");
                actionBox.setFillHeight(false);
            }

            @Override
            protected void updateItem(Revenue item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });
    }

    private void configureExpenseTable() {
        expenseIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getExpense().getId()));
        expenseDateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getExpense().getExpenseDate())));
        expenseAmountColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getExpense().getAmount()));
        expenseCategoryColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullSafe(cell.getValue().getExpense().getCategory())));
        expenseDescriptionColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullSafe(cell.getValue().getExpense().getDescription())));
        expenseRevenueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getRevenueLabel()));
        expenseActionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button editButton = createActionButton("Modify", "table-edit-button");
            private final Button deleteButton = new Button("Delete");
            private final HBox actionBox = new HBox(8.0, editButton, deleteButton);

            {
                editButton.setOnAction(event -> {
                    ExpenseRow row = getTableView().getItems().get(getIndex());
                    openExpenseEditDialog(row);
                });
                deleteButton.setOnAction(event -> {
                    ExpenseRow row = getTableView().getItems().get(getIndex());
                    deleteExpense(row.getExpense());
                });
                deleteButton.getStyleClass().add("table-delete-button");
                actionBox.setFillHeight(false);
            }

            @Override
            protected void updateItem(ExpenseRow item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : actionBox);
            }
        });
    }

    private void configureFormDefaults() {
        revenueTypeComboBox.setValue("FIXE");
        expenseCategoryComboBox.setValue("Alimentation");
        revenueSortByComboBox.setValue("Date");
        expenseSortByComboBox.setValue("Date");
        revenueDirectionComboBox.setValue("Desc");
        expenseDirectionComboBox.setValue("Desc");
        revenueDatePicker.setValue(LocalDate.now());
        expenseDatePicker.setValue(LocalDate.now());
        feedbackLabel.setText("Connected as user id 1.");
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

            refreshRevenueTable();
            refreshExpenseTable();
            refreshDashboard();
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

    private void refreshRevenueTable() {
        String search = normalizeText(revenueSearchField.getText()).toLowerCase(Locale.ROOT);
        Comparator<Revenue> comparator = buildRevenueComparator();

        List<Revenue> filtered = revenues.stream()
                .filter(revenue -> matchesRevenue(revenue, search))
                .sorted(applyDirection(comparator, revenueDirectionComboBox.getValue()))
                .collect(Collectors.toList());

        revenueTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void refreshExpenseTable() {
        String search = normalizeText(expenseSearchField.getText()).toLowerCase(Locale.ROOT);
        Comparator<ExpenseRow> comparator = buildExpenseComparator();

        List<ExpenseRow> filtered = expenses.stream()
                .filter(expense -> matchesExpense(expense, search))
                .sorted(applyDirection(comparator, expenseDirectionComboBox.getValue()))
                .collect(Collectors.toList());

        expenseTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private void refreshDashboard() {
        double totalIncome = revenues.stream().mapToDouble(Revenue::getAmount).sum();
        double totalExpenses = expenses.stream().mapToDouble(row -> row.getExpense().getAmount()).sum();
        double netBalance = totalIncome - totalExpenses;

        LocalDate lastRevenueDate = revenues.stream()
                .map(Revenue::getReceivedAt)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        LocalDate lastExpenseDate = expenses.stream()
                .map(row -> row.getExpense().getExpenseDate())
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        LocalDate lastTransactionDate = lastRevenueDate;
        if (lastTransactionDate == null || (lastExpenseDate != null && lastExpenseDate.isAfter(lastTransactionDate))) {
            lastTransactionDate = lastExpenseDate;
        }

        totalIncomeLabel.setText(formatMoney(totalIncome));
        totalExpensesLabel.setText(formatMoney(totalExpenses));
        netBalanceLabel.setText(formatMoney(netBalance));
        lastTransactionLabel.setText(lastTransactionDate == null ? "--/--/----" : formatDate(lastTransactionDate));
    }

    private Comparator<Revenue> buildRevenueComparator() {
        String sortBy = revenueSortByComboBox.getValue();
        if ("Amount".equals(sortBy)) {
            return Comparator.comparingDouble(Revenue::getAmount);
        }
        if ("Type".equals(sortBy)) {
            return Comparator.comparing(revenue -> nullSafe(revenue.getType()), String.CASE_INSENSITIVE_ORDER);
        }
        if ("Id".equals(sortBy)) {
            return Comparator.comparingInt(Revenue::getId);
        }
        return Comparator.comparing(Revenue::getReceivedAt, Comparator.nullsLast(LocalDate::compareTo));
    }

    private Comparator<ExpenseRow> buildExpenseComparator() {
        String sortBy = expenseSortByComboBox.getValue();
        if ("Amount".equals(sortBy)) {
            return Comparator.comparingDouble(row -> row.getExpense().getAmount());
        }
        if ("Category".equals(sortBy)) {
            return Comparator.comparing(row -> nullSafe(row.getExpense().getCategory()), String.CASE_INSENSITIVE_ORDER);
        }
        if ("Id".equals(sortBy)) {
            return Comparator.comparingInt(row -> row.getExpense().getId());
        }
        return Comparator.comparing(row -> row.getExpense().getExpenseDate(), Comparator.nullsLast(LocalDate::compareTo));
    }

    private <T> Comparator<T> applyDirection(Comparator<T> comparator, String direction) {
        return "Asc".equalsIgnoreCase(direction) ? comparator : comparator.reversed();
    }

    private boolean matchesRevenue(Revenue revenue, String search) {
        if (search.isBlank()) {
            return true;
        }
        return nullSafe(revenue.getType()).toLowerCase(Locale.ROOT).contains(search)
                || nullSafe(revenue.getDescription()).toLowerCase(Locale.ROOT).contains(search);
    }

    private boolean matchesExpense(ExpenseRow row, String search) {
        if (search.isBlank()) {
            return true;
        }
        return nullSafe(row.getExpense().getCategory()).toLowerCase(Locale.ROOT).contains(search)
                || nullSafe(row.getExpense().getDescription()).toLowerCase(Locale.ROOT).contains(search)
                || row.getRevenueLabel().toLowerCase(Locale.ROOT).contains(search);
    }

    private void deleteRevenue(Revenue revenue) {
        try {
            revenueService.delete(revenue.getId());
            showInfo("Revenue deleted.");
            loadData();
        } catch (SQLException exception) {
            showError("Unable to delete revenue: " + exception.getMessage());
        }
    }

    private void deleteExpense(Expense expense) {
        try {
            expenseService.delete(expense.getId());
            showInfo("Expense deleted.");
            loadData();
        } catch (SQLException exception) {
            showError("Unable to delete expense: " + exception.getMessage());
        }
    }

    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private void openRevenueEditDialog(Revenue revenue) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Expense/Revenue/update/revenue-edit-view.fxml"));
            Parent root = loader.load();

            RevenueEditController controller = loader.getController();
            controller.setRevenue(revenue);
            controller.setOnSaved(() -> {
                showInfo("Revenue updated successfully.");
                loadData();
            });

            showDialog(root, "Modify Revenue");
        } catch (IOException exception) {
            showError("Unable to open revenue editor: " + exception.getMessage());
        }
    }

    private void openExpenseEditDialog(ExpenseRow row) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Expense/Revenue/update/expense-edit-view.fxml"));
            Parent root = loader.load();

            ExpenseEditController controller = loader.getController();
            controller.setExpense(row.getExpense());
            controller.setAvailableRevenues(new ArrayList<>(revenues));
            controller.setOnSaved(() -> {
                showInfo("Expense updated successfully.");
                loadData();
            });

            showDialog(root, "Modify Expense");
        } catch (IOException exception) {
            showError("Unable to open expense editor: " + exception.getMessage());
        }
    }

    private void showDialog(Parent root, String title) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (feedbackLabel != null && feedbackLabel.getScene() != null) {
            stage.initOwner(feedbackLabel.getScene().getWindow());
        }
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.showAndWait();
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

    private String requireValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    private void clearRevenueForm() {
        revenueAmountField.clear();
        revenueDescriptionArea.clear();
        revenueDatePicker.setValue(LocalDate.now());
        revenueTypeComboBox.setValue("FIXE");
    }

    private void clearExpenseForm() {
        expenseAmountField.clear();
        expenseDescriptionArea.clear();
        expenseDatePicker.setValue(LocalDate.now());
        expenseCategoryComboBox.setValue("Alimentation");
        expenseRevenueComboBox.getSelectionModel().clearSelection();
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

    private String formatMoney(double amount) {
        return String.format(Locale.US, "%.2f TND", amount);
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

        public String getRevenueLabel() {
            if (revenue == null) {
                return "Revenue #" + (expense.getRevenue() != null ? expense.getRevenue().getId() : "-");
            }
            return "Revenue #" + revenue.getId() + " - " + nullSafeStatic(revenue.getType());
        }

        private static String nullSafeStatic(String value) {
            return value == null ? "" : value;
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
            setText("Revenue #" + item.getId() + " - " + item.getType() + " - " + String.format(Locale.US, "%.2f TND", item.getAmount()));
        }
    }
}
