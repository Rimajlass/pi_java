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
import pi.entities.Revenue;
import pi.entities.User;
import pi.mains.Main;
import pi.services.RevenueExpenseService.RevenueService;
import pi.services.UserTransactionService.TransactionService;
import pi.tools.ThemeManager;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class RevenueBackController {

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
    private Button revenueSubmitButton;
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
    private VBox menuList;

    private final RevenueService revenueService = new RevenueService();
    private final TransactionService transactionService = new TransactionService();
    private final ObservableList<Revenue> revenues = FXCollections.observableArrayList();
    private final User currentUser = createCurrentUser();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Revenue editingRevenue;

    @FXML
    public void initialize() {
        configureFilters();
        configureRevenueTable();
        configureFormDefaults();
        loadData();
    }

    @FXML
    private void handleAddRevenue() {
        try {
            Revenue revenue = editingRevenue != null ? editingRevenue : new Revenue();
            revenue.setUser(currentUser);
            revenue.setAmount(parseAmount(revenueAmountField.getText(), "Revenue amount"));
            revenue.setType(normalizeRevenueType(requireValue(revenueTypeComboBox.getValue(), "Revenue type")));
            revenue.setReceivedAt(Objects.requireNonNullElse(revenueDatePicker.getValue(), LocalDate.now()));
            revenue.setDescription(normalizeText(revenueDescriptionArea.getText()));
            revenue.setCreatedAt(editingRevenue != null && editingRevenue.getCreatedAt() != null ? editingRevenue.getCreatedAt() : LocalDateTime.now());

            boolean updating = editingRevenue != null;
            if (updating) {
                revenueService.update(revenue);
                showInfo("Revenue updated successfully.");
            } else {
                revenueService.add(revenue);
                showInfo("Revenue added successfully.");
            }
            double amount = parseAmount(revenueAmountField.getText(), "Revenue amount");
            revenue.setAmount(amount);
            revenue.setType(requireValue(revenueTypeComboBox.getValue(), "Revenue type"));
            LocalDate txDate = Objects.requireNonNullElse(revenueDatePicker.getValue(), LocalDate.now());
            revenue.setReceivedAt(txDate);
            String description = normalizeText(revenueDescriptionArea.getText());
            revenue.setDescription(description);
            revenue.setCreatedAt(LocalDateTime.now());

            revenueService.add(revenue);
            transactionService.insertTransactionForUser(
                    currentUser.getId(),
                    "SAVING",
                    amount,
                    txDate,
                    description,
                    "revenue-back-office"
            );
            showInfo("Revenue added successfully.");
            clearRevenueForm();
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
    private void handleOpenExpenseInterface() {
        openWindow("/Expense/Revenue/BACK/expense-back-view.fxml", "Expense Back Office");
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
            } else if ("Expenses".equalsIgnoreCase(key)) {
                handleOpenExpenseInterface();
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
            Scene scene = new Scene(root, 1460, 900);
            if (resource != null && resource.contains("/pi/mains/transactions-management-view.fxml")) {
                scene.getStylesheets().add(Main.class.getResource("/pi/styles/admin-backend.css").toExternalForm());
                scene.getStylesheets().add(Main.class.getResource("/pi/styles/user-show.css").toExternalForm());
                scene.getStylesheets().add(Main.class.getResource("/pi/styles/edit-user.css").toExternalForm());
                scene.getStylesheets().add(Main.class.getResource("/pi/styles/transactions-management.css").toExternalForm());
                ThemeManager.registerScene(scene);
            }
            stage.setScene(scene);
            if (feedbackLabel != null && feedbackLabel.getScene() != null) {
                stage.initOwner(feedbackLabel.getScene().getWindow());
            }
            stage.show();
        } catch (IOException exception) {
            showError("Unable to open interface: " + exception.getMessage());
        }
    }

    private void configureFilters() {
        revenueTypeComboBox.setItems(FXCollections.observableArrayList("FIXED", "BONUS", "FREELANCE", "OTHER"));
        if (revenueSortByComboBox != null) {
            revenueSortByComboBox.setItems(FXCollections.observableArrayList("Date", "Amount", "Type", "Id"));
            revenueSortByComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshRevenueTable());
        }
        if (revenueDirectionComboBox != null) {
            revenueDirectionComboBox.setItems(FXCollections.observableArrayList("Desc", "Asc"));
            revenueDirectionComboBox.valueProperty().addListener((observable, oldValue, newValue) -> refreshRevenueTable());
        }
        if (revenueSearchField != null) {
            revenueSearchField.textProperty().addListener((observable, oldValue, newValue) -> refreshRevenueTable());
        }
    }

    private void configureRevenueTable() {
        revenueIdColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getId()));
        revenueDateColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatDate(cell.getValue().getReceivedAt())));
        revenueAmountColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getAmount()));
        revenueTypeColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(localizeRevenueType(cell.getValue().getType())));
        revenueDescriptionColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(nullSafe(cell.getValue().getDescription())));
        revenueActionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");
            private final HBox actionBox = new HBox(8.0, deleteButton);

            {
                deleteButton.setOnAction(event -> deleteRevenue(getTableView().getItems().get(getIndex())));
                deleteButton.getStyleClass().add("table-delete-button");
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

    private void configureFormDefaults() {
        revenueTypeComboBox.setValue("FIXED");
        if (revenueSortByComboBox != null) {
            revenueSortByComboBox.setValue("Date");
        }
        if (revenueDirectionComboBox != null) {
            revenueDirectionComboBox.setValue("Desc");
        }
        revenueDatePicker.setValue(LocalDate.now());
        if (revenueSubmitButton != null) {
            revenueSubmitButton.setText("Add Revenue");
        }
        feedbackLabel.setText("Revenue interface ready.");
    }

    private void loadData() {
        try {
            List<Revenue> revenueData = revenueService.getAll().stream()
                    .filter(revenue -> revenue.getUser() != null && revenue.getUser().getId() == currentUser.getId())
                    .collect(Collectors.toList());
            revenues.setAll(revenueData);
            refreshRevenueTable();
        } catch (SQLException exception) {
            showError("Database error: " + exception.getMessage());
        }
    }

    private void refreshRevenueTable() {
        String search = revenueSearchField == null ? "" : normalizeText(revenueSearchField.getText()).toLowerCase(Locale.ROOT);
        Comparator<Revenue> comparator = buildRevenueComparator();

        List<Revenue> filtered = revenues.stream()
                .filter(revenue -> matchesRevenue(revenue, search))
                .sorted(applyDirection(comparator, revenueDirectionComboBox == null ? "Desc" : revenueDirectionComboBox.getValue()))
                .collect(Collectors.toList());

        revenueTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private Comparator<Revenue> buildRevenueComparator() {
        String sortBy = revenueSortByComboBox == null ? "Date" : revenueSortByComboBox.getValue();
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

    private void deleteRevenue(Revenue revenue) {
        try {
            boolean wasEditingCurrentRevenue = editingRevenue != null && editingRevenue.getId() == revenue.getId();
            revenueService.delete(revenue.getId());
            if (wasEditingCurrentRevenue) {
                clearRevenueForm();
            }
            showInfo("Revenue deleted.");
            loadData();
        } catch (SQLException exception) {
            showError("Unable to delete revenue: " + exception.getMessage());
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

    private void clearRevenueForm() {
        editingRevenue = null;
        revenueAmountField.clear();
        revenueDescriptionArea.clear();
        revenueDatePicker.setValue(LocalDate.now());
        revenueTypeComboBox.setValue("FIXED");
        if (revenueTable != null) {
            revenueTable.getSelectionModel().clearSelection();
        }
        if (revenueSubmitButton != null) {
            revenueSubmitButton.setText("Add Revenue");
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
            revenueSubmitButton.setText("Update Revenue");
        }
        showInfo("Revenue loaded. Double-click another row to edit it, then click Update Revenue.");
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

    private static final class TableRowWithRevenue extends javafx.scene.control.TableRow<Revenue> {
    }
}
