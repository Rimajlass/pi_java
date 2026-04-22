package pi.savings.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;
import javafx.stage.Stage;
import pi.controllers.ExpenseRevenueController.FRONT.SalaryExpenseController;
import pi.controllers.UserTransactionController.AboutController;
import pi.controllers.UserTransactionController.ContactController;
import pi.controllers.UserTransactionController.SalaryHomeController;
import pi.controllers.UserTransactionController.ServiceController;
import pi.entities.User;
import pi.mains.Main;
import pi.savings.repository.SavingsTransactionRepository;
import pi.savings.service.SavingsModuleService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

final class SavingsGoalsView {

    private final SavingsMockData.DashboardData mockData = SavingsMockData.create();

    private SavingsUiController controller;
    private Label balanceValueLabel;
    private Label activeGoalsValueLabel;
    private Label progressValueLabel;
    private Label nearestDeadlineValueLabel;
    private Label accountBalanceLabel;
    private Label createdOnLabel;
    private TextField rateField;
    private TextField depositAmountField;
    private TextField depositDescriptionField;
    private TextField historySearchField;
    private ComboBox<String> historySortComboBox;
    private ComboBox<String> historyDirectionComboBox;
    private TextField goalsSearchField;
    private ComboBox<String> goalsSortComboBox;
    private ComboBox<String> goalsDirectionComboBox;
    private TextField goalNameField;
    private TextField goalTargetField;
    private TextField goalCurrentField;
    private DatePicker goalDeadlinePicker;
    private TextField goalPriorityField;
    private Button addGoalButton;
    private Button autoPlanButton;
    private Integer editingGoalId;
    private VBox historyRowsBox;
    private VBox goalsListBox;
    private Label feedbackLabel;
    private HBox feedbackBox;
    private TabPane tabPane;
    private ScrollPane pageScrollPane;
    private Label historyCountValueLabel;
    private Label historyDepositsValueLabel;
    private Label historyContributionsValueLabel;
    private Label historyAverageValueLabel;
    private Label goalsCountValueLabel;
    private Label goalsCompletedValueLabel;
    private Label goalsTargetValueLabel;
    private Label goalsRemainingValueLabel;
    private Label historyPaginationLabel;
    private Button historyPreviousButton;
    private Button historyNextButton;
    private int historyPageIndex;
    private static final int HISTORY_PAGE_SIZE = 5;
    private Label goalsPaginationLabel;
    private Button goalsPreviousButton;
    private Button goalsNextButton;
    private int goalsPageIndex;
    private static final int GOALS_PAGE_SIZE = 4;

    Parent build(SavingsUiController controller) {
        this.controller = controller;

        VBox page = new VBox();
        page.getStyleClass().add("page");
        page.getChildren().add(buildTopShell());
        page.getChildren().add(buildContent());
        page.getChildren().add(buildFooter());

        pageScrollPane = new ScrollPane(page);
        pageScrollPane.setFitToWidth(true);
        pageScrollPane.getStyleClass().add("page-scroll");

        BorderPane root = new BorderPane(pageScrollPane);
        root.getStylesheets().add(
                SavingsGoalsView.class.getResource("/pi/savings/ui/savings-goals.css").toExternalForm()
        );

        renderEmptyState();
        initializeDataAsync();
        return root;
    }

    private void initializeDataAsync() {
        showInfo("Loading dashboard data...");

        Task<SavingsUiController.OperationResult> initializationTask = new Task<>() {
            @Override
            protected SavingsUiController.OperationResult call() {
                return controller.initialize();
            }
        };

        initializationTask.setOnSucceeded(event -> {
            SavingsUiController.OperationResult initResult = initializationTask.getValue();
            refreshAll();
            if (initResult.success()) {
                showInfo("Savings account display, deposit, and interest rate update are now JDBC-backed.");
            } else {
                showError(initResult.message());
            }
        });

        initializationTask.setOnFailed(event -> {
            Throwable exception = initializationTask.getException();
            System.err.println("Startup error while loading Savings & Goals data: " + exception.getMessage());
            exception.printStackTrace(System.err);
            showError("Impossible de charger le module Savings & Goals.");
        });

        Thread loaderThread = new Thread(initializationTask, "savings-dashboard-loader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private void renderEmptyState() {
        balanceValueLabel.setText("0 TND");
        activeGoalsValueLabel.setText("0");
        progressValueLabel.setText("0%");
        nearestDeadlineValueLabel.setText("--/--/----");
        accountBalanceLabel.setText("0 TND");
        createdOnLabel.setText("--/--/----");
        rateField.setText("0");

        if (historyRowsBox != null) {
            historyRowsBox.getChildren().clear();
        }
        if (goalsListBox != null) {
            goalsListBox.getChildren().clear();
        }
    }

    private VBox buildTopShell() {
        VBox topShell = new VBox(18);
        topShell.getStyleClass().add("top-shell");
        topShell.setPadding(new Insets(22, 28, 0, 28));

        HBox header = new HBox(24);
        header.getStyleClass().add("top-nav");
        header.setAlignment(Pos.CENTER_LEFT);

        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);
        Label brandBadge = new Label("DS");
        brandBadge.getStyleClass().add("brand-badge");
        Label brandName = new Label("Decide$");
        brandName.getStyleClass().add("brand-mark");
        brand.getChildren().addAll(brandBadge, brandName);

        HBox nav = new HBox(
                headerNavButton("Home", this::openHomePage),
                headerNavButton("About", this::openAboutPage),
                headerNavButton("Service", this::openServicePage),
                headerNavButton("Contact", this::openContactPage)
        );
        nav.getStyleClass().add("nav-links");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox profilePill = new HBox(10);
        profilePill.getStyleClass().add("profile-pill");
        profilePill.setAlignment(Pos.CENTER);
        Label profileIcon = new Label("S");
        profileIcon.getStyleClass().add("profile-icon");
        Label profileName = new Label("Savings Workspace");
        profileName.getStyleClass().add("profile-name");
        profilePill.getChildren().addAll(profileIcon, profileName);

        Button startButton = actionButton("To Start", "header-start-btn");
        startButton.setOnAction(event -> openHomePage());

        Button logoutButton = actionButton("Logout", "header-logout-btn");
        logoutButton.setOnAction(event -> openLoginPage());

        HBox headerActions = new HBox(12, profilePill, startButton, logoutButton);
        headerActions.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(brand, nav, spacer, headerActions);
        VBox hero = new VBox(14);
        hero.getStyleClass().add("hero-banner");
        hero.setAlignment(Pos.CENTER);

        Label title = new Label("Savings & Goals");
        title.getStyleClass().add("hero-title");
        title.setWrapText(true);

        Label subtitle = new Label("Track savings, create goals, contribute, and monitor progress - all in one place.");
        subtitle.getStyleClass().add("hero-subtitle");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(760);
        subtitle.setAlignment(Pos.CENTER);

        Label breadcrumb = new Label("Home / Modules / Savings & Goals");
        breadcrumb.getStyleClass().add("hero-breadcrumb");

        Button addDepositButton = actionButton("+ Add Deposit", "primary-hero-btn");
        addDepositButton.setOnAction(event -> navigateToSavingsForm());

        Button createGoalButton = actionButton("Create Goal", "soft-hero-btn");
        createGoalButton.setOnAction(event -> navigateToGoalsForm());

        Button calendarButton = actionButton("Calendar", "soft-hero-btn");
        calendarButton.setOnAction(event -> showInfo("Calendar action is planned for a later step."));

        Button simulationButton = actionButton("What-if?", "soft-hero-btn");
        simulationButton.setOnAction(event -> showInfo("Simulation action is planned for a later step."));

        HBox actions = new HBox(12, addDepositButton, createGoalButton, calendarButton, simulationButton);
        actions.getStyleClass().add("hero-actions");
        actions.setAlignment(Pos.CENTER);

        hero.getChildren().addAll(title, subtitle, breadcrumb, actions);
        topShell.getChildren().addAll(header, hero);
        return topShell;
    }

    private VBox buildContent() {
        VBox content = new VBox(18);
        content.getStyleClass().add("content");

        VBox sectionHeader = new VBox(4);
        Label kicker = new Label("DECIDE$ MODULE");
        kicker.getStyleClass().add("section-kicker");
        Label title = new Label("Manage Savings & Financial Goals");
        title.getStyleClass().add("section-title");
        Label subtitle = new Label("Simple dashboard. Clear actions. Visible progress.");
        subtitle.getStyleClass().add("section-subtitle");
        sectionHeader.getChildren().addAll(kicker, title, subtitle);

        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackBox = new HBox(feedbackLabel);
        feedbackBox.getStyleClass().addAll("feedback-box", "feedback-info");
        feedbackBox.setVisible(false);
        feedbackBox.setManaged(false);

        content.getChildren().add(sectionHeader);
        content.getChildren().add(feedbackBox);
        content.getChildren().add(buildKpis());
        content.getChildren().add(buildInsightPanels());
        content.getChildren().add(buildTabs());
        return content;
    }

