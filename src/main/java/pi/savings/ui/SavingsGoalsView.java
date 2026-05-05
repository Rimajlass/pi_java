package pi.savings.ui;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
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
import pi.assistant.CommandIntent;
import pi.assistant.CommandInterpreterService;
import pi.assistant.SmartVoiceAgentController;
import pi.assistant.SmartVoiceAgentPane;
import pi.savings.dto.AttributeStatsDTO;
import pi.savings.dto.CalendarEventDTO;
import pi.savings.dto.GoalAnalyticsDTO;
import pi.savings.dto.GoalRiskDTO;
import pi.savings.dto.WhatIfScenarioDTO;
import pi.mains.Main;
import pi.entities.CalendarEvent;
import pi.savings.repository.SavingsTransactionRepository;
import pi.savings.service.GoalsAnalyticsService;
import pi.savings.service.SavingsCalendarService;
import pi.savings.service.SavingsStatsService;
import pi.savings.service.SavingsModuleService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;
import java.util.function.UnaryOperator;

final class SavingsGoalsView {
    private static final DecimalFormat MONEY_FORMAT;
    private static final String THEME_PREFERENCE_KEY = "savingsGoals.darkMode";
    private final Preferences preferences = Preferences.userNodeForPackage(SavingsGoalsView.class);

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        MONEY_FORMAT = new DecimalFormat("#,##0.##", symbols);
    }

    private SavingsUiController controller;
    private Label balanceValueLabel;
    private Label activeGoalsValueLabel;
    private Label progressValueLabel;
    private Label nearestDeadlineValueLabel;
    private Label accountBalanceLabel;
    private Label createdOnLabel;
    private Label convertedBalanceLabel;
    private Label rateApiLabel;
    private Label convertedGoalsLabel;
    private Label holidayRiskLabel;
    private Label accountConvertedBalanceLabel;
    private Label accountRateApiLabel;
    private Label accountConvertedGoalsLabel;
    private Label accountHolidayRiskLabel;
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
    private Label historyTotalLabel;
    private Button historyPreviousButton;
    private Button historyNextButton;
    private int historyPageIndex;
    private static final int HISTORY_PAGE_SIZE = 5;
    private Label goalsPaginationLabel;
    private Label goalsTotalLabel;
    private Button goalsPreviousButton;
    private Button goalsNextButton;
    private int goalsPageIndex;
    private static final int GOALS_PAGE_SIZE = 4;
    private SavingsStatsService.FrontStatsSnapshot frontStats;
    private SmartVoiceAgentController smartVoiceAgentController;
    private ToggleButton themeToggleButton;
    private StackPane rootContainer;
    private boolean darkModeEnabled;

    Parent build(SavingsUiController controller) {
        this.controller = controller;
        this.darkModeEnabled = preferences.getBoolean(THEME_PREFERENCE_KEY, false);
        this.smartVoiceAgentController = new SmartVoiceAgentController(createAssistantGateway());

        VBox page = new VBox();
        page.getStyleClass().add("page");
        page.getChildren().add(buildTopShell());
        page.getChildren().add(buildContent());
        page.getChildren().add(buildFooter());

        pageScrollPane = new ScrollPane(page);
        pageScrollPane.setFitToWidth(true);
        pageScrollPane.getStyleClass().add("page-scroll");

        BorderPane mainLayout = new BorderPane(pageScrollPane);
        rootContainer = new StackPane(mainLayout);
        SmartVoiceAgentPane smartVoiceAgentPane = new SmartVoiceAgentPane(
                smartVoiceAgentController,
                result -> {
                    if (shouldRefreshAfterAssistantAction(result.getIntent(), result.isSuccess())) {
                        refreshAll();
                    }
                }
        );
        StackPane.setAlignment(smartVoiceAgentPane, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(smartVoiceAgentPane, new Insets(0, 26, 24, 0));
        rootContainer.getChildren().add(smartVoiceAgentPane);

        rootContainer.getStylesheets().add(
                SavingsGoalsView.class.getResource("/pi/savings/ui/savings-goals.css").toExternalForm()
        );
        applyTheme();

        renderEmptyState();
        initializeDataAsync();
        return rootContainer;
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
        if (convertedBalanceLabel != null) {
            convertedBalanceLabel.setText("0 TND");
        }
        if (rateApiLabel != null) {
            rateApiLabel.setText("1 TND = 1 TND");
        }
        if (convertedGoalsLabel != null) {
            convertedGoalsLabel.setText("0 TND");
        }
        if (holidayRiskLabel != null) {
            holidayRiskLabel.setText("0");
        }
        if (accountConvertedBalanceLabel != null) {
            accountConvertedBalanceLabel.setText("0 TND");
        }
        if (accountRateApiLabel != null) {
            accountRateApiLabel.setText("1 TND = 1 TND");
        }
        if (accountConvertedGoalsLabel != null) {
            accountConvertedGoalsLabel.setText("0 TND");
        }
        if (accountHolidayRiskLabel != null) {
            accountHolidayRiskLabel.setText("0");
        }
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
        Label profileIcon = new Label("\uD83D\uDCB0");
        profileIcon.getStyleClass().add("profile-icon");
        Label profileName = new Label("Savings Workspace");
        profileName.getStyleClass().add("profile-name");
        profilePill.getChildren().addAll(profileIcon, profileName);

        Button startButton = actionButton("To Start", "header-start-btn");
        startButton.setOnAction(event -> openHomePage());

        Button logoutButton = actionButton("Logout", "header-logout-btn");
        logoutButton.setOnAction(event -> openLoginPage());

        themeToggleButton = new ToggleButton();
        themeToggleButton.getStyleClass().addAll("ghost-btn", "theme-toggle-btn");
        themeToggleButton.setSelected(darkModeEnabled);
        updateThemeToggleButton();
        themeToggleButton.setOnAction(event -> toggleDarkMode(themeToggleButton.isSelected()));

        HBox headerActions = new HBox(12, profilePill, themeToggleButton, startButton, logoutButton);
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
        calendarButton.setOnAction(event -> showCalendarWindow());

        HBox actions = new HBox(12, addDepositButton, createGoalButton, calendarButton);
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

        grid.add(buildKpiCard("Savings Balance", "Current account balance", balanceValueLabel, "\uD83D\uDCB0"), 0, 0);
        grid.add(buildKpiCard("Active Goals", "Goals in progress", activeGoalsValueLabel, "\uD83C\uDFAF"), 1, 0);
        grid.add(buildKpiCard("Goals Progress", "Average completion", progressValueLabel, "\uD83D\uDCC8"), 2, 0);
        grid.add(buildKpiCard("Nearest Deadline", "Closest goal date", nearestDeadlineValueLabel, "\uD83D\uDCC5"), 3, 0);
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

    private boolean shouldRefreshAfterAssistantAction(CommandIntent intent, boolean success) {
        if (!success || intent == null) {
            return false;
        }
        return intent == CommandIntent.CREATE_GOAL
                || intent == CommandIntent.UPDATE_GOAL
                || intent == CommandIntent.DELETE_GOAL
                || intent == CommandIntent.CONTRIBUTE_TO_GOAL;
    }

    private CommandInterpreterService.CommandGateway createAssistantGateway() {
        return new CommandInterpreterService.CommandGateway() {
            @Override
            public CommandInterpreterService.CommandGatewaySnapshot loadSnapshot() {
                SavingsUiController.OperationResult initResult = controller.initialize();
                if (!initResult.success()) {
                    return new CommandInterpreterService.CommandGatewaySnapshot(
                            0,
                            BigDecimal.ZERO,
                            List.of()
                    );
                }

                SavingsModuleService.DashboardSnapshot snapshot = controller.getSnapshot();
                List<CommandInterpreterService.CommandGoalSnapshot> goals = snapshot.goals().stream()
                        .map(goal -> new CommandInterpreterService.CommandGoalSnapshot(
                                goal.id(),
                                goal.name(),
                                goal.target(),
                                goal.current(),
                                goal.deadline()
                        ))
                        .toList();

                return new CommandInterpreterService.CommandGatewaySnapshot(
                        snapshot.accountId(),
                        snapshot.balance(),
                        goals
                );
            }

            @Override
            public CommandInterpreterService.CommandGatewayResult createGoal(
                    String goalName,
                    BigDecimal targetAmount,
                    LocalDate deadline
            ) {
                LocalDate resolvedDeadline = deadline == null ? LocalDate.now().plusDays(30) : deadline;
                SavingsUiController.OperationResult result = controller.safeCreateGoal(
                        goalName,
                        targetAmount.toPlainString(),
                        "0",
                        resolvedDeadline.toString(),
                        "3"
                );
                return result.success()
                        ? CommandInterpreterService.CommandGatewayResult.success("Action completed successfully.")
                        : CommandInterpreterService.CommandGatewayResult.failure(result.message());
            }

            @Override
            public CommandInterpreterService.CommandGatewayResult updateGoal(
                    String goalName,
                    BigDecimal targetAmount,
                    LocalDate deadline
            ) {
                SavingsModuleService.DashboardSnapshot snapshot = controller.getSnapshot();
                SavingsModuleService.GoalSnapshot existingGoal = snapshot.goals().stream()
                        .filter(goal -> equalsIgnoreCase(goal.name(), goalName))
                        .findFirst()
                        .orElse(null);
                if (existingGoal == null) {
                    return CommandInterpreterService.CommandGatewayResult.failure("Goal not found.");
                }

                BigDecimal resolvedTarget = targetAmount == null ? existingGoal.target() : targetAmount;
                LocalDate resolvedDeadline = deadline == null ? existingGoal.deadline() : deadline;
                if (resolvedDeadline == null) {
                    resolvedDeadline = LocalDate.now().plusDays(30);
                }

                SavingsUiController.OperationResult result = controller.safeUpdateGoal(
                        existingGoal.id(),
                        existingGoal.name(),
                        resolvedTarget.toPlainString(),
                        existingGoal.current().toPlainString(),
                        resolvedDeadline.toString(),
                        String.valueOf(existingGoal.priority())
                );
                return result.success()
                        ? CommandInterpreterService.CommandGatewayResult.success("Action completed successfully.")
                        : CommandInterpreterService.CommandGatewayResult.failure(result.message());
            }

            @Override
            public CommandInterpreterService.CommandGatewayResult contributeToGoal(String goalName, BigDecimal amount) {
                SavingsModuleService.DashboardSnapshot snapshot = controller.getSnapshot();
                SavingsModuleService.GoalSnapshot existingGoal = snapshot.goals().stream()
                        .filter(goal -> equalsIgnoreCase(goal.name(), goalName))
                        .findFirst()
                        .orElse(null);
                if (existingGoal == null) {
                    return CommandInterpreterService.CommandGatewayResult.failure("Goal not found.");
                }

                SavingsUiController.OperationResult result = controller.safeContributeToGoal(
                        existingGoal.id(),
                        amount.toPlainString()
                );
                return result.success()
                        ? CommandInterpreterService.CommandGatewayResult.success("Action completed successfully.")
                        : CommandInterpreterService.CommandGatewayResult.failure(result.message());
            }

            @Override
            public CommandInterpreterService.CommandGatewayResult deleteGoal(String goalName) {
                SavingsModuleService.DashboardSnapshot snapshot = controller.getSnapshot();
                SavingsModuleService.GoalSnapshot existingGoal = snapshot.goals().stream()
                        .filter(goal -> equalsIgnoreCase(goal.name(), goalName))
                        .findFirst()
                        .orElse(null);
                if (existingGoal == null) {
                    return CommandInterpreterService.CommandGatewayResult.failure("Goal not found.");
                }

                SavingsUiController.OperationResult result = controller.safeDeleteGoal(existingGoal.id());
                return result.success()
                        ? CommandInterpreterService.CommandGatewayResult.success("Action completed successfully.")
                        : CommandInterpreterService.CommandGatewayResult.failure(result.message());
            }
        };
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
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
        accountConvertedBalanceLabel = valueLabel();
        accountRateApiLabel = valueLabel();
        accountConvertedGoalsLabel = valueLabel();
        accountHolidayRiskLabel = valueLabel();

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

        Button exportAccountCsvButton = actionButton("Account CSV", "ghost-btn");
        exportAccountCsvButton.setOnAction(event -> handleSavingAccountsExportCsv());

        Button exportAccountPdfButton = actionButton("Account PDF", "ghost-btn");
        exportAccountPdfButton.setOnAction(event -> handleSavingAccountsExportPdf());

        card.getChildren().addAll(
                cardHeader("My Savings Account", "balance + deposit", "\uD83D\uDCB0"),
                infoRow("Balance (solde)", accountBalanceLabel),
                infoRow("Currency API rate", accountRateApiLabel),
                infoRow("Converted balance", accountConvertedBalanceLabel),
                infoRow("Goals converted", accountConvertedGoalsLabel),
                infoRow("Holiday risks", accountHolidayRiskLabel),
                buildRateEditor(saveRateButton),
                infoRow("Created on", createdOnLabel),
                new Separator(),
                miniSectionTitle("Add a Deposit"),
                fieldBlock("Amount", depositAmountField),
                fieldBlock("Description", depositDescriptionField),
                saveDepositButton,
                new HBox(10, exportAccountCsvButton, exportAccountPdfButton)
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
        historySortComboBox.getItems().addAll("All", "Date", "Type", "Amount", "Description");
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

        card.getChildren().add(cardHeader("Savings History", "search - sort - export - stats", "\uD83D\uDCB3"));
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
                compactStatCard("Transactions", historyCountValueLabel),
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
        addHeaderCell(header, "Date", 0);
        addHeaderCell(header, "Type", 1);
        addHeaderCell(header, "Amount", 2);
        addHeaderCell(header, "Description", 3);

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

        card.getChildren().addAll(
                cardHeader("Create a Goal", "create + rules", "\uD83C\uDFAF"),
                fieldBlock("Goal name", goalNameField),
                twoFields(fieldBlock("Target (TND)", goalTargetField), fieldBlock("Current (TND)", goalCurrentField)),
                twoFields(fieldBlock("Deadline", goalDeadlinePicker), fieldBlock("Priority (1-5)", goalPriorityField)),
                addGoalButton
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

        Button goalsStatsButton = actionButton("Analytics Dashboard", "ghost-btn");
        goalsStatsButton.setOnAction(event -> showGoalsStatsSummary());

        Button goalsExportCsvButton = actionButton("Export CSV", "ghost-btn");
        goalsExportCsvButton.setOnAction(event -> handleGoalsExportCsv());

        Button goalsExportPdfButton = actionButton("Generate Smart PDF Report", "primary-btn");
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

        card.getChildren().add(cardHeader("Your Goals", "search - sort - export - analytics", "\uD83C\uDFAF"));
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
        historyTotalLabel = new Label("0 transactions au total");
        historyTotalLabel.getStyleClass().add("mini-row-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(10, historyPreviousButton, historyPaginationLabel, historyNextButton, spacer, historyTotalLabel);
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
        goalsTotalLabel = new Label("0 goals au total");
        goalsTotalLabel.getStyleClass().add("mini-row-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(10, goalsPreviousButton, goalsPaginationLabel, goalsNextButton, spacer, goalsTotalLabel);
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
        String convertedLine = "";
        if (frontStats != null) {
            BigDecimal rate = BigDecimal.valueOf(frontStats.rateSnapshot().rateToTnd());
            convertedLine = " - " + frontStats.rateSnapshot().currency() + " -> TND: "
                    + formatMoney(goal.current().multiply(rate)) + " / " + formatMoney(goal.target().multiply(rate));
        }
        Label meta = new Label(
                "Target: " + formatMoney(goal.target())
                        + " - Current: " + formatMoney(goal.current())
                        + " - Deadline: " + (goal.deadline() == null ? "--/--/----" : goal.deadline())
                        + convertedLine
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

    private void handleSavingAccountsExportCsv() {
        SavingsUiController.OperationResult result = controller.safeExportSavingAccountsCsv(
                java.nio.file.Paths.get("target", "exports")
        );
        if (result.success()) {
            showSuccess(result.message());
            showModal("CSV Export", "Saving account exported", result.message(), Alert.AlertType.INFORMATION);
        } else {
            showError(result.message());
            showModal("CSV Export", "Saving account export failed", result.message(), Alert.AlertType.ERROR);
        }
    }

    private void handleSavingAccountsExportPdf() {
        SavingsUiController.OperationResult result = controller.safeExportSavingAccountsPdf(
                java.nio.file.Paths.get("target", "exports")
        );
        if (result.success()) {
            showSuccess(result.message());
            showModal("PDF Export", "Saving account exported", result.message(), Alert.AlertType.INFORMATION);
        } else {
            showError(result.message());
            showModal("PDF Export", "Saving account export failed", result.message(), Alert.AlertType.ERROR);
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
        frontStats = null;
        try {
            frontStats = controller.loadFrontStats();
        } catch (RuntimeException exception) {
            showError(exception.getMessage() == null ? "Impossible de charger les donnees API." : exception.getMessage());
        }

        balanceValueLabel.setText(formatMoney(state.balance()));
        activeGoalsValueLabel.setText(frontStats == null ? String.valueOf(state.activeGoals()) : String.valueOf(frontStats.activeGoals()));
        progressValueLabel.setText(frontStats == null ? state.averageProgress() + "%" : frontStats.averageProgress().stripTrailingZeros().toPlainString() + "%");
        nearestDeadlineValueLabel.setText(frontStats == null ? state.nearestDeadline() : frontStats.nearestDeadline());

        accountBalanceLabel.setText(formatMoney(state.balance()));
        createdOnLabel.setText(state.createdOn().toString());
        rateField.setText(formatPlain(state.interestRate()));
        updateCurrencyWidgets();

        refreshHistory(controller.filterAndSortHistory(historySearchValue(), historySortAttributeValue(), historySortDirectionValue()));
        refreshGoals(controller.filterAndSortGoals(goalsSearchValue(), goalsSortAttributeValue(), goalsSortDirectionValue()));
    }

    private void updateCurrencyWidgets() {
        if (frontStats == null) {
            return;
        }

        String rateText = "1 " + frontStats.rateSnapshot().currency() + " = "
                + formatPlain(BigDecimal.valueOf(frontStats.rateSnapshot().rateToTnd())) + " TND";
        String convertedBalanceText = formatMoney(frontStats.convertedBalance());
        String convertedGoalsText = formatMoney(frontStats.totalCurrent().multiply(BigDecimal.valueOf(frontStats.rateSnapshot().rateToTnd())));
        String holidayRiskText = String.valueOf(frontStats.nearHolidayGoals());

        if (rateApiLabel != null) {
            rateApiLabel.setText(rateText);
        }
        if (convertedBalanceLabel != null) {
            convertedBalanceLabel.setText(convertedBalanceText);
        }
        if (convertedGoalsLabel != null) {
            convertedGoalsLabel.setText(convertedGoalsText);
        }
        if (holidayRiskLabel != null) {
            holidayRiskLabel.setText(holidayRiskText);
        }
        if (accountRateApiLabel != null) {
            accountRateApiLabel.setText(rateText);
        }
        if (accountConvertedBalanceLabel != null) {
            accountConvertedBalanceLabel.setText(convertedBalanceText);
        }
        if (accountConvertedGoalsLabel != null) {
            accountConvertedGoalsLabel.setText(convertedGoalsText);
        }
        if (accountHolidayRiskLabel != null) {
            accountHolidayRiskLabel.setText(holidayRiskText);
        }
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
            addBodyCell(line, "--/--/----", 0, "table-cell");
            addBodyCell(line, "No data", 1, "table-cell");
            addBodyCell(line, "0 TND", 2, "amount-cell");
            addBodyCell(line, "No transaction matches the current filters.", 3, "table-cell");
            historyRowsBox.getChildren().add(line);
            return;
        }

        for (SavingsTransactionRepository.TransactionRow entry : page.items()) {
            GridPane line = new GridPane();
            line.setPadding(new Insets(12, 14, 12, 14));
            configureHistoryColumns(line);
            addBodyCell(line, entry.date().toLocalDate().toString(), 0, "table-cell");
            addBodyCell(line, friendlyTransactionType(entry.type()), 1, "table-cell");
            addBodyCell(line, formatMoney(entry.amount()), 2, "amount-cell");
            addBodyCell(line, safeText(entry.description()), 3, "table-cell");
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
            historyPaginationLabel.setText("Page " + (page.pageIndex() + 1) + " / " + page.pageCount());
        }
        if (historyTotalLabel != null) {
            historyTotalLabel.setText(page.totalItems() + (page.totalItems() == 1 ? " transaction au total" : " transactions au total"));
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
            goalsPaginationLabel.setText("Page " + (page.pageIndex() + 1) + " / " + page.pageCount());
        }
        if (goalsTotalLabel != null) {
            goalsTotalLabel.setText(page.totalItems() + (page.totalItems() == 1 ? " goal au total" : " goals au total"));
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
        c0.setPercentWidth(22);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(22);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(18);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(38);
        grid.getColumnConstraints().setAll(c0, c1, c2, c3);
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
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        value.setWrapText(true);
        card.getChildren().addAll(title, value);
        card.setMinWidth(170);
        card.setPrefWidth(180);
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
        GoalAnalyticsDTO initialAnalytics = controller.loadGoalsAnalytics(
                GoalsAnalyticsService.AnalyzeAttribute.PRIORITY.label(),
                goalsSearchValue(),
                goalsSortAttributeValue(),
                goalsSortDirectionValue()
        );
        showDynamicGoalsAnalyticsDashboard(initialAnalytics);
    }

    private void showDynamicGoalsAnalyticsDashboard(GoalAnalyticsDTO initialAnalytics) {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        Window owner = currentWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }

        Label headerLabel = new Label("Dynamic Goals Analytics Dashboard");
        headerLabel.getStyleClass().add("section-title");
        Label subtitleLabel = new Label("Smart statistics by selected attribute (API-based analytics over live MySQL data)");
        subtitleLabel.getStyleClass().add("section-subtitle");

        ComboBox<String> analyzeByCombo = new ComboBox<>();
        analyzeByCombo.getItems().setAll(controller.getGoalsAnalyzeByOptions());
        analyzeByCombo.getStyleClass().add("toolbar-combo");
        String initialAttribute = initialAnalytics == null ? GoalsAnalyticsService.AnalyzeAttribute.PRIORITY.label() : initialAnalytics.selectedAttribute();
        if (!analyzeByCombo.getItems().contains(initialAttribute)) {
            analyzeByCombo.getItems().add(0, initialAttribute);
        }
        analyzeByCombo.setValue(initialAttribute);

        Button refreshButton = actionButton("Refresh Analytics", "primary-btn");
        Button backToGoalsButton = actionButton("Back to Goals", "ghost-btn");
        HBox controls = new HBox(10, new Label("Analyze by"), analyzeByCombo, refreshButton, backToGoalsButton);
        controls.setAlignment(Pos.CENTER_LEFT);

        HBox kpiRow = new HBox(10);
        HBox chartsRow = new HBox(18);

        Label healthMessageLabel = new Label();
        healthMessageLabel.getStyleClass().add("mini-row-label");

        VBox riskRows = buildRiskRowsContainer();
        Label riskCountLabel = new Label("0 goals analyzed");
        riskCountLabel.getStyleClass().add("mini-row-label");

        TextField monthlyInput = new TextField("250");
        monthlyInput.getStyleClass().add("field");
        monthlyInput.setPrefWidth(140);
        monthlyInput.setPromptText("TND / month");
        attachMoneyValidation(monthlyInput, "Monthly contribution", 7);

        Button conservativeButton = actionButton("Conservative (100 TND)", "ghost-btn");
        Button balancedButton = actionButton("Balanced (250 TND)", "ghost-btn");
        Button aggressiveButton = actionButton("Aggressive (500 TND)", "ghost-btn");
        Button runWhatIfButton = actionButton("Run Simulation", "primary-btn");

        VBox scenarioRows = buildScenarioRowsContainer();
        Label whatIfHintLabel = new Label("Simulate how monthly contributions affect goal completion dates.");
        whatIfHintLabel.getStyleClass().add("mini-row-label");

        HBox scenarioActions = new HBox(10, new Label("Monthly contribution"), monthlyInput, conservativeButton, balancedButton, aggressiveButton, runWhatIfButton);
        scenarioActions.setAlignment(Pos.CENTER_LEFT);
        VBox whatIfCard = new VBox(10);
        whatIfCard.getStyleClass().add("glass-card");
        Label whatIfTitle = new Label("What-if Simulation");
        whatIfTitle.getStyleClass().add("card-title");
        whatIfCard.getChildren().addAll(whatIfTitle, whatIfHintLabel, scenarioActions, scenarioRows);

        VBox riskCard = new VBox(10);
        riskCard.getStyleClass().add("glass-card");
        Label riskTitle = new Label("Risk Analysis Table");
        riskTitle.getStyleClass().add("card-title");
        riskCard.getChildren().addAll(riskTitle, riskCountLabel, riskRows);

        VBox content = new VBox(16);
        content.getChildren().addAll(headerLabel, subtitleLabel, controls, kpiRow, healthMessageLabel, chartsRow, riskCard, whatIfCard);
        content.setPadding(new Insets(20));

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-scroll");

        GoalAnalyticsDTO[] analyticsState = new GoalAnalyticsDTO[]{initialAnalytics};

        Runnable renderDashboard = () -> {
            GoalAnalyticsDTO analytics = controller.loadGoalsAnalytics(
                    analyzeByCombo.getValue(),
                    goalsSearchValue(),
                    goalsSortAttributeValue(),
                    goalsSortDirectionValue()
            );
            analyticsState[0] = analytics;
            renderGoalAnalyticsDashboard(analytics, kpiRow, chartsRow, healthMessageLabel, riskRows, riskCountLabel);
        };

        java.util.function.BiConsumer<String, String> runScenario = (scenarioName, value) -> {
            monthlyInput.setText(value);
            WhatIfScenarioDTO scenario = controller.runWhatIfScenario(
                    analyticsState[0],
                    scenarioName,
                    parseMoneyValue(monthlyInput.getText())
            );
            renderScenarioRows(scenarioRows, scenario.projections());
        };

        analyzeByCombo.setOnAction(event -> renderDashboard.run());
        refreshButton.setOnAction(event -> renderDashboard.run());
        backToGoalsButton.setOnAction(event -> {
            if (tabPane != null) {
                tabPane.getSelectionModel().select(1);
            }
            stage.close();
        });
        conservativeButton.setOnAction(event -> runScenario.accept("Conservative", "100"));
        balancedButton.setOnAction(event -> runScenario.accept("Balanced", "250"));
        aggressiveButton.setOnAction(event -> runScenario.accept("Aggressive", "500"));
        runWhatIfButton.setOnAction(event -> {
            WhatIfScenarioDTO scenario = controller.runWhatIfScenario(
                    analyticsState[0],
                    "Custom",
                    parseMoneyValue(monthlyInput.getText())
            );
            renderScenarioRows(scenarioRows, scenario.projections());
        });

        renderDashboard.run();
        runScenario.accept("Balanced", monthlyInput.getText());

        Scene scene = new Scene(scrollPane, 1320, 900);
        applySceneTheme(scene);
        stage.setTitle("Goals Intelligence Dashboard");
        stage.setScene(scene);
        stage.show();
    }

    private void renderGoalAnalyticsDashboard(
            GoalAnalyticsDTO analytics,
            HBox kpiRow,
            HBox chartsRow,
            Label healthMessageLabel,
            VBox riskRows,
            Label riskCountLabel
    ) {
        if (analytics == null) {
            return;
        }

        kpiRow.getChildren().setAll(
                compactStatCard("Total Goals", statLabel(String.valueOf(analytics.totalGoals()))),
                compactStatCard("Completed Goals", statLabel(String.valueOf(analytics.completedGoals()))),
                compactStatCard("At Risk Goals", statLabel(String.valueOf(analytics.atRiskGoals()))),
                compactStatCard("Average Progress", statLabel(analytics.averageProgressPercentage().stripTrailingZeros().toPlainString() + "%")),
                compactStatCard("Financial Health Score", statLabel(analytics.financialHealthScore() + "/100")),
                compactStatCard("Need / Month", statLabel(formatMoney(analytics.requiredMonthlyContribution()))),
                compactStatCard(
                        "Nearest Deadline",
                        statLabel(analytics.nearestDeadline() == null ? "--/--/----" : analytics.nearestDeadline().toString())
                )
        );
        kpiRow.getStyleClass().add("stats-row");

        String healthText = "Financial health: " + analytics.financialHealthStatus()
                + " | Overdue goals: " + analytics.overdueGoals()
                + " | At risk goals: " + analytics.atRiskGoals();
        healthMessageLabel.setText(healthText);

        HBox dynamicCharts = buildDynamicAttributeCharts(analytics);
        chartsRow.getChildren().setAll(dynamicCharts.getChildren());
        renderRiskRows(riskRows, analytics.goalRisks());
        riskCountLabel.setText(analytics.goalRisks().size() + " goals analyzed");
    }

    private HBox buildDynamicAttributeCharts(GoalAnalyticsDTO analytics) {
        String selected = analytics.selectedAttribute() == null ? "" : analytics.selectedAttribute();
        javafx.scene.Node leftChart;
        javafx.scene.Node rightChart;
        String leftTitle;
        String rightTitle;

        if ("Deadline".equalsIgnoreCase(selected) || "Predicted Completion Date".equalsIgnoreCase(selected) || "Contribution Month".equalsIgnoreCase(selected)) {
            leftTitle = "Goals Timeline by " + selected;
            rightTitle = "Required Monthly Contribution by Goal";
            leftChart = createGoalCountLineChart(analytics.attributeStats());
            rightChart = createRequiredMonthlyByGoalChart(analytics.goalRisks());
        } else if ("Progress Percentage".equalsIgnoreCase(selected)) {
            leftTitle = "Progress Percentage by Goal";
            rightTitle = "Required Monthly Contribution by Goal";
            leftChart = createGoalProgressBarChart(analytics.goalRisks());
            rightChart = createRequiredMonthlyByGoalChart(analytics.goalRisks());
        } else if ("Risk Level".equalsIgnoreCase(selected)) {
            leftTitle = "Risk Distribution";
            rightTitle = "Required Monthly Contribution by Goal";
            leftChart = createCountPieChart(analytics.riskDistribution());
            rightChart = createRequiredMonthlyByGoalChart(analytics.goalRisks());
        } else {
            leftTitle = "Target / Current / Remaining by " + selected;
            rightTitle = "Required Monthly Contribution by Goal";
            leftChart = createAttributeTotalsBarChart(analytics.attributeStats());
            rightChart = createRequiredMonthlyByGoalChart(analytics.goalRisks());
        }

        HBox charts = new HBox(18,
                buildChartCard(leftTitle, leftChart),
                buildChartCard(rightTitle, rightChart)
        );
        HBox.setHgrow(charts.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(charts.getChildren().get(1), Priority.ALWAYS);
        return charts;
    }

    private VBox buildRiskRowsContainer() {
        VBox rows = new VBox(10);
        rows.getStyleClass().add("analytics-list");
        Label placeholder = new Label("No goals to analyze.");
        placeholder.getStyleClass().add("analytics-placeholder");
        rows.getChildren().add(placeholder);
        return rows;
    }

    private VBox buildScenarioRowsContainer() {
        VBox rows = new VBox(10);
        rows.getStyleClass().add("analytics-list");
        Label placeholder = new Label("No simulation result yet.");
        placeholder.getStyleClass().add("analytics-placeholder");
        rows.getChildren().add(placeholder);
        return rows;
    }

    private void renderRiskRows(VBox rows, List<GoalRiskDTO> risks) {
        rows.getChildren().clear();
        if (risks == null || risks.isEmpty()) {
            Label placeholder = new Label("No goals to analyze.");
            placeholder.getStyleClass().add("analytics-placeholder");
            rows.getChildren().add(placeholder);
            return;
        }
        for (GoalRiskDTO risk : risks) {
            rows.getChildren().add(buildRiskCard(risk));
        }
    }

    private void renderScenarioRows(VBox rows, List<WhatIfScenarioDTO.GoalProjectionDTO> projections) {
        rows.getChildren().clear();
        if (projections == null || projections.isEmpty()) {
            Label placeholder = new Label("No simulation result yet.");
            placeholder.getStyleClass().add("analytics-placeholder");
            rows.getChildren().add(placeholder);
            return;
        }
        for (WhatIfScenarioDTO.GoalProjectionDTO projection : projections) {
            rows.getChildren().add(buildScenarioCard(projection));
        }
    }

    private VBox buildRiskCard(GoalRiskDTO risk) {
        VBox card = new VBox(8);
        card.getStyleClass().add("risk-item-card");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(risk.goalName());
        title.getStyleClass().add("risk-item-title");
        Label priority = new Label("P" + risk.priority());
        priority.getStyleClass().add("risk-priority-pill");
        Label riskLevel = new Label(risk.riskLevel());
        riskLevel.getStyleClass().addAll("risk-level-pill", riskLevelClass(risk.riskLevel()));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(title, priority, spacer, riskLevel);

        HBox metrics = new HBox(8,
                metricChip("Target", formatMoney(risk.targetAmount())),
                metricChip("Current", formatMoney(risk.currentAmount())),
                metricChip("Remaining", formatMoney(risk.remainingAmount())),
                metricChip("Progress", risk.progressPercentage().stripTrailingZeros().toPlainString() + "%"),
                metricChip("Required / Month", formatMoney(risk.requiredMonthlyContribution()))
        );
        metrics.getStyleClass().add("analytics-chip-row");

        String deadline = risk.deadline() == null ? "--/--/----" : risk.deadline().toString();
        String predicted = risk.predictedCompletionDate() == null ? "No prediction" : risk.predictedCompletionDate().toString();
        String daysLeft = risk.daysLeft() >= 9999 ? "--" : String.valueOf(risk.daysLeft());
        Label footer = new Label("Deadline: " + deadline + "  |  Predicted: " + predicted + "  |  Days left: " + daysLeft + "  |  Status: " + risk.status());
        footer.getStyleClass().add("mini-row-label");
        footer.setWrapText(true);

        card.getChildren().addAll(top, metrics, footer);
        return card;
    }

    private VBox buildScenarioCard(WhatIfScenarioDTO.GoalProjectionDTO projection) {
        VBox card = new VBox(8);
        card.getStyleClass().add("scenario-item-card");

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(projection.goalName());
        title.getStyleClass().add("risk-item-title");
        Label verdict = new Label(projection.completesBeforeDeadline() ? "On time" : "At risk");
        verdict.getStyleClass().addAll("risk-level-pill", projection.completesBeforeDeadline() ? "risk-level-low" : "risk-level-critical");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        top.getChildren().addAll(title, spacer, verdict);

        HBox metrics = new HBox(8,
                metricChip("Monthly Allocation", formatMoney(projection.monthlyAllocation())),
                metricChip("Predicted Completion", projection.predictedCompletionDate() == null ? "No prediction" : projection.predictedCompletionDate().toString()),
                metricChip("Diff (days)", String.valueOf(projection.daysDifferenceFromDeadline()))
        );
        metrics.getStyleClass().add("analytics-chip-row");

        Label status = new Label(projection.statusMessage());
        status.getStyleClass().add("mini-row-label");
        status.setWrapText(true);

        card.getChildren().addAll(top, metrics, status);
        return card;
    }

    private VBox metricChip(String labelText, String valueText) {
        VBox chip = new VBox(2);
        chip.getStyleClass().add("analytics-chip");
        Label label = new Label(labelText);
        label.getStyleClass().add("analytics-chip-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("analytics-chip-value");
        chip.getChildren().addAll(label, value);
        return chip;
    }

    private String riskLevelClass(String riskLevel) {
        if (riskLevel == null) {
            return "risk-level-low";
        }
        return switch (riskLevel.toLowerCase(Locale.ROOT)) {
            case "critical" -> "risk-level-critical";
            case "high" -> "risk-level-high";
            case "medium" -> "risk-level-medium";
            default -> "risk-level-low";
        };
    }

    private BarChart<String, Number> createAttributeTotalsBarChart(Map<String, AttributeStatsDTO> attributeStats) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setCategoryGap(12);
        chart.setBarGap(4);
        chart.setPrefHeight(320);

        XYChart.Series<String, Number> targetSeries = new XYChart.Series<>();
        targetSeries.setName("Target");
        XYChart.Series<String, Number> currentSeries = new XYChart.Series<>();
        currentSeries.setName("Current");
        XYChart.Series<String, Number> remainingSeries = new XYChart.Series<>();
        remainingSeries.setName("Remaining");

        if (attributeStats == null || attributeStats.isEmpty()) {
            targetSeries.getData().add(new XYChart.Data<>("No data", 0));
        } else {
            attributeStats.forEach((key, stats) -> {
                String label = truncate(key, 16);
                targetSeries.getData().add(new XYChart.Data<>(label, stats.totalTargetAmount().doubleValue()));
                currentSeries.getData().add(new XYChart.Data<>(label, stats.totalCurrentAmount().doubleValue()));
                remainingSeries.getData().add(new XYChart.Data<>(label, stats.totalRemainingAmount().doubleValue()));
            });
        }

        chart.getData().setAll(targetSeries, currentSeries, remainingSeries);
        return chart;
    }

    private BarChart<String, Number> createRequiredMonthlyBarChart(Map<String, AttributeStatsDTO> attributeStats) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(320);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (attributeStats == null || attributeStats.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No data", 0));
        } else {
            attributeStats.forEach((key, stats) -> series.getData().add(
                    new XYChart.Data<>(truncate(key, 16), stats.averageRequiredMonthlyContribution().doubleValue())
            ));
        }
        chart.getData().setAll(series);
        return chart;
    }

    private LineChart<String, Number> createGoalCountLineChart(Map<String, AttributeStatsDTO> attributeStats) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(320);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (attributeStats == null || attributeStats.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No data", 0));
        } else {
            attributeStats.forEach((key, stats) -> series.getData().add(
                    new XYChart.Data<>(truncate(key, 16), stats.goalCount())
            ));
        }
        chart.getData().setAll(series);
        return chart;
    }

    private BarChart<String, Number> createGoalProgressBarChart(List<GoalRiskDTO> goals) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(320);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (goals == null || goals.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No data", 0));
        } else {
            for (GoalRiskDTO goal : goals) {
                series.getData().add(new XYChart.Data<>(truncate(goal.goalName(), 14), goal.progressPercentage().doubleValue()));
            }
        }
        chart.getData().setAll(series);
        return chart;
    }

    private BarChart<String, Number> createRequiredMonthlyByGoalChart(List<GoalRiskDTO> goals) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(320);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (goals == null || goals.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No data", 0));
        } else {
            goals.stream()
                    .sorted(Comparator.comparing(GoalRiskDTO::requiredMonthlyContribution).reversed())
                    .forEach(goal -> series.getData().add(
                            new XYChart.Data<>(
                                    truncate(goal.goalName(), 14),
                                    goal.requiredMonthlyContribution().doubleValue()
                            )
                    ));
        }
        chart.getData().setAll(series);
        return chart;
    }

    private PieChart createCountPieChart(Map<String, Integer> distribution) {
        List<PieChart.Data> items = new ArrayList<>();
        if (distribution == null || distribution.isEmpty()) {
            items.add(new PieChart.Data("No data", 1));
        } else {
            distribution.forEach((key, value) -> {
                if (value != null && value > 0) {
                    items.add(new PieChart.Data(key, value));
                }
            });
            if (items.isEmpty()) {
                items.add(new PieChart.Data("No data", 1));
            }
        }
        PieChart chart = new PieChart(FXCollections.observableArrayList(items));
        chart.setPrefHeight(320);
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        return chart;
    }

    private PieChart createAttributeRiskPieChart(
            Map<String, AttributeStatsDTO> attributeStats,
            Map<String, Integer> fallbackRiskDistribution
    ) {
        List<PieChart.Data> items = new ArrayList<>();
        if (attributeStats != null && !attributeStats.isEmpty()) {
            attributeStats.forEach((key, value) -> {
                int weightedRisk = value.highRiskCount() * 3 + value.mediumRiskCount() * 2 + value.lowRiskCount();
                if (weightedRisk > 0) {
                    items.add(new PieChart.Data(truncate(key, 16), weightedRisk));
                }
            });
        }
        if (items.isEmpty()) {
            return createCountPieChart(fallbackRiskDistribution);
        }

        PieChart chart = new PieChart(FXCollections.observableArrayList(items));
        chart.setPrefHeight(320);
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        return chart;
    }

    private BigDecimal parseMoneyValue(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim().replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private void adjustTableHeight(TableView<?> table, int rowCount, double minHeight, double maxHeight) {
        if (table == null) {
            return;
        }
        double fixedCellSize = table.getFixedCellSize() > 0 ? table.getFixedCellSize() : 34;
        double headerHeight = 34;
        double computed = headerHeight + (Math.max(1, rowCount) * fixedCellSize) + 4;
        double bounded = Math.max(minHeight, Math.min(maxHeight, computed));
        table.setPrefHeight(bounded);
        table.setMinHeight(Math.min(minHeight, bounded));
        table.setMaxHeight(maxHeight);
    }

    private void showAdvancedFrontStats() {
        if (frontStats == null) {
            showError("Les statistiques enrichies ne sont pas disponibles.");
            return;
        }

        VBox content = new VBox(18);
        HBox cards = new HBox(10,
                compactStatCard("Total goals", statLabel(String.valueOf(frontStats.totalGoals()))),
                compactStatCard("Completed", statLabel(String.valueOf(frontStats.completedGoals()))),
                compactStatCard("Late", statLabel(String.valueOf(frontStats.lateGoals()))),
                compactStatCard("Near holiday", statLabel(String.valueOf(frontStats.nearHolidayGoals())))
        );

        HBox charts = new HBox(18,
                buildChartCard("Savings vs Goal Totals", createAdvancedAmountsChart(frontStats)),
                buildChartCard("Exchange Rate Trend", createRateTrendChart(frontStats.historicalRates()))
        );
        HBox.setHgrow(charts.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(charts.getChildren().get(1), Priority.ALWAYS);

        VBox details = buildAttributeSummaryCard(
                "API enriched insights",
                List.of(
                        "Urgent goal: " + frontStats.urgentGoal(),
                        "Best progress: " + frontStats.bestProgressGoal(),
                        "Forecast: " + frontStats.forecast(),
                        "Rate date: " + frontStats.rateSnapshot().rateDate(),
                        "Provider: " + frontStats.rateSnapshot().provider()
                )
        );
        content.getChildren().addAll(cards, charts, details);
        showStatsWindow(
                "Advanced Savings Stats",
                "Advanced statistics",
                "MySQL data enriched with Currency API and Nager.Date.",
                content
        );
    }

    private void showCalendarWindow() {
        Stage stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        Window owner = currentWindow();
        if (owner != null) {
            stage.initOwner(owner);
        }

        YearMonth[] visibleMonth = new YearMonth[]{YearMonth.now()};
        LocalDate[] selectedDate = new LocalDate[]{null};
        SavingsUiController.CalendarViewData[] dataState = new SavingsUiController.CalendarViewData[1];
        boolean[] detailsVisible = new boolean[]{true};

        Label title = new Label("Savings Calendar");
        title.getStyleClass().add("section-title");
        title.setStyle("-fx-font-size:34px;");
        Label subtitle = new Label("Holidays, goals, contributions and exchange rates");
        subtitle.getStyleClass().add("section-subtitle");
        subtitle.setWrapText(true);

        ComboBox<String> baseCurrencyBox = new ComboBox<>();
        baseCurrencyBox.getItems().setAll(controller.getSupportedCurrencies());
        baseCurrencyBox.setValue("EUR");
        baseCurrencyBox.getStyleClass().add("toolbar-combo");

        ComboBox<String> targetCurrencyBox = new ComboBox<>();
        targetCurrencyBox.getItems().setAll(controller.getSupportedCurrencies());
        targetCurrencyBox.setValue("TND");
        targetCurrencyBox.getStyleClass().add("toolbar-combo");

        ComboBox<String> monthSelector = new ComboBox<>();
        monthSelector.getItems().setAll(List.of(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        ));
        monthSelector.setValue(monthDisplayName(visibleMonth[0].getMonth()));
        monthSelector.getStyleClass().add("toolbar-combo");

        ComboBox<Integer> yearSelector = new ComboBox<>();
        List<Integer> years = new ArrayList<>();
        for (int year = 2020; year <= 2040; year++) {
            years.add(year);
        }
        yearSelector.getItems().setAll(years);
        yearSelector.setValue(visibleMonth[0].getYear());
        yearSelector.getStyleClass().add("toolbar-combo");

        ToggleButton showHolidays = new ToggleButton("Holidays");
        ToggleButton showDeadlines = new ToggleButton("Goals");
        ToggleButton showContributions = new ToggleButton("Contributions");
        ToggleButton showRates = new ToggleButton("Currency");
        showHolidays.getStyleClass().add("chip");
        showDeadlines.getStyleClass().add("chip");
        showContributions.getStyleClass().add("chip");
        showRates.getStyleClass().add("chip");
        showHolidays.setSelected(true);
        showDeadlines.setSelected(true);
        showContributions.setSelected(true);
        showRates.setSelected(true);

        Label monthTitleLabel = new Label();
        monthTitleLabel.getStyleClass().add("card-title");
        monthTitleLabel.setStyle("-fx-font-size:20px;");

        Button backButton = actionButton("\u2190 Back to Savings & Goals", "ghost-btn");
        backButton.setOnAction(event -> stage.close());
        Button todayButton = actionButton("Today", "ghost-btn");
        Button previous = actionButton("\u2190 Previous", "ghost-btn");
        Button next = actionButton("Next \u2192", "ghost-btn");
        Button goButton = actionButton("Go", "primary-btn");
        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topRow = new HBox(8, topSpacer, backButton);
        topRow.setAlignment(Pos.CENTER_RIGHT);
        HBox titleRow = new HBox(title);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox subtitleRow = new HBox(subtitle);
        subtitleRow.setAlignment(Pos.CENTER_LEFT);

        HBox centeredMonthRow = new HBox(monthTitleLabel);
        centeredMonthRow.setAlignment(Pos.CENTER);

        HBox navRow = new HBox(8, previous, todayButton, next);
        navRow.setAlignment(Pos.CENTER);

        Label jumpLabel = new Label("Jump to:");
        jumpLabel.getStyleClass().add("mini-row-label");
        HBox jumpRow = new HBox(8, jumpLabel, monthSelector, yearSelector, goButton);
        jumpRow.setAlignment(Pos.CENTER_LEFT);

        HBox currencyRow = new HBox(10,
                new Label("Base currency"), baseCurrencyBox,
                new Label("Target currency"), targetCurrencyBox
        );
        currencyRow.setAlignment(Pos.CENTER_LEFT);

        Button refreshButton = actionButton("Refresh Data", "primary-btn");
        currencyRow.getChildren().add(refreshButton);

        Label refreshTimeLabel = new Label("Last refresh: --");
        Label updateMessageLabel = new Label("Calendar updated successfully");
        refreshTimeLabel.getStyleClass().add("mini-row-label");
        updateMessageLabel.getStyleClass().add("mini-row-label");

        HBox legendRow = new HBox(
                8,
                buildLegendItem("#2563eb", "Public Holiday"),
                buildLegendItem("#7c3aed", "Goal Deadline"),
                buildLegendItem("#16a34a", "Contribution / Savings Event"),
                buildLegendItem("#f97316", "Exchange Rate")
        );
        legendRow.setAlignment(Pos.CENTER_LEFT);

        HBox filtersRow = new HBox(10, new Label("Filters:"), showHolidays, showDeadlines, showContributions, showRates);
        filtersRow.setAlignment(Pos.CENTER_LEFT);
        HBox infoRow = new HBox(14, filtersRow, new Region(), updateMessageLabel, refreshTimeLabel);
        HBox.setHgrow(infoRow.getChildren().get(1), Priority.ALWAYS);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        VBox calendarContainer = new VBox(10);
        VBox agendaContainer = new VBox(10);
        VBox detailsPanel = new VBox(10);
        detailsPanel.getStyleClass().add("glass-card");
        detailsPanel.setPrefWidth(360);
        detailsPanel.setMinWidth(340);
        detailsPanel.setMaxWidth(380);

        TabPane viewTabs = new TabPane();
        viewTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab monthTab = new Tab("Month View", calendarContainer);
        Tab agendaTab = new Tab("Agenda View", agendaContainer);
        viewTabs.getTabs().addAll(monthTab, agendaTab);

        Button showDetailsButton = actionButton("Show Details", "ghost-btn");
        showDetailsButton.setManaged(false);
        showDetailsButton.setVisible(false);
        showDetailsButton.setOnAction(event -> {
            detailsVisible[0] = true;
            detailsPanel.setManaged(true);
            detailsPanel.setVisible(true);
            showDetailsButton.setManaged(false);
            showDetailsButton.setVisible(false);
            renderCurrentCalendarPanel(calendarContainer, detailsPanel, visibleMonth[0], selectedDate[0], dataState[0],
                    currentCalendarFilters(showHolidays, showDeadlines, showContributions, showRates),
                    selected -> selectedDate[0] = selected,
                    baseCurrencyBox.getValue(),
                    targetCurrencyBox.getValue(),
                    true,
                    () -> {
                        detailsVisible[0] = false;
                        detailsPanel.setManaged(false);
                        detailsPanel.setVisible(false);
                        showDetailsButton.setManaged(true);
                        showDetailsButton.setVisible(true);
                    });
        });

        VBox centerPane = new VBox(6, showDetailsButton, viewTabs);
        HBox mainContent = new HBox(10, centerPane, detailsPanel);
        HBox.setHgrow(calendarContainer, Priority.ALWAYS);
        HBox.setHgrow(viewTabs, Priority.ALWAYS);
        HBox.setHgrow(centerPane, Priority.ALWAYS);
        HBox.setHgrow(detailsPanel, Priority.NEVER);

        VBox content = new VBox(6,
                topRow,
                titleRow,
                subtitleRow,
                centeredMonthRow,
                navRow,
                jumpRow,
                currencyRow,
                legendRow,
                infoRow,
                mainContent
        );
        content.setPadding(new Insets(10));
        content.getStyleClass().add("glass-card");

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.getStyleClass().add("page-scroll");

        Runnable renderCurrent = () -> renderCurrentCalendarPanel(
                calendarContainer,
                detailsPanel,
                visibleMonth[0],
                selectedDate[0],
                dataState[0],
                new CalendarFilters(
                        showHolidays.isSelected(),
                        showDeadlines.isSelected(),
                        showContributions.isSelected(),
                        showRates.isSelected()
                ),
                selected -> selectedDate[0] = selected,
                baseCurrencyBox.getValue(),
                targetCurrencyBox.getValue(),
                detailsVisible[0],
                () -> {
                    detailsVisible[0] = false;
                    detailsPanel.setManaged(false);
                    detailsPanel.setVisible(false);
                    showDetailsButton.setManaged(true);
                    showDetailsButton.setVisible(true);
                }
        );

        Runnable reloadData = () -> {
            try {
                dataState[0] = controller.loadCalendarViewData(
                        visibleMonth[0],
                        baseCurrencyBox.getValue(),
                        targetCurrencyBox.getValue()
                );
                boolean hasError = (dataState[0].holidayStatus() != null && !dataState[0].holidayStatus().connected())
                        || (dataState[0].currencyStatus() != null && !dataState[0].currencyStatus().connected())
                        || (dataState[0].mysqlStatus() != null && !dataState[0].mysqlStatus().connected());
                updateMessageLabel.setText(
                        hasError
                                ? "Some calendar data could not be updated. Please try again later."
                                : "Calendar updated successfully"
                );
                refreshTimeLabel.setText("Last refresh: " + formatCalendarTimestamp(dataState[0].refreshedAt()));
                monthTitleLabel.setText(formatMonthYear(visibleMonth[0]));
                monthSelector.setValue(monthDisplayName(visibleMonth[0].getMonth()));
                yearSelector.setValue(visibleMonth[0].getYear());
                if (selectedDate[0] == null || !YearMonth.from(selectedDate[0]).equals(visibleMonth[0])) {
                    selectedDate[0] = visibleMonth[0].atDay(1);
                }
                renderCurrent.run();
                renderAgendaView(
                        agendaContainer,
                        visibleMonth[0],
                        dataState[0],
                        new CalendarFilters(
                                showHolidays.isSelected(),
                                showDeadlines.isSelected(),
                                showContributions.isSelected(),
                                showRates.isSelected()
                        )
                );
            } catch (RuntimeException exception) {
                updateMessageLabel.setText("Some calendar data could not be updated. Please try again later.");
                refreshTimeLabel.setText("Last refresh: failed");
                calendarContainer.getChildren().setAll(new Label("Unable to load calendar data. Please check your connection."));
                detailsPanel.getChildren().setAll(new Label("No day details available."));
                agendaContainer.getChildren().setAll(new Label("No agenda data available."));
            }
        };

        todayButton.setOnAction(event -> {
            visibleMonth[0] = YearMonth.now();
            selectedDate[0] = LocalDate.now();
            reloadData.run();
        });
        previous.setOnAction(event -> {
            visibleMonth[0] = visibleMonth[0].minusMonths(1);
            reloadData.run();
        });
        next.setOnAction(event -> {
            visibleMonth[0] = visibleMonth[0].plusMonths(1);
            reloadData.run();
        });
        goButton.setOnAction(event -> {
            String monthValue = monthSelector.getValue();
            Integer yearValue = yearSelector.getValue();
            if (monthValue != null && yearValue != null) {
                visibleMonth[0] = YearMonth.of(yearValue, monthFromDisplayName(monthValue));
                selectedDate[0] = visibleMonth[0].atDay(1);
                reloadData.run();
            }
        });
        baseCurrencyBox.setOnAction(event -> reloadData.run());
        targetCurrencyBox.setOnAction(event -> reloadData.run());
        showHolidays.setOnAction(event -> {
            renderCurrent.run();
            renderAgendaView(agendaContainer, visibleMonth[0], dataState[0], currentCalendarFilters(showHolidays, showDeadlines, showContributions, showRates));
        });
        showDeadlines.setOnAction(event -> {
            renderCurrent.run();
            renderAgendaView(agendaContainer, visibleMonth[0], dataState[0], currentCalendarFilters(showHolidays, showDeadlines, showContributions, showRates));
        });
        showContributions.setOnAction(event -> {
            renderCurrent.run();
            renderAgendaView(agendaContainer, visibleMonth[0], dataState[0], currentCalendarFilters(showHolidays, showDeadlines, showContributions, showRates));
        });
        showRates.setOnAction(event -> {
            renderCurrent.run();
            renderAgendaView(agendaContainer, visibleMonth[0], dataState[0], currentCalendarFilters(showHolidays, showDeadlines, showContributions, showRates));
        });
        refreshButton.setOnAction(event -> {
            controller.refreshCalendarApiCaches();
            reloadData.run();
        });

        reloadData.run();
        Scene scene = new Scene(scrollPane, 1220, 780);
        applySceneTheme(scene);
        stage.setTitle("Savings Calendar");
        stage.setScene(scene);
        stage.show();
    }

    private CalendarFilters currentCalendarFilters(ToggleButton holidays, ToggleButton deadlines, ToggleButton contributions, ToggleButton rates) {
        return new CalendarFilters(
                holidays.isSelected(),
                deadlines.isSelected(),
                contributions.isSelected(),
                rates.isSelected()
        );
    }

    private void renderCurrentCalendarPanel(
            VBox calendarContainer,
            VBox detailsPanel,
            YearMonth month,
            LocalDate selectedDate,
            SavingsUiController.CalendarViewData data,
            CalendarFilters filters,
            java.util.function.Consumer<LocalDate> selectionHandler,
            String baseCurrency,
            String targetCurrency,
            boolean detailsVisible,
            Runnable onHideDetails
    ) {
        renderCalendarUi(
                calendarContainer,
                detailsPanel,
                month,
                selectedDate,
                data,
                filters,
                selectionHandler,
                baseCurrency,
                targetCurrency,
                detailsVisible,
                onHideDetails
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
        SavingsUiController.OperationResult result = controller.safeGenerateSmartPdfReport(
                goalsSearchValue(),
                goalsSortAttributeValue(),
                goalsSortDirectionValue(),
                java.nio.file.Paths.get("target", "exports")
        );
        if (result.success()) {
            showSuccess(result.message());
            showModal("Smart PDF Report API", "Smart PDF report generated", result.message(), Alert.AlertType.INFORMATION);
        } else {
            showError(result.message());
            showModal("Smart PDF Report API", "PDF generation failed", result.message(), Alert.AlertType.ERROR);
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
        applySceneTheme(scene);

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
                compactStatCard("Transactions", statLabel(String.valueOf(stats.transactionCount()))),
                compactStatCard("Deposits", statLabel(formatMoney(stats.totalDeposited()))),
                compactStatCard("Contributions", statLabel(formatMoney(stats.totalContributedToGoals()))),
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
                        "Descriptions included: " + summarizeTransactionDescriptions(transactions),
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

    private HBox buildLegendItem(String colorHex, String meaning) {
        Label dot = new Label("\u25CF");
        dot.setStyle("-fx-text-fill:" + colorHex + "; -fx-font-size:14px;");
        Label label = new Label(meaning);
        label.getStyleClass().add("mini-row-label");
        HBox row = new HBox(8, dot, label);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void renderCalendarUi(
            VBox calendarContainer,
            VBox detailsPanel,
            YearMonth month,
            LocalDate selectedDate,
            SavingsUiController.CalendarViewData data,
            CalendarFilters filters,
            java.util.function.Consumer<LocalDate> selectionHandler,
            String baseCurrency,
            String targetCurrency,
            boolean detailsVisible,
            Runnable onHideDetails
    ) {
        calendarContainer.getChildren().clear();
        if (data == null) {
            calendarContainer.getChildren().add(new Label("No calendar data available."));
            detailsPanel.getChildren().setAll(new Label("No day details available."));
            return;
        }

        List<CalendarEventDTO> filteredEvents = data.events().stream()
                .filter(event -> isVisibleByFilter(event, filters))
                .toList();
        Map<LocalDate, List<CalendarEventDTO>> eventsByDate = filteredEvents.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        CalendarEventDTO::date,
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.toList()
                ));

        GridPane calendarGrid = new GridPane();
        calendarGrid.setHgap(10);
        calendarGrid.setVgap(10);

        List<String> headers = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        for (int index = 0; index < headers.size(); index++) {
            Label header = new Label(headers.get(index));
            header.getStyleClass().add("table-head");
            calendarGrid.add(header, index, 0);
        }

        LocalDate firstDay = month.atDay(1);
        int offset = firstDay.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        int totalDays = month.lengthOfMonth();
        int totalCells = offset + totalDays;
        int totalRows = (int) Math.ceil(totalCells / 7.0);
        LocalDate today = LocalDate.now();

        for (int day = 1; day <= totalDays; day++) {
            LocalDate date = month.atDay(day);
            int index = offset + day - 1;
            int row = (index / 7) + 1;
            int column = index % 7;

            VBox dayCard = new VBox(6);
            dayCard.getStyleClass().add("goal-card");
            dayCard.setPrefWidth(138);
            dayCard.setMinHeight(95);
            dayCard.setMaxHeight(95);
            dayCard.setPadding(new Insets(6));

            String borderColor = "#dbe4ee";
            if (date.equals(today)) {
                borderColor = "#00bfe5";
            }
            if (date.equals(selectedDate)) {
                borderColor = "#0ea5e9";
            }
            dayCard.setStyle("-fx-border-color:" + borderColor + ";-fx-border-width:2;-fx-border-radius:18;-fx-background-radius:18;");

            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.getStyleClass().add("goal-title");
            dayCard.getChildren().add(dayLabel);

            List<CalendarEventDTO> dayEvents = eventsByDate.getOrDefault(date, List.of());
            List<CalendarEventDTO> visibleDayEvents = dayEvents.stream()
                    .filter(event -> !"CURRENCY_RATE".equals(event.type()))
                    .toList();
            int maxLines = 3;
            for (int i = 0; i < Math.min(maxLines, visibleDayEvents.size()); i++) {
                CalendarEventDTO event = visibleDayEvents.get(i);
                Label eventLabel = new Label(shortEventLabel(event));
                eventLabel.getStyleClass().add("mini-row-label");
                eventLabel.setWrapText(true);
                eventLabel.setStyle(colorStyle(event.type()));
                Tooltip.install(eventLabel, buildCalendarEventTooltip(event));
                dayCard.getChildren().add(eventLabel);
            }
            if (visibleDayEvents.size() > maxLines) {
                Label moreLabel = new Label("+" + (visibleDayEvents.size() - maxLines) + " more");
                moreLabel.getStyleClass().add("mini-row-label");
                dayCard.getChildren().add(moreLabel);
            }

            dayCard.setOnMouseClicked(event -> {
                selectionHandler.accept(date);
                renderCalendarUi(
                        calendarContainer,
                        detailsPanel,
                        month,
                        date,
                        data,
                        filters,
                        selectionHandler,
                        baseCurrency,
                        targetCurrency,
                        detailsVisible,
                        onHideDetails
                );
            });
            calendarGrid.add(dayCard, column, row);
        }

        // Ensure months with 6 weeks stay visible and not cut.
        for (int rowIndex = 1; rowIndex <= Math.max(6, totalRows); rowIndex++) {
            if (calendarGrid.getRowConstraints().size() <= rowIndex) {
                javafx.scene.layout.RowConstraints constraints = new javafx.scene.layout.RowConstraints();
                constraints.setMinHeight(86);
                constraints.setPrefHeight(86);
                constraints.setVgrow(Priority.NEVER);
                calendarGrid.getRowConstraints().add(constraints);
            }
        }

        calendarContainer.getChildren().add(calendarGrid);
        if (detailsVisible) {
            renderSelectedDayDetails(
                    detailsPanel,
                    selectedDate,
                    selectedDate == null ? List.of() : eventsByDate.getOrDefault(selectedDate, List.of()),
                    data.dailyRates(),
                    filters,
                    baseCurrency,
                    targetCurrency,
                    onHideDetails
            );
        }
    }

    private void renderSelectedDayDetails(
            VBox detailsPanel,
            LocalDate selectedDate,
            List<CalendarEventDTO> selectedDayEvents,
            Map<LocalDate, Double> dailyRates,
            CalendarFilters filters,
            String baseCurrency,
            String targetCurrency,
            Runnable onHideDetails
    ) {
        detailsPanel.getChildren().clear();
        BorderPane header = new BorderPane();
        Label detailsTitle = new Label("Selected Day Details");
        detailsTitle.getStyleClass().add("card-title");
        detailsTitle.setWrapText(false);
        Button hideButton = actionButton("Hide", "ghost-btn");
        hideButton.setMinWidth(90);
        hideButton.setPrefWidth(90);
        hideButton.setMaxWidth(90);
        hideButton.setOnAction(event -> onHideDetails.run());
        header.setLeft(detailsTitle);
        header.setRight(hideButton);
        BorderPane.setAlignment(detailsTitle, Pos.CENTER_LEFT);
        BorderPane.setAlignment(hideButton, Pos.CENTER_RIGHT);

        if (selectedDate == null) {
            Label hint = new Label("Select a day to view details.");
            hint.getStyleClass().add("mini-row-label");
            detailsPanel.getChildren().addAll(header, hint);
            return;
        }
        Label dateLabel = new Label("Selected date: " + selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)));
        dateLabel.getStyleClass().add("mini-row-label");

        List<CalendarEventDTO> holidays = selectedDayEvents.stream()
                .filter(event -> "PUBLIC_HOLIDAY".equals(event.type()))
                .toList();
        List<CalendarEventDTO> goals = selectedDayEvents.stream()
                .filter(event -> "GOAL_DEADLINE".equals(event.type()))
                .toList();
        List<CalendarEventDTO> contributions = selectedDayEvents.stream()
                .filter(event -> "GOAL_CONTRIBUTION".equals(event.type()) || "SAVINGS_EVENT".equals(event.type()))
                .toList();

        VBox blocks = new VBox(8);
        blocks.getChildren().add(buildDetailsBlock(
                "Holiday",
                filters.showPublicHolidays()
                        ? (holidays.isEmpty() ? List.of("No holiday")
                        : holidays.stream().map(CalendarEventDTO::title).toList())
                        : List.of("Filtered out")
        ));
        blocks.getChildren().add(buildDetailsBlock(
                "Goals",
                filters.showGoalDeadlines()
                        ? (goals.isEmpty() ? List.of("No goals")
                        : goals.stream().map(this::goalSummaryLine).toList())
                        : List.of("Filtered out")
        ));
        blocks.getChildren().add(buildDetailsBlock(
                "Contributions",
                filters.showContributions()
                        ? (contributions.isEmpty() ? List.of("No contributions")
                        : contributions.stream().map(this::contributionDetailsLine).toList())
                        : List.of("Filtered out")
        ));

        List<String> currencyLines;
        if (!filters.showCurrencyRates()) {
            currencyLines = List.of("Filtered out");
        } else if (dailyRates == null || !dailyRates.containsKey(selectedDate)) {
            currencyLines = List.of("Exchange rate unavailable for this date");
        } else {
            currencyLines = List.of(
                    "1 " + baseCurrency + " = " + String.format(Locale.US, "%.4f", dailyRates.get(selectedDate)) + " " + targetCurrency
            );
        }
        blocks.getChildren().add(buildDetailsBlock("Exchange Rate", currencyLines));

        detailsPanel.getChildren().addAll(header, dateLabel, blocks);
    }

    private void renderAgendaView(
            VBox agendaContainer,
            YearMonth month,
            SavingsUiController.CalendarViewData data,
            CalendarFilters filters
    ) {
        agendaContainer.getChildren().clear();
        if (data == null) {
            agendaContainer.getChildren().add(new Label("No agenda data available."));
            return;
        }

        Label agendaTitle = new Label("Agenda - " + month.getMonth() + " " + month.getYear());
        agendaTitle.getStyleClass().add("card-title");

        TableView<CalendarEventDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(620);

        TableColumn<CalendarEventDTO, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().date() == null ? "-" : cell.getValue().date().toString()
        ));
        TableColumn<CalendarEventDTO, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(humanType(cell.getValue().type())));
        TableColumn<CalendarEventDTO, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().title()));
        TableColumn<CalendarEventDTO, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().amount() == null ? "-" : cell.getValue().amount().toPlainString()
        ));
        table.getColumns().setAll(dateCol, typeCol, titleCol, amountCol);

        List<CalendarEventDTO> rows = data.events().stream()
                .filter(event -> YearMonth.from(event.date()).equals(month))
                .filter(event -> isVisibleByFilter(event, filters))
                .sorted(Comparator.comparing(CalendarEventDTO::date).thenComparing(CalendarEventDTO::type))
                .toList();
        table.setItems(FXCollections.observableArrayList(rows));

        agendaContainer.getChildren().addAll(agendaTitle, table);
    }

    private VBox buildDetailsBlock(String title, List<String> lines) {
        VBox block = new VBox(4);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("table-head");
        block.getChildren().add(titleLabel);
        for (String line : lines) {
            Label lineLabel = new Label(line);
            lineLabel.getStyleClass().add("mini-row-label");
            lineLabel.setWrapText(true);
            block.getChildren().add(lineLabel);
        }
        return block;
    }

    private String contributionDetailsLine(CalendarEventDTO event) {
        String amount = event.amount() == null ? "-" : event.amount().toPlainString() + " TND";
        String goalName = event.goalName() == null || event.goalName().isBlank() ? "N/A" : event.goalName();
        return "+" + amount + " \u2192 " + goalName;
    }

    private String goalSummaryLine(CalendarEventDTO event) {
        String goalName = event.goalName() == null || event.goalName().isBlank()
                ? event.title().replace("Goal: ", "")
                : event.goalName();
        String description = event.description() == null ? "" : event.description();
        String targetText = extractSegment(description, "Target:");
        String progressText = extractSegment(description, "Progress:");
        if (!targetText.isBlank() || !progressText.isBlank()) {
            return goalName + " | " + targetText + " | " + progressText;
        }
        return goalName;
    }

    private String extractSegment(String source, String label) {
        if (source == null || source.isBlank()) {
            return "";
        }
        int start = source.indexOf(label);
        if (start < 0) {
            return "";
        }
        int end = source.indexOf('|', start + label.length());
        String segment = (end < 0 ? source.substring(start) : source.substring(start, end)).trim();
        return segment;
    }

    private boolean isVisibleByFilter(CalendarEventDTO event, CalendarFilters filters) {
        return switch (event.type()) {
            case "PUBLIC_HOLIDAY" -> filters.showPublicHolidays();
            case "GOAL_DEADLINE" -> filters.showGoalDeadlines();
            case "GOAL_CONTRIBUTION", "SAVINGS_EVENT" -> filters.showContributions();
            case "CURRENCY_RATE" -> filters.showCurrencyRates();
            default -> true;
        };
    }

    private String shortEventLabel(CalendarEventDTO event) {
        return switch (event.type()) {
            case "PUBLIC_HOLIDAY" -> truncate("Holiday", 18);
            case "GOAL_DEADLINE" -> truncate("Goal: " + (event.goalName() == null ? event.title().replace("Goal: ", "") : event.goalName()), 18);
            case "GOAL_CONTRIBUTION" -> {
                String amount = event.amount() == null ? "" : "+" + event.amount().toPlainString() + " TND";
                yield truncate(amount, 18);
            }
            case "SAVINGS_EVENT" -> truncate("Savings", 18);
            case "CURRENCY_RATE" -> "";
            default -> truncate(event.title(), 18);
        };
    }

    private Tooltip buildCalendarEventTooltip(CalendarEventDTO event) {
        String amount = event.amount() == null ? "-" : event.amount().toPlainString();
        String goal = event.goalName() == null || event.goalName().isBlank() ? "-" : event.goalName();
        String description = event.description() == null || event.description().isBlank() ? "-" : event.description();
        String text = "Type: " + humanType(event.type()) + "\n"
                + "Title: " + event.title() + "\n"
                + "Goal: " + goal + "\n"
                + "Amount: " + amount + "\n"
                + "Date: " + event.date() + "\n"
                + "Description: " + description;
        return new Tooltip(text);
    }

    private String humanType(String type) {
        return switch (type) {
            case "PUBLIC_HOLIDAY" -> "Public Holiday";
            case "GOAL_DEADLINE" -> "Goal Deadline";
            case "GOAL_CONTRIBUTION" -> "Contribution";
            case "SAVINGS_EVENT" -> "Savings Event";
            case "CURRENCY_RATE" -> "Currency Rate";
            default -> "Event";
        };
    }

    private String colorStyle(String type) {
        return switch (type) {
            case "PUBLIC_HOLIDAY" -> "-fx-text-fill:#2563eb;-fx-font-weight:bold;";
            case "GOAL_DEADLINE" -> "-fx-text-fill:#7c3aed;-fx-font-weight:bold;";
            case "GOAL_CONTRIBUTION", "SAVINGS_EVENT" -> "-fx-text-fill:#16a34a;-fx-font-weight:bold;";
            case "CURRENCY_RATE" -> "-fx-text-fill:#f97316;-fx-font-weight:bold;";
            default -> "-fx-text-fill:#0f172a;";
        };
    }

    private String formatApiStatus(String label, SavingsCalendarService.ApiConnectionStatus status) {
        if (status == null) {
            return label + ": Error";
        }
        if (status.connected()) {
            return label + ": Connected";
        }
        return label + ": Error";
    }

    private void styleStatusBadge(Label badge, SavingsCalendarService.ApiConnectionStatus status) {
        boolean connected = status != null && status.connected();
        String background = connected ? "rgba(16,185,129,0.16)" : "rgba(239,68,68,0.16)";
        String border = connected ? "rgba(16,185,129,0.45)" : "rgba(239,68,68,0.45)";
        String text = connected ? "#065f46" : "#7f1d1d";
        badge.setStyle(
                "-fx-background-color:" + background + ";" +
                        "-fx-border-color:" + border + ";" +
                        "-fx-text-fill:" + text + ";" +
                        "-fx-background-radius:999;" +
                        "-fx-border-radius:999;" +
                        "-fx-padding:6 10 6 10;" +
                        "-fx-font-weight:bold;"
        );
        Tooltip.install(badge, new Tooltip(status == null ? "Status unavailable" : status.message()));
    }

    private String formatCalendarTimestamp(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "--";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private String formatMonthYear(YearMonth month) {
        return monthDisplayName(month.getMonth()) + " " + month.getYear();
    }

    private String monthDisplayName(java.time.Month month) {
        String raw = month.name().toLowerCase(Locale.ENGLISH);
        return raw.substring(0, 1).toUpperCase(Locale.ENGLISH) + raw.substring(1);
    }

    private java.time.Month monthFromDisplayName(String value) {
        if (value == null || value.isBlank()) {
            return java.time.Month.JANUARY;
        }
        return java.time.Month.valueOf(value.trim().toUpperCase(Locale.ENGLISH));
    }

    private record CalendarFilters(
            boolean showPublicHolidays,
            boolean showGoalDeadlines,
            boolean showContributions,
            boolean showCurrencyRates
    ) {
    }

    private BarChart<String, Number> createAdvancedAmountsChart(SavingsStatsService.FrontStatsSnapshot stats) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setPrefHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Balance", stats.balance().doubleValue()));
        series.getData().add(new XYChart.Data<>("Target", stats.totalTarget().doubleValue()));
        series.getData().add(new XYChart.Data<>("Current", stats.totalCurrent().doubleValue()));
        series.getData().add(new XYChart.Data<>("Remaining", stats.remaining().doubleValue()));
        chart.getData().setAll(series);
        return chart;
    }

    private javafx.scene.chart.LineChart<String, Number> createRateTrendChart(Map<String, Double> rates) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        javafx.scene.chart.LineChart<String, Number> chart = new javafx.scene.chart.LineChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setPrefHeight(300);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        rates.forEach((date, rate) -> series.getData().add(new XYChart.Data<>(date.substring(5), rate)));
        chart.getData().setAll(series);
        return chart;
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
        label.setStyle("-fx-font-size:20px;");
        label.setWrapText(true);
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

    private String summarizeTransactionDescriptions(List<SavingsTransactionRepository.TransactionRow> transactions) {
        return transactions.stream()
                .map(SavingsTransactionRepository.TransactionRow::description)
                .map(this::safeText)
                .filter(value -> !value.isBlank())
                .map(value -> truncate(value, 18))
                .distinct()
                .limit(4)
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
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String friendlyTransactionType(String type) {
        if (type == null || type.isBlank()) {
            return "Transaction";
        }
        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "EPARGNE" -> "Deposit";
            case "GOAL_CONTRIBUTION" -> "Goal Contribution";
            default -> type.replace('_', ' ');
        };
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
        if (darkModeEnabled) {
            if (!alert.getDialogPane().getStyleClass().contains("dark-dialog")) {
                alert.getDialogPane().getStyleClass().add("dark-dialog");
            }
        } else {
            alert.getDialogPane().getStyleClass().remove("dark-dialog");
        }
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<ButtonType> ignored = alert.showAndWait();
    }

    private void openGoalsBackOffice() {
        try {
            GoalsBackOfficeView backOfficeView = new GoalsBackOfficeView();
            Stage stage = new Stage();
            Scene scene = new Scene(backOfficeView.build(new SavingsUiController()), 1400, 860);
            applySceneTheme(scene);
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

    private void toggleDarkMode(boolean enabled) {
        darkModeEnabled = enabled;
        preferences.putBoolean(THEME_PREFERENCE_KEY, enabled);
        applyTheme();
        showInfo(enabled ? "Dark mode activated." : "Light mode activated.");
    }

    private void updateThemeToggleButton() {
        if (themeToggleButton == null) {
            return;
        }
        themeToggleButton.setSelected(darkModeEnabled);
        themeToggleButton.setText(darkModeEnabled ? "\u2600\uFE0F Light Mode" : "\uD83C\uDF19 Dark Mode");
    }

    private void applyTheme() {
        updateThemeToggleButton();
        if (rootContainer == null) {
            return;
        }
        if (darkModeEnabled) {
            if (!rootContainer.getStyleClass().contains("dark-mode")) {
                rootContainer.getStyleClass().add("dark-mode");
            }
        } else {
            rootContainer.getStyleClass().remove("dark-mode");
        }
    }

    private void applySceneTheme(Scene scene) {
        if (scene == null) {
            return;
        }
        String stylesheet = SavingsGoalsView.class.getResource("/pi/savings/ui/savings-goals.css").toExternalForm();
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
        if (darkModeEnabled) {
            if (!scene.getRoot().getStyleClass().contains("dark-mode")) {
                scene.getRoot().getStyleClass().add("dark-mode");
            }
        } else {
            scene.getRoot().getStyleClass().remove("dark-mode");
        }
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
            applySceneTheme(scene);

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
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        String formatted = MONEY_FORMAT.format(safeValue.setScale(2, RoundingMode.HALF_UP).doubleValue());
        return formatted + " TND";
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
