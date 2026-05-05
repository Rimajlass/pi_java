package pi.controllers.UserTransactionController;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import javafx.util.Duration;
import pi.entities.Transaction;
import pi.entities.User;
import pi.mains.Main;
import pi.services.UserTransactionService.TransactionService;
import pi.services.UserTransactionService.UserService;
import pi.tools.AdminNavigation;
import pi.tools.FxmlResources;
import pi.tools.TransactionDetailsDialog;
import pi.tools.UiDialog;

import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private VBox contentRoot;

    @FXML
    private VBox headerSection;

    @FXML
    private HBox kpiRow;

    @FXML
    private VBox kpiExpenseCard;

    @FXML
    private VBox kpiSavingCard;

    @FXML
    private VBox kpiInvestmentCard;

    @FXML
    private VBox filtersCard;

    @FXML
    private VBox createCard;

    @FXML
    private VBox tableCard;

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
    private Button resetFiltersButton;

    @FXML
    private Button applyFiltersButton;

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
    private Button addTransactionButton;

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
        try {
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
            wireButtonActions();
            buildTableColumns();
            reloadUsersCombo();
            applyFiltersAndRefresh();
            initializeVisualEnhancements();
            Platform.runLater(this::playSectionIntroAnimations);
        } catch (Exception e) {
            safeFallbackState();
            System.err.println("[TransactionsManagement] initialize failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void wireButtonActions() {
        if (applyFiltersButton != null) {
            applyFiltersButton.setOnAction(e -> handleApplyFilters());
        }
        if (resetFiltersButton != null) {
            resetFiltersButton.setOnAction(e -> handleResetFilters());
        }
        if (addTransactionButton != null) {
            addTransactionButton.setOnAction(e -> handleAddTransaction());
        }
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
            return new SimpleStringProperty(name + " - " + u.getEmail());
        });

        TableColumn<Transaction, String> typeCol = new TableColumn<>("TYPE");
        typeCol.setPrefWidth(100);
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) {
                    setGraphic(null);
                    return;
                }
                String type = item.toUpperCase(Locale.ROOT);
                badge.setText(type);
                badge.getStyleClass().setAll("tx-type-badge", mapTypeBadgeClass(type), mapBadgeClass(type));
                setGraphic(badge);
            }
        });

        TableColumn<Transaction, Transaction> amountCol = new TableColumn<>("AMOUNT");
        amountCol.setPrefWidth(100);
        amountCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tx-amount-cell", "tx-amount-expense", "tx-amount-saving", "tx-amount-investment", "tx-amount-revenue");
                if (empty || item == null) {
                    setText(null);
                } else {
                    String type = item.getType() == null ? "" : item.getType().toUpperCase(Locale.ROOT);
                    setText(moneyFormat.format(item.getMontant()) + " DT");
                    getStyleClass().add("tx-amount-cell");
                    switch (type) {
                        case "EXPENSE" -> getStyleClass().add("tx-amount-expense");
                        case "SAVING" -> getStyleClass().add("tx-amount-saving");
                        case "INVESTMENT" -> getStyleClass().add("tx-amount-investment");
                        case "REVENUE", "SALARY" -> getStyleClass().add("tx-amount-revenue");
                        default -> { }
                    }
                }
            }
        });

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
        balanceCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    getStyleClass().remove("tx-balance-cell");
                } else {
                    setText(item);
                    if (!getStyleClass().contains("tx-balance-cell")) {
                        getStyleClass().add("tx-balance-cell");
                    }
                }
            }
        });

        TableColumn<Transaction, Void> actionsCol = new TableColumn<>("ACTIONS");
        actionsCol.setPrefWidth(160);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final HBox box = new HBox(6, viewBtn, editBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                viewBtn.getStyleClass().addAll("tx-mini-view", "button-outline");
                editBtn.getStyleClass().addAll("tx-mini-edit", "button-outline");
                setupButtonHoverMotion(viewBtn, false);
                setupButtonHoverMotion(editBtn, false);
                viewBtn.setOnAction(e -> {
                    Transaction t = getTableView().getItems().get(getIndex());
                    if (t != null) {
                        showTransactionDetails(t);
                    }
                });
                editBtn.setOnAction(e -> showInfo("Edit", "L'edition detaillee sera branchee sur un formulaire."));
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
        transactionTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Transaction item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tx-table-row-even", "tx-table-row-odd");
                if (!empty && item != null) {
                    getStyleClass().add(getIndex() % 2 == 0 ? "tx-table-row-even" : "tx-table-row-odd");
                }
            }
        });
    }

    private void initializeVisualEnhancements() {
        setupCardHoverMotion(kpiExpenseCard, Color.web("#FF6B6B"));
        setupCardHoverMotion(kpiSavingCard, Color.web("#22C55E"));
        setupCardHoverMotion(kpiInvestmentCard, Color.web("#8B5CF6"));
        setupCardHoverMotion(filtersCard, Color.web("#3A8DFF"));
        setupCardHoverMotion(createCard, Color.web("#3A8DFF"));
        setupCardHoverMotion(tableCard, Color.web("#3A8DFF"));

        setupButtonHoverMotion(resetFiltersButton, false);
        setupButtonHoverMotion(applyFiltersButton, false);
        setupButtonHoverMotion(addTransactionButton, true);

        List<Node> focusNodes = new ArrayList<>();
        focusNodes.add(filterUserField);
        focusNodes.add(filterTypeCombo);
        focusNodes.add(filterDateFrom);
        focusNodes.add(filterDateTo);
        focusNodes.add(filterSortCombo);
        focusNodes.add(filterOrderCombo);
        focusNodes.add(formUserCombo);
        focusNodes.add(formTypeCombo);
        focusNodes.add(formAmountField);
        focusNodes.add(formDatePicker);
        focusNodes.add(formDescriptionArea);
        focusNodes.add(formSourceField);
        focusNodes.forEach(this::setupFocusMotion);
    }

    private void playSectionIntroAnimations() {
        animateEntrance(headerSection, 0);
        animateEntrance(kpiRow, 70);
        animateEntrance(filtersCard, 140);
        animateEntrance(createCard, 210);
        animateEntrance(tableCard, 280);
    }

    private void animateEntrance(Node node, int delayMs) {
        if (node == null) {
            return;
        }
        node.setOpacity(0);
        node.setTranslateY(10);
        FadeTransition fade = new FadeTransition(Duration.millis(320), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(delayMs));

        TranslateTransition slide = new TranslateTransition(Duration.millis(320), node);
        slide.setFromY(10);
        slide.setToY(0);
        slide.setDelay(Duration.millis(delayMs));
        new ParallelTransition(fade, slide).play();
    }

    private void setupCardHoverMotion(Node node, Color glowColor) {
        if (node == null) {
            return;
        }
        DropShadow shadow = new DropShadow(18, glowColor.deriveColor(0, 1, 1, 0.26));
        shadow.setSpread(0.18);
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(140), node);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
            node.setEffect(shadow);
            node.setTranslateY(-1);
        });
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(140), node);
            st.setToX(1);
            st.setToY(1);
            st.play();
            node.setEffect(null);
            node.setTranslateY(0);
        });
    }

    private void setupButtonHoverMotion(Button button, boolean addGlow) {
        if (button == null) {
            return;
        }
        DropShadow glow = new DropShadow(20, Color.web("#3A8DFF66"));
        glow.setSpread(0.2);
        button.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(130), button);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
            if (addGlow) {
                button.setEffect(glow);
            }
        });
        button.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(130), button);
            st.setToX(1);
            st.setToY(1);
            st.play();
            if (addGlow) {
                button.setEffect(null);
            }
        });
    }

    private void setupFocusMotion(Node node) {
        if (node == null) {
            return;
        }
        node.focusedProperty().addListener((obs, oldVal, focused) -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
            st.setToX(focused ? 1.01 : 1);
            st.setToY(focused ? 1.01 : 1);
            st.play();
            if (focused) {
                DropShadow focusGlow = new DropShadow(14, Color.web("#3A8DFF55"));
                focusGlow.setSpread(0.16);
                node.setEffect(focusGlow);
            } else {
                node.setEffect(null);
            }
        });
    }

    private String mapTypeBadgeClass(String type) {
        return switch (type) {
            case "EXPENSE" -> "tx-type-expense";
            case "SAVING" -> "tx-type-saving";
            case "INVESTMENT" -> "tx-type-investment";
            case "REVENUE", "SALARY" -> "tx-type-revenue";
            default -> "tx-type-default";
        };
    }

    private String mapBadgeClass(String type) {
        return switch (type) {
            case "EXPENSE" -> "badge-expense";
            case "SAVING" -> "badge-saving";
            case "INVESTMENT" -> "badge-investment";
            default -> "badge-default";
        };
    }

    public void setContext(User adminUser) {
        if (adminUser == null) {
            return;
        }
        this.currentUser = adminUser;
        if (userLabel != null) {
            userLabel.setText(adminUser.getNom() != null && !adminUser.getNom().isBlank() ? adminUser.getNom() : adminUser.getEmail());
        }
        if (roleLabel != null) {
            roleLabel.setText("Admin Panel");
        }
        reloadUsersCombo();
        applyFiltersAndRefresh();
    }

    private void reloadUsersCombo() {
        try {
            List<User> users = userService.findForAdminIndex("", "", "nom", "ASC");
            formUserCombo.setItems(FXCollections.observableArrayList(users));
            if (!users.isEmpty()) {
                formUserCombo.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            formUserCombo.setItems(FXCollections.observableArrayList());
            System.err.println("[TransactionsManagement] reloadUsersCombo failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleNavUsers() {
        if (currentUser == null) {
            return;
        }
        try {
            Stage stage = resolveCurrentStage();
            if (stage == null) {
                showError("Navigation", "Fenetre introuvable.");
                return;
            }
            AdminNavigation.showUsersManagement(stage, currentUser);
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
            } else if ("Revenues".equalsIgnoreCase(key)) {
                openRevenueBackOffice();
            } else if ("Expenses".equalsIgnoreCase(key)) {
                openExpenseBackOffice();
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
            showError("Transaction", "Selectionnez un utilisateur.");
            return;
        }
        String type = formTypeCombo.getValue();
        if (type == null) {
            showError("Transaction", "Selectionnez un type.");
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
            showError("Transaction", "La date ne peut pas etre dans le futur.");
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
            showInfo("Transaction", "Transaction ajoutee.");
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
        try {
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
        } catch (Exception e) {
            safeFallbackState();
            System.err.println("[TransactionsManagement] applyFiltersAndRefresh failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void safeFallbackState() {
        if (kpiExpenseLabel != null) {
            kpiExpenseLabel.setText("0");
        }
        if (kpiSavingLabel != null) {
            kpiSavingLabel.setText("0");
        }
        if (kpiInvestmentLabel != null) {
            kpiInvestmentLabel.setText("0");
        }
        if (transactionTable != null) {
            transactionTable.setItems(FXCollections.observableArrayList());
        }
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
        Stage stage = resolveCurrentStage();
        if (stage == null) {
            showError("Transaction", "Fenetre introuvable.");
            return;
        }
        TransactionDetailsDialog.show(stage, t);
    }

    private void openRevenueBackOffice() {
        try {
            Stage stage = resolveCurrentStage();
            if (stage == null) {
                showError("Navigation error", "Unable to resolve current window.");
                return;
            }
            Parent root = FXMLLoader.load(Main.class.getResource("/Expense/Revenue/BACK/revenue-back-view.fxml"));
            stage.setTitle("Revenue Back Office");
            stage.setScene(new Scene(root, 1400, 900));
            stage.show();
        } catch (IOException e) {
            showError("Navigation error", "Unable to open revenue back office:\n" + String.valueOf(e.getMessage()));
        }
    }

    private void openExpenseBackOffice() {
        try {
            Stage stage = resolveCurrentStage();
            if (stage == null) {
                showError("Navigation error", "Unable to resolve current window.");
                return;
            }
            Parent root = FXMLLoader.load(Main.class.getResource("/Expense/Revenue/BACK/expense-back-view.fxml"));
            stage.setTitle("Expense Back Office");
            stage.setScene(new Scene(root, 1400, 900));
            stage.show();
        } catch (IOException e) {
            showError("Navigation error", "Unable to open expense back office:\n" + String.valueOf(e.getMessage()));
        }
    }

    private void showInfo(String title, String content) {
        Stage stage = resolveCurrentStage();
        if (stage != null) {
            UiDialog.show(stage, UiDialog.Type.SUCCESS, title, title, content);
        } else {
            System.err.println("[TransactionsManagement] showInfo skipped (stage unresolved): " + title + " / " + content);
        }
    }

    private void showError(String title, String content) {
        Stage stage = resolveCurrentStage();
        if (stage != null) {
            UiDialog.show(stage, UiDialog.Type.ERROR, title, title, content);
        } else {
            System.err.println("[TransactionsManagement] showError skipped (stage unresolved): " + title + " / " + content);
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/login-view.fxml");
            Parent root = (Parent) loader.getRoot();

            Stage stage = resolveCurrentStage();
            if (stage == null) {
                throw new IllegalStateException("Fenetre introuvable.");
            }
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

    private Stage resolveCurrentStage() {
        Stage fromControls = resolveStageFromControls(
                contentRoot, transactionTable, addTransactionButton, applyFiltersButton, resetFiltersButton,
                filterUserField, formAmountField, formSourceField, formDescriptionArea, formUserCombo, formTypeCombo
        );
        if (fromControls != null) {
            return fromControls;
        }
        if (contentRoot != null && contentRoot.getScene() != null && contentRoot.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        if (transactionTable != null && transactionTable.getScene() != null && transactionTable.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        if (userLabel != null && userLabel.getScene() != null && userLabel.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    private Stage resolveStageFromControls(Node... nodes) {
        if (nodes == null) {
            return null;
        }
        for (Node node : nodes) {
            if (node != null && node.getScene() != null && node.getScene().getWindow() instanceof Stage stage) {
                return stage;
            }
        }
        return null;
    }
}

