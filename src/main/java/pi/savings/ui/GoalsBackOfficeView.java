package pi.savings.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pi.savings.repository.SavingsTransactionRepository;
import pi.savings.service.SavingsModuleService;
import pi.savings.service.SavingsStatsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

final class GoalsBackOfficeView {

    private final ObservableList<SavingsTransactionRepository.TransactionRow> savingsItems = FXCollections.observableArrayList();
    private final ObservableList<SavingsModuleService.GoalSnapshot> goalsItems = FXCollections.observableArrayList();

    private SavingsUiController controller;
    private TableView<SavingsTransactionRepository.TransactionRow> savingsTable;
    private TableView<SavingsModuleService.GoalSnapshot> goalsTable;
    private TextField savingsSearchField;
    private ComboBox<String> savingsSortFieldCombo;
    private ComboBox<String> savingsSortDirectionCombo;
    private TextField goalsSearchField;
    private ComboBox<String> goalsSortFieldCombo;
    private ComboBox<String> goalsSortDirectionCombo;
    private Label feedbackLabel;
    private Label balanceValue;
    private Label interestRateValue;
    private Label createdOnValue;
    private Label savingsRowsValue;
    private Label goalsRowsValue;
    private Label completedGoalsValue;
    private Label remainingAmountValue;
    private Label nearestDeadlineValue;
    private Label activeSearchValue;
    private Label activeSortValue;
    private TabPane tabPane;
    private ComboBox<String> currencyComboBox;
    private PieChart savingsPieChart;
    private BarChart<String, Number> savingsBarChart;
    private PieChart goalsPieChart;
    private BarChart<String, Number> goalsBarChart;
    private Label savingsPaginationLabel;
    private Button savingsPreviousButton;
    private Button savingsNextButton;
    private int savingsPageIndex;
    private static final int SAVINGS_PAGE_SIZE = 8;
    private Label goalsPaginationLabel;
    private Button goalsPreviousButton;
    private Button goalsNextButton;
    private int goalsPageIndex;
    private static final int GOALS_PAGE_SIZE = 8;

    Parent build(SavingsUiController controller) {
        return build(controller, "Goals", false);
    }

    Parent build(SavingsUiController controller, String initialTab, boolean embeddedMode) {
        this.controller = controller;

        BorderPane root = new BorderPane();
        root.getStyleClass().add("backoffice-root");
        root.getStylesheets().add(
                GoalsBackOfficeView.class.getResource("/pi/savings/ui/goals-backoffice.css").toExternalForm()
        );

        VBox page = new VBox(22);
        page.getStyleClass().add("backoffice-page");
        if (!embeddedMode) {
            page.getChildren().add(buildHeader());
        }
        page.getChildren().add(buildBody());

        root.setCenter(page);
        if (embeddedMode) {
            page.setFillWidth(true);
            page.setStyle("-fx-padding: 0;");
        }
        selectInitialTab(initialTab);
        initializeDataAsync();
        return root;
    }

