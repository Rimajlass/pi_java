package pi.controllers.UserTransactionController;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import pi.entities.Transaction;
import pi.entities.User;
import pi.mains.Main;
import pi.services.UserTransactionService.TransactionService;
import pi.services.UserTransactionService.UserService;
import pi.tools.AdminNavigation;
import pi.tools.FxmlResources;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class TransactionsManagementController {

    @FXML
    private Label userLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label kpiExpenseLabel;

    @FXML
    private Label kpiSavingLabel;

    @FXML
    private Label kpiInvestmentLabel;

    @FXML
    private TextField filterUserField;

    @FXML
    private ComboBox<String> filterTypeCombo;

    @FXML
    private DatePicker filterDateFrom;

    @FXML
    private DatePicker filterDateTo;

    @FXML
    private ComboBox<String> filterSortCombo;

    @FXML
    private ComboBox<String> filterOrderCombo;

    @FXML
    private ComboBox<User> formUserCombo;

    @FXML
    private ComboBox<String> formTypeCombo;

    @FXML
    private TextField formAmountField;

    @FXML
    private DatePicker formDatePicker;

    @FXML
    private TextArea formDescriptionArea;

    @FXML
    private TextField formSourceField;

    @FXML
    private TableView<Transaction> transactionTable;

    @FXML
    private VBox menuList;

    private final TransactionService transactionService = new TransactionService();
    private final UserService userService = new UserService();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");
    private static final Pattern SOURCE_PATTERN = Pattern.compile("^[A-Za-z0-9 _-]{0,80}$");
    private User currentUser;

    @FXML
    public void initialize() {
        filterTypeCombo.getItems().setAll("All types", "EXPENSE", "SAVING", "INVESTMENT");
        filterTypeCombo.setValue("All types");
        filterSortCombo.getItems().setAll("Sort by date", "Sort by user", "Sort by type", "Sort by amount", "Sort by description", "Sort by source", "Sort by ID");
        filterSortCombo.setValue("Sort by date");
        filterOrderCombo.getItems().setAll("DESC", "ASC");
        filterOrderCombo.setValue("DESC");

        formTypeCombo.getItems().setAll("EXPENSE", "SAVING", "INVESTMENT");
        formTypeCombo.setValue("EXPENSE");

        formDatePicker.setValue(LocalDate.now());

        setupUserCombo();
        buildTableColumns();
    }

    private void setupUserCombo() {
        StringConverter<User> conv = new StringConverter<>() {
            @Override
            public String toString(User u) {
                if (u == null) {
                    return "";
                }
                String n = u.getNom() != null && !u.getNom().isBlank() ? u.getNom() : u.getEmail();
                return n + " (" + u.getEmail() + ")";
            }

            @Override
            public User fromString(String s) {
                return null;
            }
        };
        formUserCombo.setConverter(conv);
        formUserCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : conv.toString(u));
            }
        });
        formUserCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                setText(empty || u == null ? null : conv.toString(u));
            }
        });
    }

    private void buildTableColumns() {
        TableColumn<Transaction, Transaction> numCol = new TableColumn<>("#");
        numCol.setPrefWidth(44);
        numCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        numCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });

        TableColumn<Transaction, String> userCol = new TableColumn<>("USER");
        userCol.setPrefWidth(180);
        userCol.setCellValueFactory(c -> {
            User u = c.getValue().getUser();
            if (u == null) {
                return new SimpleStringProperty("-");
            }
            String name = u.getNom() != null && !u.getNom().isBlank() ? u.getNom() : u.getEmail();
            return new SimpleStringProperty(name + " — " + u.getEmail());
        });

        TableColumn<Transaction, String> typeCol = new TableColumn<>("TYPE");
        typeCol.setPrefWidth(100);
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<Transaction, String> amountCol = new TableColumn<>("AMOUNT");
        amountCol.setPrefWidth(100);
        amountCol.setCellValueFactory(c -> new SimpleStringProperty(
                moneyFormat.format(c.getValue().getMontant()) + " DT"));

        TableColumn<Transaction, String> dateCol = new TableColumn<>("DATE");
        dateCol.setPrefWidth(96);
        dateCol.setCellValueFactory(c -> {
            LocalDate d = c.getValue().getDate();
            return new SimpleStringProperty(d == null ? "-" : d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });

        TableColumn<Transaction, String> descCol = new TableColumn<>("DESCRIPTION");
        descCol.setPrefWidth(140);
        descCol.setCellValueFactory(c -> {
            String d = c.getValue().getDescription();
            return new SimpleStringProperty(d == null || d.isBlank() ? "-" : d);
        });

        TableColumn<Transaction, String> sourceCol = new TableColumn<>("SOURCE");
        sourceCol.setPrefWidth(120);
        sourceCol.setCellValueFactory(c -> {
            String s = c.getValue().getModuleSource();
            return new SimpleStringProperty(s == null || s.isBlank() ? "-" : s.replace('_', ' '));
        });

        TableColumn<Transaction, String> balanceCol = new TableColumn<>("BALANCE");
        balanceCol.setPrefWidth(110);
        balanceCol.setCellValueFactory(c -> {
            User u = c.getValue().getUser();
            double b = u != null ? u.getSoldeTotal() : 0;
            return new SimpleStringProperty(moneyFormat.format(b) + " DT");
        });

        TableColumn<Transaction, Void> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setPrefWidth(160);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final HBox box = new HBox(6, viewBtn, editBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                viewBtn.getStyleClass().add("tx-mini-outline");
                editBtn.getStyleClass().add("tx-mini-outline-secondary");
                viewBtn.setOnAction(e -> {
                    Transaction t = getTableView().getItems().get(getIndex());
                    if (t != null) {
                        showTransactionDetails(t);
                    }
                });
                editBtn.setOnAction(e -> showInfo("Edit", "L’édition détaillée sera branchée sur un formulaire (même logique que Symfony)."));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        transactionTable.getColumns().setAll(
                numCol, userCol, typeCol, amountCol, dateCol, descCol, sourceCol, balanceCol, actionsCol
        );
    }

    public void setContext(User adminUser) {
        if (adminUser == null) {
            return;
        }
        this.currentUser = adminUser;
        userLabel.setText(adminUser.getNom() != null && !adminUser.getNom().isBlank() ? adminUser.getNom() : adminUser.getEmail());
        roleLabel.setText("Admin Panel");
        reloadUsersCombo();
        applyFiltersAndRefresh();
    }

    private void reloadUsersCombo() {
        List<User> users = userService.findForAdminIndex("", "", "nom", "ASC");
        formUserCombo.setItems(FXCollections.observableArrayList(users));
        if (!users.isEmpty()) {
            formUserCombo.getSelectionModel().selectFirst();
        }
    }

    @FXML
    private void handleNavUsers() {
        if (currentUser == null) {
            return;
        }
        try {
            AdminNavigation.showUsersManagement((Stage) userLabel.getScene().getWindow(), currentUser);
        } catch (Exception e) {
            showError("Navigation", e.getMessage() != null ? e.getMessage() : String.valueOf(e));
        }
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
                handleNavUsers();
            }
        }
    }

    @FXML
    private void handleApplyFilters() {
        applyFiltersAndRefresh();
    }

    @FXML
    private void handleResetFilters() {
        filterUserField.clear();
        filterTypeCombo.setValue("All types");
        filterDateFrom.setValue(null);
        filterDateTo.setValue(null);
        filterSortCombo.setValue("Sort by date");
        filterOrderCombo.setValue("DESC");
        applyFiltersAndRefresh();
    }

    @FXML
    private void handleAddTransaction() {
        User u = formUserCombo.getSelectionModel().getSelectedItem();
        if (u == null) {
            showError("Transaction", "Sélectionnez un utilisateur.");
            return;
        }
        String type = formTypeCombo.getValue();
        if (type == null) {
            showError("Transaction", "Sélectionnez un type.");
            return;
        }
        double amount;
        try {
            String raw = formAmountField.getText() == null ? "0" : formAmountField.getText().trim().replace(',', '.');
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            showError("Transaction", "Montant invalide.");
            return;
        }
        if (amount <= 0 || amount > 10_000_000) {
            showError("Transaction", "Le montant doit etre > 0 et raisonnable.");
            return;
        }
        LocalDate d = formDatePicker.getValue();
        if (d == null) {
            showError("Transaction", "Choisissez une date.");
            return;
        }
        if (d.isAfter(LocalDate.now())) {
            showError("Transaction", "La date ne peut pas être dans le futur.");
            return;
        }
        String desc = formDescriptionArea.getText() == null ? "" : formDescriptionArea.getText().trim();
        String src = formSourceField.getText() == null ? "" : formSourceField.getText().trim();
        if (desc.length() > 255) {
            showError("Transaction", "Description trop longue (max 255 caracteres).");
            return;
        }
        if (!SOURCE_PATTERN.matcher(src).matches()) {
            showError("Transaction", "Source invalide (lettres/chiffres/espace/-/_, max 80).");
            return;
        }

        try {
            transactionService.insertTransactionForUser(
                    u.getId(),
                    type,
                    amount,
                    d,
                    desc,
                    src
            );
            showInfo("Transaction", "Transaction ajoutée.");
            formAmountField.setText("0.00");
            formDescriptionArea.clear();
            formSourceField.clear();
            formDatePicker.setValue(LocalDate.now());
            applyFiltersAndRefresh();
            reloadUsersCombo();
        } catch (Exception ex) {
            showError("Transaction", ex.getMessage() != null ? ex.getMessage() : String.valueOf(ex));
        }
    }

    private void applyFiltersAndRefresh() {
        String typeFilter = filterTypeCombo.getValue();
        if (typeFilter == null || "All types".equals(typeFilter)) {
            typeFilter = null;
        } else {
            typeFilter = typeFilter.toUpperCase(Locale.ROOT);
        }
        LocalDate from = filterDateFrom.getValue();
        LocalDate to = filterDateTo.getValue();
        if (from != null && to != null && from.isAfter(to)) {
            showError("Filtres", "Date from doit etre inferieure ou egale a Date to.");
            return;
        }
        String userNom = filterUserField.getText() == null ? "" : filterUserField.getText().trim();
        if (userNom.length() > 80) {
            showError("Filtres", "Nom utilisateur trop long (max 80).");
            return;
        }
        if (userNom.isEmpty()) {
            userNom = null;
        }
        String sortBy = mapTxSort(filterSortCombo.getValue());
        String order = "ASC".equalsIgnoreCase(filterOrderCombo.getValue()) ? "ASC" : "DESC";

        Map<String, Integer> kpi = transactionService.countGroupedByType(typeFilter, from, to, userNom);
        kpiExpenseLabel.setText(String.valueOf(kpi.getOrDefault("EXPENSE", 0)));
        kpiSavingLabel.setText(String.valueOf(kpi.getOrDefault("SAVING", 0)));
        kpiInvestmentLabel.setText(String.valueOf(kpi.getOrDefault("INVESTMENT", 0)));

        List<Transaction> rows = transactionService.findAllForAdmin(typeFilter, from, to, userNom, sortBy, order);
        transactionTable.setItems(FXCollections.observableArrayList(rows));
        transactionTable.refresh();
    }

    private String mapTxSort(String displaySort) {
        if (displaySort == null) {
            return "date";
        }
        return switch (displaySort) {
            case "Sort by user" -> "user";
            case "Sort by type" -> "type";
            case "Sort by amount" -> "amount";
            case "Sort by description" -> "description";
            case "Sort by source" -> "source";
            case "Sort by ID" -> "id";
            default -> "date";
        };
    }

    private void showTransactionDetails(Transaction t) {
        User u = t.getUser();
        String uline = u == null ? "-" : (u.getNom() != null ? u.getNom() : u.getEmail()) + " / " + (u != null ? u.getEmail() : "");
        String msg = "Type: " + t.getType()
                + "\nMontant: " + moneyFormat.format(t.getMontant()) + " DT"
                + "\nDate: " + (t.getDate() != null ? t.getDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-")
                + "\nUtilisateur: " + uline
                + "\nDescription: " + (t.getDescription() != null ? t.getDescription() : "-")
                + "\nSource: " + (t.getModuleSource() != null ? t.getModuleSource() : "-");
        showInfo("Transaction #" + t.getId(), msg);
    }

    private void showInfo(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/login-view.fxml");
            Parent root = (Parent) loader.getRoot();

            Stage stage = (Stage) userLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/login.css");

            stage.setUserData(null);
            stage.setTitle("User Secure Login");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de revenir vers la page login.", e);
        }
    }
}
