package pi.controllers.UserTransactionController;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import pi.entities.Transaction;
import pi.entities.User;
import pi.mains.Main;
import pi.services.UserTransactionService.TransactionService;
import pi.tools.FxmlResources;
import pi.tools.ThemeManager;
import pi.tools.TransactionDetailsDialog;
import pi.tools.UiDialog;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SalaryProfileController {

    @FXML
    private Label profileInitialLabel;
    @FXML
    private Label profileNameLabel;
    @FXML
    private Label profileEmailLabel;
    @FXML
    private Label locationFlagLabel;
    @FXML
    private Label locationTextLabel;
    @FXML
    private Label headerBalanceLabel;
    @FXML
    private Label liveBalanceLabel;
    @FXML
    private Label liveCurrencyBadgeLabel;
    @FXML
    private Label sideRateLabel;
    @FXML
    private Label metricExpenseLabel;
    @FXML
    private Label metricSavingsLabel;
    @FXML
    private Label txCountLabel;
    @FXML
    private Label converterRateLabel;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField passwordField;
    @FXML
    private TextField conversionAmountField;
    @FXML
    private TableView<Transaction> txTable;
    @FXML
    private ComboBox<String> countryComboBox;
    @FXML
    private ComboBox<String> liveCurrencyCombo;
    @FXML
    private ComboBox<String> fromCurrencyCombo;
    @FXML
    private ComboBox<String> toCurrencyCombo;
    @FXML
    private ScrollPane salaryProfileScrollPane;
    @FXML
    private VBox profileCardSection;
    @FXML
    private VBox transactionsSectionCard;
    @FXML
    private VBox insightsSectionCard;
    @FXML
    private VBox converterSectionCard;
    @FXML
    private StackPane darkToggleShell;
    @FXML
    private Region darkToggleKnob;
    @FXML
    private Button saveProfileButton;
    @FXML
    private Button nearbyMapButton;
    @FXML
    private AnchorPane salaryProfileRoot;

    private final TransactionService transactionService = new TransactionService();
    private final UserController userController = new UserController();
    private final DecimalFormat money = new DecimalFormat("#,##0.00");
    private final DecimalFormat rateFormat = new DecimalFormat("0.0000");
    private final DecimalFormat amountFormat = new DecimalFormat("#,##0.00");
    private User currentUser;
    private double baseBalanceTnd = 0.0;
    private String selectedProfileImagePath;

    private static final Map<String, String> COUNTRY_TO_CURRENCY = new LinkedHashMap<>();
    private static final Map<String, String> COUNTRY_TO_FLAG = new LinkedHashMap<>();
    private static final Map<String, String> FLAG_TO_CURRENCY = new LinkedHashMap<>();
    private static final Map<String, Double> USD_PER_CURRENCY = new LinkedHashMap<>();

    static {
        COUNTRY_TO_CURRENCY.put("Tunisia", "TND");
        COUNTRY_TO_CURRENCY.put("France", "EUR");
        COUNTRY_TO_CURRENCY.put("United States", "USD");
        COUNTRY_TO_CURRENCY.put("United Kingdom", "GBP");
        COUNTRY_TO_CURRENCY.put("Morocco", "MAD");
        COUNTRY_TO_CURRENCY.put("Algeria", "DZD");
        COUNTRY_TO_CURRENCY.put("Canada", "CAD");
        COUNTRY_TO_CURRENCY.put("Japan", "JPY");
        COUNTRY_TO_CURRENCY.put("Saudi Arabia", "SAR");
        COUNTRY_TO_CURRENCY.put("United Arab Emirates", "AED");
        COUNTRY_TO_FLAG.put("Tunisia", "🇹🇳");
        COUNTRY_TO_FLAG.put("France", "🇫🇷");
        COUNTRY_TO_FLAG.put("United States", "🇺🇸");
        COUNTRY_TO_FLAG.put("United Kingdom", "🇬🇧");
        COUNTRY_TO_FLAG.put("Morocco", "🇲🇦");
        COUNTRY_TO_FLAG.put("Algeria", "🇩🇿");
        COUNTRY_TO_FLAG.put("Canada", "🇨🇦");
        COUNTRY_TO_FLAG.put("Japan", "🇯🇵");
        COUNTRY_TO_FLAG.put("Saudi Arabia", "🇸🇦");
        COUNTRY_TO_FLAG.put("United Arab Emirates", "🇦🇪");

        USD_PER_CURRENCY.put("USD", 1.0000);
        USD_PER_CURRENCY.put("EUR", 1.0800);
        USD_PER_CURRENCY.put("TND", 0.3442);
        USD_PER_CURRENCY.put("GBP", 1.2700);
        USD_PER_CURRENCY.put("MAD", 0.1010);
        USD_PER_CURRENCY.put("DZD", 0.0074);
        USD_PER_CURRENCY.put("CAD", 0.7400);
        USD_PER_CURRENCY.put("JPY", 0.0067);
        USD_PER_CURRENCY.put("SAR", 0.2667);
        USD_PER_CURRENCY.put("AED", 0.2723);

        for (Map.Entry<String, String> entry : COUNTRY_TO_CURRENCY.entrySet()) {
            String country = entry.getKey();
            String currency = entry.getValue();
            String flag = COUNTRY_TO_FLAG.get(country);
            if (flag != null) {
                FLAG_TO_CURRENCY.put(flag, currency);
            }
        }
    }

    @FXML
    public void initialize() {
        buildTable();
        initializeCountryAndCurrencyTools();
        Platform.runLater(() -> {
            Stage stage = resolveStage();
            if (stage != null && stage.getScene() != null) {
                ThemeManager.registerScene(stage.getScene());
                updateDarkToggleVisual(ThemeManager.isDarkMode(stage.getScene()));
                applyDashboardModeClass();
                stage.getScene().getStylesheets().addListener((javafx.collections.ListChangeListener<String>) change -> applyDashboardModeClass());
            } else {
                updateDarkToggleVisual(ThemeManager.isDarkSelected());
                applyDashboardModeClass();
            }
            applyVisualAnimations();
        });
    }

    public void setUser(User user) {
        if (user == null) {
            return;
        }
        this.currentUser = user;
        String name = user.getNom() == null || user.getNom().isBlank() ? "User" : user.getNom();
        String email = user.getEmail() == null ? "-" : user.getEmail();
        String initial = name.substring(0, 1).toUpperCase();

        profileInitialLabel.setText(initial);
        profileNameLabel.setText(name);
        profileEmailLabel.setText(email);
        locationFlagLabel.setText(toFlagEmoji(user.getGeoCountryCode()));
        locationTextLabel.setText(buildLocationText(user));
        headerBalanceLabel.setText(money.format(user.getSoldeTotal()) + " TND");
        baseBalanceTnd = user.getSoldeTotal();

        fullNameField.setText(name);
        emailField.setText(email);
        passwordField.clear();
        selectedProfileImagePath = user.getImage();
        selectCountryFromUser(user);
        refreshLiveBalance();
        handleConvert();

        loadTransactions(user.getId());
    }

    private void loadTransactions(int userId) {
        List<Transaction> rows = transactionService.findByUserId(userId);
        txTable.setItems(FXCollections.observableArrayList(rows));

        double totalExpense = 0;
        double totalSavings = 0;
        for (Transaction tx : rows) {
            if ("EXPENSE".equalsIgnoreCase(tx.getType())) {
                totalExpense += tx.getMontant();
            } else if ("SAVING".equalsIgnoreCase(tx.getType())) {
                totalSavings += tx.getMontant();
            }
        }

        metricExpenseLabel.setText(money.format(totalExpense) + " TND");
        metricSavingsLabel.setText(money.format(totalSavings) + " TND");
        txCountLabel.setText(String.valueOf(rows.size()));
    }

    private void buildTable() {
        TableColumn<Transaction, String> typeCol = new TableColumn<>("TYPE");
        typeCol.setPrefWidth(130);
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("tx-type-expense", "tx-type-saving", "tx-type-info");
                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    return;
                }
                String normalized = item.trim().toUpperCase();
                setText(normalized);
                if ("EXPENSE".equals(normalized)) {
                    getStyleClass().add("tx-type-expense");
                } else if ("SAVING".equals(normalized)) {
                    getStyleClass().add("tx-type-saving");
                } else {
                    getStyleClass().add("tx-type-info");
                }
            }
        });

        TableColumn<Transaction, String> amountCol = new TableColumn<>("AMOUNT");
        amountCol.setPrefWidth(140);
        amountCol.setCellValueFactory(c -> new SimpleStringProperty(money.format(c.getValue().getMontant()) + " TND"));

        TableColumn<Transaction, String> dateCol = new TableColumn<>("DATE");
        dateCol.setPrefWidth(120);
        dateCol.setCellValueFactory(c -> {
            LocalDate date = c.getValue().getDate();
            return new SimpleStringProperty(date == null ? "-" : date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        });

        TableColumn<Transaction, String> descCol = new TableColumn<>("DESCRIPTION");
        descCol.setPrefWidth(420);
        descCol.setCellValueFactory(c -> {
            String desc = c.getValue().getDescription();
            return new SimpleStringProperty(desc == null || desc.isBlank() ? "-" : desc);
        });

        txTable.getColumns().setAll(typeCol, amountCol, dateCol, descCol);
        Label placeholder = new Label("No transactions found.\nOnce you make transactions, they will appear here.");
        placeholder.getStyleClass().add("tx-empty-placeholder");
        txTable.setPlaceholder(placeholder);
        txTable.setRowFactory(table -> {
            javafx.scene.control.TableRow<Transaction> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Stage stage = resolveStage();
                    if (stage != null) {
                        TransactionDetailsDialog.show(stage, row.getItem());
                    }
                }
            });
            return row;
        });
    }

    private void initializeCountryAndCurrencyTools() {
        List<String> flags = new ArrayList<>(FLAG_TO_CURRENCY.keySet());
        List<String> currencies = new ArrayList<>(USD_PER_CURRENCY.keySet());

        countryComboBox.setItems(FXCollections.observableArrayList(flags));
        liveCurrencyCombo.setItems(FXCollections.observableArrayList(flags));
        fromCurrencyCombo.setItems(FXCollections.observableArrayList(currencies));
        toCurrencyCombo.setItems(FXCollections.observableArrayList(currencies));

        if (!flags.isEmpty()) {
            countryComboBox.getSelectionModel().select("🇹🇳");
        }
        fromCurrencyCombo.getSelectionModel().select("USD");
        toCurrencyCombo.getSelectionModel().select("EUR");
        liveCurrencyCombo.getSelectionModel().select("🇹🇳");

        countryComboBox.setOnAction(event -> handleCountryChanged());
        fromCurrencyCombo.setOnAction(event -> handleConvert());
        toCurrencyCombo.setOnAction(event -> handleConvert());
        conversionAmountField.textProperty().addListener((obs, oldVal, newVal) -> handleConvert());
    }

    private void selectCountryFromUser(User user) {
        if (user == null) {
            return;
        }

        String country = safeTrim(user.getGeoCountryName());
        if (country == null) {
            country = resolveCountryByCode(user.getGeoCountryCode());
        }

        if (country != null && COUNTRY_TO_CURRENCY.containsKey(country)) {
            String flag = COUNTRY_TO_FLAG.get(country);
            if (flag != null) {
                countryComboBox.getSelectionModel().select(flag);
                handleCountryChanged();
            }
        }
    }

    private String resolveCountryByCode(String code) {
        if (code == null) {
            return null;
        }
        return switch (code.trim().toUpperCase()) {
            case "TN" -> "Tunisia";
            case "FR" -> "France";
            case "US" -> "United States";
            case "GB" -> "United Kingdom";
            case "MA" -> "Morocco";
            case "DZ" -> "Algeria";
            case "CA" -> "Canada";
            case "JP" -> "Japan";
            case "SA" -> "Saudi Arabia";
            case "AE" -> "United Arab Emirates";
            default -> null;
        };
    }

    @FXML
    private void handleCountryChanged() {
        String selectedFlag = countryComboBox.getValue();
        if (selectedFlag == null) {
            return;
        }

        String currency = FLAG_TO_CURRENCY.get(selectedFlag);
        if (currency == null) {
            return;
        }

        liveCurrencyCombo.getSelectionModel().select(selectedFlag);
        toCurrencyCombo.getSelectionModel().select(currency);
        refreshLiveBalance();
        handleConvert();
    }

    @FXML
    private void handleLiveCurrencyChanged() {
        refreshLiveBalance();
    }

    private void refreshLiveBalance() {
        String selectedFlag = liveCurrencyCombo.getValue();
        String selectedCurrency = selectedFlag == null ? null : FLAG_TO_CURRENCY.get(selectedFlag);
        if (selectedCurrency == null) {
            selectedCurrency = "TND";
            selectedFlag = "🇹🇳";
        }

        double converted = convert(baseBalanceTnd, "TND", selectedCurrency);
        liveBalanceLabel.setText(amountFormat.format(converted));
        liveCurrencyBadgeLabel.setText(selectedFlag + " " + selectedCurrency);

        double rate = convert(1.0, "TND", selectedCurrency);
        sideRateLabel.setText("1 TND = " + rateFormat.format(rate) + " " + selectedCurrency);
    }

    @FXML
    private void handleConvert() {
        String from = fromCurrencyCombo.getValue();
        String to = toCurrencyCombo.getValue();
        if (from == null || to == null) {
            return;
        }

        double amount;
        try {
            String raw = conversionAmountField.getText() == null ? "" : conversionAmountField.getText().trim().replace(',', '.');
            amount = raw.isEmpty() ? 0.0 : Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            converterRateLabel.setText("Invalid amount.");
            return;
        }

        double converted = convert(amount, from, to);
        double unitRate = convert(1.0, from, to);
        converterRateLabel.setText(amountFormat.format(amount) + " " + from + " = "
                + amountFormat.format(converted) + " " + to
                + "   |   1 " + from + " = " + rateFormat.format(unitRate) + " " + to);
    }

    private double convert(double amount, String from, String to) {
        Double fromUsd = USD_PER_CURRENCY.get(from);
        Double toUsd = USD_PER_CURRENCY.get(to);
        if (fromUsd == null || toUsd == null || toUsd == 0.0) {
            return amount;
        }
        return amount * fromUsd / toUsd;
    }

    @FXML
    private void handleSwapCurrencies() {
        String from = fromCurrencyCombo.getValue();
        String to = toCurrencyCombo.getValue();
        fromCurrencyCombo.getSelectionModel().select(to);
        toCurrencyCombo.getSelectionModel().select(from);
        handleConvert();
    }

    @FXML
    private void handleChooseProfileImage() {
        Stage stage = resolveStage();
        if (stage == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose profile image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        java.io.File selected = chooser.showOpenDialog(stage);
        if (selected == null) {
            return;
        }
        selectedProfileImagePath = selected.getAbsolutePath();
        UiDialog.show(stage, UiDialog.Type.SUCCESS, "Profile image", "Profile image selected", selected.getName());
    }

    @FXML
    private void handleSaveProfile() {
        if (currentUser == null) {
            return;
        }
        Stage stage = resolveStage();
        try {
            User updated = copyUser(currentUser);
            updated.setNom(fullNameField.getText() == null ? "" : fullNameField.getText().trim());
            updated.setEmail(emailField.getText() == null ? "" : emailField.getText().trim());
            updated.setImage(selectedProfileImagePath);

            String newPassword = passwordField.getText() == null ? "" : passwordField.getText().trim();
            User saved = userController.edit(updated, newPassword.isBlank() ? null : newPassword);
            currentUser = saved;
            setUser(saved);
            if (stage != null) {
                UiDialog.show(stage, UiDialog.Type.SUCCESS, "Profile", "Profile updated", "Your profile has been saved.");
            }
        } catch (Exception e) {
            if (stage != null) {
                UiDialog.show(stage, UiDialog.Type.ERROR, "Profile", "Save failed", e.getMessage());
            }
        }
    }

    @FXML
    private void handleSyncCurrencyWidgets() {
        refreshLiveBalance();
        handleConvert();
        Stage stage = resolveStage();
        if (stage != null) {
            UiDialog.show(stage, UiDialog.Type.SUCCESS, "Currency sync", "Sync complete", "Currency widgets have been refreshed.");
        }
    }

    @FXML
    private void handleViewAllTransactions() {
        scrollToNode(txTable);
        Stage stage = resolveStage();
        if (stage != null) {
            UiDialog.show(stage, UiDialog.Type.INFO, "Transactions", "Tip", "Double-click a row to open transaction details.");
        }
    }

    @FXML
    private void handleGoProfileSection() {
        scrollToNode(profileCardSection);
    }

    @FXML
    private void handleGoTransactionsSection() {
        scrollToNode(transactionsSectionCard);
    }

    @FXML
    private void handleGoAnalyticsSection() {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/analytics-view.fxml");
            Parent root = loader.getRoot();
            AnalyticsController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            Stage stage = resolveStage();
            if (stage == null) {
                return;
            }
            if (currentUser != null) {
                stage.setUserData(currentUser);
            }

            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/analytics.css").toExternalForm());
            ThemeManager.registerScene(scene);
            stage.setTitle("Analytics");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            Stage stage = resolveStage();
            if (stage != null) {
                UiDialog.show(stage, UiDialog.Type.ERROR, "Analytics", "Navigation failed", e.getMessage());
            }
        }
    }

    @FXML
    private void handleGoInsightsSection() {
        scrollToNode(insightsSectionCard);
    }

    @FXML
    private void handleGoConverterSection() {
        scrollToNode(converterSectionCard);
    }

    @FXML
    private void handleOpenSettings() {
        handleDarkModeToggle();
        Stage stage = resolveStage();
        if (stage != null) {
            UiDialog.show(stage, UiDialog.Type.INFO, "Settings", "Theme switched", "Use the dark mode toggle to switch theme.");
        }
    }

    @FXML
    private void handleOpenContactSupport() {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/contact-view.fxml");
            Parent root = loader.getRoot();
            ContactController controller = loader.getController();

            Stage stage = resolveStage();
            if (stage == null) {
                return;
            }
            if (currentUser != null) {
                controller.setUser(currentUser);
                stage.setUserData(currentUser);
            }

            Scene scene = new Scene(root, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/contact.css");
            ThemeManager.registerScene(scene);
            stage.setTitle("Contact");
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            Stage stage = resolveStage();
            if (stage != null) {
                UiDialog.show(stage, UiDialog.Type.ERROR, "Reclamations", "Navigation failed", e.getMessage());
            }
        }
    }

    @FXML
    private void handleUpgradeNow() {
        Stage stage = resolveStage();
        if (stage != null) {
            UiDialog.show(stage, UiDialog.Type.INFO, "Premium", "Premium plan", "Upgrade flow connected. Payment integration can be plugged here.");
        }
    }

    @FXML
    private void handleDarkModeToggle() {
        Stage stage = resolveStage();
        if (stage == null || stage.getScene() == null) {
            return;
        }
        boolean darkEnabled = ThemeManager.toggleTheme(stage.getScene());
        updateDarkToggleVisual(darkEnabled);
        applyDashboardModeClass();
    }

    private void applyDashboardModeClass() {
        if (salaryProfileRoot == null) {
            return;
        }
        Stage stage = resolveStage();
        boolean darkEnabled = stage != null && stage.getScene() != null
                ? ThemeManager.isDarkMode(stage.getScene())
                : ThemeManager.isDarkSelected();
        salaryProfileRoot.getStyleClass().removeAll("dark-mode-dashboard", "light-mode-dashboard");
        salaryProfileRoot.getStyleClass().add(darkEnabled ? "dark-mode-dashboard" : "light-mode-dashboard");
    }

    @FXML
    private void handleOpenLinkedIn() {
        openExternal("https://www.linkedin.com");
    }

    @FXML
    private void handleOpenGithub() {
        openExternal("https://github.com");
    }

    @FXML
    private void handleOpenX() {
        openExternal("https://x.com");
    }

    @FXML
    private void handleOpenMail() {
        openExternal("mailto:support@decide.local");
    }

    @FXML
    private void handleOpenNearbyBanksMap() {
        try {
            Stage owner = resolveStage();
            Stage dialog = new Stage();
            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.initModality(Modality.NONE);
            dialog.setTitle("Banques / DAB proches");

            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();
            webEngine.loadContent(buildNearbyBanksMapHtml(), "text/html");

            Scene scene = new Scene(webView, 980, 640);
            dialog.setScene(scene);
            dialog.show();
            dialog.toFront();
        } catch (Exception e) {
            Stage stage = resolveStage();
            if (stage != null) {
                UiDialog.show(stage, UiDialog.Type.ERROR, "Map", "Unable to open map", e.getMessage());
            }
        }
    }

    private String buildNearbyBanksMapHtml() {
        double[] fallback = resolveProfileFallbackCoordinates();
        String fallbackLabel = resolveProfileFallbackLabel();
        return """
                <!doctype html>
                <html lang="fr">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>Banques / DAB proches</title>
                  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
                  <style>
                    html, body { margin:0; padding:0; width:100%%; height:100%%; overflow:hidden; font-family:Segoe UI, Arial, sans-serif; background:#0b1220; color:#e2e8f0; }
                    #app { position:relative; width:100%%; height:100%%; }
                    #top { position:absolute; top:0; left:0; right:0; height:82px; padding:8px 12px; background:#0f172a; border-bottom:1px solid #334155; font-size:13px; z-index:1000; box-sizing:border-box; }
                    .row { display:flex; gap:8px; align-items:center; margin-bottom:6px; }
                    .row2 { display:flex; gap:8px; align-items:center; }
                    #map { position:absolute; top:82px; left:0; right:0; bottom:0; }
                    #status { color:#93c5fd; margin-left:8px; }
                    #placeInput { height:30px; border-radius:8px; border:1px solid #3b4e77; background:#111d36; color:#e2e8f0; padding:0 10px; width:270px; }
                    #findBtn { height:30px; border-radius:8px; border:1px solid #3b4e77; background:#1d4ed8; color:#fff; padding:0 12px; cursor:pointer; }
                  </style>
                </head>
                <body>
                  <div id="app">
                    <div id="top">
                      <div class="row">
                        <strong>Banques et distributeurs proches</strong>
                        <span id="status">Localisation en cours...</span>
                      </div>
                      <div class="row2">
                        <input id="placeInput" type="text" placeholder="Ex: Tunis, Tunisia" value="Tunis, Tunisia" />
                        <button id="findBtn" type="button">Rechercher cette zone</button>
                      </div>
                    </div>
                    <div id="map"></div>
                  </div>

                  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                  <script>
                    const statusEl = document.getElementById('status');
                    const FALLBACK_LAT = %f;
                    const FALLBACK_LON = %f;
                    const FALLBACK_LABEL = '%s';
                    const map = L.map('map').setView([FALLBACK_LAT, FALLBACK_LON], 13);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                      maxZoom: 19,
                      attribution: '&copy; OpenStreetMap contributors'
                    }).addTo(map);
                    window.addEventListener('load', () => setTimeout(() => map.invalidateSize(), 120));
                    window.addEventListener('resize', () => map.invalidateSize());

                    const userIcon = L.divIcon({
                      html: '<div style="width:14px;height:14px;border-radius:50%%;background:#22c55e;border:2px solid #fff;box-shadow:0 0 0 2px rgba(34,197,94,.35)"></div>',
                      className: '', iconSize: [18,18], iconAnchor: [9,9]
                    });
                    const bankIcon = L.divIcon({
                      html: '<div style="width:13px;height:13px;border-radius:50%%;background:#3b82f6;border:2px solid #fff"></div>',
                      className: '', iconSize: [17,17], iconAnchor: [8,8]
                    });
                    const atmIcon = L.divIcon({
                      html: '<div style="width:13px;height:13px;border-radius:50%%;background:#ef4444;border:2px solid #fff"></div>',
                      className: '', iconSize: [17,17], iconAnchor: [8,8]
                    });

                    function setStatus(msg) { statusEl.textContent = msg; }

                    async function loadNearby(lat, lon) {
                      setStatus('Recherche des banques/DAB...');
                      const query = `
                        [out:json][timeout:25];
                        (
                          node["amenity"="bank"](around:4000,${lat},${lon});
                          way["amenity"="bank"](around:4000,${lat},${lon});
                          relation["amenity"="bank"](around:4000,${lat},${lon});
                          node["amenity"="atm"](around:4000,${lat},${lon});
                          way["amenity"="atm"](around:4000,${lat},${lon});
                          relation["amenity"="atm"](around:4000,${lat},${lon});
                        );
                        out center tags;
                      `;

                      const resp = await fetch('https://overpass-api.de/api/interpreter', { method:'POST', body:query });
                      if (!resp.ok) throw new Error('Service cartographique indisponible');
                      const data = await resp.json();
                      const points = data.elements || [];

                      if (points.length === 0) {
                        setStatus('Aucun resultat dans un rayon de 4 km.');
                        return;
                      }

                      const bounds = [];
                      points.forEach((el) => {
                        const plat = el.lat ?? (el.center && el.center.lat);
                        const plon = el.lon ?? (el.center && el.center.lon);
                        if (plat == null || plon == null) return;
                        const kind = (el.tags && el.tags.amenity) || 'point';
                        const name = (el.tags && el.tags.name) || (kind === 'atm' ? 'DAB' : 'Banque');
                        L.marker([plat, plon], { icon: kind === 'atm' ? atmIcon : bankIcon })
                          .addTo(map)
                          .bindPopup(`<strong>${name}</strong><br/>Type: ${kind.toUpperCase()}`);
                        bounds.push([plat, plon]);
                      });
                      if (bounds.length > 0) map.fitBounds(bounds, { padding:[40,40] });
                      setStatus(points.length + ' points trouves autour de vous.');
                    }

                    function onLocation(lat, lon, statusMessage, label) {
                      L.marker([lat, lon], { icon:userIcon }).addTo(map).bindPopup('Votre position').openPopup();
                      map.setView([lat, lon], 14);
                      if (statusMessage) setStatus(statusMessage);
                      loadNearby(lat, lon).catch((e) => setStatus('Erreur: ' + e.message));
                    }

                    async function geocodeAndSearch() {
                      const query = (document.getElementById('placeInput').value || '').trim();
                      if (!query) {
                        setStatus('Saisissez une ville ou une adresse.');
                        return;
                      }
                      setStatus('Recherche de la zone...');
                      try {
                        const url = 'https://nominatim.openstreetmap.org/search?format=json&limit=1&q=' + encodeURIComponent(query);
                        const resp = await fetch(url, { headers: { 'Accept': 'application/json' } });
                        if (!resp.ok) throw new Error('Geocoding indisponible');
                        const data = await resp.json();
                        if (!data || data.length === 0) throw new Error('Lieu introuvable');
                        const lat = parseFloat(data[0].lat);
                        const lon = parseFloat(data[0].lon);
                        onLocation(lat, lon, 'Zone manuelle: ' + query, query);
                      } catch (e) {
                        setStatus('Recherche manuelle impossible: ' + e.message);
                      }
                    }
                    document.getElementById('findBtn').addEventListener('click', geocodeAndSearch);
                    document.getElementById('placeInput').addEventListener('keydown', (e) => {
                      if (e.key === 'Enter') geocodeAndSearch();
                    });

                    if (!navigator.geolocation) {
                      onLocation(FALLBACK_LAT, FALLBACK_LON, 'Geolocalisation non supportee. Position profil utilisee: ' + FALLBACK_LABEL);
                    } else {
                      navigator.geolocation.getCurrentPosition(
                        pos => onLocation(pos.coords.latitude, pos.coords.longitude, 'Position detectee.'),
                        err => onLocation(FALLBACK_LAT, FALLBACK_LON, 'Localisation refusee ou indisponible. Position profil utilisee: ' + FALLBACK_LABEL),
                        { enableHighAccuracy:true, timeout:12000, maximumAge:0 }
                      );
                    }
                  </script>
                </body>
                </html>
                """.formatted(fallback[0], fallback[1], fallbackLabel.replace("'", " "));
    }

    private double[] resolveProfileFallbackCoordinates() {
        // Force Tunis fallback when JavaFX WebView geolocation is blocked.
        return new double[]{36.8065, 10.1815};
    }

    private String resolveProfileFallbackLabel() {
        return "Tunis";
    }

    private void openExternal(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                return;
            }
            throw new IllegalStateException("Desktop browse unsupported.");
        } catch (Exception e) {
            Stage stage = resolveStage();
            if (stage != null) {
                UiDialog.show(stage, UiDialog.Type.ERROR, "Open link", "Unable to open", url);
            }
        }
    }

    private void updateDarkToggleVisual(boolean darkEnabled) {
        if (darkToggleShell == null || darkToggleKnob == null) {
            return;
        }
        darkToggleShell.setStyle(darkEnabled
                ? "-fx-min-width: 36px; -fx-max-width: 36px; -fx-min-height: 20px; -fx-max-height: 20px; -fx-background-color: #4F46E5; -fx-background-radius: 999px; -fx-padding: 2px;"
                : "-fx-min-width: 36px; -fx-max-width: 36px; -fx-min-height: 20px; -fx-max-height: 20px; -fx-background-color: #94A3B8; -fx-background-radius: 999px; -fx-padding: 2px;");
        darkToggleKnob.setTranslateX(darkEnabled ? 7 : -7);
    }

    private void scrollToNode(Node target) {
        if (salaryProfileScrollPane == null || target == null || salaryProfileScrollPane.getContent() == null) {
            return;
        }
        double contentHeight = salaryProfileScrollPane.getContent().getBoundsInLocal().getHeight();
        double viewportHeight = salaryProfileScrollPane.getViewportBounds().getHeight();
        if (contentHeight <= viewportHeight) {
            salaryProfileScrollPane.setVvalue(0);
            return;
        }
        javafx.geometry.Bounds targetBounds = target.localToScene(target.getBoundsInLocal());
        javafx.geometry.Bounds contentBounds = salaryProfileScrollPane.getContent().localToScene(salaryProfileScrollPane.getContent().getBoundsInLocal());
        double yInContent = targetBounds.getMinY() - contentBounds.getMinY();
        double v = yInContent / (contentHeight - viewportHeight);
        salaryProfileScrollPane.setVvalue(Math.max(0, Math.min(1, v)));
    }

    private Stage resolveStage() {
        if (profileNameLabel != null && profileNameLabel.getScene() != null && profileNameLabel.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    private User copyUser(User source) {
        User copy = new User();
        copy.setId(source.getId());
        copy.setNom(source.getNom());
        copy.setEmail(source.getEmail());
        copy.setPassword(source.getPassword());
        copy.setRoles(source.getRoles());
        copy.setDateInscription(source.getDateInscription());
        copy.setSoldeTotal(source.getSoldeTotal());
        copy.setImage(source.getImage());
        copy.setFaceIdCredentialId(source.getFaceIdCredentialId());
        copy.setFaceIdEnabled(source.isFaceIdEnabled());
        copy.setFacePlusToken(source.getFacePlusToken());
        copy.setFacePlusEnabled(source.isFacePlusEnabled());
        copy.setEmailVerified(source.isEmailVerified());
        copy.setEmailVerificationToken(source.getEmailVerificationToken());
        copy.setEmailVerifiedAt(source.getEmailVerifiedAt());
        copy.setBlocked(source.isBlocked());
        copy.setBlockedReason(source.getBlockedReason());
        copy.setBlockedAt(source.getBlockedAt());
        copy.setGeoCountryCode(source.getGeoCountryCode());
        copy.setGeoCountryName(source.getGeoCountryName());
        copy.setGeoRegionName(source.getGeoRegionName());
        copy.setGeoCityName(source.getGeoCityName());
        copy.setGeoDetectedIp(source.getGeoDetectedIp());
        copy.setGeoVpnSuspected(source.isGeoVpnSuspected());
        copy.setGeoLastCheckedAt(source.getGeoLastCheckedAt());
        return copy;
    }

    private String buildLocationText(User user) {
        String city = safeTrim(user.getGeoCityName());
        String region = safeTrim(user.getGeoRegionName());

        List<String> parts = new ArrayList<>();
        if (city != null) {
            parts.add(city);
        }
        if (region != null && !region.equalsIgnoreCase(city)) {
            parts.add(region);
        }
        return parts.isEmpty() ? "Location detected" : String.join(", ", parts);
    }

    private String toFlagEmoji(String countryCode) {
        if (countryCode == null) {
            return "\uD83C\uDF10";
        }
        String normalized = countryCode.trim().toUpperCase();
        if (normalized.length() != 2
                || normalized.charAt(0) < 'A' || normalized.charAt(0) > 'Z'
                || normalized.charAt(1) < 'A' || normalized.charAt(1) > 'Z') {
            return "\uD83C\uDF10";
        }

        int first = Character.codePointAt(normalized, 0) - 'A' + 0x1F1E6;
        int second = Character.codePointAt(normalized, 1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void applyVisualAnimations() {
        if (profileNameLabel.getScene() == null) {
            return;
        }

        Parent root = profileNameLabel.getScene().getRoot();
        List<Node> orderedCards = new ArrayList<>();
        orderedCards.addAll(root.lookupAll(".profile-card"));
        orderedCards.addAll(root.lookupAll(".side-balance-card"));
        orderedCards.addAll(root.lookupAll(".dark-card"));
        orderedCards.addAll(root.lookupAll(".light-card"));
        orderedCards.addAll(root.lookupAll(".behavior-card"));
        orderedCards.addAll(root.lookupAll(".transactions-card"));
        orderedCards.addAll(root.lookupAll(".profile-footer"));
        for (int i = 0; i < orderedCards.size(); i++) {
            Node card = orderedCards.get(i);
            card.setOpacity(0);
            card.setTranslateY(12);

            FadeTransition fade = new FadeTransition(Duration.millis(280), card);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(i * 45L));

            TranslateTransition slide = new TranslateTransition(Duration.millis(320), card);
            slide.setFromY(12);
            slide.setToY(0);
            slide.setInterpolator(Interpolator.EASE_BOTH);
            slide.setDelay(Duration.millis(i * 45L));

            fade.play();
            slide.play();
        }

        List<Node> buttons = new ArrayList<>();
        buttons.addAll(root.lookupAll(".salary-top-btn"));
        buttons.addAll(root.lookupAll(".field-upload-btn"));
        buttons.addAll(root.lookupAll(".convert-btn"));
        buttons.addAll(root.lookupAll(".premium-button"));
        buttons.addAll(root.lookupAll(".avatar-camera-btn"));
        buttons.forEach(this::wireButtonHoverScale);
        root.lookupAll(".swap-btn").forEach(this::wireSwapButtonSpin);

        root.lookupAll(".sidebar-menu-item").forEach(this::wireSidebarItemMotion);
        root.lookupAll(".profile-card, .side-balance-card, .dark-card, .light-card, .behavior-card, .transactions-card, .premium-card, .kpi-mini, .profile-footer")
                .forEach(this::wireCardHoverMotion);
        root.lookupAll(".avatar-circle").forEach(this::wireAvatarHoverMotion);
        root.lookupAll(".field-input").forEach(this::wireFieldFocusMotion);
        root.lookupAll(".empty-state-box").forEach(this::wireEmptyStateAppear);
        root.lookupAll(".score-ring").forEach(this::wireScoreRingAppear);
        pulseLiveBalanceCard(root);

        root.lookupAll(".metric-bar").stream()
                .filter(node -> node instanceof ProgressBar)
                .map(node -> (ProgressBar) node)
                .forEach(this::animateMetricBar);
    }

    private void wireButtonHoverScale(Node node) {
        ScaleTransition in = new ScaleTransition(Duration.millis(220), node);
        in.setToX(1.05);
        in.setToY(1.05);
        in.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition out = new ScaleTransition(Duration.millis(220), node);
        out.setToX(1.0);
        out.setToY(1.0);
        out.setInterpolator(Interpolator.EASE_BOTH);

        node.setOnMouseEntered(e -> in.playFromStart());
        node.setOnMouseExited(e -> out.playFromStart());
    }

    private void animateMetricBar(ProgressBar progressBar) {
        double target = progressBar.getProgress();
        progressBar.setProgress(0);
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(360), new KeyValue(progressBar.progressProperty(), target, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

    private void wireSidebarItemMotion(Node node) {
        TranslateTransition in = new TranslateTransition(Duration.millis(130), node);
        in.setToX(4);
        in.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition out = new TranslateTransition(Duration.millis(130), node);
        out.setToX(0);
        out.setInterpolator(Interpolator.EASE_BOTH);

        node.setOnMouseEntered(e -> in.playFromStart());
        node.setOnMouseExited(e -> out.playFromStart());
    }

    private void wireSwapButtonSpin(Node node) {
        if (!(node instanceof Button swapButton)) {
            return;
        }
        swapButton.setOnMouseEntered(e -> {
            RotateTransition rotate = new RotateTransition(Duration.millis(280), swapButton);
            rotate.setFromAngle(0);
            rotate.setToAngle(90);
            rotate.setInterpolator(Interpolator.EASE_BOTH);
            rotate.playFromStart();
        });
        swapButton.setOnMouseExited(e -> {
            RotateTransition rotateBack = new RotateTransition(Duration.millis(280), swapButton);
            rotateBack.setFromAngle(swapButton.getRotate());
            rotateBack.setToAngle(0);
            rotateBack.setInterpolator(Interpolator.EASE_BOTH);
            rotateBack.playFromStart();
        });
    }

    private void wireCardHoverMotion(Node node) {
        ScaleTransition in = new ScaleTransition(Duration.millis(140), node);
        in.setToX(1.01);
        in.setToY(1.01);
        in.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition out = new ScaleTransition(Duration.millis(140), node);
        out.setToX(1.0);
        out.setToY(1.0);
        out.setInterpolator(Interpolator.EASE_BOTH);

        node.setOnMouseEntered(e -> in.playFromStart());
        node.setOnMouseExited(e -> out.playFromStart());
    }

    private void wireFieldFocusMotion(Node node) {
        if (!(node instanceof TextField field)) {
            return;
        }
        DropShadow focusGlow = new DropShadow(14, Color.web("#4F46E566"));
        focusGlow.setSpread(0.2);
        field.focusedProperty().addListener((obs, oldVal, focused) -> {
            field.setEffect(focused ? focusGlow : null);
        });
    }

    private void wireAvatarHoverMotion(Node node) {
        ScaleTransition in = new ScaleTransition(Duration.millis(220), node);
        in.setToX(1.05);
        in.setToY(1.05);
        in.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition out = new ScaleTransition(Duration.millis(220), node);
        out.setToX(1.0);
        out.setToY(1.0);
        out.setInterpolator(Interpolator.EASE_BOTH);

        node.setOnMouseEntered(e -> in.playFromStart());
        node.setOnMouseExited(e -> out.playFromStart());
    }

    private void wireEmptyStateAppear(Node node) {
        node.setOpacity(0);
        node.setTranslateY(8);
        FadeTransition fade = new FadeTransition(Duration.millis(300), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), node);
        slide.setFromY(8);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private void wireScoreRingAppear(Node node) {
        node.setOpacity(0);
        node.setScaleX(0.92);
        node.setScaleY(0.92);
        FadeTransition fade = new FadeTransition(Duration.millis(320), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(320), node);
        scale.setFromX(0.92);
        scale.setFromY(0.92);
        scale.setToX(1.0);
        scale.setToY(1.0);
        new ParallelTransition(fade, scale).play();
    }

    private void pulseLiveBalanceCard(Parent root) {
        Node amountNode = root.lookup(".side-amount");
        Node liveCard = root.lookup(".side-balance-card");
        if (amountNode == null || liveCard == null) {
            return;
        }

        ScaleTransition scale = new ScaleTransition(Duration.millis(360), amountNode);
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(1.03);
        scale.setToY(1.03);
        scale.setAutoReverse(true);
        scale.setCycleCount(6);

        DropShadow cardGlow = new DropShadow(24, Color.web("#4F46E54D"));
        cardGlow.setSpread(0.12);
        liveCard.setEffect(cardGlow);

        Timeline glowPulse = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(cardGlow.radiusProperty(), 20, Interpolator.EASE_BOTH),
                        new KeyValue(cardGlow.spreadProperty(), 0.08, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(360),
                        new KeyValue(cardGlow.radiusProperty(), 28, Interpolator.EASE_BOTH),
                        new KeyValue(cardGlow.spreadProperty(), 0.16, Interpolator.EASE_BOTH))
        );
        glowPulse.setAutoReverse(true);
        glowPulse.setCycleCount(6);

        new ParallelTransition(scale).play();
        glowPulse.play();
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/salary-home-view.fxml"));
            Parent root = loader.load();
            SalaryHomeController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            Stage stage = (Stage) profileNameLabel.getScene().getWindow();
            if (currentUser != null) {
                stage.setUserData(currentUser);
            }
            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/salary-home.css").toExternalForm());
            stage.setTitle("Salary Home");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible de revenir au dashboard salary.", e);
        }
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/pi/mains/login-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) profileNameLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource("/pi/styles/login.css").toExternalForm());
            stage.setUserData(null);
            stage.setTitle("User Secure Login");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'ouvrir la page Login.", e);
        }
    }
}