    private VBox buildHeader() {
        VBox header = new VBox(6);
        header.getStyleClass().add("page-hero");

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        top.setSpacing(12);

        Label title = new Label("Savings & Goals Back Office");
        title.getStyleClass().add("hero-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button openMainButton = new Button("Open Front Workspace");
        openMainButton.getStyleClass().add("secondary-action-btn");
        openMainButton.setOnAction(event -> showInfo("Front workspace remains available through SavingsGoalsApp."));

        Button refreshButton = new Button("Refresh All");
        refreshButton.getStyleClass().add("primary-action-btn");
        refreshButton.setOnAction(event -> reloadDashboard());

        currencyComboBox = new ComboBox<>();
        currencyComboBox.getItems().setAll(controller.getSupportedCurrencies());
        currencyComboBox.setValue(controller.getSelectedCurrency());
        currencyComboBox.getStyleClass().add("toolbar-combo");
        currencyComboBox.setOnAction(event -> handleCurrencyChange());

        Button statsButton = new Button("Global Stats");
        statsButton.getStyleClass().add("secondary-action-btn");
        statsButton.setOnAction(event -> showGlobalStatsWindow());

        Button savingsCsvButton = new Button("Savings CSV");
        savingsCsvButton.getStyleClass().add("secondary-action-btn");
        savingsCsvButton.setOnAction(event -> exportAllSavingsCsv());

        Button savingsPdfButton = new Button("Savings PDF");
        savingsPdfButton.getStyleClass().add("secondary-action-btn");
        savingsPdfButton.setOnAction(event -> exportAllSavingsPdf());

        Button goalsCsvButton = new Button("Goals CSV");
        goalsCsvButton.getStyleClass().add("secondary-action-btn");
        goalsCsvButton.setOnAction(event -> exportAllGoalsCsv());

        Button goalsPdfButton = new Button("Goals PDF");
        goalsPdfButton.getStyleClass().add("secondary-action-btn");
        goalsPdfButton.setOnAction(event -> exportAllGoalsPdf());

        top.getChildren().addAll(title, spacer, currencyComboBox, statsButton, savingsCsvButton, savingsPdfButton, goalsCsvButton, goalsPdfButton, openMainButton, refreshButton);

        Label subtitle = new Label("Admin-style interface for browsing, searching, sorting, and monitoring savings transactions and financial goals.");
        subtitle.getStyleClass().add("hero-subtitle");

        feedbackLabel = new Label();
        feedbackLabel.getStyleClass().add("feedback-label");
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);

        header.getChildren().addAll(top, subtitle, feedbackLabel);
        return header;
    }

    private HBox buildBody() {
        HBox body = new HBox(18);
        body.setAlignment(Pos.TOP_LEFT);

        VBox sidePanel = buildOverviewPanel();
        VBox tablePanel = buildTablePanel();
        HBox.setHgrow(tablePanel, Priority.ALWAYS);

        body.getChildren().addAll(sidePanel, tablePanel);
        return body;
    }

    private VBox buildOverviewPanel() {
        VBox panel = new VBox(16);
        panel.getStyleClass().add("side-card");
        panel.setPrefWidth(320);

        balanceValue = summaryValue();
        interestRateValue = summaryValue();
        createdOnValue = summaryValue();
        savingsRowsValue = summaryValue();
        goalsRowsValue = summaryValue();
        completedGoalsValue = summaryValue();
        remainingAmountValue = summaryValue();
        nearestDeadlineValue = summaryValue();
        activeSearchValue = summaryValue();
        activeSortValue = summaryValue();

        panel.getChildren().addAll(
                cardTitle("Overview", "Live summary of the back-office filters and current data."),
                summaryBlock("Balance", balanceValue),
                summaryBlock("Interest rate", interestRateValue),
                summaryBlock("Created on", createdOnValue),
                summaryBlock("Savings rows", savingsRowsValue),
                summaryBlock("Goals rows", goalsRowsValue),
                summaryBlock("Completed goals", completedGoalsValue),
                summaryBlock("Remaining amount", remainingAmountValue),
                summaryBlock("Nearest deadline", nearestDeadlineValue),
                summaryBlock("Active search", activeSearchValue),
                summaryBlock("Active sort", activeSortValue)
        );
        return panel;
    }

    private VBox buildChartsCard(String titleText, PieChart pieChart, BarChart<String, Number> barChart) {
        VBox card = new VBox(12);
        card.getStyleClass().add("summary-block");

        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");
        card.getChildren().addAll(title, pieChart, barChart);
        return card;
    }

    private PieChart createPieChart() {
        PieChart chart = new PieChart();
        chart.setLegendVisible(true);
        chart.setLabelsVisible(true);
        chart.setPrefHeight(230);
        chart.getStyleClass().add("stats-pie-chart");
        return chart;
    }

