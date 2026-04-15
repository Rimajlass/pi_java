package pi.savings.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
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
import pi.savings.repository.SavingsTransactionRepository;
import pi.savings.service.SavingsModuleService;

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

    Parent build(SavingsUiController controller) {
        this.controller = controller;

        BorderPane root = new BorderPane();
        root.getStyleClass().add("backoffice-root");
        root.getStylesheets().add(
                GoalsBackOfficeView.class.getResource("/pi/savings/ui/goals-backoffice.css").toExternalForm()
        );

        VBox page = new VBox(22);
        page.getStyleClass().add("backoffice-page");
        page.getChildren().add(buildHeader());
        page.getChildren().add(buildBody());

        root.setCenter(page);
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

        top.getChildren().addAll(title, spacer, openMainButton, refreshButton);

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

    private VBox buildSavingsTab() {
        VBox box = new VBox(14);

        savingsSearchField = new TextField();
        savingsSearchField.setPromptText("Search by id, date, type, amount, description, module source, user...");
        savingsSearchField.getStyleClass().add("toolbar-field");
        HBox.setHgrow(savingsSearchField, Priority.ALWAYS);
        savingsSearchField.textProperty().addListener((obs, oldValue, newValue) -> applySavingsSearchAndSort());

        savingsSortFieldCombo = new ComboBox<>();
        savingsSortFieldCombo.getItems().addAll("ID", "Date", "Type", "Amount", "Description", "Module Source", "User ID");
        savingsSortFieldCombo.setValue("Date");
        savingsSortFieldCombo.getStyleClass().add("toolbar-combo");
        savingsSortFieldCombo.setOnAction(event -> applySavingsSearchAndSort());

        savingsSortDirectionCombo = new ComboBox<>();
        savingsSortDirectionCombo.getItems().addAll("Descending", "Ascending");
        savingsSortDirectionCombo.setValue("Descending");
        savingsSortDirectionCombo.getStyleClass().add("toolbar-combo");
        savingsSortDirectionCombo.setOnAction(event -> applySavingsSearchAndSort());

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
                savingsIdColumn(),
                savingsDateColumn(),
                savingsTypeColumn(),
                savingsAmountColumn(),
                savingsDescriptionColumn(),
                savingsModuleColumn(),
                savingsUserColumn()
        );
        VBox.setVgrow(savingsTable, Priority.ALWAYS);

        box.getChildren().addAll(toolbar, savingsTable);
        return box;
    }

    private VBox buildGoalsTab() {
        VBox box = new VBox(14);

        goalsSearchField = new TextField();
        goalsSearchField.setPromptText("Search by id, name, target, current, deadline, priority or progress...");
        goalsSearchField.getStyleClass().add("toolbar-field");
        HBox.setHgrow(goalsSearchField, Priority.ALWAYS);
        goalsSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyGoalsSearchAndSort());

        goalsSortFieldCombo = new ComboBox<>();
        goalsSortFieldCombo.getItems().addAll("ID", "Name", "Target", "Current", "Deadline", "Priority", "Progress");
        goalsSortFieldCombo.setValue("Priority");
        goalsSortFieldCombo.getStyleClass().add("toolbar-combo");
        goalsSortFieldCombo.setOnAction(event -> applyGoalsSearchAndSort());

        goalsSortDirectionCombo = new ComboBox<>();
        goalsSortDirectionCombo.getItems().addAll("Descending", "Ascending");
        goalsSortDirectionCombo.setValue("Descending");
        goalsSortDirectionCombo.getStyleClass().add("toolbar-combo");
        goalsSortDirectionCombo.setOnAction(event -> applyGoalsSearchAndSort());

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
                goalIdColumn(),
                goalNameColumn(),
                goalTargetColumn(),
                goalCurrentColumn(),
                goalRemainingColumn(),
                goalDeadlineColumn(),
                goalPriorityColumn(),
                goalProgressColumn()
        );
        VBox.setVgrow(goalsTable, Priority.ALWAYS);

        box.getChildren().addAll(toolbar, goalsTable);
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
        refreshTablesFromController();
        if (result.success()) {
            showInfo("Back-office tables refreshed.");
        } else {
            showError(result.message());
        }
    }

    private void refreshTablesFromController() {
        savingsItems.setAll(controller.filterAndSortHistory(
                savingsSearchValue(),
                savingsSortFieldValue(),
                savingsSortDirectionValue()
        ));
        goalsItems.setAll(controller.filterAndSortGoals(
                goalsSearchValue(),
                goalsSortFieldValue(),
                goalsSortDirectionValue()
        ));
        refreshOverviewFromCurrentTab();
    }

    private void applySavingsSearchAndSort() {
        savingsItems.setAll(controller.filterAndSortHistory(
                savingsSearchValue(),
                savingsSortFieldValue(),
                savingsSortDirectionValue()
        ));
        refreshOverviewFromCurrentTab();
        showInfo("Savings search/sort applied.");
    }

    private void applyGoalsSearchAndSort() {
        goalsItems.setAll(controller.filterAndSortGoals(
                goalsSearchValue(),
                goalsSortFieldValue(),
                goalsSortDirectionValue()
        ));
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
        } else {
            activeSearchValue.setText(goalsSearchValue().isBlank() ? "All goals" : goalsSearchValue());
            activeSortValue.setText(goalsSortFieldValue() + " / " + goalsSortDirectionValue());
        }
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
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-error", "feedback-info");
        feedbackLabel.getStyleClass().add("feedback-info");
        feedbackLabel.setManaged(true);
        feedbackLabel.setVisible(true);
    }

    private void showError(String message) {
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
}
