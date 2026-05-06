package pi.controllers.ExpenseRevenueController.FRONT;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import pi.entities.Expense;
import pi.entities.Revenue;
import pi.entities.User;
import pi.mains.Main;
import pi.services.CurrencyService.CurrencyConverterService;
import pi.services.RevenueExpenseService.AIExpenseCategorizationService;
import pi.services.RevenueExpenseService.EmailService;
import pi.services.RevenueExpenseService.ExpensePredictionService;
import pi.services.RevenueExpenseService.ExpenseService;
import pi.services.RevenueExpenseService.FinancialAdvisorService;
import pi.services.RevenueExpenseService.RevenueService;
import pi.services.RevenueExpenseService.RevenueExpensePdfExportService;
import pi.services.UserTransactionService.UserService;
import pi.services.UserTransactionService.TransactionService;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.util.Duration;

public class SalaryExpenseController {

    private static final Logger LOGGER = Logger.getLogger(SalaryExpenseController.class.getName());

    @FXML
    private Label totalIncomeLabel;
    @FXML
    private Label totalExpensesLabel;
    @FXML
    private Label netBalanceLabel;
    @FXML
    private Label lastTransactionLabel;
    @FXML
    private ComboBox<String> incomeMonthFilterComboBox;
    @FXML
    private ComboBox<String> expenseMonthFilterComboBox;
    @FXML
    private Label feedbackLabel;
    @FXML
    private Label expensePredictionLabel;
    @FXML
    private Label budgetAlertLabel;
    @FXML
    private TextField expenseBudgetField;
    @FXML
    private Label aiAdviceTypeLabel;
    @FXML
    private Label aiAdviceSummaryLabel;
    @FXML
    private Label aiAdviceWarningLabel;
    @FXML
    private Label aiAdviceTipsLabel;
    @FXML
    private ListView<String> savedAdviceListView;
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
    private Button revenueSubmitButton;
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
    private TextField currencyAmountField;
    @FXML
    private ComboBox<String> fromCurrencyComboBox;
    @FXML
    private ComboBox<String> toCurrencyComboBox;
    @FXML
    private Label currencyResultLabel;

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