    private BarChart<String, Number> createBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickLabelsVisible(true);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(18);
        chart.setBarGap(6);
        chart.setPrefHeight(240);
        chart.getStyleClass().add("stats-bar-chart");
        return chart;
    }

    private VBox buildTablePanel() {
        VBox panel = new VBox(16);
        panel.getStyleClass().add("table-card");

        panel.getChildren().add(cardTitle("Management Tables", "Search and sort every available attribute from both savings and goals."));

        tabPane = new TabPane();
        tabPane.getStyleClass().add("backoffice-tabs");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab savingsTab = new Tab("Savings", buildSavingsTab());
        Tab goalsTab = new Tab("Goals", buildGoalsTab());
        tabPane.getTabs().addAll(savingsTab, goalsTab);
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> refreshOverviewFromCurrentTab());

        panel.getChildren().add(tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        return panel;
    }

    private void selectInitialTab(String initialTab) {
        if (tabPane == null || initialTab == null) {
            return;
        }
        if ("savings".equalsIgnoreCase(initialTab)) {
            tabPane.getSelectionModel().select(0);
        } else if ("goals".equalsIgnoreCase(initialTab)) {
            tabPane.getSelectionModel().select(1);
        }
    }

    private VBox buildSavingsTab() {
        VBox box = new VBox(14);

        savingsSearchField = new TextField();
        savingsSearchField.setPromptText("Search by id, date, type, amount, description, module source, user...");
        savingsSearchField.getStyleClass().add("toolbar-field");
        HBox.setHgrow(savingsSearchField, Priority.ALWAYS);
        savingsSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
            savingsPageIndex = 0;
            applySavingsSearchAndSort();
        });

        savingsSortFieldCombo = new ComboBox<>();
        savingsSortFieldCombo.getItems().addAll("ID", "Date", "Type", "Amount", "Description", "Module Source", "User ID");
        savingsSortFieldCombo.setValue("Date");
        savingsSortFieldCombo.getStyleClass().add("toolbar-combo");
        savingsSortFieldCombo.setOnAction(event -> {
            savingsPageIndex = 0;
            applySavingsSearchAndSort();
        });

        savingsSortDirectionCombo = new ComboBox<>();
        savingsSortDirectionCombo.getItems().addAll("Descending", "Ascending");
        savingsSortDirectionCombo.setValue("Descending");
        savingsSortDirectionCombo.getStyleClass().add("toolbar-combo");
        savingsSortDirectionCombo.setOnAction(event -> {
            savingsPageIndex = 0;
            applySavingsSearchAndSort();
        });

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("primary-action-btn");
        refreshButton.setOnAction(event -> reloadDashboard());

        HBox toolbar = new HBox(10, savingsSortFieldCombo, savingsSortDirectionCombo, savingsSearchField, refreshButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        savingsTable = new TableView<>(savingsItems);
        savingsTable.getStyleClass().add("backoffice-table");
        savingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        savingsTable.setPlaceholder(new Label("No savings transactions available."));
        savingsTable.getColumns().addAll(
                savingsDateColumn(),
                savingsTypeColumn(),
                savingsAmountColumn(),
                savingsDescriptionColumn(),
                savingsModuleColumn(),
                savingsUserColumn()
        );
        VBox.setVgrow(savingsTable, Priority.ALWAYS);

        savingsPieChart = createPieChart();
        savingsBarChart = createBarChart();

        box.getChildren().addAll(toolbar, savingsTable, buildSavingsPaginationBar(), buildChartsCard("Savings charts", savingsPieChart, savingsBarChart));
        return box;
    }

    private VBox buildGoalsTab() {
        VBox box = new VBox(14);

        goalsSearchField = new TextField();
        goalsSearchField.setPromptText("Search by id, name, target, current, deadline, priority or progress...");
        goalsSearchField.getStyleClass().add("toolbar-field");
        HBox.setHgrow(goalsSearchField, Priority.ALWAYS);
        goalsSearchField.textProperty().addListener((obs, oldValue, newValue) -> {
            goalsPageIndex = 0;
            applyGoalsSearchAndSort();
        });

        goalsSortFieldCombo = new ComboBox<>();
        goalsSortFieldCombo.getItems().addAll("ID", "Name", "Target", "Current", "Deadline", "Priority", "Progress");
        goalsSortFieldCombo.setValue("Priority");
        goalsSortFieldCombo.getStyleClass().add("toolbar-combo");
        goalsSortFieldCombo.setOnAction(event -> {
            goalsPageIndex = 0;
            applyGoalsSearchAndSort();
        });

        goalsSortDirectionCombo = new ComboBox<>();
        goalsSortDirectionCombo.getItems().addAll("Descending", "Ascending");
        goalsSortDirectionCombo.setValue("Descending");
        goalsSortDirectionCombo.getStyleClass().add("toolbar-combo");
        goalsSortDirectionCombo.setOnAction(event -> {
            goalsPageIndex = 0;
            applyGoalsSearchAndSort();
        });

        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("primary-action-btn");
        refreshButton.setOnAction(event -> reloadDashboard());

        HBox toolbar = new HBox(10, goalsSortFieldCombo, goalsSortDirectionCombo, goalsSearchField, refreshButton);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        goalsTable = new TableView<>(goalsItems);
        goalsTable.getStyleClass().add("backoffice-table");
        goalsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        goalsTable.setPlaceholder(new Label("No goals available."));
        goalsTable.getColumns().addAll(
                goalNameColumn(),
                goalTargetColumn(),
                goalCurrentColumn(),
                goalRemainingColumn(),
                goalDeadlineColumn(),
                goalPriorityColumn(),
                goalProgressColumn()
        );
        VBox.setVgrow(goalsTable, Priority.ALWAYS);

        goalsPieChart = createPieChart();
        goalsBarChart = createBarChart();

        box.getChildren().addAll(toolbar, goalsTable, buildGoalsPaginationBar(), buildChartsCard("Goals charts", goalsPieChart, goalsBarChart));
        return box;
    }

    private HBox buildSavingsPaginationBar() {
        savingsPreviousButton = new Button("Previous");
        savingsPreviousButton.getStyleClass().add("secondary-action-btn");
        savingsPreviousButton.setOnAction(event -> {
            if (savingsPageIndex > 0) {
                savingsPageIndex--;
                applySavingsSearchAndSort();
            }
        });

        savingsNextButton = new Button("Next");
        savingsNextButton.getStyleClass().add("secondary-action-btn");
        savingsNextButton.setOnAction(event -> {
            savingsPageIndex++;
            applySavingsSearchAndSort();
        });

        savingsPaginationLabel = new Label("Page 1 / 1");
        savingsPaginationLabel.getStyleClass().add("summary-value");

        HBox box = new HBox(10, savingsPreviousButton, savingsPaginationLabel, savingsNextButton);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private HBox buildGoalsPaginationBar() {
        goalsPreviousButton = new Button("Previous");
        goalsPreviousButton.getStyleClass().add("secondary-action-btn");
        goalsPreviousButton.setOnAction(event -> {
            if (goalsPageIndex > 0) {
                goalsPageIndex--;
                applyGoalsSearchAndSort();
            }
        });

        goalsNextButton = new Button("Next");
        goalsNextButton.getStyleClass().add("secondary-action-btn");
        goalsNextButton.setOnAction(event -> {
            goalsPageIndex++;
            applyGoalsSearchAndSort();
        });

        goalsPaginationLabel = new Label("Page 1 / 1");
        goalsPaginationLabel.getStyleClass().add("summary-value");

        HBox box = new HBox(10, goalsPreviousButton, goalsPaginationLabel, goalsNextButton);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private TableColumn<SavingsTransactionRepository.TransactionRow, Number> savingsIdColumn() {
        TableColumn<SavingsTransactionRepository.TransactionRow, Number> column = new TableColumn<>("ID");
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().id()));
        column.setMaxWidth(80);
        return column;
    }

    private TableColumn<SavingsTransactionRepository.TransactionRow, String> savingsDateColumn() {
        TableColumn<SavingsTransactionRepository.TransactionRow, String> column = new TableColumn<>("Date");
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().date().toString()));
        column.setMinWidth(170);
        return column;
    }

    private TableColumn<SavingsTransactionRepository.TransactionRow, String> savingsTypeColumn() {
        TableColumn<SavingsTransactionRepository.TransactionRow, String> column = new TableColumn<>("Type");
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().type()));
        column.setMinWidth(130);
        return column;
    }

    private TableColumn<SavingsTransactionRepository.TransactionRow, String> savingsAmountColumn() {
        TableColumn<SavingsTransactionRepository.TransactionRow, String> column = new TableColumn<>("Amount");
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatMoney(data.getValue().amount())));
        column.setMinWidth(120);
        return column;
    }

    private TableColumn<SavingsTransactionRepository.TransactionRow, String> savingsDescriptionColumn() {
        TableColumn<SavingsTransactionRepository.TransactionRow, String> column = new TableColumn<>("Description");
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().description()));
        column.setMinWidth(220);
        return column;
    }

    private TableColumn<SavingsTransactionRepository.TransactionRow, String> savingsModuleColumn() {
        TableColumn<SavingsTransactionRepository.TransactionRow, String> column = new TableColumn<>("Module Source");
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().moduleSource()));
        column.setMinWidth(130);
        return column;
    }

    private TableColumn<SavingsTransactionRepository.TransactionRow, Number> savingsUserColumn() {
        TableColumn<SavingsTransactionRepository.TransactionRow, Number> column = new TableColumn<>("User ID");
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().userId()));
        column.setMaxWidth(90);
        return column;
    }

    private TableColumn<SavingsModuleService.GoalSnapshot, Number> goalIdColumn() {
        TableColumn<SavingsModuleService.GoalSnapshot, Number> column = new TableColumn<>("ID");
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().id()));
        column.setMaxWidth(70);
        return column;
    }

    private TableColumn<SavingsModuleService.GoalSnapshot, String> goalNameColumn() {
        TableColumn<SavingsModuleService.GoalSnapshot, String> column = new TableColumn<>("Name");
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        column.setMinWidth(180);
        return column;
    }

    private TableColumn<SavingsModuleService.GoalSnapshot, String> goalTargetColumn() {
        TableColumn<SavingsModuleService.GoalSnapshot, String> column = new TableColumn<>("Target");
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatMoney(data.getValue().target())));
        column.setMinWidth(120);
        return column;
    }

    private TableColumn<SavingsModuleService.GoalSnapshot, String> goalCurrentColumn() {
        TableColumn<SavingsModuleService.GoalSnapshot, String> column = new TableColumn<>("Current");
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatMoney(data.getValue().current())));
        column.setMinWidth(120);
        return column;
    }

    private TableColumn<SavingsModuleService.GoalSnapshot, String> goalRemainingColumn() {
        TableColumn<SavingsModuleService.GoalSnapshot, String> column = new TableColumn<>("Remaining");
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                formatMoney(data.getValue().target().subtract(data.getValue().current()).max(BigDecimal.ZERO))
        ));
        column.setMinWidth(130);
        return column;
    }

    private TableColumn<SavingsModuleService.GoalSnapshot, String> goalDeadlineColumn() {
        TableColumn<SavingsModuleService.GoalSnapshot, String> column = new TableColumn<>("Deadline");
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(
                data.getValue().deadline() == null ? "--/--/----" : data.getValue().deadline().toString()
        ));
        column.setMinWidth(120);
        return column;
    }

    private TableColumn<SavingsModuleService.GoalSnapshot, Number> goalPriorityColumn() {
        TableColumn<SavingsModuleService.GoalSnapshot, Number> column = new TableColumn<>("Priority");
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().priority()));
        column.setMaxWidth(90);
        return column;
    }

    private TableColumn<SavingsModuleService.GoalSnapshot, SavingsModuleService.GoalSnapshot> goalProgressColumn() {
        TableColumn<SavingsModuleService.GoalSnapshot, SavingsModuleService.GoalSnapshot> column = new TableColumn<>("Progress");
        column.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue()));
        column.setMinWidth(180);
        column.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar();
            private final Label label = new Label();
            private final HBox content = new HBox(10, progressBar, label);

            {
                content.setAlignment(Pos.CENTER_LEFT);
                progressBar.setPrefWidth(110);
                progressBar.getStyleClass().add("progress-track");
                label.getStyleClass().add("progress-text");
            }

            @Override
            protected void updateItem(SavingsModuleService.GoalSnapshot item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    progressBar.setProgress(item.progressPercent() / 100.0);
                    label.setText((int) Math.round(item.progressPercent()) + "%");
                    setGraphic(content);
                }
            }
        });
        return column;
    }

    private void initializeDataAsync() {
        Task<SavingsUiController.OperationResult> task = new Task<>() {
            @Override
            protected SavingsUiController.OperationResult call() {
                return controller.initialize();
            }
        };

        task.setOnSucceeded(event -> {
            refreshTablesFromController();
            SavingsUiController.OperationResult result = task.getValue();
            if (result.success()) {
                showInfo("Savings & Goals back-office ready.");
            } else {
                showError(result.message());
            }
        });

        task.setOnFailed(event -> showError("Unable to load savings and goals back-office data."));

        Thread thread = new Thread(task, "savings-goals-backoffice-loader");
        thread.setDaemon(true);
        thread.start();
    }

    private void reloadDashboard() {
        SavingsUiController.OperationResult result = controller.initialize();
        savingsPageIndex = 0;
        goalsPageIndex = 0;
        refreshTablesFromController();
        if (result.success()) {
            showInfo("Back-office tables refreshed.");
        } else {
            showError(result.message());
        }
    }

    private void refreshTablesFromController() {
        SavingsUiController.PageSlice<SavingsTransactionRepository.TransactionRow> savingsPage = controller.paginate(
                controller.filterAndSortHistory(
                        savingsSearchValue(),
                        savingsSortFieldValue(),
                        savingsSortDirectionValue()
                ),
                savingsPageIndex,
                SAVINGS_PAGE_SIZE
        );
        savingsPageIndex = savingsPage.pageIndex();
        savingsItems.setAll(savingsPage.items());
        updateSavingsPagination(savingsPage);

        SavingsUiController.PageSlice<SavingsModuleService.GoalSnapshot> goalsPage = controller.paginate(
                controller.filterAndSortGoals(
                        goalsSearchValue(),
                        goalsSortFieldValue(),
                        goalsSortDirectionValue()
                ),
                goalsPageIndex,
                GOALS_PAGE_SIZE
        );
        goalsPageIndex = goalsPage.pageIndex();
        goalsItems.setAll(goalsPage.items());
        updateGoalsPagination(goalsPage);

        refreshOverviewFromCurrentTab();
    }

    private void applySavingsSearchAndSort() {
        SavingsUiController.PageSlice<SavingsTransactionRepository.TransactionRow> savingsPage = controller.paginate(
                controller.filterAndSortHistory(
                        savingsSearchValue(),
                        savingsSortFieldValue(),
                        savingsSortDirectionValue()
                ),
                savingsPageIndex,
                SAVINGS_PAGE_SIZE
        );
        savingsPageIndex = savingsPage.pageIndex();
        savingsItems.setAll(savingsPage.items());
        updateSavingsPagination(savingsPage);
        refreshOverviewFromCurrentTab();
        showInfo("Savings search/sort applied.");
    }

    private void applyGoalsSearchAndSort() {
        SavingsUiController.PageSlice<SavingsModuleService.GoalSnapshot> goalsPage = controller.paginate(
                controller.filterAndSortGoals(
                        goalsSearchValue(),
                        goalsSortFieldValue(),
                        goalsSortDirectionValue()
                ),
                goalsPageIndex,
                GOALS_PAGE_SIZE
        );
        goalsPageIndex = goalsPage.pageIndex();
        goalsItems.setAll(goalsPage.items());
        updateGoalsPagination(goalsPage);
        refreshOverviewFromCurrentTab();
        showInfo("Goals search/sort applied.");
    }

    private void refreshOverviewFromCurrentTab() {
        SavingsModuleService.DashboardSnapshot snapshot = controller.getSnapshot();
        SavingsModuleService.GoalStats goalStats = controller.calculateGoalStats(goalsItems);

        balanceValue.setText(formatMoney(snapshot.balance()));
        interestRateValue.setText(formatPlain(snapshot.interestRate()) + "%");
        createdOnValue.setText(snapshot.createdOn().toString());
        savingsRowsValue.setText(String.valueOf(savingsItems.size()));
        goalsRowsValue.setText(String.valueOf(goalsItems.size()));
        completedGoalsValue.setText(String.valueOf(goalStats.completedGoalCount()));
        remainingAmountValue.setText(formatMoney(goalStats.remainingAmount()));
        nearestDeadlineValue.setText(goalStats.nearestDeadline());

        if (tabPane != null && tabPane.getSelectionModel().getSelectedIndex() == 0) {
            activeSearchValue.setText(savingsSearchValue().isBlank() ? "All savings rows" : savingsSearchValue());
            activeSortValue.setText(savingsSortFieldValue() + " / " + savingsSortDirectionValue());
            refreshSavingsCharts();
        } else {
            activeSearchValue.setText(goalsSearchValue().isBlank() ? "All goals" : goalsSearchValue());
            activeSortValue.setText(goalsSortFieldValue() + " / " + goalsSortDirectionValue());
            refreshGoalsCharts();
        }
    }

    private void refreshSavingsCharts() {
        SavingsModuleService.HistoryStats stats = controller.calculateHistoryStats(List.copyOf(savingsItems));
        if (savingsPieChart == null || savingsBarChart == null) {
            return;
        }

        savingsPieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Deposits", stats.totalDeposited().doubleValue()),
                new PieChart.Data("Contributions", stats.totalContributedToGoals().doubleValue())
        ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Rows", stats.transactionCount()));
        series.getData().add(new XYChart.Data<>("Deposits", stats.depositCount()));
        series.getData().add(new XYChart.Data<>("Goals", stats.goalContributionCount()));

        savingsBarChart.getData().setAll(series);
    }

    private void refreshGoalsCharts() {
        SavingsModuleService.GoalStats stats = controller.calculateGoalStats(List.copyOf(goalsItems));
        int inProgressCount = Math.max(0, stats.goalCount() - stats.completedGoalCount());
        if (goalsPieChart == null || goalsBarChart == null) {
            return;
        }

        goalsPieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Completed", stats.completedGoalCount()),
                new PieChart.Data("In progress", inProgressCount)
        ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Target", stats.totalTarget().doubleValue()));
        series.getData().add(new XYChart.Data<>("Current", stats.totalCurrent().doubleValue()));
        series.getData().add(new XYChart.Data<>("Remaining", stats.remainingAmount().doubleValue()));

        goalsBarChart.getData().setAll(series);
    }

    private VBox cardTitle(String titleText, String subtitleText) {
        VBox box = new VBox(4);
        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("card-subtitle");
        subtitle.setWrapText(true);
        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private VBox summaryBlock(String labelText, Label valueLabel) {
        VBox block = new VBox(6);
        block.getStyleClass().add("summary-block");
        Label label = new Label(labelText);
        label.getStyleClass().add("summary-label");
        block.getChildren().addAll(label, valueLabel);
        return block;
    }

    private Label summaryValue() {
        Label label = new Label("--");
        label.getStyleClass().add("summary-value");
        return label;
    }

    private void showInfo(String message) {
        if (feedbackLabel == null) {
            return;
        }
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-info");
        feedbackLabel.getStyleClass().add("feedback-info");
        feedbackLabel.setManaged(true);
        feedbackLabel.setVisible(true);
    }

    private void showError(String message) {
        if (feedbackLabel == null) {
            return;
        }
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-info");
        feedbackLabel.getStyleClass().add("feedback-error");
        feedbackLabel.setManaged(true);
        feedbackLabel.setVisible(true);
    }

    private String savingsSearchValue() {
        return savingsSearchField == null ? "" : savingsSearchField.getText();
    }

    private String savingsSortFieldValue() {
        return savingsSortFieldCombo == null || savingsSortFieldCombo.getValue() == null ? "Date" : savingsSortFieldCombo.getValue();
    }

    private String savingsSortDirectionValue() {
        return savingsSortDirectionCombo == null || savingsSortDirectionCombo.getValue() == null ? "Descending" : savingsSortDirectionCombo.getValue();
    }

    private String goalsSearchValue() {
        return goalsSearchField == null ? "" : goalsSearchField.getText();
    }

    private String goalsSortFieldValue() {
        return goalsSortFieldCombo == null || goalsSortFieldCombo.getValue() == null ? "Priority" : goalsSortFieldCombo.getValue();
    }

    private String goalsSortDirectionValue() {
        return goalsSortDirectionCombo == null || goalsSortDirectionCombo.getValue() == null ? "Descending" : goalsSortDirectionCombo.getValue();
    }

    private String formatMoney(BigDecimal value) {
        return formatPlain(value) + " TND";
    }

    private String formatPlain(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private void updateSavingsPagination(SavingsUiController.PageSlice<SavingsTransactionRepository.TransactionRow> page) {
        if (savingsPaginationLabel != null) {
            savingsPaginationLabel.setText(
                    "Page " + (page.pageIndex() + 1) + " / " + page.pageCount() + " (" + page.totalItems() + " rows)"
            );
        }
        if (savingsPreviousButton != null) {
            savingsPreviousButton.setDisable(page.pageIndex() <= 0);
        }
        if (savingsNextButton != null) {
            savingsNextButton.setDisable(page.pageIndex() >= page.pageCount() - 1);
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

    private void handleCurrencyChange() {
        SavingsUiController.OperationResult result = controller.selectCurrency(
                currencyComboBox == null ? "TND" : currencyComboBox.getValue()
        );
        if (result.success()) {
            refreshOverviewFromCurrentTab();
            showInfo(result.message());
        } else {
            showError(result.message());
        }
    }

    private void exportAllSavingsCsv() {
        SavingsUiController.OperationResult result = controller.safeExportAllSavingAccountsCsv(java.nio.file.Paths.get("target", "exports"));
        if (result.success()) {
            showInfo(result.message());
        } else {
            showError(result.message());
        }
    }

    private void exportAllSavingsPdf() {
        SavingsUiController.OperationResult result = controller.safeExportAllSavingAccountsPdf(java.nio.file.Paths.get("target", "exports"));
        if (result.success()) {
            showInfo(result.message());
        } else {
            showError(result.message());
        }
    }

    private void exportAllGoalsCsv() {
        SavingsUiController.OperationResult result = controller.safeExportAllGoalsCsv(java.nio.file.Paths.get("target", "exports"));
        if (result.success()) {
            showInfo(result.message());
        } else {
            showError(result.message());
        }
    }

    private void exportAllGoalsPdf() {
        SavingsUiController.OperationResult result = controller.safeExportAllGoalsPdf(java.nio.file.Paths.get("target", "exports"));
        if (result.success()) {
            showInfo(result.message());
        } else {
            showError(result.message());
        }
    }

    private void showGlobalStatsWindow() {
        SavingsStatsService.BackOfficeStatsSnapshot stats = controller.loadBackOfficeStats();
        Stage stage = new Stage();
        VBox root = new VBox(18);
        root.setStyle("-fx-padding: 22;");
        root.getStyleClass().add("table-card");

        Label title = new Label("Back Office Statistics");
        title.getStyleClass().add("hero-title");
        title.setStyle("-fx-font-size: 28px;");

        Label subtitle = new Label("Global figures enriched with Currency API and holiday risk detection.");
        subtitle.getStyleClass().add("hero-subtitle");

        HBox cards = new HBox(10,
                summaryBlock("Accounts", valueOf(String.valueOf(stats.totalSavingsAccounts()))),
                summaryBlock("Balance", valueOf(formatMoney(stats.totalBalance()))),
                summaryBlock("Goals", valueOf(String.valueOf(stats.totalGoals()))),
                summaryBlock("Near holiday", valueOf(String.valueOf(stats.nearHolidayGoals())))
        );

        VBox topUsers = cardTitle("Top Users", stats.topUsers().stream()
                .map(row -> row.userName() + " - " + row.userEmail() + " - " + formatMoney(row.balance()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No users"));
        VBox topGoals = cardTitle("Top Goals", stats.topGoals().stream()
                .map(row -> row.goalName() + " - " + row.progressPercent().stripTrailingZeros().toPlainString() + "% - " + row.status())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No goals"));

        root.getChildren().addAll(title, subtitle, cards, topUsers, topGoals);

        Scene scene = new Scene(root, 920, 620);
        scene.getStylesheets().add(GoalsBackOfficeView.class.getResource("/pi/savings/ui/goals-backoffice.css").toExternalForm());
        stage.setTitle("Back Office Statistics");
        stage.setScene(scene);
        stage.show();
    }

    private Label valueOf(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("summary-value");
        return label;
    }
}
