package pi.controllers.UserTransactionController;

import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public class DecideDashboardController {

    @FXML
    private Button myProfileButton;
    @FXML
    private Button dashboardButton;
    @FXML
    private Button transactionsButton;
    @FXML
    private Button analyticsButton;
    @FXML
    private Button aiInsightsButton;
    @FXML
    private Button converterNavButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button notificationsButton;
    @FXML
    private Button backButton;
    @FXML
    private Button logoutButton;
    @FXML
    private Button refreshTransactionsButton;
    @FXML
    private Button convertButton;

    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label balanceValueLabel;
    @FXML
    private Label balanceRateLabel;
    @FXML
    private Label conversionResultLabel;

    @FXML
    private ComboBox<String> balanceCurrencyCombo;
    @FXML
    private ComboBox<String> fromCurrencyCombo;
    @FXML
    private ComboBox<String> toCurrencyCombo;
    @FXML
    private TextField amountInputField;

    @FXML
    private TableView<TransactionRow> transactionsTable;
    @FXML
    private TableColumn<TransactionRow, String> typeColumn;
    @FXML
    private TableColumn<TransactionRow, String> amountColumn;
    @FXML
    private TableColumn<TransactionRow, String> dateColumn;
    @FXML
    private TableColumn<TransactionRow, String> descriptionColumn;

    private final DecimalFormat amountFormat = new DecimalFormat("#,##0.00");
    private final Map<String, Double> usdPerCurrency = new LinkedHashMap<>();
    private double baseBalanceUsd = 12450.00;

    @FXML
    public void initialize() {
        initRates();
        initProfile();
        initCombos();
        initTable();
        initInteractions();
    }

    private void initRates() {
        usdPerCurrency.put("USD", 1.0000);
        usdPerCurrency.put("EUR", 1.0800);
        usdPerCurrency.put("TND", 0.3442);
        usdPerCurrency.put("GBP", 1.2700);
        usdPerCurrency.put("AED", 0.2723);
    }

    private void initProfile() {
        nameLabel.setText("Zakaria");
        emailLabel.setText("zakaria@finance.com");
    }

    private void initCombos() {
        var currencies = FXCollections.observableArrayList(usdPerCurrency.keySet());

        balanceCurrencyCombo.setItems(currencies);
        fromCurrencyCombo.setItems(currencies);
        toCurrencyCombo.setItems(currencies);

        balanceCurrencyCombo.getSelectionModel().select("USD");
        fromCurrencyCombo.getSelectionModel().select("USD");
        toCurrencyCombo.getSelectionModel().select("EUR");

        updateBalanceDisplay();
        updateConverterPreview();

        balanceCurrencyCombo.setOnAction(e -> updateBalanceDisplay());
        fromCurrencyCombo.setOnAction(e -> updateConverterPreview());
        toCurrencyCombo.setOnAction(e -> updateConverterPreview());
        amountInputField.textProperty().addListener((obs, o, n) -> updateConverterPreview());
    }

    private void initTable() {
        typeColumn.setCellValueFactory(c -> c.getValue().typeProperty());
        amountColumn.setCellValueFactory(c -> c.getValue().amountProperty());
        dateColumn.setCellValueFactory(c -> c.getValue().dateProperty());
        descriptionColumn.setCellValueFactory(c -> c.getValue().descriptionProperty());

        transactionsTable.setItems(FXCollections.observableArrayList(
                new TransactionRow("EXPENSE", "320.00 TND", "26/04/2026", "Supermarket and groceries"),
                new TransactionRow("SAVING", "500.00 TND", "25/04/2026", "Monthly savings transfer"),
                new TransactionRow("EXPENSE", "89.00 TND", "24/04/2026", "Streaming + cloud subscriptions"),
                new TransactionRow("INVEST", "250.00 TND", "23/04/2026", "ETF auto-invest")
        ));
    }

    private void initInteractions() {
        wireHoverScale(myProfileButton, dashboardButton, transactionsButton, analyticsButton, aiInsightsButton,
                converterNavButton, settingsButton, notificationsButton, backButton, logoutButton,
                refreshTransactionsButton, convertButton);

        refreshTransactionsButton.setOnAction(e -> transactionsTable.refresh());
    }

    @FXML
    private void handleConvert() {
        updateConverterPreview();
    }

    private void updateBalanceDisplay() {
        String targetCurrency = balanceCurrencyCombo.getValue();
        double converted = convert(baseBalanceUsd, "USD", targetCurrency);
        double rate = convert(1.0, "USD", targetCurrency);

        balanceValueLabel.setText(amountFormat.format(converted));
        balanceRateLabel.setText("1 USD = " + amountFormat.format(rate) + " " + targetCurrency);
    }

    private void updateConverterPreview() {
        String from = fromCurrencyCombo.getValue();
        String to = toCurrencyCombo.getValue();
        if (from == null || to == null) {
            return;
        }

        double amount;
        try {
            String raw = amountInputField.getText() == null ? "" : amountInputField.getText().trim().replace(',', '.');
            amount = raw.isEmpty() ? 0.0 : Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            conversionResultLabel.setText("Invalid amount");
            return;
        }

        double result = convert(amount, from, to);
        conversionResultLabel.setText(
                amountFormat.format(amount) + " " + from + " = " + amountFormat.format(result) + " " + to
        );
    }

    private double convert(double amount, String from, String to) {
        Double fromRate = usdPerCurrency.get(from);
        Double toRate = usdPerCurrency.get(to);
        if (fromRate == null || toRate == null || toRate == 0.0) {
            return amount;
        }
        return amount * fromRate / toRate;
    }

    private void wireHoverScale(Node... nodes) {
        for (Node node : nodes) {
            if (node == null) {
                continue;
            }
            node.setOnMouseEntered(e -> animateScale(node, 1.03));
            node.setOnMouseExited(e -> animateScale(node, 1.0));
        }
    }

    private void animateScale(Node node, double scale) {
        ScaleTransition st = new ScaleTransition(Duration.millis(130), node);
        st.setToX(scale);
        st.setToY(scale);
        st.playFromStart();
    }

    public static class TransactionRow {
        private final SimpleStringProperty type;
        private final SimpleStringProperty amount;
        private final SimpleStringProperty date;
        private final SimpleStringProperty description;

        public TransactionRow(String type, String amount, String date, String description) {
            this.type = new SimpleStringProperty(type);
            this.amount = new SimpleStringProperty(amount);
            this.date = new SimpleStringProperty(date);
            this.description = new SimpleStringProperty(description);
        }

        public SimpleStringProperty typeProperty() {
            return type;
        }

        public SimpleStringProperty amountProperty() {
            return amount;
        }

        public SimpleStringProperty dateProperty() {
            return date;
        }

        public SimpleStringProperty descriptionProperty() {
            return description;
        }
    }
}
