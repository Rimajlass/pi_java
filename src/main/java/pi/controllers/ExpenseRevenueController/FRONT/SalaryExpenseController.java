package pi.controllers.ExpenseRevenueController.FRONT;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pi.controllers.ExpenseRevenueController.UPDATE.ExpenseEditController;
import pi.controllers.ExpenseRevenueController.UPDATE.RevenueEditController;
import pi.entities.Expense;
import pi.entities.Revenue;
import pi.entities.User;
import pi.mains.Main;
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
    private Label profileNameLabel;
    @FXML
    private Button overviewNavButton;
    @FXML
    private Button revenueNavButton;
    @FXML
    private Button expenseNavButton;
    @FXML
    private VBox overviewPanel;
    @FXML
    private VBox revenuePanel;
    @FXML
    private VBox expensePanel;
    @FXML
    private ScrollPane overviewPanelScroll;
    @FXML
    private ScrollPane revenuePanelScroll;
    @FXML
    private ScrollPane expensePanelScroll;

    @FXML
    private TextField revenueAmountField;
    @FXML
    private ComboBox<String> revenueTypeComboBox;
    @FXML
    private DatePicker revenueDatePicker;
    @FXML
    private TextArea revenueDescriptionArea;
    @FXML
    private TextField revenueAmountFieldSecondary;
    @FXML
    private ComboBox<String> revenueTypeComboBoxSecondary;
    @FXML
    private DatePicker revenueDatePickerSecondary;
    @FXML
    private TextArea revenueDescriptionAreaSecondary;
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
        configureBackOfficeNavigation();
        loadData();
    }

    public void setUser(User user) {
        if (user == null) {
            return;
        }
        currentUser.setId(user.getId());
        if (profileNameLabel != null && user.getNom() != null && !user.getNom().isBlank()) {
            profileNameLabel.setText(user.getNom());
        }
        loadData();
    }

    @FXML
    private void handleAddRevenue() {
        addRevenueFromForm(
                revenueAmountField,
                revenueTypeComboBox,
                revenueDatePicker,
                revenueDescriptionArea
        );
    }

    @FXML
    private void handleAddRevenueFromRevenuePanel() {
        addRevenueFromForm(
                revenueAmountFieldSecondary,
                revenueTypeComboBoxSecondary,
                revenueDatePickerSecondary,
                revenueDescriptionAreaSecondary
        );
    }

    private void addRevenueFromForm(
            TextField amountField,
            ComboBox<String> typeComboBox,
            DatePicker datePicker,
            TextArea descriptionArea
    ) {
        try {
            Revenue revenue = new Revenue();
            revenue.setUser(currentUser);
            revenue.setAmount(parseAmount(amountField.getText(), "Revenue amount"));
            revenue.setType(requireValue(typeComboBox.getValue(), "Revenue type"));
            revenue.setReceivedAt(Objects.requireNonNullElse(datePicker.getValue(), LocalDate.now()));
            revenue.setDescription(normalizeText(descriptionArea.getText()));
            revenue.setCreatedAt(LocalDateTime.now());

            revenueService.add(revenue);
            showInfo("Revenue added successfully.");
            clearRevenueForms();
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

            double expenseAmount = parseAmount(expenseAmountField.getText(), "Expense amount");
            validateExpenseAgainstRevenue(expenseAmount, linkedRevenue);

            Expense expense = new Expense(
                    linkedRevenue,
                    currentUser,
                    expenseAmount,
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

    @FXML
    private void handleOpenHomeAction(ActionEvent event) {
        openPage(event, "/pi/mains/salary-home-view.fxml", "/pi/styles/salary-home.css", "Salary Home");
    }

    @FXML
    private void handleOpenAboutAction(ActionEvent event) {
        openPage(event, "/pi/mains/about-view.fxml", "/pi/styles/about.css", "About Us");
    }

    @FXML
    private void handleOpenServiceAction(ActionEvent event) {
        openPage(event, "/pi/mains/service-view.fxml", "/pi/styles/service.css", "Services");
    }

    @FXML
    private void handleOpenContactAction(ActionEvent event) {
        openPage(event, "/pi/mains/contact-view.fxml", "/pi/styles/contact.css", "Contact");
    }

    @FXML
    private void handleLogoutAction(ActionEvent event) {
        openPage(event, "/pi/mains/login-view.fxml", "/pi/styles/login.css", "User Secure Login");
    }

    private void configureFilters() {
        revenueTypeComboBox.setItems(FXCollections.observableArrayList("FIXE", "BONUS", "FREELANCE", "OTHER"));
        if (revenueTypeComboBoxSecondary != null) {
            revenueTypeComboBoxSecondary.setItems(FXCollections.observableArrayList("FIXE", "BONUS", "FREELANCE", "OTHER"));
        }
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
        expenseAmountField.textProperty().addListener((observable, oldValue, newValue) -> validateExpenseInputHint());
        expenseRevenueComboBox.valueProperty().addListener((observable, oldValue, newValue) -> validateExpenseInputHint());
    }

    private void configureBackOfficeNavigation() {
        if (overviewPanel == null || revenuePanel == null) {
            return;
        }
        showPanel("overview");
    }

    @FXML
    private void handleShowOverview() {
        showPanel("overview");
    }

    @FXML
    private void handleShowRevenuePanel() {
        showPanel("revenue");
    }

    @FXML
    private void handleShowExpensePanel() {
        showPanel("expense");
    }

    @FXML
    private void handleOpenFrontInterface() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Expense/Revenue/FRONT/salary-expense-view.fxml"));
            Parent root = loader.load();

            Stage frontStage = new Stage();
            frontStage.setTitle("Income & Expense Front Office");
            frontStage.setScene(new Scene(root, 1400, 900));
            if (feedbackLabel != null && feedbackLabel.getScene() != null) {
                frontStage.initOwner(feedbackLabel.getScene().getWindow());
            }
            frontStage.show();
            showInfo("Front interface opened in a new window.");
        } catch (IOException exception) {
            showError("Unable to open front interface: " + exception.getMessage());
        }
    }

    private void showPanel(String panelName) {
        boolean overviewVisible = "overview".equals(panelName);
        boolean revenueVisible = "revenue".equals(panelName);
        boolean expenseVisible = "expense".equals(panelName);

        if (overviewPanelScroll != null) {
            overviewPanelScroll.setVisible(overviewVisible);
            overviewPanelScroll.setManaged(overviewVisible);
        } else if (overviewPanel != null) {
            overviewPanel.setVisible(overviewVisible);
            overviewPanel.setManaged(overviewVisible);
        }
        if (revenuePanelScroll != null) {
            revenuePanelScroll.setVisible(revenueVisible);
            revenuePanelScroll.setManaged(revenueVisible);
        } else if (revenuePanel != null) {
            revenuePanel.setVisible(revenueVisible);
            revenuePanel.setManaged(revenueVisible);
        }
        if (expensePanelScroll != null) {
            expensePanelScroll.setVisible(expenseVisible);
            expensePanelScroll.setManaged(expenseVisible);
        } else if (expensePanel != null) {
            expensePanel.setVisible(expenseVisible);
            expensePanel.setManaged(expenseVisible);
        }

        updateSidebarSelection(overviewVisible, revenueVisible, expenseVisible);
    }

    private void updateSidebarSelection(boolean overviewVisible, boolean revenueVisible, boolean expenseVisible) {
        updateNavButtonState(overviewNavButton, overviewVisible);
        updateNavButtonState(revenueNavButton, revenueVisible);
        updateNavButtonState(expenseNavButton, expenseVisible);
    }

    private void updateNavButtonState(Button button, boolean active) {
        if (button == null) {
            return;
        }
        if (active) {
            if (!button.getStyleClass().contains("sidebar-button-active")) {
                button.getStyleClass().add("sidebar-button-active");
            }
        } else {
            button.getStyleClass().remove("sidebar-button-active");
        }
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
        if (revenueTypeComboBoxSecondary != null) {
            revenueTypeComboBoxSecondary.setValue("FIXE");
        }
        expenseCategoryComboBox.setValue("Alimentation");
        revenueSortByComboBox.setValue("Date");
        expenseSortByComboBox.setValue("Date");
        revenueDirectionComboBox.setValue("Desc");
        expenseDirectionComboBox.setValue("Desc");
        revenueDatePicker.setValue(LocalDate.now());
        if (revenueDatePickerSecondary != null) {
            revenueDatePickerSecondary.setValue(LocalDate.now());
        }
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
            if (expenseRevenueComboBox != null) {
                expenseRevenueComboBox.setItems(FXCollections.observableArrayList(revenueData));
                expenseRevenueComboBox.setCellFactory(listView -> new RevenueListCell());
                expenseRevenueComboBox.setButtonCell(new RevenueListCell());
            }

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

        if (totalIncomeLabel != null) {
            totalIncomeLabel.setText(formatMoney(totalIncome));
        }
        if (totalExpensesLabel != null) {
            totalExpensesLabel.setText(formatMoney(totalExpenses));
        }
        if (netBalanceLabel != null) {
            netBalanceLabel.setText(formatMoney(netBalance));
        }
        if (lastTransactionLabel != null) {
            lastTransactionLabel.setText(lastTransactionDate == null ? "--/--/----" : formatDate(lastTransactionDate));
        }
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
                showInfo(String.format(
                        Locale.US,
                        "Selected revenue limit: %.2f TND.",
                        linkedRevenue.getAmount()
                ));
            }
        } catch (IllegalArgumentException ignored) {
            // Keep existing feedback unchanged until the user enters a valid number.
        }
    }

    private String requireValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }

    private void clearRevenueForms() {
        if (revenueAmountField != null) {
            revenueAmountField.clear();
        }
        if (revenueDescriptionArea != null) {
            revenueDescriptionArea.clear();
        }
        if (revenueDatePicker != null) {
            revenueDatePicker.setValue(LocalDate.now());
        }
        if (revenueTypeComboBox != null) {
            revenueTypeComboBox.setValue("FIXE");
        }
        if (revenueAmountFieldSecondary != null) {
            revenueAmountFieldSecondary.clear();
        }
        if (revenueDescriptionAreaSecondary != null) {
            revenueDescriptionAreaSecondary.clear();
        }
        if (revenueDatePickerSecondary != null) {
            revenueDatePickerSecondary.setValue(LocalDate.now());
        }
        if (revenueTypeComboBoxSecondary != null) {
            revenueTypeComboBoxSecondary.setValue("FIXE");
        }
    }

    private void clearExpenseForm() {
        expenseAmountField.clear();
        expenseDescriptionArea.clear();
        expenseDatePicker.setValue(LocalDate.now());
        expenseCategoryComboBox.setValue("Alimentation");
        expenseRevenueComboBox.getSelectionModel().clearSelection();
    }

    private void openPage(ActionEvent event, String fxmlPath, String cssPath, String title) {
        try {
            Node source = (Node) event.getSource();
            Stage stage = (Stage) source.getScene().getWindow();
            Object userData = stage.getUserData();

            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1460, 780);
            if (cssPath != null) {
                scene.getStylesheets().add(Main.class.getResource(cssPath).toExternalForm());
            }

            stage.setUserData("/pi/mains/login-view.fxml".equals(fxmlPath) ? null : userData);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException exception) {
            showError("Unable to open page: " + exception.getMessage());
        }
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
