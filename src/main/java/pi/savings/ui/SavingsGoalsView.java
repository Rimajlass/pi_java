package pi.savings.ui;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
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
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.stage.Stage;
import pi.savings.repository.SavingsTransactionRepository;
import pi.savings.service.SavingsModuleService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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
    private Label historyCountValueLabel;
    private Label historyDepositsValueLabel;
    private Label historyContributionsValueLabel;
    private Label historyAverageValueLabel;
    private Label goalsCountValueLabel;
    private Label goalsCompletedValueLabel;
    private Label goalsTargetValueLabel;
    private Label goalsRemainingValueLabel;

    Parent build(SavingsUiController controller) {
        this.controller = controller;

        VBox page = new VBox();
        page.getStyleClass().add("page");
        page.getChildren().add(buildHeader());
        page.getChildren().add(buildHero());
        page.getChildren().add(buildContent());
        page.getChildren().add(buildFooter());

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-scroll");

        BorderPane root = new BorderPane(scrollPane);
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

    private HBox buildHeader() {
        HBox header = new HBox(24);
        header.getStyleClass().add("top-header");
        header.setAlignment(Pos.CENTER_LEFT);

        HBox brand = new HBox(12);
        brand.setAlignment(Pos.CENTER_LEFT);
        StackPane logo = iconBubble("$");
        logo.getStyleClass().add("brand-bubble");
        Label brandName = new Label("Decide$");
        brandName.getStyleClass().add("brand-name");
        brand.getChildren().addAll(logo, brandName);

        HBox nav = new HBox(
                headerNavButton("Home", "Main navigation remains informational in desktop mode."),
                headerNavButton("About", "About section remains informational in desktop mode."),
                headerNavButton("Service", "Service section remains informational in desktop mode."),
                headerNavButton("Income & Expenses", "Income & Expenses section remains informational in desktop mode."),
                headerNavButton("Savings", "You are already inside the Savings module."),
                headerNavButton("Unexpected Events", "Unexpected Events section remains informational in desktop mode."),
                headerNavButton("Contact", "Contact section remains informational in desktop mode.")
        );
        nav.getStyleClass().add("top-nav");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button themeButton = actionButton("Theme", "header-icon-btn");
        themeButton.setOnAction(event -> showInfo("Theme switching is not connected yet."));

        Button startButton = actionButton("To Start", "header-start-btn");
        startButton.setOnAction(event -> {
            tabPane.getSelectionModel().select(0);
            showInfo("Navigation focus moved to the Savings workspace.");
        });

        Button logoutButton = actionButton("Logout", "header-logout-btn");
        logoutButton.setOnAction(event -> showInfo("Logout action is not connected in this desktop prototype."));

        HBox actions = new HBox(12, themeButton, startButton, logoutButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(brand, nav, spacer, actions);
        return header;
    }

    private VBox buildHero() {
        VBox hero = new VBox(14);
        hero.getStyleClass().add("hero");
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
        addDepositButton.setOnAction(event -> {
            tabPane.getSelectionModel().select(0);
            depositAmountField.requestFocus();
        });

        Button createGoalButton = actionButton("Create Goal", "soft-hero-btn");
        createGoalButton.setOnAction(event -> {
            tabPane.getSelectionModel().select(1);
            goalNameField.requestFocus();
            showInfo("Goal creation form is ready.");
        });

        Button backOfficeButton = actionButton("Test Back Office", "soft-hero-btn");
        backOfficeButton.setOnAction(event -> openGoalsBackOffice());

        Button calendarButton = actionButton("Calendar", "soft-hero-btn");
        calendarButton.setOnAction(event -> showInfo("Calendar action is planned for a later step."));

        Button simulationButton = actionButton("What-if?", "soft-hero-btn");
        simulationButton.setOnAction(event -> showInfo("Simulation action is planned for a later step."));

        HBox actions = new HBox(12, addDepositButton, createGoalButton, backOfficeButton, calendarButton, simulationButton);
        actions.getStyleClass().add("hero-actions");
        actions.setAlignment(Pos.CENTER);

        hero.getChildren().addAll(title, subtitle, breadcrumb, actions);
        return hero;
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
        rateField.setTextFormatter(createMoneyTextFormatter(3));
        rateField.setOnAction(event -> handleUpdateRate());

        Button saveRateButton = actionButton("Save", "primary-btn");
        saveRateButton.setPadding(new Insets(10, 16, 10, 16));
        saveRateButton.setOnAction(event -> handleUpdateRate());

        depositAmountField = new TextField();
        depositAmountField.setPromptText("Ex: 200");
        depositAmountField.getStyleClass().add("field");
        depositAmountField.setTextFormatter(createMoneyTextFormatter(7));
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

        HBox footerActions = new HBox(10, statsButton, exportCsvButton, exportPdfButton);
        footerActions.setAlignment(Pos.CENTER_LEFT);

        HBox toolbar = new HBox(10, searchButton, historySearchField, historySortComboBox, historyDirectionComboBox, allButton, resetButton);
        toolbar.getStyleClass().add("toolbar-row");

        card.getChildren().add(cardHeader("Savings History", "search - sort - export - stats", "H"));
        card.getChildren().add(toolbar);
        card.getChildren().add(buildHistoryStatsPanel());
        card.getChildren().add(buildHistoryTableWrapper());
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
        goalTargetField.setTextFormatter(createMoneyTextFormatter(7));
        goalTargetField.setOnAction(event -> handleSaveGoal());

        goalCurrentField = new TextField("0");
        goalCurrentField.getStyleClass().add("field");
        goalCurrentField.setTextFormatter(createMoneyTextFormatter(7));
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
        goalPriorityField.setTextFormatter(createPriorityFormatter());
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
        card.getChildren().add(footerActions);
        return card;
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
        contributeField.setTextFormatter(createMoneyTextFormatter(7));
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
        SavingsUiController.OperationResult result = controller.safeUpdateInterestRate(rateField.getText());
        if (result.success()) {
            refreshAll();
            showSuccess(result.message());
        } else {
            showError(result.message());
        }
    }

    private void handleSaveGoal() {
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
        if (historyEntries.isEmpty()) {
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

        for (SavingsTransactionRepository.TransactionRow entry : historyEntries) {
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
        if (goals.isEmpty()) {
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
        for (SavingsModuleService.GoalSnapshot goal : goals) {
            goalsListBox.getChildren().add(buildGoalCard(goal));
        }
    }

    private void applyHistorySearchAndSort() {
        refreshHistory(controller.filterAndSortHistory(historySearchValue(), historySortAttributeValue(), historySortDirectionValue()));
        showInfo("Savings history search/sort applied.");
    }

    private void applyHistoryDefaultView() {
        historySearchField.clear();
        historySortComboBox.setValue("Date");
        historyDirectionComboBox.setValue("Descending");
        refreshHistory(controller.filterAndSortHistory("", historySortAttributeValue(), historySortDirectionValue()));
        showInfo("Savings history default view restored.");
    }

    private void resetHistorySearchAndSort() {
        historySearchField.clear();
        historySortComboBox.setValue("All");
        historyDirectionComboBox.setValue("Descending");
        refreshHistory(controller.filterAndSortHistory("", historySortAttributeValue(), historySortDirectionValue()));
        showInfo("Savings history filters reset.");
    }

    private void applyGoalsSearchAndSort() {
        refreshGoals(controller.filterAndSortGoals(goalsSearchValue(), goalsSortAttributeValue(), goalsSortDirectionValue()));
        showInfo("Goals search/sort applied.");
    }

    private void applyGoalsDefaultView() {
        goalsSearchField.clear();
        goalsSortComboBox.setValue("Priority");
        goalsDirectionComboBox.setValue("Descending");
        refreshGoals(controller.filterAndSortGoals("", goalsSortAttributeValue(), goalsSortDirectionValue()));
        showInfo("Goals default view restored.");
    }

    private void resetGoalsSearchAndSort() {
        goalsSearchField.clear();
        goalsSortComboBox.setValue("All");
        goalsDirectionComboBox.setValue("Descending");
        refreshGoals(controller.filterAndSortGoals("", goalsSortAttributeValue(), goalsSortDirectionValue()));
        showInfo("Goals filters reset.");
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
        SavingsModuleService.HistoryStats stats = controller.getHistoryStats(
                historySearchValue(),
                historySortAttributeValue(),
                historySortDirectionValue()
        );
        String message = "Rows: " + stats.transactionCount()
                + "\nDeposits: " + formatMoney(stats.totalDeposited())
                + "\nContributions: " + formatMoney(stats.totalContributedToGoals())
                + "\nAverage: " + formatMoney(stats.averageAmount())
                + "\nLatest: " + stats.latestTransactionDate();
        showInfo(message.replace('\n', ' '));
        showModal("Savings Stats", "Savings history statistics", message, Alert.AlertType.INFORMATION);
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
        SavingsModuleService.GoalStats stats = controller.getGoalStats(
                goalsSearchValue(),
                goalsSortAttributeValue(),
                goalsSortDirectionValue()
        );
        String message = "Goals: " + stats.goalCount()
                + "\nCompleted: " + stats.completedGoalCount()
                + "\nCompletion rate: " + stats.completionRate() + "%"
                + "\nTarget: " + formatMoney(stats.totalTarget())
                + "\nCurrent: " + formatMoney(stats.totalCurrent())
                + "\nRemaining: " + formatMoney(stats.remainingAmount())
                + "\nNearest: " + stats.nearestDeadline();
        showInfo(message.replace('\n', ' '));
        showModal("Goals Stats", "Goals statistics", message, Alert.AlertType.INFORMATION);
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

    private Button headerNavButton(String text, String message) {
        Button button = actionButton(text, "header-nav-btn");
        button.setOnAction(event -> showInfo(message));
        if ("Savings".equals(text)) {
            button.getStyleClass().add("header-nav-active");
        }
        return button;
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