    private VBox buildFooter() {
        VBox footer = new VBox(18);
        footer.getStyleClass().add("page-footer");

        HBox topRow = new HBox(28);
        topRow.setAlignment(Pos.TOP_LEFT);

        VBox brandBlock = new VBox(8);
        brandBlock.getStyleClass().add("footer-block-wide");
        Label footerBrand = new Label("Decide$ Savings & Goals");
        footerBrand.getStyleClass().add("footer-title");
        Label footerText = new Label("Desktop workspace for deposits, goals, progress tracking, and financial discipline.");
        footerText.getStyleClass().add("footer-text");
        footerText.setWrapText(true);
        brandBlock.getChildren().addAll(footerBrand, footerText);

        VBox quickLinks = footerColumn("Quick Links", "Savings Dashboard", "Create Goal", "Transactions", "Progress Tracking");
        VBox controls = footerColumn("Controls", "Search and sort history", "Search and sort goals", "Edit or delete goals", "Update interest rate");
        VBox status = footerColumn("Status", "JDBC-connected savings account", "Validated deposits", "Goal contributions enabled", "Desktop UI active");

        HBox.setHgrow(brandBlock, Priority.ALWAYS);
        HBox.setHgrow(quickLinks, Priority.ALWAYS);
        HBox.setHgrow(controls, Priority.ALWAYS);
        HBox.setHgrow(status, Priority.ALWAYS);
        topRow.getChildren().addAll(brandBlock, quickLinks, controls, status);

        Separator separator = new Separator();
        separator.getStyleClass().add("footer-separator");

        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        Label copyright = new Label("Desktop Savings & Goals module - JavaFX + JDBC");
        copyright.getStyleClass().add("footer-caption");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label credits = new Label("Design adapted from the Symfony module");
        credits.getStyleClass().add("footer-caption");
        bottomRow.getChildren().addAll(copyright, spacer, credits);

        footer.getChildren().addAll(topRow, separator, bottomRow);
        return footer;
    }

    private GridPane buildKpis() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("kpi-grid");

        for (int i = 0; i < 4; i++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(25);
            constraints.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(constraints);
        }

        balanceValueLabel = new Label();
        balanceValueLabel.getStyleClass().add("card-value");
        activeGoalsValueLabel = new Label();
        activeGoalsValueLabel.getStyleClass().add("card-value");
        progressValueLabel = new Label();
        progressValueLabel.getStyleClass().add("card-value");
        nearestDeadlineValueLabel = new Label();
        nearestDeadlineValueLabel.getStyleClass().add("card-value");