    private final RevenueService revenueService = new RevenueService();
    private final ExpenseService expenseService = new ExpenseService();
    private final UserService userService = new UserService();
    private final AIExpenseCategorizationService aiExpenseCategorizationService = new AIExpenseCategorizationService();
    private final EmailService emailService = new EmailService();
    private final ExpensePredictionService expensePredictionService = new ExpensePredictionService();
    private final FinancialAdvisorService financialAdvisorService = new FinancialAdvisorService();
    private final RevenueExpensePdfExportService pdfExportService = new RevenueExpensePdfExportService();
    private final CurrencyConverterService currencyConverterService = new CurrencyConverterService();
    private final TransactionService transactionService = new TransactionService();
    private final ObservableList<Revenue> revenues = FXCollections.observableArrayList();
    private final ObservableList<ExpenseRow> expenses = FXCollections.observableArrayList();
    private final ObservableList<String> savedAdviceHistory = FXCollections.observableArrayList();
    private final User currentUser = createCurrentUser();
    private final Timeline autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(10), event -> loadData()));
    private final PauseTransition expenseCategorizationDebounce = new PauseTransition(Duration.millis(800));
    private final ExecutorService expenseCategorizationExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "expense-ai-categorizer");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService financialAdvisorExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "financial-advisor");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService currencyConversionExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "currency-converter");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicLong expenseCategorizationRequestSequence = new AtomicLong();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final List<String> DEFAULT_CURRENCIES = List.of("TND", "EUR", "USD", "GBP");
    private static final String BASE_CURRENCY = "TND";
    private static final String ALL_MONTHS = "All months";
    private static final double DEFAULT_MONTHLY_BUDGET = 2500.0;
    private String currentAdviceSnapshot = "";
    private double monthlyExpenseBudget = DEFAULT_MONTHLY_BUDGET;
    private String statisticsCurrency = BASE_CURRENCY;
    private Revenue editingRevenue;
    private Expense editingExpense;

    @FXML
    public void initialize() {
        attachUserFromStageIfAvailable();
        configureFilters();
        configureCurrencyConverter();
        configureExpenseAICategorization();
        configureRevenueTable();
        configureExpenseTable();
        configureSavedAdviceList();
        configureFormDefaults();
        configureBackOfficeNavigation();
        loadData();
        configureAutoRefresh();
    }

    public void setUser(User user) {
        if (user == null) {
            return;
        }
        User persistedUser = user.getId() > 0 ? userService.findById(user.getId()) : null;
        User sourceUser = persistedUser != null ? persistedUser : user;

        currentUser.setId(sourceUser.getId());
        currentUser.setNom(sourceUser.getNom());
        currentUser.setEmail(sourceUser.getEmail());
        currentUser.setRoles(sourceUser.getRoles());
        currentUser.setDateInscription(sourceUser.getDateInscription());
        currentUser.setSoldeTotal(sourceUser.getSoldeTotal());

        if (profileNameLabel != null && sourceUser.getNom() != null && !sourceUser.getNom().isBlank()) {
            profileNameLabel.setText(sourceUser.getNom());
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
            ensureConnectedUser();
            double amount = parseAmount(amountField.getText(), "Revenue amount");
            String type = normalizeRevenueType(requireValue(typeComboBox.getValue(), "Revenue type"));
            LocalDate txDate = Objects.requireNonNullElse(datePicker.getValue(), LocalDate.now());
            String description = normalizeText(descriptionArea.getText());

            Revenue revenue = editingRevenue != null ? editingRevenue : new Revenue();
            revenue.setUser(currentUser);
            revenue.setAmount(amount);
            revenue.setType(type);
            revenue.setReceivedAt(txDate);
            revenue.setDescription(description);
            revenue.setCreatedAt(editingRevenue != null && editingRevenue.getCreatedAt() != null ? editingRevenue.getCreatedAt() : LocalDateTime.now());

            boolean updating = editingRevenue != null;
            if (updating) {
                revenueService.update(revenue);
                showInfo("Revenue updated successfully.");
            } else {
                revenueService.add(revenue);
                transactionService.insertTransactionForUser(
                        currentUser.getId(),
                        "SAVING",
                        amount,
                        txDate,
                        description,
                        "salary-expense-front"
                );
                showInfo(buildRevenueSuccessMessage(revenue));
            }
            clearRevenueForms();
            loadData();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleConvertCurrency() {
        try {
            double amount = parseAmount(currencyAmountField.getText(), "Conversion amount");
            String fromCurrency = requireValue(fromCurrencyComboBox.getValue(), "From currency");
            String toCurrency = requireValue(toCurrencyComboBox.getValue(), "To currency");

            if (currencyResultLabel != null) {
                currencyResultLabel.setText("Converting...");
            }

            CompletableFuture
                    .supplyAsync(() -> currencyConverterService.convert(amount, fromCurrency, toCurrency), currencyConversionExecutor)
                    .thenAccept(convertedAmount -> Platform.runLater(() -> {
                        if (currencyResultLabel != null) {
                            currencyResultLabel.setText(String.format(
                                    Locale.US,
                                    "%.2f %s = %.2f %s",
                                    amount,
                                    fromCurrency.toUpperCase(Locale.ROOT),
                                    convertedAmount,
                                    toCurrency.toUpperCase(Locale.ROOT)
                            ));
                        }
                        showInfo("Currency converted successfully.");
                    }))
                    .exceptionally(exception -> {
                        Platform.runLater(() -> {
                            if (currencyResultLabel != null) {
                                currencyResultLabel.setText("Conversion unavailable.");
                            }
                            showError("Currency conversion failed: " + unwrapAsyncErrorMessage(exception));
                        });
                        return null;
                    });
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleAddExpense() {
        try {
            ensureConnectedUser();
            Revenue linkedRevenue = expenseRevenueComboBox.getValue();
            if (linkedRevenue == null || linkedRevenue.getId() <= 0) {
                throw new IllegalArgumentException("Select an associated revenue before adding an expense.");
            }

            double expenseAmount = parseAmount(expenseAmountField.getText(), "Expense amount");
            validateExpenseAgainstRevenue(expenseAmount, linkedRevenue);
            LocalDate txDate = Objects.requireNonNullElse(expenseDatePicker.getValue(), LocalDate.now());
            String description = normalizeText(expenseDescriptionArea.getText());
            String category = normalizeExpenseCategory(requireValue(expenseCategoryComboBox.getValue(), "Expense category"));

            boolean updating = editingExpense != null;
            Expense expense;
            if (updating) {
                expense = editingExpense;
                expense.setRevenue(linkedRevenue);
                expense.setUser(currentUser);
                expense.setAmount(expenseAmount);
                expense.setCategory(category);
                expense.setExpenseDate(txDate);
                expense.setDescription(description);
                expenseService.update(expense);
                showInfo("Expense updated successfully.");
            } else {
                expense = new Expense(
                        linkedRevenue,
                        currentUser,
                        expenseAmount,
                        category,
                        txDate,
                        description
                );
                expenseService.add(expense);
                transactionService.insertTransactionForUser(
                        currentUser.getId(),
                        "EXPENSE",
                        expenseAmount,
                        txDate,
                        description,
                        "salary-expense-front"
                );
                showInfo(buildExpenseSuccessMessage(expense));
            }
            clearExpenseForm();
            loadData();
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    @FXML
    private void handleApplyExpenseBudget() {
        try {
            monthlyExpenseBudget = parseAmount(expenseBudgetField.getText(), "Monthly budget");
            expenseBudgetField.setText(String.format(Locale.US, "%.2f", monthlyExpenseBudget));
            refreshExpensePrediction();
            showInfo("Expense forecast budget updated.");
        } catch (Exception exception) {
            showError(exception.getMessage());
        }
    }

    private void configureAutoRefresh() {
        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();

        feedbackLabel.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                autoRefreshTimeline.stop();
                return;
            }

            Window window = newScene.getWindow();
            if (window != null) {
                bindAutoRefreshToWindow(window);
            }

            newScene.windowProperty().addListener((windowObservable, oldWindow, newWindow) -> {
                if (newWindow != null) {
                    bindAutoRefreshToWindow(newWindow);
                }
            });
        });
    }

    private void bindAutoRefreshToWindow(Window window) {
        window.showingProperty().addListener((observable, wasShowing, isShowing) -> {
            if (isShowing) {
                loadData();
                autoRefreshTimeline.play();
            } else {
                autoRefreshTimeline.stop();
            }
        });
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
        revenueTypeComboBox.setItems(FXCollections.observableArrayList("FIXED", "BONUS", "FREELANCE", "OTHER"));
        if (revenueTypeComboBoxSecondary != null) {
            revenueTypeComboBoxSecondary.setItems(FXCollections.observableArrayList("FIXED", "BONUS", "FREELANCE", "OTHER"));
        }
        expenseCategoryComboBox.setItems(FXCollections.observableArrayList(
                "Food", "Transport", "Rent", "Health", "Education", "Leisure", "Other"
        ));
        revenueSortByComboBox.setItems(FXCollections.observableArrayList("Date", "Amount", "Type"));
        expenseSortByComboBox.setItems(FXCollections.observableArrayList("Date", "Amount", "Category"));
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
        if (incomeMonthFilterComboBox != null) {
            incomeMonthFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshDashboard());
        }
        if (expenseMonthFilterComboBox != null) {
            expenseMonthFilterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshDashboard());
        }
    }

    private void configureCurrencyConverter() {
        if (fromCurrencyComboBox == null || toCurrencyComboBox == null) {
            return;
        }

        fromCurrencyComboBox.setItems(FXCollections.observableArrayList(DEFAULT_CURRENCIES));
        toCurrencyComboBox.setItems(FXCollections.observableArrayList(DEFAULT_CURRENCIES));
        fromCurrencyComboBox.setValue("TND");
        toCurrencyComboBox.setValue("USD");
        if (currencyResultLabel != null) {
            currencyResultLabel.setText("Result will appear here.");
        }
    }

    private void configureExpenseAICategorization() {
        if (expenseDescriptionArea == null || expenseCategoryComboBox == null) {
            return;
        }

        expenseCategorizationDebounce.setOnFinished(event -> requestExpenseCategorySuggestion(expenseDescriptionArea.getText()));
        expenseDescriptionArea.textProperty().addListener((observable, oldValue, newValue) -> {
            String description = normalizeText(newValue);
            if (description.isBlank()) {
                expenseCategorizationDebounce.stop();
                return;
            }
            expenseCategorizationDebounce.playFromStart();
        });
    }

    private void requestExpenseCategorySuggestion(String description) {
        String normalizedDescription = normalizeText(description);
        if (normalizedDescription.isBlank()) {
            return;
        }

        long requestId = expenseCategorizationRequestSequence.incrementAndGet();
        CompletableFuture
                .supplyAsync(() -> aiExpenseCategorizationService.categorizeExpense(normalizedDescription), expenseCategorizationExecutor)
                .exceptionally(exception -> "Other")
                .thenAccept(category -> {
                    if (requestId != expenseCategorizationRequestSequence.get()) {
                        return;
                    }

                    Platform.runLater(() -> {
                        if (!normalizedDescription.equals(normalizeText(expenseDescriptionArea.getText()))) {
                            return;
                        }
                        expenseCategoryComboBox.setValue(normalizeExpenseCategory(category));
                    });
                });
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

    @FXML
    private void handleShowRevenueChart() {
        showStatisticsWindow("Revenue Statistics", true);
    }

    @FXML
    private void handleShowExpenseChart() {
        showStatisticsWindow("Expense Statistics", false);
    }

    @FXML
    private void handleExportRevenuePdf() {
        try {
            List<Revenue> revenueItems = new ArrayList<>(revenueTable.getItems());
            if (revenueItems.isEmpty()) {
                throw new IllegalStateException("No revenue data available to export.");
            }

            File file = choosePdfFile("Save Revenue PDF", "revenue-report.pdf");
            if (file == null) {
                return;
            }

            pdfExportService.exportRevenues(file.toPath(), revenueItems);
            showInfo("Revenue PDF exported: " + file.getName());
        } catch (Exception exception) {
            showError("Unable to export revenue PDF: " + exception.getMessage());
        }
    }

    @FXML
    private void handleExportExpensePdf() {
        try {
            List<ExpenseRow> expenseRows = new ArrayList<>(expenseTable.getItems());
            if (expenseRows.isEmpty()) {
                throw new IllegalStateException("No expense data available to export.");
            }

            File file = choosePdfFile("Save Expense PDF", "expense-report.pdf");
            if (file == null) {
                return;
            }

            List<Expense> expenseItems = expenseRows.stream()
                    .map(ExpenseRow::getExpense)
                    .collect(Collectors.toList());
            List<RevenueExpensePdfExportService.ExpenseRowData> exportRows = expenseRows.stream()
                    .map(row -> new RevenueExpensePdfExportService.ExpenseRowData(row.getRevenueLabel()))
                    .collect(Collectors.toList());

            pdfExportService.exportExpenses(file.toPath(), expenseItems, exportRows);
            showInfo("Expense PDF exported: " + file.getName());
        } catch (Exception exception) {
            showError("Unable to export expense PDF: " + exception.getMessage());
        }
    }

    @FXML
    private void handleSendMonthlyReport() {
        CompletableFuture
                .runAsync(() -> {
                    try {
                        MonthlyReportData report = buildCurrentMonthReport();
                        String subject = "Monthly financial report - " + report.getMonthLabel();
                        emailService.sendMonthlyReport(
                                buildMonthlyReportRecipient(),
                                subject,
                                buildMonthlyReportHtml(report),
                                buildMonthlyReportText(report)
                        );
                        Platform.runLater(() -> showInfo("Monthly report sent successfully."));
                    } catch (Exception exception) {
                        LOGGER.log(Level.WARNING, "Unable to send monthly report.", exception);
                        Platform.runLater(() -> showError("Unable to send monthly report: " + exception.getMessage()));
                    }
                });
    }

    @FXML
    private void handleShowAiAdvice() {
        double totalRevenue = revenues.stream().mapToDouble(Revenue::getAmount).sum();
        double totalExpense = expenses.stream().mapToDouble(row -> row.getExpense().getAmount()).sum();
        double netBalance = totalRevenue - totalExpense;
        String dominantCategory = resolveDominantExpenseCategory(
                expenses.stream().map(ExpenseRow::getExpense).filter(Objects::nonNull).collect(Collectors.toList())
        );

        showInfo("Generating AI financial advice...");
        updateAiAdvicePanel("Loading summary...", "Checking risk...", "1. Loading...\n2. Loading...\n3. Loading...", "Pending", false);

        CompletableFuture
                .supplyAsync(
                        () -> financialAdvisorService.generateAdvice(totalRevenue, totalExpense, netBalance, dominantCategory),
                        financialAdvisorExecutor
                )
                .thenAccept(result -> Platform.runLater(() -> {
                    boolean warningState = totalExpense > totalRevenue || netBalance < 0;
                    String adviceType = resolveAdviceType(totalRevenue, totalExpense, netBalance);
                    applyStructuredAdvice(result.message(), adviceType, warningState);
                    showInfo("Advice updated.");
                }))
                .exceptionally(exception -> {
                    LOGGER.log(Level.WARNING, "Unable to generate AI advice.", exception);
                    Platform.runLater(() -> {
                        updateAiAdvicePanel("Advice is unavailable.", "The current financial review could not be completed.", "1. Try again later.\n2. Check your setup.\n3. Keep tracking manually.", "Unavailable", true);
                        showError("Unable to generate AI advice: " + exception.getMessage());
                    });
                    return null;
                });
    }

    @FXML
    private void handleSaveAiAdvice() {
        if (currentAdviceSnapshot == null || currentAdviceSnapshot.isBlank()) {
            showInfo("No advice is available to save yet.");
            return;
        }

        savedAdviceHistory.add(0, currentAdviceSnapshot);
        while (savedAdviceHistory.size() > 12) {
            savedAdviceHistory.remove(savedAdviceHistory.size() - 1);
        }
        showInfo("Advice saved.");
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

    private VBox buildRevenueStatisticsContent(StatisticsConversion conversion) {
        PieChart revenueTypeChart = new PieChart();
        revenueTypeChart.setTitle("Revenue by Type (" + conversion.currency() + ")");
        revenueTypeChart.setLabelsVisible(true);
        revenueTypeChart.setLegendVisible(true);
        revenueTypeChart.getStyleClass().add("stats-pie-chart");
        revenueTypeChart.setData(FXCollections.observableArrayList(buildRevenueTypeData(conversion.rate())));

        BarChart<String, Number> totalsChart = createTotalsComparisonChart("Revenue vs Expense Totals", conversion.currency());
        totalsChart.getData().setAll(buildTotalsSeries(conversion.rate(), conversion.currency()));

        VBox content = new VBox(18.0,
                createStatsHeader("Revenue statistics", "Breakdown by revenue type and global totals.", conversion),
                revenueTypeChart,
                totalsChart
        );
        content.getStyleClass().addAll("panel-card", "stats-window-card");
        return content;
    }

    private VBox buildExpenseStatisticsContent(StatisticsConversion conversion) {
        PieChart expenseCategoryChart = new PieChart();
        expenseCategoryChart.setTitle("Expenses by Category (" + conversion.currency() + ")");
        expenseCategoryChart.setLabelsVisible(true);
        expenseCategoryChart.setLegendVisible(true);
        expenseCategoryChart.getStyleClass().add("stats-pie-chart");
        expenseCategoryChart.setData(FXCollections.observableArrayList(buildExpenseCategoryData(conversion.rate())));

        BarChart<String, Number> totalsChart = createTotalsComparisonChart("Revenue vs Expense Totals", conversion.currency());
        totalsChart.getData().setAll(buildTotalsSeries(conversion.rate(), conversion.currency()));

        VBox content = new VBox(18.0,
                createStatsHeader("Expense statistics", "Breakdown by expense category and comparison with revenues.", conversion),
                expenseCategoryChart,
                totalsChart
        );
        content.getStyleClass().addAll("panel-card", "stats-window-card");
        return content;
    }

    private HBox createStatsHeader(String title, String subtitle, StatisticsConversion conversion) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("panel-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("section-subtitle");
        subtitleLabel.setWrapText(true);

        VBox textBox = new VBox(6.0, titleLabel, subtitleLabel);

        double totalRevenue = revenues.stream().mapToDouble(Revenue::getAmount).sum() * conversion.rate();
        double totalExpense = expenses.stream().mapToDouble(row -> row.getExpense().getAmount()).sum() * conversion.rate();
        Label summaryLabel = new Label("Revenue: "
                + formatMoney(totalRevenue, conversion.currency())
                + " | Expense: "
                + formatMoney(totalExpense, conversion.currency()));
        summaryLabel.getStyleClass().add("feedback-pill");

        HBox header = new HBox(16.0, textBox, summaryLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);
        return header;
    }

    private List<PieChart.Data> buildRevenueTypeData(double conversionRate) {
        return revenues.stream()
                .collect(Collectors.groupingBy(
                        revenue -> localizeRevenueType(revenue.getType()),
                        Collectors.summingDouble(Revenue::getAmount)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue() * conversionRate))
                .collect(Collectors.toList());
    }

    private List<PieChart.Data> buildExpenseCategoryData(double conversionRate) {
        return expenses.stream()
                .collect(Collectors.groupingBy(
                        row -> localizeExpenseCategory(row.getExpense().getCategory()),
                        Collectors.summingDouble(row -> row.getExpense().getAmount())
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(entry -> new PieChart.Data(entry.getKey(), entry.getValue() * conversionRate))
                .collect(Collectors.toList());
    }

    private BarChart<String, Number> createTotalsComparisonChart(String title, String currency) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Metric");
        yAxis.setLabel("Amount (" + currency + ")");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(20);
        chart.setBarGap(8);
        chart.setPrefHeight(280.0);
        chart.setMinHeight(280.0);
        chart.getStyleClass().add("stats-bar-chart");
        return chart;
    }

    private List<XYChart.Series<String, Number>> buildTotalsSeries(double conversionRate, String currency) {
        double totalRevenue = revenues.stream().mapToDouble(Revenue::getAmount).sum() * conversionRate;
        double totalExpense = expenses.stream().mapToDouble(row -> row.getExpense().getAmount()).sum() * conversionRate;
        double netBalance = totalRevenue - totalExpense;

        return List.of(
                createMetricSeries("Revenues", totalRevenue, currency, "#08b8e2", "#24c9ee"),
                createMetricSeries("Expenses", totalExpense, currency, "#f59e0b", "#fbbf24"),
                createMetricSeries("Net", netBalance, currency, "#4f46e5", "#818cf8")
        );
    }

    private XYChart.Series<String, Number> createMetricSeries(
            String metric,
            double value,
            String currency,
            String colorStart,
            String colorEnd
    ) {
        XYChart.Data<String, Number> data = new XYChart.Data<>(metric, value);
        String tooltipText = metric + ": " + formatMoney(value, currency);
        data.nodeProperty().addListener((observable, oldNode, newNode) -> styleMetricBar(newNode, tooltipText, colorStart, colorEnd));
        styleMetricBar(data.getNode(), tooltipText, colorStart, colorEnd);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(metric);
        series.getData().add(data);
        return series;
    }

    private void styleMetricBar(Node node, String tooltipText, String colorStart, String colorEnd) {
        if (node == null) {
            return;
        }

        node.setStyle("-fx-background-color: linear-gradient(to top, "
                + colorStart
                + ", "
                + colorEnd
                + "); -fx-background-radius: 10 10 0 0;");
        Tooltip.install(node, new Tooltip(tooltipText));
    }

    private void showStatisticsWindow(String title, boolean revenueStatistics) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (feedbackLabel != null && feedbackLabel.getScene() != null) {
            stage.initOwner(feedbackLabel.getScene().getWindow());
        }
        stage.setTitle(title);

        ComboBox<String> currencyComboBox = new ComboBox<>(FXCollections.observableArrayList(DEFAULT_CURRENCIES));
        currencyComboBox.setValue(statisticsCurrency);
        currencyComboBox.setPrefWidth(130.0);

        Label statusLabel = new Label("Preparing chart...");
        statusLabel.getStyleClass().add("feedback-pill");

        Button applyButton = new Button("Apply");
        applyButton.getStyleClass().add("action-button-secondary");

        VBox statsContainer = new VBox();
        statsContainer.setFillWidth(true);

        Runnable refreshCharts = () -> renderStatisticsContent(
                revenueStatistics,
                currencyComboBox.getValue(),
                statsContainer,
                statusLabel
        );
        applyButton.setOnAction(event -> refreshCharts.run());
        currencyComboBox.setOnAction(event -> refreshCharts.run());

        HBox controls = new HBox(12.0,
                new Label("Display currency"),
                currencyComboBox,
                applyButton,
                statusLabel
        );
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.getStyleClass().addAll("panel-card", "stats-currency-controls");

        VBox shell = new VBox(18.0, controls, statsContainer);
        shell.getStyleClass().addAll("page-root", "content-shell", "stats-window-shell");

        ScrollPane scrollPane = new ScrollPane(shell);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("page-scroll");

        Scene scene = new Scene(scrollPane, 900, 760);
        scene.getStylesheets().add(Main.class.getResource("/styles/RevenueExpenseFRONT/salary-expense.css").toExternalForm());
        stage.setScene(scene);
        stage.show();
        refreshCharts.run();
    }

    private void renderStatisticsContent(boolean revenueStatistics, String targetCurrency, VBox statsContainer, Label statusLabel) {
        if (statusLabel != null) {
            statusLabel.setText("Updating chart...");
        }

        CompletableFuture
                .supplyAsync(() -> resolveStatisticsConversion(targetCurrency), currencyConversionExecutor)
                .thenAccept(conversion -> Platform.runLater(() -> {
                    statisticsCurrency = conversion.currency();
                    VBox content = revenueStatistics
                            ? buildRevenueStatisticsContent(conversion)
                            : buildExpenseStatisticsContent(conversion);
                    statsContainer.getChildren().setAll(content);
                    if (statusLabel != null) {
                        statusLabel.setText("Showing " + conversion.currency());
                    }
                }))
                .exceptionally(exception -> {
                    Platform.runLater(() -> {
                        if (statusLabel != null) {
                            statusLabel.setText("Currency update failed.");
                        }
                        showError("Unable to update statistics currency: " + unwrapAsyncErrorMessage(exception));
                    });
                    return null;
                });
    }

    private StatisticsConversion resolveStatisticsConversion(String targetCurrency) {
        String currency = normalizeCurrencyCode(targetCurrency);
        double rate = BASE_CURRENCY.equals(currency)
                ? 1.0
                : currencyConverterService.convert(1.0, BASE_CURRENCY, currency);
        return new StatisticsConversion(currency, rate);
    }

    private File choosePdfFile(String title, String defaultFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        Window owner = feedbackLabel != null && feedbackLabel.getScene() != null ? feedbackLabel.getScene().getWindow() : null;
        return fileChooser.showSaveDialog(owner);
    }

    private void configureRevenueTable() {
        if (revenueIdColumn != null) {
            revenueIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
            revenueIdColumn.setVisible(false);
        }
        revenueDateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getReceivedAt())));
        revenueAmountColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getAmount()));
        revenueAmountColumn.setCellFactory(column -> createCurrencyCell());
        revenueTypeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(localizeRevenueType(cell.getValue().getType())));
        revenueDescriptionColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullSafe(cell.getValue().getDescription())));
        revenueActionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");
            private final HBox actionBox = new HBox(8.0, deleteButton);

            {
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
        revenueTable.setRowFactory(table -> {
            TableRowWithRevenue row = new TableRowWithRevenue();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    startRevenueEdit(row.getItem());
                }
            });
            return row;
        });
    }

    private void configureExpenseTable() {
        if (expenseIdColumn != null) {
            expenseIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getExpense().getId()));
            expenseIdColumn.setVisible(false);
        }
        expenseDateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getExpense().getExpenseDate())));
        expenseAmountColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getExpense().getAmount()));
        expenseAmountColumn.setCellFactory(column -> createCurrencyCell());
        expenseCategoryColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(localizeExpenseCategory(cell.getValue().getExpense().getCategory())));
        expenseDescriptionColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullSafe(cell.getValue().getExpense().getDescription())));
        expenseRevenueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getRevenueLabel()));
        expenseActionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");
            private final HBox actionBox = new HBox(8.0, deleteButton);

            {
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
        revenueTypeComboBox.setValue("FIXED");
        if (revenueTypeComboBoxSecondary != null) {
            revenueTypeComboBoxSecondary.setValue("FIXED");
        }
        expenseCategoryComboBox.setValue("Food");
        revenueSortByComboBox.setValue("Date");
        expenseSortByComboBox.setValue("Date");
        revenueDirectionComboBox.setValue("Desc");
        expenseDirectionComboBox.setValue("Desc");
        revenueDatePicker.setValue(LocalDate.now());
        if (revenueDatePickerSecondary != null) {
            revenueDatePickerSecondary.setValue(LocalDate.now());
        }
        expenseDatePicker.setValue(LocalDate.now());
        if (revenueSubmitButton != null) {
            revenueSubmitButton.setText("Add");
        }
        if (expenseSubmitButton != null) {
            expenseSubmitButton.setText("Add");
        }
        if (expenseBudgetField != null) {
            expenseBudgetField.setText(String.format(Locale.US, "%.2f", monthlyExpenseBudget));
        }
        feedbackLabel.setText("");
        updateAiAdvicePanel(
                "Click \"AI Advice\" to get a short summary.",
                "Your warning will appear here.",
                "1. Generate advice.\n2. Review it.\n3. Save it if useful.",
                "Not generated",
                false
        );
    }

    private void configureSavedAdviceList() {
        if (savedAdviceListView == null) {
            return;
        }

        savedAdviceListView.setItems(savedAdviceHistory);
        savedAdviceListView.setPlaceholder(new Label("No saved advice yet."));
        savedAdviceListView.setCellFactory(listView -> new ListCell<>() {
            private final Label content = new Label();

            {
                content.setWrapText(true);
                content.setMaxWidth(720);
                content.getStyleClass().add("advice-body-label");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                content.setText(item);
                setGraphic(content);
            }
        });
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
            refreshMonthFilterOptions();
            refreshDashboard();
        } catch (SQLException exception) {
            showError("Database error: " + exception.getMessage());
        } catch (RuntimeException exception) {
            showError(exception.getMessage());
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
        double globalTotalIncome = revenues.stream().mapToDouble(Revenue::getAmount).sum();
        double globalTotalExpenses = expenses.stream().mapToDouble(row -> row.getExpense().getAmount()).sum();
        double netBalance = globalTotalIncome - globalTotalExpenses;
        double filteredIncome = filterRevenuesBySelectedMonth().stream().mapToDouble(Revenue::getAmount).sum();
        double filteredExpenses = filterExpensesBySelectedMonth().stream().mapToDouble(row -> row.getExpense().getAmount()).sum();

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
            totalIncomeLabel.setText(formatMoney(filteredIncome));
        }
        if (totalExpensesLabel != null) {
            totalExpensesLabel.setText(formatMoney(filteredExpenses));
        }
        if (netBalanceLabel != null) {
            netBalanceLabel.setText(formatMoney(netBalance));
        }
        if (lastTransactionLabel != null) {
            lastTransactionLabel.setText(lastTransactionDate == null ? "--/--/----" : formatDate(lastTransactionDate));
        }

        syncUserBalance(netBalance);
        refreshExpensePrediction();
    }

    private void refreshExpensePrediction() {
        if (expensePredictionLabel == null || budgetAlertLabel == null) {
            return;
        }

        List<Expense> expenseItems = expenses.stream()
                .map(ExpenseRow::getExpense)
                .collect(Collectors.toList());

        double predictedMonthlyTotal = expensePredictionService.predictMonthlyTotal(expenseItems);
        double daysToExceedBudget = expensePredictionService.estimateDaysToExceedBudget(expenseItems, monthlyExpenseBudget);

        expensePredictionLabel.setText("Projected monthly expenses: " + formatMoney(predictedMonthlyTotal));

        budgetAlertLabel.getStyleClass().removeAll("prediction-warning-label", "prediction-ok-label");
        if (expenseItems.isEmpty()) {
            budgetAlertLabel.setText("Budget status: insufficient expense history to generate a forecast.");
            budgetAlertLabel.getStyleClass().add("prediction-ok-label");
            return;
        }

        if (predictedMonthlyTotal > monthlyExpenseBudget) {
            if (Double.isInfinite(daysToExceedBudget)) {
                budgetAlertLabel.setText("Budget alert: spending trend is currently unavailable.");
            } else if (daysToExceedBudget <= 0) {
                budgetAlertLabel.setText("Budget alert: the current monthly budget has already been exceeded.");
            } else {
                budgetAlertLabel.setText("Budget alert: at the current pace, your budget will be exceeded in " + formatDays(daysToExceedBudget) + ".");
            }
            budgetAlertLabel.getStyleClass().add("prediction-warning-label");
        } else {
            if (Double.isInfinite(daysToExceedBudget)) {
                budgetAlertLabel.setText("Budget status: spending is stable and remains within budget.");
            } else {
                budgetAlertLabel.setText("Budget status: projected spending remains within the monthly budget of " + formatMoney(monthlyExpenseBudget) + ".");
            }
            budgetAlertLabel.getStyleClass().add("prediction-ok-label");
        }
    }

    private MonthlyReportData buildCurrentMonthReport() {
        LocalDate now = LocalDate.now();
        List<Revenue> currentMonthRevenues = revenues.stream()
                .filter(revenue -> isSameMonth(revenue.getReceivedAt(), now))
                .collect(Collectors.toList());

        List<Expense> currentMonthExpenses = expenses.stream()
                .map(ExpenseRow::getExpense)
                .filter(Objects::nonNull)
                .filter(expense -> isSameMonth(expense.getExpenseDate(), now))
                .collect(Collectors.toList());

        double totalMonthlyRevenue = currentMonthRevenues.stream()
                .mapToDouble(Revenue::getAmount)
                .sum();
        double totalMonthlyExpense = currentMonthExpenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        return new MonthlyReportData(
                MONTH_FORMATTER.format(now),
                totalMonthlyRevenue,
                totalMonthlyExpense,
                totalMonthlyRevenue - totalMonthlyExpense,
                resolveDominantExpenseCategory(currentMonthExpenses),
                formatDate(resolveLastTransactionDate(currentMonthRevenues, currentMonthExpenses)),
                currentMonthRevenues.size(),
                currentMonthExpenses.size()
        );
    }

    private boolean isSameMonth(LocalDate date, LocalDate referenceDate) {
        return date != null
                && referenceDate != null
                && date.getYear() == referenceDate.getYear()
                && date.getMonth() == referenceDate.getMonth();
    }

    private LocalDate resolveLastTransactionDate(List<Revenue> monthlyRevenues, List<Expense> monthlyExpenses) {
        LocalDate lastRevenueDate = monthlyRevenues.stream()
                .map(Revenue::getReceivedAt)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        LocalDate lastExpenseDate = monthlyExpenses.stream()
                .map(Expense::getExpenseDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);

        if (lastRevenueDate == null) {
            return lastExpenseDate;
        }
        if (lastExpenseDate == null) {
            return lastRevenueDate;
        }
        return lastExpenseDate.isAfter(lastRevenueDate) ? lastExpenseDate : lastRevenueDate;
    }

    private String resolveDominantExpenseCategory(List<Expense> monthlyExpenses) {
        if (monthlyExpenses.isEmpty()) {
            return "None";
        }

        return monthlyExpenses.stream()
                .collect(Collectors.groupingBy(
                        expense -> localizeExpenseCategory(expense.getCategory()),
                        LinkedHashMap::new,
                        Collectors.summingDouble(Expense::getAmount)
                ))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("None");
    }

    private User buildMonthlyReportRecipient() {
        if (currentUser.getEmail() != null && !currentUser.getEmail().isBlank()) {
            return currentUser;
        }

        User reportRecipient = new User();
        reportRecipient.setNom(currentUser.getNom());
        reportRecipient.setEmail("report-recipient@example.com"); // TODO replace with a project-specific fallback if needed.
        return reportRecipient;
    }

    private String buildMonthlyReportHtml(MonthlyReportData report) {
        return """
                <html>
                  <body style="margin:0;padding:24px;background:#f6f4ee;font-family:Arial,sans-serif;color:#1f2937;">
                    <div style="max-width:720px;margin:0 auto;background:#ffffff;border:1px solid #e5ded1;border-radius:18px;overflow:hidden;">
                      <div style="padding:24px 28px;background:#163028;color:#ffffff;">
                        <h2 style="margin:0 0 8px 0;font-size:24px;">Monthly Financial Report</h2>
                        <p style="margin:0;font-size:14px;opacity:0.9;">Reporting period: %s</p>
                      </div>
                      <div style="padding:28px;">
                        <p style="margin:0 0 18px 0;font-size:15px;">Here is your current month financial summary.</p>
                        <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                          <tr><td style="padding:10px 0;border-bottom:1px solid #ece7dd;"><strong>Total revenues</strong></td><td style="padding:10px 0;border-bottom:1px solid #ece7dd;text-align:right;">%s</td></tr>
                          <tr><td style="padding:10px 0;border-bottom:1px solid #ece7dd;"><strong>Total expenses</strong></td><td style="padding:10px 0;border-bottom:1px solid #ece7dd;text-align:right;">%s</td></tr>
                          <tr><td style="padding:10px 0;border-bottom:1px solid #ece7dd;"><strong>Net balance</strong></td><td style="padding:10px 0;border-bottom:1px solid #ece7dd;text-align:right;">%s</td></tr>
                          <tr><td style="padding:10px 0;border-bottom:1px solid #ece7dd;"><strong>Dominant expense category</strong></td><td style="padding:10px 0;border-bottom:1px solid #ece7dd;text-align:right;">%s</td></tr>
                          <tr><td style="padding:10px 0;border-bottom:1px solid #ece7dd;"><strong>Last transaction date</strong></td><td style="padding:10px 0;border-bottom:1px solid #ece7dd;text-align:right;">%s</td></tr>
                          <tr><td style="padding:10px 0;border-bottom:1px solid #ece7dd;"><strong>Number of revenues</strong></td><td style="padding:10px 0;border-bottom:1px solid #ece7dd;text-align:right;">%d</td></tr>
                          <tr><td style="padding:10px 0;"><strong>Number of expenses</strong></td><td style="padding:10px 0;text-align:right;">%d</td></tr>
                        </table>
                      </div>
                    </div>
                  </body>
                </html>
                """.formatted(
                report.getMonthLabel(),
                formatMoney(report.getTotalMonthlyRevenue()),
                formatMoney(report.getTotalMonthlyExpense()),
                formatMoney(report.getMonthlyNetBalance()),
                report.getDominantCategory(),
                report.getLastTransactionDate(),
                report.getRevenueCount(),
                report.getExpenseCount()
        );
    }

    private String buildMonthlyReportText(MonthlyReportData report) {
        return """
                Monthly Financial Report
                Reporting period: %s

                Total revenues: %s
                Total expenses: %s
                Net balance: %s
                Dominant expense category: %s
                Last transaction date: %s
                Number of revenues: %d
                Number of expenses: %d
                """.formatted(
                report.getMonthLabel(),
                formatMoney(report.getTotalMonthlyRevenue()),
                formatMoney(report.getTotalMonthlyExpense()),
                formatMoney(report.getMonthlyNetBalance()),
                report.getDominantCategory(),
                report.getLastTransactionDate(),
                report.getRevenueCount(),
                report.getExpenseCount()
        );
    }

    private String formatDays(double days) {
        long roundedDays = Math.max(1L, (long) Math.ceil(days));
        return roundedDays + (roundedDays == 1 ? " day" : " days");
    }

    private <S> TableCell<S, Number> createCurrencyCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format(Locale.US, "%.2f", item.doubleValue()));
            }
        };
    }

    private void refreshMonthFilterOptions() {
        if (incomeMonthFilterComboBox != null) {
            String previousValue = incomeMonthFilterComboBox.getValue();
            List<String> incomeMonths = new ArrayList<>();
            incomeMonths.add(ALL_MONTHS);
            incomeMonths.addAll(revenues.stream()
                    .map(Revenue::getReceivedAt)
                    .filter(Objects::nonNull)
                    .map(this::formatMonth)
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList()));
            incomeMonthFilterComboBox.setItems(FXCollections.observableArrayList(incomeMonths));
            incomeMonthFilterComboBox.setValue(resolveMonthSelection(previousValue, incomeMonths));
        }

        if (expenseMonthFilterComboBox != null) {
            String previousValue = expenseMonthFilterComboBox.getValue();
            List<String> expenseMonths = new ArrayList<>();
            expenseMonths.add(ALL_MONTHS);
            expenseMonths.addAll(expenses.stream()
                    .map(row -> row.getExpense().getExpenseDate())
                    .filter(Objects::nonNull)
                    .map(this::formatMonth)
                    .distinct()
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList()));
            expenseMonthFilterComboBox.setItems(FXCollections.observableArrayList(expenseMonths));
            expenseMonthFilterComboBox.setValue(resolveMonthSelection(previousValue, expenseMonths));
        }
    }

    private List<Revenue> filterRevenuesBySelectedMonth() {
        String selectedMonth = incomeMonthFilterComboBox == null ? ALL_MONTHS : incomeMonthFilterComboBox.getValue();
        if (selectedMonth == null || ALL_MONTHS.equals(selectedMonth)) {
            return revenues;
        }
        return revenues.stream()
                .filter(revenue -> revenue.getReceivedAt() != null && selectedMonth.equals(formatMonth(revenue.getReceivedAt())))
                .collect(Collectors.toList());
    }

    private List<ExpenseRow> filterExpensesBySelectedMonth() {
        String selectedMonth = expenseMonthFilterComboBox == null ? ALL_MONTHS : expenseMonthFilterComboBox.getValue();
        if (selectedMonth == null || ALL_MONTHS.equals(selectedMonth)) {
            return expenses;
        }
        return expenses.stream()
                .filter(row -> row.getExpense().getExpenseDate() != null && selectedMonth.equals(formatMonth(row.getExpense().getExpenseDate())))
                .collect(Collectors.toList());
    }

    private String resolveMonthSelection(String previousValue, List<String> availableValues) {
        if (previousValue != null && availableValues.contains(previousValue)) {
            return previousValue;
        }
        return ALL_MONTHS;
    }

    private String formatMonth(LocalDate date) {
        return MONTH_FORMATTER.format(date);
    }

    private Comparator<Revenue> buildRevenueComparator() {
        String sortBy = revenueSortByComboBox.getValue();
        if ("Amount".equals(sortBy)) {
            return Comparator.comparingDouble(Revenue::getAmount);
        }
        if ("Type".equals(sortBy)) {
            return Comparator.comparing(revenue -> localizeRevenueType(revenue.getType()), String.CASE_INSENSITIVE_ORDER);
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

    private boolean matchesRevenue(Revenue revenue, String search) {
        if (search.isBlank()) {
            return true;
        }
        return localizeRevenueType(revenue.getType()).toLowerCase(Locale.ROOT).contains(search)
                || nullSafe(revenue.getDescription()).toLowerCase(Locale.ROOT).contains(search);
    }

    private boolean matchesExpense(ExpenseRow row, String search) {
        if (search.isBlank()) {
            return true;
        }
        return localizeExpenseCategory(row.getExpense().getCategory()).toLowerCase(Locale.ROOT).contains(search)
                || nullSafe(row.getExpense().getDescription()).toLowerCase(Locale.ROOT).contains(search)
                || row.getRevenueLabel().toLowerCase(Locale.ROOT).contains(search);
    }

    private void deleteRevenue(Revenue revenue) {
        try {
            boolean wasEditingCurrentRevenue = editingRevenue != null && editingRevenue.getId() == revenue.getId();
            revenueService.delete(revenue.getId());
            if (wasEditingCurrentRevenue) {
                clearRevenueForms();
            }
            showInfo("Revenue deleted.");
            loadData();
        } catch (SQLException exception) {
            showError("Unable to delete revenue: " + exception.getMessage());
        }
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

    private Button createActionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private double parseAmount(String value, String fieldName) {
        String normalized = normalizeText(value).replace(',', '.');
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        double amount;
        try {
            amount = Double.parseDouble(normalized);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }
        return amount;
    }

    private void validateExpenseAgainstRevenue(double expenseAmount, Revenue linkedRevenue) {
        if (linkedRevenue != null && expenseAmount > linkedRevenue.getAmount()) {
            sendExpenseLimitAlert(expenseAmount, linkedRevenue);
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

    private void sendExpenseLimitAlert(double expenseAmount, Revenue linkedRevenue) {
        try {
            emailService.sendExpenseLimitAlert(
                    currentUser,
                    expenseAmount,
                    linkedRevenue,
                    expenseCategoryComboBox != null ? expenseCategoryComboBox.getValue() : null,
                    expenseDatePicker != null ? expenseDatePicker.getValue() : null,
                    expenseDescriptionArea != null ? normalizeText(expenseDescriptionArea.getText()) : null
            );
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Expense limit alert email failed.", exception);
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
        editingRevenue = null;
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
            revenueTypeComboBox.setValue("FIXED");
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
            revenueTypeComboBoxSecondary.setValue("FIXED");
        }
        if (revenueTable != null) {
            revenueTable.getSelectionModel().clearSelection();
        }
        if (revenueSubmitButton != null) {
            revenueSubmitButton.setText("Add");
        }
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
            expenseSubmitButton.setText("Add");
        }
    }

    private void startRevenueEdit(Revenue revenue) {
        if (revenue == null) {
            return;
        }
        editingRevenue = revenue;
        revenueAmountField.setText(String.format(Locale.US, "%.2f", revenue.getAmount()));
        revenueTypeComboBox.setValue(localizeRevenueType(revenue.getType()));
        revenueDatePicker.setValue(Objects.requireNonNullElse(revenue.getReceivedAt(), LocalDate.now()));
        revenueDescriptionArea.setText(nullSafe(revenue.getDescription()));
        if (revenueSubmitButton != null) {
            revenueSubmitButton.setText("Update");
        }
        showInfo("Revenue loaded. Double-click another row to edit it, then click Update.");
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
            expenseSubmitButton.setText("Update");
        }
        showInfo("Expense loaded. Double-click another row to edit it, then click Update.");
    }

    private String normalizeRevenueType(String value) {
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

    private String localizeRevenueType(String value) {
        return normalizeRevenueType(value);
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

    private String buildRevenueSuccessMessage(Revenue revenue) {
        try {
            emailService.sendRevenueNotification(currentUser, revenue);
            return "Revenue added successfully. Email notification sent.";
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Revenue saved but email notification failed.", exception);
            return "Revenue added successfully. Email notification failed: " + exception.getMessage();
        }
    }

    private String buildExpenseSuccessMessage(Expense expense) {
        try {
            emailService.sendExpenseNotification(currentUser, expense);
            return "Expense added successfully. Email notification sent.";
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Expense saved but email notification failed.", exception);
            return "Expense added successfully. Email notification failed: " + exception.getMessage();
        }
    }

    private String unwrapAsyncErrorMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }

        String message = current == null ? "" : current.getMessage();
        return message == null || message.isBlank() ? "Unexpected error." : message;
    }

    private void showError(String message) {
        feedbackLabel.setText(message);
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (feedbackLabel != null && feedbackLabel.getScene() != null) {
            stage.initOwner(feedbackLabel.getScene().getWindow());
        }
        stage.setTitle("Input Error");
        stage.setResizable(false);

        Label iconLabel = new Label("!");
        iconLabel.getStyleClass().add("error-dialog-icon");

        Label titleLabel = new Label("Operation failed");
        titleLabel.getStyleClass().add("error-dialog-title");

        Label subtitleLabel = new Label("Please correct the highlighted input and try again.");
        subtitleLabel.getStyleClass().add("error-dialog-subtitle");
        subtitleLabel.setWrapText(true);

        VBox titleBox = new VBox(4, titleLabel, subtitleLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(14, iconLabel, titleBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("error-dialog-header");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("error-dialog-message");
        messageLabel.setWrapText(true);

        Button okButton = new Button("OK");
        okButton.getStyleClass().add("error-dialog-button");
        okButton.setDefaultButton(true);
        okButton.setOnAction(event -> stage.close());

        HBox actions = new HBox(okButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.getStyleClass().add("error-dialog-actions");

        VBox root = new VBox(18, header, messageLabel, actions);
        root.getStyleClass().add("error-dialog-root");

        Scene scene = new Scene(root, 440, 230);
        scene.getStylesheets().add(Main.class.getResource("/styles/RevenueExpenseFRONT/salary-expense.css").toExternalForm());
        stage.setScene(scene);
        stage.showAndWait();
    }

    private void applyStructuredAdvice(String advice, String adviceType, boolean warningState) {
        String normalizedAdvice = advice == null ? "" : advice.trim();
        String[] lines = normalizedAdvice.split("\\R");
        List<String> contentLines = java.util.Arrays.stream(lines)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());

        String summary = contentLines.size() > 0 ? stripAdvicePrefix(contentLines.get(0)) : "No financial summary is available.";
        String warning = contentLines.size() > 1 ? stripAdvicePrefix(contentLines.get(1)) : "No warning is currently available.";
        List<String> tips = contentLines.stream()
                .skip(2)
                .map(this::stripAdvicePrefix)
                .collect(Collectors.toList());

        while (tips.size() < 3) {
            tips.add("Keep tracking your finances weekly.");
        }

        String tipsText = "1. " + removeLeadingNumbering(tips.get(0))
                + "\n2. " + removeLeadingNumbering(tips.get(1))
                + "\n3. " + removeLeadingNumbering(tips.get(2));

        updateAiAdvicePanel(summary, warning, tipsText, adviceType, warningState);
        currentAdviceSnapshot = "[" + adviceType + "] " + summary + "\n" + warning + "\n" + tipsText;
    }

    private void updateAiAdvicePanel(String summary, String warning, String tips, String adviceType, boolean warningState) {
        if (aiAdviceSummaryLabel != null) {
            aiAdviceSummaryLabel.setText(summary);
        }
        if (aiAdviceWarningLabel != null) {
            aiAdviceWarningLabel.setText(warning);
        }
        if (aiAdviceTipsLabel != null) {
            aiAdviceTipsLabel.setText(tips);
        }
        if (aiAdviceTypeLabel != null) {
            aiAdviceTypeLabel.setText(adviceType);
            aiAdviceTypeLabel.getStyleClass().removeAll(
                    "advice-type-neutral",
                    "advice-type-stable",
                    "advice-type-warning",
                    "advice-type-critical"
            );
            aiAdviceTypeLabel.getStyleClass().add(resolveAdviceTypeStyleClass(adviceType, warningState));
        }
    }

    private String resolveAdviceType(double totalRevenue, double totalExpense, double netBalance) {
        if (netBalance < 0 || totalExpense > totalRevenue) {
            return "Critical";
        }
        if (totalRevenue > 0 && (totalExpense / totalRevenue) >= 0.85) {
            return "Warning";
        }
        return "Stable";
    }

    private String resolveAdviceTypeStyleClass(String adviceType, boolean warningState) {
        if ("Critical".equalsIgnoreCase(adviceType)) {
            return "advice-type-critical";
        }
        if ("Warning".equalsIgnoreCase(adviceType) || warningState) {
            return "advice-type-warning";
        }
        if ("Stable".equalsIgnoreCase(adviceType)) {
            return "advice-type-stable";
        }
        return "advice-type-neutral";
    }

    private String stripAdvicePrefix(String line) {
        String sanitized = line == null ? "" : line.trim();
        sanitized = sanitized.replaceFirst("^(Resume|Résumé)\\s*:\\s*", "");
        sanitized = sanitized.replaceFirst("^Alerte\\s*:\\s*", "");
        sanitized = sanitized.replaceFirst("^Summary\\s*:\\s*", "");
        sanitized = sanitized.replaceFirst("^Warning\\s*:\\s*", "");
        return sanitized.trim();
    }

    private String removeLeadingNumbering(String line) {
        return line == null ? "" : line.trim().replaceFirst("^\\d+[\\.)]?\\s*", "");
    }

    private String formatDate(LocalDate date) {
        return date == null ? "--/--/----" : DATE_FORMATTER.format(date);
    }

    private String formatMoney(double amount) {
        return formatMoney(amount, BASE_CURRENCY);
    }

    private String formatMoney(double amount, String currency) {
        return String.format(Locale.US, "%.2f %s", amount, normalizeCurrencyCode(currency));
    }

    private String normalizeCurrencyCode(String value) {
        String currency = normalizeText(value).toUpperCase(Locale.ROOT);
        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a valid 3-letter code.");
        }
        return currency;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private void syncUserBalance(double netBalance) {
        if (currentUser.getId() <= 0) {
            return;
        }
        if (Double.compare(currentUser.getSoldeTotal(), netBalance) == 0) {
            return;
        }

        userService.updateSoldeTotal(currentUser.getId(), netBalance);
        currentUser.setSoldeTotal(netBalance);
    }

    private User createCurrentUser() {
        User user = new User();
        user.setId(0);
        return user;
    }

    private record StatisticsConversion(String currency, double rate) {
    }

    private static final class MonthlyReportData {
        private final String monthLabel;
        private final double totalMonthlyRevenue;
        private final double totalMonthlyExpense;
        private final double monthlyNetBalance;
        private final String dominantCategory;
        private final String lastTransactionDate;
        private final int revenueCount;
        private final int expenseCount;

        private MonthlyReportData(
                String monthLabel,
                double totalMonthlyRevenue,
                double totalMonthlyExpense,
                double monthlyNetBalance,
                String dominantCategory,
                String lastTransactionDate,
                int revenueCount,
                int expenseCount
        ) {
            this.monthLabel = monthLabel;
            this.totalMonthlyRevenue = totalMonthlyRevenue;
            this.totalMonthlyExpense = totalMonthlyExpense;
            this.monthlyNetBalance = monthlyNetBalance;
            this.dominantCategory = dominantCategory;
            this.lastTransactionDate = lastTransactionDate;
            this.revenueCount = revenueCount;
            this.expenseCount = expenseCount;
        }

        private String getMonthLabel() {
            return monthLabel;
        }

        private double getTotalMonthlyRevenue() {
            return totalMonthlyRevenue;
        }

        private double getTotalMonthlyExpense() {
            return totalMonthlyExpense;
        }

        private double getMonthlyNetBalance() {
            return monthlyNetBalance;
        }

        private String getDominantCategory() {
            return dominantCategory;
        }

        private String getLastTransactionDate() {
            return lastTransactionDate;
        }

        private int getRevenueCount() {
            return revenueCount;
        }

        private int getExpenseCount() {
            return expenseCount;
        }
    }

    private void attachUserFromStageIfAvailable() {
        if (feedbackLabel == null) {
            return;
        }
        feedbackLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getWindow() instanceof Stage stage && stage.getUserData() instanceof User user) {
                setUser(user);
            }
        });
    }

    private void ensureConnectedUser() {
        if (currentUser.getId() > 0) {
            return;
        }
        if (feedbackLabel != null && feedbackLabel.getScene() != null
                && feedbackLabel.getScene().getWindow() instanceof Stage stage
                && stage.getUserData() instanceof User user
                && user.getId() > 0) {
            setUser(user);
            return;
        }
        throw new IllegalStateException("No connected user found. Please login again.");
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
                return "Unassigned revenue";
            }
            return prettifyRevenueTypeStatic(revenue.getType()) + " income • " + String.format(Locale.US, "%.2f TND", revenue.getAmount());
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

        private static String prettifyRevenueTypeStatic(String value) {
            return switch (localizeRevenueTypeStatic(value)) {
                case "FIXED" -> "Fixed";
                case "BONUS" -> "Bonus";
                case "FREELANCE" -> "Freelance";
                default -> "Other";
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
            setText(prettifyRevenueTypeStatic(item.getType()) + " income • " + String.format(Locale.US, "%.2f TND", item.getAmount()));
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

        private static String prettifyRevenueTypeStatic(String value) {
            return switch (localizeRevenueTypeStatic(value)) {
                case "FIXED" -> "Fixed";
                case "BONUS" -> "Bonus";
                case "FREELANCE" -> "Freelance";
                default -> "Other";
            };
        }
    }

    private static final class TableRowWithRevenue extends javafx.scene.control.TableRow<Revenue> {
    }

    private static final class TableRowWithExpense extends javafx.scene.control.TableRow<ExpenseRow> {
    }
}