        grid.add(buildKpiCard("Savings Balance", "Current account balance", balanceValueLabel, "S"), 0, 0);
        grid.add(buildKpiCard("Active Goals", "Goals in progress", activeGoalsValueLabel, "G"), 1, 0);
        grid.add(buildKpiCard("Goals Progress", "Average completion", progressValueLabel, "%"), 2, 0);
        grid.add(buildKpiCard("Nearest Deadline", "Closest goal date", nearestDeadlineValueLabel, "D"), 3, 0);
        return grid;
    }

    private HBox buildKpiCard(String titleText, String subtitleText, Label valueLabel, String iconText) {
        VBox textBox = new VBox(8);
        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("card-subtitle");
        textBox.getChildren().addAll(title, subtitle, valueLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        StackPane icon = iconBubble(iconText);
        HBox card = new HBox(12, textBox, spacer, icon);
        card.setAlignment(Pos.TOP_LEFT);
        card.getStyleClass().add("glass-card");
        return card;
    }

    private HBox buildInsightPanels() {
        HBox row = new HBox(18);
        for (SavingsMockData.InsightData insight : mockData.insights()) {
            row.getChildren().add(buildInsightCard(insight));
        }
        return row;
    }

    private VBox buildInsightCard(SavingsMockData.InsightData insight) {
        VBox card = new VBox(14);
        card.getStyleClass().add("glass-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefWidth(360);

        HBox header = new HBox(12);
        VBox text = new VBox(4);
        Label title = new Label(insight.title());
        title.getStyleClass().add("card-title");
        Label subtitle = new Label(insight.subtitle());
        subtitle.getStyleClass().add("card-subtitle");
        text.getChildren().addAll(title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(text, spacer, iconBubble(insight.icon()));

        VBox lines = new VBox(10);
        for (String line : insight.lines()) {
            HBox item = new HBox(8);
            item.setAlignment(Pos.TOP_LEFT);
            Label dot = new Label("*");
            dot.setStyle("-fx-text-fill:#00bfe5;-fx-font-size:16px;-fx-font-weight:bold;");
            Label textLine = new Label(line);
            textLine.setWrapText(true);
            textLine.getStyleClass().add("mini-row-label");
            item.getChildren().addAll(dot, textLine);
            lines.getChildren().add(item);
        }

        card.getChildren().addAll(header, lines);
        return card;
    }

    private VBox buildTabs() {
        VBox container = new VBox(12);

        HBox tabBar = new HBox(tabPill("Savings", true), tabPill("Goals", false));
        tabBar.getStyleClass().add("tab-bar");

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color:transparent;");

        Tab savingsTab = new Tab("Savings", buildSavingsTab());
        Tab goalsTab = new Tab("Goals", buildGoalsTab());
        tabPane.getTabs().addAll(savingsTab, goalsTab);

        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldValue, newValue) -> {
            Button savingsBtn = (Button) tabBar.getChildren().get(0);
            Button goalsBtn = (Button) tabBar.getChildren().get(1);
            boolean savingsSelected = newValue.intValue() == 0;
            updateTabButton(savingsBtn, savingsSelected);
            updateTabButton(goalsBtn, !savingsSelected);
        });

        ((Button) tabBar.getChildren().get(0)).setOnAction(event -> tabPane.getSelectionModel().select(0));
        ((Button) tabBar.getChildren().get(1)).setOnAction(event -> tabPane.getSelectionModel().select(1));

        container.getChildren().addAll(tabBar, tabPane);
        return container;
    }

    private HBox buildSavingsTab() {
        HBox columns = new HBox(18);

        VBox left = new VBox(18);
        left.getStyleClass().add("left-column");
        left.getChildren().add(buildSavingsAccountCard());

        VBox right = new VBox(18);
        right.getStyleClass().add("right-column");
        right.getChildren().add(buildHistoryCard());
        HBox.setHgrow(right, Priority.ALWAYS);

        columns.getChildren().addAll(left, right);
        return columns;
    }

    private HBox buildGoalsTab() {
        HBox columns = new HBox(18);

        VBox left = new VBox(18);
        left.getStyleClass().add("left-column");
        left.getChildren().add(buildGoalFormCard());

        VBox right = new VBox(18);
        right.getStyleClass().add("right-column");
        right.getChildren().add(buildGoalsCard());
        HBox.setHgrow(right, Priority.ALWAYS);

        columns.getChildren().addAll(left, right);
        return columns;
    }

    private VBox buildSavingsAccountCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("glass-card");

        accountBalanceLabel = valueLabel();
        createdOnLabel = valueLabel();

        rateField = new TextField();
        rateField.getStyleClass().add("field");
        rateField.setPrefWidth(90);
        attachMoneyValidation(rateField, "Interest rate", 3);
        rateField.setOnAction(event -> handleUpdateRate());

        Button saveRateButton = actionButton("Save", "primary-btn");
        saveRateButton.setPadding(new Insets(10, 16, 10, 16));
        saveRateButton.setOnAction(event -> handleUpdateRate());

        depositAmountField = new TextField();
        depositAmountField.setPromptText("Ex: 200");
        depositAmountField.getStyleClass().add("field");
        attachMoneyValidation(depositAmountField, "Amount", 7);
        depositAmountField.setOnAction(event -> handleDeposit());

        depositDescriptionField = new TextField();
        depositDescriptionField.setPromptText("Ex: monthly savings");
        depositDescriptionField.getStyleClass().add("field");
        depositDescriptionField.setTextFormatter(createLengthFormatter(120));
        depositDescriptionField.setOnAction(event -> handleDeposit());

        Button saveDepositButton = actionButton("Save Deposit", "primary-btn");
        saveDepositButton.setOnAction(event -> handleDeposit());

        Button noteButton = actionButton("Add Note / Tag (later)", "ghost-btn");
        noteButton.setOnAction(event -> showInfo("Notes and tags will be added in a later step."));

        card.getChildren().addAll(
                cardHeader("My Savings Account", "balance + deposit", "S"),
                infoRow("Balance (solde)", accountBalanceLabel),
                buildRateEditor(saveRateButton),
                infoRow("Created on", createdOnLabel),
                new Separator(),
                miniSectionTitle("Add a Deposit"),
                fieldBlock("Amount", depositAmountField),
                fieldBlock("Description", depositDescriptionField),
                saveDepositButton,
                noteButton
        );

        return card;
    }

    private VBox buildRateEditor(Button saveRateButton) {
        VBox wrapper = new VBox(6);
        HBox row = new HBox(10);
        row.getStyleClass().add("mini-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("Interest rate");
        label.getStyleClass().add("mini-row-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label percent = new Label("%");
        percent.getStyleClass().add("mini-row-value");

        row.getChildren().addAll(label, spacer, rateField, percent, saveRateButton);
        wrapper.getChildren().add(row);
        return wrapper;
    }

    private VBox buildHistoryCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("glass-card");

        historySearchField = new TextField();
        historySearchField.setPromptText("Search transactions...");
        historySearchField.getStyleClass().add("field");
        historySearchField.setOnAction(event -> applyHistorySearchAndSort());
        HBox.setHgrow(historySearchField, Priority.ALWAYS);

        Button searchButton = actionButton("Search", "ghost-btn");
        searchButton.setOnAction(event -> applyHistorySearchAndSort());

        historySortComboBox = new ComboBox<>();
        historySortComboBox.getItems().addAll("All", "ID", "Date", "Type", "Amount", "Description");
        historySortComboBox.setValue("Date");
        historySortComboBox.getStyleClass().add("toolbar-combo");
        historySortComboBox.setOnAction(event -> applyHistorySearchAndSort());

        historyDirectionComboBox = new ComboBox<>();
        historyDirectionComboBox.getItems().addAll("Descending", "Ascending");
        historyDirectionComboBox.setValue("Descending");
        historyDirectionComboBox.getStyleClass().add("toolbar-combo");
        historyDirectionComboBox.setOnAction(event -> applyHistorySearchAndSort());

        Button allButton = actionButton("All", "chip");
        allButton.getStyleClass().add("chip-active");
        allButton.setOnAction(event -> applyHistoryDefaultView());

        Button resetButton = actionButton("Reset", "chip");
        resetButton.setOnAction(event -> resetHistorySearchAndSort());

        historyRowsBox = new VBox();
        historyRowsBox.getStyleClass().add("history-table");

        Button statsButton = actionButton("Stats", "ghost-btn");
        statsButton.setOnAction(event -> showHistoryStatsSummary());

        Button exportCsvButton = actionButton("Export CSV", "ghost-btn");
        exportCsvButton.setOnAction(event -> handleExportCsv());

        Button exportPdfButton = actionButton("Export PDF", "ghost-btn");
        exportPdfButton.setOnAction(event -> handleExportPdf());

        HBox paginationBar = buildHistoryPaginationBar();
        HBox footerActions = new HBox(10, statsButton, exportCsvButton, exportPdfButton);
        footerActions.setAlignment(Pos.CENTER_LEFT);

        HBox toolbar = new HBox(10, searchButton, historySearchField, historySortComboBox, historyDirectionComboBox, allButton, resetButton);
        toolbar.getStyleClass().add("toolbar-row");

        card.getChildren().add(cardHeader("Savings History", "search - sort - export - stats", "H"));
        card.getChildren().add(toolbar);
        card.getChildren().add(buildHistoryStatsPanel());
        card.getChildren().add(buildHistoryTableWrapper());
        card.getChildren().add(paginationBar);
        card.getChildren().add(footerActions);
        return card;
    }

    private HBox buildHistoryStatsPanel() {
        historyCountValueLabel = valueLabel();
        historyDepositsValueLabel = valueLabel();
        historyContributionsValueLabel = valueLabel();
        historyAverageValueLabel = valueLabel();

        HBox row = new HBox(10,
                compactStatCard("Rows", historyCountValueLabel),
                compactStatCard("Deposits", historyDepositsValueLabel),
                compactStatCard("Contributions", historyContributionsValueLabel),
                compactStatCard("Average", historyAverageValueLabel)
        );
        row.getStyleClass().add("stats-row");
        return row;
    }

    private VBox buildHistoryTableWrapper() {
        VBox wrapper = new VBox();

        GridPane header = new GridPane();
        header.getStyleClass().add("history-header");
        configureHistoryColumns(header);
        addHeaderCell(header, "#", 0);
        addHeaderCell(header, "Date", 1);
        addHeaderCell(header, "Type", 2);
        addHeaderCell(header, "Amount", 3);
        addHeaderCell(header, "Description", 4);

        ScrollPane rowsScroll = new ScrollPane(historyRowsBox);
        rowsScroll.setFitToWidth(true);
        rowsScroll.setPannable(true);
        rowsScroll.setPrefViewportHeight(260);
        rowsScroll.getStyleClass().add("inner-scroll");

        wrapper.getChildren().addAll(header, rowsScroll);
        return wrapper;
    }

    private VBox buildGoalFormCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("glass-card");

        goalNameField = new TextField();
        goalNameField.setPromptText("Ex: Buy a car");
        goalNameField.getStyleClass().add("field");
        goalNameField.setTextFormatter(createLengthFormatter(60));
        goalNameField.setOnAction(event -> handleSaveGoal());

        goalTargetField = new TextField();
        goalTargetField.getStyleClass().add("field");
        goalTargetField.setPromptText("Ex: 10000");
        attachMoneyValidation(goalTargetField, "Target", 7);
        goalTargetField.setOnAction(event -> handleSaveGoal());

        goalCurrentField = new TextField("0");
        goalCurrentField.getStyleClass().add("field");
        attachMoneyValidation(goalCurrentField, "Current", 7);
        goalCurrentField.setOnAction(event -> handleSaveGoal());

        goalDeadlinePicker = new DatePicker();
        goalDeadlinePicker.setPromptText("yyyy-mm-dd");
        goalDeadlinePicker.getStyleClass().add("field");
        goalDeadlinePicker.setEditable(false);
        goalDeadlinePicker.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override
            public void updateItem(java.time.LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setDisable(empty || item == null || item.isBefore(java.time.LocalDate.now()));
            }
        });

        goalPriorityField = new TextField("3");
        goalPriorityField.getStyleClass().add("field");
        attachPriorityValidation(goalPriorityField, "Priority");
        goalPriorityField.setOnAction(event -> handleSaveGoal());

        addGoalButton = actionButton("Add Goal", "primary-btn");
        addGoalButton.setOnAction(event -> handleSaveGoal());

        autoPlanButton = actionButton("Auto-plan (later)", "ghost-btn");
        autoPlanButton.setOnAction(event -> handleSecondaryGoalAction());

        card.getChildren().addAll(
                cardHeader("Create a Goal", "create + rules", "G"),
                fieldBlock("Goal name", goalNameField),
                twoFields(fieldBlock("Target (TND)", goalTargetField), fieldBlock("Current (TND)", goalCurrentField)),
                twoFields(fieldBlock("Deadline", goalDeadlinePicker), fieldBlock("Priority (1-5)", goalPriorityField)),
                addGoalButton,
                autoPlanButton
        );
        return card;
    }

    private VBox buildGoalsCard() {
        VBox card = new VBox(14);
        card.getStyleClass().add("glass-card");

        goalsSearchField = new TextField();
        goalsSearchField.setPromptText("Search: name, priority, deadline, amounts...");
        goalsSearchField.getStyleClass().add("field");
        goalsSearchField.setOnAction(event -> applyGoalsSearchAndSort());
        HBox.setHgrow(goalsSearchField, Priority.ALWAYS);

        Button searchButton = actionButton("Search", "ghost-btn");
        searchButton.setOnAction(event -> applyGoalsSearchAndSort());

        goalsSortComboBox = new ComboBox<>();
        goalsSortComboBox.getItems().addAll("All", "Name", "Target", "Current", "Deadline", "Priority", "Progress");
        goalsSortComboBox.setValue("Priority");
        goalsSortComboBox.getStyleClass().add("toolbar-combo");
        goalsSortComboBox.setOnAction(event -> applyGoalsSearchAndSort());

        goalsDirectionComboBox = new ComboBox<>();
        goalsDirectionComboBox.getItems().addAll("Descending", "Ascending");
        goalsDirectionComboBox.setValue("Descending");
        goalsDirectionComboBox.getStyleClass().add("toolbar-combo");
        goalsDirectionComboBox.setOnAction(event -> applyGoalsSearchAndSort());

        Button allButton = actionButton("All", "chip");
        allButton.getStyleClass().add("chip-active");
        allButton.setOnAction(event -> applyGoalsDefaultView());

        Button resetButton = actionButton("Reset", "chip");
        resetButton.setOnAction(event -> resetGoalsSearchAndSort());

        HBox toolbar = new HBox(10, searchButton, goalsSearchField, goalsSortComboBox, goalsDirectionComboBox, allButton, resetButton);
        toolbar.getStyleClass().add("toolbar-row");

        Button goalsStatsButton = actionButton("Stats", "ghost-btn");
        goalsStatsButton.setOnAction(event -> showGoalsStatsSummary());

        Button goalsExportCsvButton = actionButton("Export CSV", "ghost-btn");
        goalsExportCsvButton.setOnAction(event -> handleGoalsExportCsv());

        Button goalsExportPdfButton = actionButton("Export PDF", "ghost-btn");
        goalsExportPdfButton.setOnAction(event -> handleGoalsExportPdf());

        HBox paginationBar = buildGoalsPaginationBar();
        HBox footerActions = new HBox(10, goalsStatsButton, goalsExportCsvButton, goalsExportPdfButton);
        footerActions.setAlignment(Pos.CENTER_LEFT);

        goalsListBox = new VBox(12);
        ScrollPane goalsScroll = new ScrollPane(goalsListBox);
        goalsScroll.setFitToWidth(true);
        goalsScroll.setPannable(true);
        goalsScroll.setPrefViewportHeight(360);
        goalsScroll.getStyleClass().add("inner-scroll");

        card.getChildren().add(cardHeader("Your Goals", "search - sort - export - stats", "P"));
        card.getChildren().add(toolbar);
        card.getChildren().add(buildGoalsStatsPanel());
        card.getChildren().add(goalsScroll);
        card.getChildren().add(paginationBar);
        card.getChildren().add(footerActions);
        return card;
    }

    private HBox buildHistoryPaginationBar() {
        historyPreviousButton = actionButton("Previous", "chip");
        historyPreviousButton.setOnAction(event -> {
            if (historyPageIndex > 0) {
                historyPageIndex--;
                refreshHistory(controller.filterAndSortHistory(historySearchValue(), historySortAttributeValue(), historySortDirectionValue()));
            }
        });

        historyNextButton = actionButton("Next", "chip");
        historyNextButton.setOnAction(event -> {
            historyPageIndex++;
            refreshHistory(controller.filterAndSortHistory(historySearchValue(), historySortAttributeValue(), historySortDirectionValue()));
        });

        historyPaginationLabel = new Label("Page 1 / 1");
        historyPaginationLabel.getStyleClass().add("mini-row-value");

        HBox box = new HBox(10, historyPreviousButton, historyPaginationLabel, historyNextButton);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox buildGoalsPaginationBar() {
        goalsPreviousButton = actionButton("Previous", "chip");
        goalsPreviousButton.setOnAction(event -> {
            if (goalsPageIndex > 0) {
                goalsPageIndex--;
                refreshGoals(controller.filterAndSortGoals(goalsSearchValue(), goalsSortAttributeValue(), goalsSortDirectionValue()));
            }
        });

        goalsNextButton = actionButton("Next", "chip");
        goalsNextButton.setOnAction(event -> {
            goalsPageIndex++;
            refreshGoals(controller.filterAndSortGoals(goalsSearchValue(), goalsSortAttributeValue(), goalsSortDirectionValue()));
        });

        goalsPaginationLabel = new Label("Page 1 / 1");
        goalsPaginationLabel.getStyleClass().add("mini-row-value");

        HBox box = new HBox(10, goalsPreviousButton, goalsPaginationLabel, goalsNextButton);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox buildGoalsStatsPanel() {
        goalsCountValueLabel = valueLabel();
        goalsCompletedValueLabel = valueLabel();
        goalsTargetValueLabel = valueLabel();
        goalsRemainingValueLabel = valueLabel();

        HBox row = new HBox(10,
                compactStatCard("Goals", goalsCountValueLabel),
                compactStatCard("Completed", goalsCompletedValueLabel),
                compactStatCard("Target", goalsTargetValueLabel),
                compactStatCard("Remaining", goalsRemainingValueLabel)
        );
        row.getStyleClass().add("stats-row");
        return row;
    }

    private VBox buildGoalCard(SavingsModuleService.GoalSnapshot goal) {
        VBox card = new VBox(12);
        card.getStyleClass().add("goal-card");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox text = new VBox(4);
        Label name = new Label(goal.name());
        name.getStyleClass().add("goal-title");
        Label meta = new Label(
                "Target: " + formatMoney(goal.target())
                        + " - Current: " + formatMoney(goal.current())
                        + " - Deadline: " + (goal.deadline() == null ? "--/--/----" : goal.deadline())
        );
        meta.getStyleClass().add("goal-meta");
        text.getChildren().addAll(name, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("P" + goal.priority());
        badge.getStyleClass().add("priority-badge");

        top.getChildren().addAll(text, spacer, badge);

        HBox progressLine = new HBox(10);
        progressLine.setAlignment(Pos.CENTER_LEFT);
        ProgressBar bar = new ProgressBar(goal.progressPercent() / 100.0);
        bar.getStyleClass().add("progress-track");
        bar.setPrefWidth(380);
        HBox.setHgrow(bar, Priority.ALWAYS);
        Label percent = new Label((int) Math.round(goal.progressPercent()) + "%");
        percent.getStyleClass().add("progress-value");
        progressLine.getChildren().addAll(bar, percent);

        TextField contributeField = new TextField();
        contributeField.setPromptText("Add TND");
        contributeField.getStyleClass().add("field");
        contributeField.setPrefWidth(120);
        attachMoneyValidation(contributeField, "Contribution", 7);
        contributeField.setOnAction(event -> handleContribute(goal, contributeField));

        Button contributeButton = actionButton("Contribute", "primary-btn");
        contributeButton.setOnAction(event -> handleContribute(goal, contributeField));

        boolean goalReached = goal.current().compareTo(goal.target()) >= 0;
        if (goalReached) {
            contributeField.setDisable(true);
            contributeField.setPromptText("Goal reached");
            contributeButton.setDisable(true);
        }

        Button editButton = actionButton("Edit", "ghost-btn");
        editButton.setOnAction(event -> startGoalEdit(goal));

        Button deleteButton = actionButton("Delete", "ghost-btn");
        deleteButton.setOnAction(event -> handleDeleteGoal(goal));

        HBox actions = new HBox(10, contributeField, contributeButton, editButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(top, progressLine, actions);
        return card;
    }

    private void handleDeposit() {
        if (!validateMoneyField(depositAmountField, "Amount", 7)) {
            return;
        }

        SavingsUiController.OperationResult result = controller.safeDeposit(
                depositAmountField.getText(),
                depositDescriptionField.getText()
        );

        if (result.success()) {
            depositAmountField.clear();
            depositDescriptionField.clear();
            refreshAll();
            showSuccess(result.message());
        } else {
            showError(result.message());
        }
    }

    private void handleUpdateRate() {
        if (!validateMoneyField(rateField, "Interest rate", 3)) {
            return;
        }

        SavingsUiController.OperationResult result = controller.safeUpdateInterestRate(rateField.getText());
        if (result.success()) {
            refreshAll();
            showSuccess(result.message());
        } else {
            showError(result.message());
        }
    }

    private void handleSaveGoal() {
        if (!validateMoneyField(goalTargetField, "Target", 7)
                || !validateMoneyField(goalCurrentField, "Current", 7)
                || !validatePriorityField(goalPriorityField, "Priority")) {
            return;
        }

        SavingsUiController.OperationResult result = editingGoalId == null
                ? controller.safeCreateGoal(goalNameField.getText(), goalTargetField.getText(), goalCurrentField.getText(), goalDeadlineValue(), goalPriorityField.getText())
                : controller.safeUpdateGoal(editingGoalId, goalNameField.getText(), goalTargetField.getText(), goalCurrentField.getText(), goalDeadlineValue(), goalPriorityField.getText());

        if (result.success()) {
            clearGoalForm();
            refreshAll();
            showSuccess(result.message());
        } else {
            showError(result.message());
        }
    }

    private void handleSecondaryGoalAction() {
        if (editingGoalId != null) {
            clearGoalForm();
            showInfo("Goal edit canceled.");
            return;
        }
        showInfo("Auto-plan remains a future enhancement.");
    }

    private void handleContribute(SavingsModuleService.GoalSnapshot goal, TextField contributeField) {
        if (!validateMoneyField(contributeField, "Contribution", 7)) {
            return;
        }

        SavingsUiController.OperationResult result = controller.safeContributeToGoal(goal.id(), contributeField.getText());
        if (result.success()) {
            contributeField.clear();
            refreshAll();
            showSuccess(result.message());
        } else {
            showError(result.message());
        }
    }

    private void startGoalEdit(SavingsModuleService.GoalSnapshot goal) {
        editingGoalId = goal.id();
        goalNameField.setText(goal.name());
        goalTargetField.setText(formatPlain(goal.target()));
        goalCurrentField.setText(formatPlain(goal.current()));
        goalDeadlinePicker.setValue(goal.deadline());
        goalPriorityField.setText(String.valueOf(goal.priority()));
        addGoalButton.setText("Update Goal");
        autoPlanButton.setText("Cancel Edit");
        tabPane.getSelectionModel().select(1);
        goalNameField.requestFocus();
        showInfo("Goal loaded into the form for editing.");
    }

    private void handleDeleteGoal(SavingsModuleService.GoalSnapshot goal) {
        SavingsUiController.OperationResult result = controller.safeDeleteGoal(goal.id());
        if (result.success()) {
            if (editingGoalId != null && editingGoalId == goal.id()) {
                clearGoalForm();
            }
            refreshAll();
            showSuccess(result.message());
        } else {
            showError(result.message());
        }
    }

    private void clearGoalForm() {
        editingGoalId = null;
        goalNameField.clear();
        goalTargetField.clear();
        goalCurrentField.setText("0");
        goalDeadlinePicker.setValue(null);
        goalPriorityField.setText("3");
        addGoalButton.setText("Add Goal");
        autoPlanButton.setText("Auto-plan (later)");
    }

    private void navigateToSavingsForm() {
        if (tabPane != null) {
            tabPane.getSelectionModel().select(0);
        }
        if (pageScrollPane != null) {
            pageScrollPane.setVvalue(0.52);
        }
        if (depositAmountField != null) {
            depositAmountField.requestFocus();
        }
    }

    private void navigateToGoalsForm() {
        if (tabPane != null) {
            tabPane.getSelectionModel().select(1);
        }
        if (pageScrollPane != null) {
            pageScrollPane.setVvalue(0.52);
        }
        if (goalNameField != null) {
            goalNameField.requestFocus();
        }
    }

    private void refreshAll() {
        SavingsModuleService.DashboardSnapshot state = controller.getSnapshot();

        balanceValueLabel.setText(formatMoney(state.balance()));
        activeGoalsValueLabel.setText(String.valueOf(state.activeGoals()));
        progressValueLabel.setText(state.averageProgress() + "%");
        nearestDeadlineValueLabel.setText(state.nearestDeadline());

        accountBalanceLabel.setText(formatMoney(state.balance()));
        createdOnLabel.setText(state.createdOn().toString());
        rateField.setText(formatPlain(state.interestRate()));

        refreshHistory(controller.filterAndSortHistory(historySearchValue(), historySortAttributeValue(), historySortDirectionValue()));
        refreshGoals(controller.filterAndSortGoals(goalsSearchValue(), goalsSortAttributeValue(), goalsSortDirectionValue()));
    }

    private void refreshHistory(List<SavingsTransactionRepository.TransactionRow> historyEntries) {
        historyRowsBox.getChildren().clear();
        updateHistoryStats(historyEntries);
        SavingsUiController.PageSlice<SavingsTransactionRepository.TransactionRow> page = controller.paginate(
                historyEntries,
                historyPageIndex,
                HISTORY_PAGE_SIZE
        );
        historyPageIndex = page.pageIndex();
        updateHistoryPagination(page);
        if (page.items().isEmpty()) {
            GridPane line = new GridPane();
            line.setPadding(new Insets(12, 14, 12, 14));
            configureHistoryColumns(line);
            addBodyCell(line, "-", 0, "table-cell");
            addBodyCell(line, "--/--/----", 1, "table-cell");
            addBodyCell(line, "No data", 2, "table-cell");
            addBodyCell(line, "0 TND", 3, "amount-cell");
            addBodyCell(line, "No transaction matches the current filters.", 4, "table-cell");
            historyRowsBox.getChildren().add(line);
            return;
        }

        for (SavingsTransactionRepository.TransactionRow entry : page.items()) {
            GridPane line = new GridPane();
            line.setPadding(new Insets(12, 14, 12, 14));
            configureHistoryColumns(line);
            addBodyCell(line, String.valueOf(entry.id()), 0, "table-cell");
            addBodyCell(line, entry.date().toLocalDate().toString(), 1, "table-cell");
            addBodyCell(line, entry.type(), 2, "table-cell");
            addBodyCell(line, formatMoney(entry.amount()), 3, "amount-cell");
            addBodyCell(line, entry.description(), 4, "table-cell");
            historyRowsBox.getChildren().add(line);
        }
    }

    private void refreshGoals(List<SavingsModuleService.GoalSnapshot> goals) {
        goalsListBox.getChildren().clear();
        updateGoalsStats(goals);
        SavingsUiController.PageSlice<SavingsModuleService.GoalSnapshot> page = controller.paginate(
                goals,
                goalsPageIndex,
                GOALS_PAGE_SIZE
        );
        goalsPageIndex = page.pageIndex();
        updateGoalsPagination(page);
        if (page.items().isEmpty()) {
            VBox emptyCard = new VBox(8);
            emptyCard.getStyleClass().add("goal-card");
            Label title = new Label("No goals found");
            title.getStyleClass().add("goal-title");
            Label message = new Label("Create a goal or adjust the current filters.");
            message.getStyleClass().add("goal-meta");
            emptyCard.getChildren().addAll(title, message);
            goalsListBox.getChildren().add(emptyCard);
            return;
        }
        for (SavingsModuleService.GoalSnapshot goal : page.items()) {
            goalsListBox.getChildren().add(buildGoalCard(goal));
        }
    }

    private void applyHistorySearchAndSort() {
        historyPageIndex = 0;
        refreshHistory(controller.filterAndSortHistory(historySearchValue(), historySortAttributeValue(), historySortDirectionValue()));
        showInfo("Savings history search/sort applied.");
    }

    private void applyHistoryDefaultView() {
        historySearchField.clear();
        historySortComboBox.setValue("Date");
        historyDirectionComboBox.setValue("Descending");
        historyPageIndex = 0;
        refreshHistory(controller.filterAndSortHistory("", historySortAttributeValue(), historySortDirectionValue()));
        showInfo("Savings history default view restored.");
    }

    private void resetHistorySearchAndSort() {
        historySearchField.clear();
        historySortComboBox.setValue("All");
        historyDirectionComboBox.setValue("Descending");
        historyPageIndex = 0;
        refreshHistory(controller.filterAndSortHistory("", historySortAttributeValue(), historySortDirectionValue()));
        showInfo("Savings history filters reset.");
    }

    private void applyGoalsSearchAndSort() {
        goalsPageIndex = 0;
        refreshGoals(controller.filterAndSortGoals(goalsSearchValue(), goalsSortAttributeValue(), goalsSortDirectionValue()));
        showInfo("Goals search/sort applied.");
    }

    private void applyGoalsDefaultView() {
        goalsSearchField.clear();
        goalsSortComboBox.setValue("Priority");
        goalsDirectionComboBox.setValue("Descending");
        goalsPageIndex = 0;
        refreshGoals(controller.filterAndSortGoals("", goalsSortAttributeValue(), goalsSortDirectionValue()));
        showInfo("Goals default view restored.");
    }

    private void resetGoalsSearchAndSort() {
        goalsSearchField.clear();
        goalsSortComboBox.setValue("All");
        goalsDirectionComboBox.setValue("Descending");
        goalsPageIndex = 0;
        refreshGoals(controller.filterAndSortGoals("", goalsSortAttributeValue(), goalsSortDirectionValue()));
        showInfo("Goals filters reset.");
    }

    private void updateHistoryPagination(SavingsUiController.PageSlice<SavingsTransactionRepository.TransactionRow> page) {
        if (historyPaginationLabel != null) {
            historyPaginationLabel.setText(
                    "Page " + (page.pageIndex() + 1) + " / " + page.pageCount() + " (" + page.totalItems() + " rows)"
            );
        }
        if (historyPreviousButton != null) {
            historyPreviousButton.setDisable(page.pageIndex() <= 0);
        }
        if (historyNextButton != null) {
            historyNextButton.setDisable(page.pageIndex() >= page.pageCount() - 1);
        }
    }

    private void updateGoalsPagination(SavingsUiController.PageSlice<SavingsModuleService.GoalSnapshot> page) {
        if (goalsPaginationLabel != null) {
            goalsPaginationLabel.setText(
                    "Page " + (page.pageIndex() + 1) + " / " + page.pageCount() + " (" + page.totalItems() + " goals)"
            );
        }
        if (goalsPreviousButton != null) {
            goalsPreviousButton.setDisable(page.pageIndex() <= 0);
        }
        if (goalsNextButton != null) {
            goalsNextButton.setDisable(page.pageIndex() >= page.pageCount() - 1);
        }
    }

    private void configureHistoryColumns(GridPane grid) {
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(8);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(20);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(18);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(18);
        ColumnConstraints c4 = new ColumnConstraints();
        c4.setPercentWidth(36);
        grid.getColumnConstraints().setAll(c0, c1, c2, c3, c4);
    }

    private void addHeaderCell(GridPane grid, String text, int column) {
        Label label = new Label(text);
        label.getStyleClass().add("table-head");
        grid.add(label, column, 0);
    }

    private void addBodyCell(GridPane grid, String text, int column, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        grid.add(label, column, 0);
    }

    private VBox fieldBlock(String labelText, TextField field) {
        VBox block = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        block.getChildren().addAll(label, field);
        return block;
    }

    private VBox fieldBlock(String labelText, DatePicker field) {
        VBox block = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        block.getChildren().addAll(label, field);
        return block;
    }

    private HBox twoFields(VBox left, VBox right) {
        HBox row = new HBox(10, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        return row;
    }

    private HBox cardHeader(String titleText, String subtitleText, String iconText) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.TOP_LEFT);

        VBox text = new VBox(4);
        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");
        title.setWrapText(true);
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("card-subtitle");
        text.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(text, spacer, iconBubble(iconText));
        return header;
    }

    private HBox infoRow(String labelText, Label valueLabel) {
        HBox row = new HBox();
        row.getStyleClass().add("mini-row");
        row.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(labelText);
        label.getStyleClass().add("mini-row-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(label, spacer, valueLabel);
        return row;
    }

    private Label miniSectionTitle(String text) {
        Label title = new Label(text);
        title.setStyle("-fx-text-fill:#0b1c3f;-fx-font-size:18px;-fx-font-weight:bold;");
        return title;
    }

    private StackPane iconBubble(String icon) {
        Label label = new Label(icon);
        label.getStyleClass().add("icon-bubble");
        return new StackPane(label);
    }

    private Label valueLabel() {
        Label label = new Label();
        label.getStyleClass().add("mini-row-value");
        return label;
    }

    private Button actionButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }

    private Button placeholderButton(String text, String message) {
        Button button = actionButton(text, "ghost-btn");
        button.setOnAction(event -> showInfo(message));
        return button;
    }

    private VBox compactStatCard(String labelText, Label value) {
        VBox card = new VBox(4);
        card.getStyleClass().add("mini-stat-card");
        Label title = new Label(labelText);
        title.getStyleClass().add("card-subtitle");
        card.getChildren().addAll(title, value);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefWidth(160);
        return card;
    }

    private void updateHistoryStats(List<SavingsTransactionRepository.TransactionRow> historyEntries) {
        SavingsModuleService.HistoryStats stats = controller.calculateHistoryStats(historyEntries);
        historyCountValueLabel.setText(String.valueOf(stats.transactionCount()));
        historyDepositsValueLabel.setText(formatMoney(stats.totalDeposited()));
        historyContributionsValueLabel.setText(formatMoney(stats.totalContributedToGoals()));
        historyAverageValueLabel.setText(formatMoney(stats.averageAmount()));
    }

    private void showHistoryStatsSummary() {
        List<SavingsTransactionRepository.TransactionRow> filteredHistory = controller.filterAndSortHistory(
                historySearchValue(),
                historySortAttributeValue(),
                historySortDirectionValue()
        );
        SavingsModuleService.HistoryStats stats = controller.calculateHistoryStats(filteredHistory);
        showInfo("Savings charts generated from the current filtered history.");
        showStatsWindow(
                "Savings Stats",
                "Savings statistics",
                "Visual analytics for the current savings filters.",
                buildSavingsStatsContent(filteredHistory, stats)
        );
    }

    private void handleExportCsv() {
        SavingsUiController.OperationResult result = controller.safeExportHistoryCsv(
                historySearchValue(),
                historySortAttributeValue(),
                historySortDirectionValue(),
                java.nio.file.Paths.get("target", "exports")
        );
        if (result.success()) {
            showSuccess(result.message());
            showModal("CSV Export", "Savings history exported", result.message(), Alert.AlertType.INFORMATION);
        } else {
            showError(result.message());
            showModal("CSV Export", "Savings history export failed", result.message(), Alert.AlertType.ERROR);
        }
    }

    private void handleExportPdf() {
        SavingsUiController.OperationResult result = controller.safeExportHistoryPdf(
                historySearchValue(),
                historySortAttributeValue(),
                historySortDirectionValue(),
                java.nio.file.Paths.get("target", "exports")
        );
        if (result.success()) {
            showSuccess(result.message());
            showModal("PDF Export", "Savings history exported", result.message(), Alert.AlertType.INFORMATION);
        } else {
            showError(result.message());
            showModal("PDF Export", "Savings history export failed", result.message(), Alert.AlertType.ERROR);
        }
    }

    private void updateGoalsStats(List<SavingsModuleService.GoalSnapshot> goals) {
        SavingsModuleService.GoalStats stats = controller.calculateGoalStats(goals);
        goalsCountValueLabel.setText(String.valueOf(stats.goalCount()));
        goalsCompletedValueLabel.setText(String.valueOf(stats.completedGoalCount()));
        goalsTargetValueLabel.setText(formatMoney(stats.totalTarget()));
        goalsRemainingValueLabel.setText(formatMoney(stats.remainingAmount()));
    }

    private void showGoalsStatsSummary() {
        List<SavingsModuleService.GoalSnapshot> filteredGoals = controller.filterAndSortGoals(
                goalsSearchValue(),
                goalsSortAttributeValue(),
                goalsSortDirectionValue()
        );
        SavingsModuleService.GoalStats stats = controller.calculateGoalStats(filteredGoals);
        showInfo("Goals charts generated from the current filtered goals.");
        showStatsWindow(
                "Goals Stats",
                "Goals statistics",
                "Visual analytics for the current goals filters.",
                buildGoalsStatsContent(filteredGoals, stats)
        );
    }

    private void handleGoalsExportCsv() {
        SavingsUiController.OperationResult result = controller.safeExportGoalsCsv(
                goalsSearchValue(),
                goalsSortAttributeValue(),
                goalsSortDirectionValue(),
                java.nio.file.Paths.get("target", "exports")
        );
        if (result.success()) {
            showSuccess(result.message());
            showModal("CSV Export", "Goals exported", result.message(), Alert.AlertType.INFORMATION);
        } else {
            showError(result.message());
            showModal("CSV Export", "Goals export failed", result.message(), Alert.AlertType.ERROR);
        }
    }

    private void handleGoalsExportPdf() {
        SavingsUiController.OperationResult result = controller.safeExportGoalsPdf(
                goalsSearchValue(),
                goalsSortAttributeValue(),
                goalsSortDirectionValue(),
                java.nio.file.Paths.get("target", "exports")
        );
        if (result.success()) {
            showSuccess(result.message());
            showModal("PDF Export", "Goals exported", result.message(), Alert.AlertType.INFORMATION);
        } else {
            showError(result.message());
            showModal("PDF Export", "Goals export failed", result.message(), Alert.AlertType.ERROR);
        }
    }

    private void showStatsWindow(String title, String header, String subtitle, Parent content) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        Window owner = currentWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }

        VBox root = new VBox(18);
        root.setPadding(new Insets(22));
        root.getStyleClass().add("glass-card");

        Label headerLabel = new Label(header);
        headerLabel.getStyleClass().add("section-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("section-subtitle");
        subtitleLabel.setWrapText(true);

        root.getChildren().addAll(headerLabel, subtitleLabel, content);

        Scene scene = new Scene(root, 980, 760);
        scene.getStylesheets().add(
                SavingsGoalsView.class.getResource("/pi/savings/ui/savings-goals.css").toExternalForm()
        );

        stage.setTitle(title);
        stage.setScene(scene);
        stage.show();
    }

    private Parent buildSavingsStatsContent(
            List<SavingsTransactionRepository.TransactionRow> transactions,
            SavingsModuleService.HistoryStats stats
    ) {
        VBox content = new VBox(18);

        HBox statRow = new HBox(10,
                compactStatCard("Rows", statLabel(String.valueOf(stats.transactionCount()))),
                compactStatCard("Deposits", statLabel(formatMoney(stats.totalDeposited()))),
                compactStatCard("Goals", statLabel(formatMoney(stats.totalContributedToGoals()))),
                compactStatCard("Latest", statLabel(stats.latestTransactionDate()))
        );

        HBox chartsRow = new HBox(18,
                buildChartCard("Amounts By Type", createSavingsTypePieChart(stats)),
                buildChartCard("Amounts By Date", createSavingsTimelineChart(transactions))
        );
        HBox.setHgrow(chartsRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(chartsRow.getChildren().get(1), Priority.ALWAYS);

        VBox attributeCard = buildAttributeSummaryCard(
                "Savings attributes",
                List.of(
                        "Types present: " + summarizeTransactionTypes(transactions),
                        "Module sources: " + summarizeModuleSources(transactions),
                        "Users covered: " + summarizeUserIds(transactions),
                        "Average amount: " + formatMoney(stats.averageAmount())
                )
        );

        content.getChildren().addAll(statRow, chartsRow, attributeCard);
        return content;
    }

    private Parent buildGoalsStatsContent(
            List<SavingsModuleService.GoalSnapshot> goals,
            SavingsModuleService.GoalStats stats
    ) {
        VBox content = new VBox(18);

        HBox statRow = new HBox(10,
                compactStatCard("Goals", statLabel(String.valueOf(stats.goalCount()))),
                compactStatCard("Completed", statLabel(String.valueOf(stats.completedGoalCount()))),
                compactStatCard("Current", statLabel(formatMoney(stats.totalCurrent()))),
                compactStatCard("Nearest", statLabel(stats.nearestDeadline()))
        );

        HBox chartsRow = new HBox(18,
                buildChartCard("Current vs Remaining", createGoalsProgressPieChart(stats)),
                buildChartCard("Target / Current / Remaining", createGoalsAmountsChart(goals))
        );
        HBox.setHgrow(chartsRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(chartsRow.getChildren().get(1), Priority.ALWAYS);

        VBox attributeCard = buildAttributeSummaryCard(
                "Goals attributes",
                List.of(
                        "Goal names: " + summarizeGoalNames(goals),
                        "Priorities: " + summarizeGoalPriorities(goals),
                        "Completion rate: " + stats.completionRate() + "%",
                        "Remaining amount: " + formatMoney(stats.remainingAmount())
                )
        );

        content.getChildren().addAll(statRow, chartsRow, attributeCard);
        return content;
    }

    private VBox buildChartCard(String titleText, javafx.scene.Node chart) {
        VBox card = new VBox(12);
        card.getStyleClass().add("glass-card");
        HBox.setHgrow(card, Priority.ALWAYS);
        VBox.setVgrow(card, Priority.ALWAYS);

        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");

        card.getChildren().addAll(title, chart);
        return card;
    }

    private VBox buildAttributeSummaryCard(String titleText, List<String> lines) {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-card");
        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");
        card.getChildren().add(title);
        for (String line : lines) {
            Label label = new Label(line);
            label.getStyleClass().add("mini-row-label");
            label.setWrapText(true);
            card.getChildren().add(label);
        }
        return card;
    }

    private Label statLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("card-value");
        return label;
    }

    private PieChart createSavingsTypePieChart(SavingsModuleService.HistoryStats stats) {
        PieChart chart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data("Deposits", stats.totalDeposited().doubleValue()),
                new PieChart.Data("Goal contributions", stats.totalContributedToGoals().doubleValue())
        ));
        chart.setPrefHeight(280);
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        installPieTooltips(chart);
        return chart;
    }

    private BarChart<String, Number> createSavingsTimelineChart(List<SavingsTransactionRepository.TransactionRow> transactions) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setPrefHeight(300);

        Map<String, BigDecimal> totalsByDate = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        for (SavingsTransactionRepository.TransactionRow row : transactions) {
            String key = row.date().toLocalDate().format(formatter);
            totalsByDate.merge(key, row.amount(), BigDecimal::add);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (totalsByDate.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No data", 0));
        } else {
            totalsByDate.forEach((label, amount) -> series.getData().add(new XYChart.Data<>(label, amount.doubleValue())));
        }
        chart.getData().setAll(series);
        return chart;
    }

    private PieChart createGoalsProgressPieChart(SavingsModuleService.GoalStats stats) {
        PieChart chart = new PieChart(FXCollections.observableArrayList(
                new PieChart.Data("Current", stats.totalCurrent().doubleValue()),
                new PieChart.Data("Remaining", stats.remainingAmount().doubleValue())
        ));
        chart.setPrefHeight(280);
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        installPieTooltips(chart);
        return chart;
    }

    private BarChart<String, Number> createGoalsAmountsChart(List<SavingsModuleService.GoalSnapshot> goals) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setPrefHeight(300);
        chart.setCategoryGap(12);
        chart.setBarGap(4);

        XYChart.Series<String, Number> targetSeries = new XYChart.Series<>();
        targetSeries.setName("Target");
        XYChart.Series<String, Number> currentSeries = new XYChart.Series<>();
        currentSeries.setName("Current");
        XYChart.Series<String, Number> remainingSeries = new XYChart.Series<>();
        remainingSeries.setName("Remaining");

        if (goals.isEmpty()) {
            targetSeries.getData().add(new XYChart.Data<>("No data", 0));
        } else {
            for (SavingsModuleService.GoalSnapshot goal : goals) {
                String name = truncate(goal.name(), 12);
                BigDecimal remaining = goal.target().subtract(goal.current()).max(BigDecimal.ZERO);
                targetSeries.getData().add(new XYChart.Data<>(name, goal.target().doubleValue()));
                currentSeries.getData().add(new XYChart.Data<>(name, goal.current().doubleValue()));
                remainingSeries.getData().add(new XYChart.Data<>(name, remaining.doubleValue()));
            }
        }

        chart.getData().setAll(targetSeries, currentSeries, remainingSeries);
        return chart;
    }

    private void installPieTooltips(PieChart chart) {
        for (PieChart.Data data : chart.getData()) {
            String tooltipText = data.getName() + ": " + formatPlain(BigDecimal.valueOf(data.getPieValue())) + " TND";
            if (data.getNode() != null) {
                Tooltip.install(data.getNode(), new Tooltip(tooltipText));
            } else {
                data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        Tooltip.install(newNode, new Tooltip(tooltipText));
                    }
                });
            }
        }
        Platform.runLater(chart::requestLayout);
    }

    private String summarizeTransactionTypes(List<SavingsTransactionRepository.TransactionRow> transactions) {
        return transactions.stream()
                .map(SavingsTransactionRepository.TransactionRow::type)
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("None");
    }

    private String summarizeModuleSources(List<SavingsTransactionRepository.TransactionRow> transactions) {
        return transactions.stream()
                .map(SavingsTransactionRepository.TransactionRow::moduleSource)
                .filter(source -> source != null && !source.isBlank())
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("None");
    }

    private String summarizeUserIds(List<SavingsTransactionRepository.TransactionRow> transactions) {
        return transactions.stream()
                .map(row -> String.valueOf(row.userId()))
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("None");
    }

    private String summarizeGoalNames(List<SavingsModuleService.GoalSnapshot> goals) {
        return goals.stream()
                .map(SavingsModuleService.GoalSnapshot::name)
                .map(name -> truncate(name, 18))
                .reduce((left, right) -> left + ", " + right)
                .orElse("None");
    }

    private String summarizeGoalPriorities(List<SavingsModuleService.GoalSnapshot> goals) {
        return goals.stream()
                .map(goal -> "P" + goal.priority())
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse("None");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private void showModal(String title, String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(header);
        Window owner = currentWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        String stylesheet = SavingsGoalsView.class.getResource("/pi/savings/ui/savings-goals.css").toExternalForm();
        if (!alert.getDialogPane().getStylesheets().contains(stylesheet)) {
            alert.getDialogPane().getStylesheets().add(stylesheet);
        }
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<ButtonType> ignored = alert.showAndWait();
    }

    private void openGoalsBackOffice() {
        try {
            GoalsBackOfficeView backOfficeView = new GoalsBackOfficeView();
            Stage stage = new Stage();
            Scene scene = new Scene(backOfficeView.build(new SavingsUiController()), 1400, 860);
            stage.setTitle("Decide$ - Goals Back Office");
            Window owner = currentWindow();
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.setScene(scene);
            stage.show();
            showInfo("Goals back-office opened in a separate window.");
        } catch (RuntimeException exception) {
            showError("Impossible d'ouvrir le back-office Goals.");
        }
    }

    private Window currentWindow() {
        if (tabPane != null && tabPane.getScene() != null) {
            return tabPane.getScene().getWindow();
        }
        if (feedbackBox != null && feedbackBox.getScene() != null) {
            return feedbackBox.getScene().getWindow();
        }
        return null;
    }

    private Button headerNavButton(String text, Runnable action) {
        Button button = actionButton(text, "header-nav-btn");
        button.setOnAction(event -> action.run());
        if ("Savings".equals(text)) {
            button.getStyleClass().add("header-nav-active");
        }
        return button;
    }

    private void openHomePage() {
        navigateToPage("/pi/mains/salary-home-view.fxml", "/pi/styles/salary-home.css", "Salary Home");
    }

    private void openAboutPage() {
        navigateToPage("/pi/mains/about-view.fxml", "/pi/styles/about.css", "About Us");
    }

    private void openServicePage() {
        navigateToPage("/pi/mains/service-view.fxml", "/pi/styles/service.css", "Services");
    }

    private void openRevenueExpensePage() {
        navigateToPage("/Expense/Revenue/FRONT/salary-expense-view.fxml", null, "Income & Expense Management");
    }

    private void openContactPage() {
        navigateToPage("/pi/mains/contact-view.fxml", "/pi/styles/contact.css", "Contact");
    }

    private void openLoginPage() {
        navigateToPage("/pi/mains/login-view.fxml", "/pi/styles/login.css", "User Secure Login", false);
    }

    private void navigateToPage(String fxmlPath, String cssPath, String title) {
        navigateToPage(fxmlPath, cssPath, title, true);
    }

    private void navigateToPage(String fxmlPath, String cssPath, String title, boolean preserveUserData) {
        Window owner = currentWindow();
        if (!(owner instanceof Stage stage)) {
            showError("Navigation window is unavailable.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent root = loader.load();
            Object userData = preserveUserData ? stage.getUserData() : null;
            applyUserContext(loader.getController(), userData);

            Scene scene = new Scene(root, 1460, 780);
            if (cssPath != null) {
                scene.getStylesheets().add(Main.class.getResource(cssPath).toExternalForm());
            }

            stage.setUserData(userData);
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException exception) {
            showError("Impossible d'ouvrir " + title + ".");
        }
    }

    private void applyUserContext(Object targetController, Object userData) {
        if (!(userData instanceof User user) || targetController == null) {
            return;
        }

        if (targetController instanceof SalaryHomeController salaryHomeController) {
            salaryHomeController.setUser(user);
        } else if (targetController instanceof ServiceController serviceController) {
            serviceController.setUser(user);
        } else if (targetController instanceof AboutController aboutController) {
            aboutController.setUser(user);
        } else if (targetController instanceof ContactController contactController) {
            contactController.setUser(user);
        } else if (targetController instanceof SalaryExpenseController salaryExpenseController) {
            salaryExpenseController.setUser(user);
        }
    }

    private Button tabPill(String text, boolean active) {
        Button button = new Button(text);
        button.getStyleClass().add("tab-pill");
        if (active) {
            button.getStyleClass().add("tab-pill-active");
        }
        return button;
    }

    private VBox footerColumn(String titleText, String... lines) {
        VBox column = new VBox(8);
        Label title = new Label(titleText);
        title.getStyleClass().add("footer-subtitle");
        column.getChildren().add(title);
        for (String line : lines) {
            Label item = new Label(line);
            item.getStyleClass().add("footer-text");
            item.setWrapText(true);
            column.getChildren().add(item);
        }
        return column;
    }

    private void updateTabButton(Button button, boolean active) {
        if (active) {
            if (!button.getStyleClass().contains("tab-pill-active")) {
                button.getStyleClass().add("tab-pill-active");
            }
        } else {
            button.getStyleClass().remove("tab-pill-active");
        }
    }

    private void showSuccess(String message) {
        showFeedback(message, "feedback-success");
    }

    private void showError(String message) {
        showFeedback(message, "feedback-error");
    }

    private void showInfo(String message) {
        showFeedback(message, "feedback-info");
    }

    private void showFeedback(String message, String styleClass) {
        feedbackBox.getStyleClass().removeAll("feedback-success", "feedback-error", "feedback-info");
        feedbackBox.getStyleClass().add(styleClass);
        feedbackLabel.setText(message);
        feedbackBox.setManaged(true);
        feedbackBox.setVisible(true);
    }

    private String formatMoney(BigDecimal value) {
        return formatPlain(value) + " TND";
    }

    private String formatPlain(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String historySearchValue() {
        return historySearchField == null ? "" : historySearchField.getText();
    }

    private String historySortAttributeValue() {
        return historySortComboBox == null || historySortComboBox.getValue() == null ? "Date" : historySortComboBox.getValue();
    }

    private String historySortDirectionValue() {
        return historyDirectionComboBox == null || historyDirectionComboBox.getValue() == null ? "Descending" : historyDirectionComboBox.getValue();
    }

    private String goalsSearchValue() {
        return goalsSearchField == null ? "" : goalsSearchField.getText();
    }

    private String goalDeadlineValue() {
        return goalDeadlinePicker == null || goalDeadlinePicker.getValue() == null ? "" : goalDeadlinePicker.getValue().toString();
    }

    private String goalsSortAttributeValue() {
        return goalsSortComboBox == null || goalsSortComboBox.getValue() == null ? "Priority" : goalsSortComboBox.getValue();
    }

    private String goalsSortDirectionValue() {
        return goalsDirectionComboBox == null || goalsDirectionComboBox.getValue() == null ? "Descending" : goalsDirectionComboBox.getValue();
    }

    private TextFormatter<String> createMoneyTextFormatter(int maxIntegerDigits) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            if (next.isEmpty()) {
                return change;
            }

            String normalized = next.replace(',', '.');
            if (!normalized.matches("\\d{0," + maxIntegerDigits + "}(\\.\\d{0,2})?")) {
                return null;
            }

            if (next.contains(",")) {
                change.setText(change.getText().replace(',', '.'));
            }
            return change;
        };
        return new TextFormatter<>(filter);
    }

    private void attachMoneyValidation(TextField field, String fieldLabel, int maxIntegerDigits) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isBlank()) {
                return;
            }

            if (!isValidMoneyInput(newValue, maxIntegerDigits)) {
                showError(fieldLabel + " must contain digits only.");
            }
        });
    }

    private boolean validateMoneyField(TextField field, String fieldLabel, int maxIntegerDigits) {
        String value = field == null ? "" : field.getText();
        if (value == null || value.isBlank()) {
            return true;
        }

        if (isValidMoneyInput(value, maxIntegerDigits)) {
            return true;
        }

        showError(fieldLabel + " must contain a valid numeric value.");
        field.requestFocus();
        field.selectAll();
        return false;
    }

    private boolean isValidMoneyInput(String value, int maxIntegerDigits) {
        String normalized = value.trim().replace(',', '.');
        return normalized.matches("\\d{0," + maxIntegerDigits + "}(\\.\\d{0,2})?");
    }

    private void attachPriorityValidation(TextField field, String fieldLabel) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || newValue.isBlank()) {
                return;
            }

            if (!isValidPriorityInput(newValue)) {
                showError(fieldLabel + " must be a number between 1 and 5.");
            }
        });
    }

    private boolean validatePriorityField(TextField field, String fieldLabel) {
        String value = field == null ? "" : field.getText();
        if (value == null || value.isBlank()) {
            return true;
        }

        if (isValidPriorityInput(value)) {
            return true;
        }

        showError(fieldLabel + " must be a number between 1 and 5.");
        field.requestFocus();
        field.selectAll();
        return false;
    }

    private boolean isValidPriorityInput(String value) {
        return value.trim().matches("[1-5]");
    }

    private TextFormatter<String> createPriorityFormatter() {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String next = change.getControlNewText();
            if (next.isEmpty() || next.matches("[1-5]?")) {
                return change;
            }
            return null;
        };
        return new TextFormatter<>(filter);
    }

    private TextFormatter<String> createLengthFormatter(int maxLength) {
        UnaryOperator<TextFormatter.Change> filter = change ->
                change.getControlNewText().length() <= maxLength ? change : null;
        return new TextFormatter<>(filter);
    }
}
