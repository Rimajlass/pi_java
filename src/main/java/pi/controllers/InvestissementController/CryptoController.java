package pi.controllers.InvestissementController;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pi.entities.Crypto;
import pi.entities.CryptoNewsArticle;
import pi.entities.CryptoPricePoint;
import pi.entities.FearGreedSnapshot;
import pi.entities.FiatRatesSnapshot;
import pi.entities.GlobalMarketSnapshot;
import pi.entities.Investissement;
import pi.entities.User;
import pi.services.InvestissementService.CoinGeckoGlobalService;
import pi.services.InvestissementService.CryptoApiService;
import pi.services.InvestissementService.CryptoChartService;
import pi.services.InvestissementService.CryptoNewsService;
import pi.services.InvestissementService.CryptoService;
import pi.services.InvestissementService.FearGreedService;
import pi.services.InvestissementService.GroqAiService;
import pi.services.InvestissementService.InvestmentAssistantService;
import pi.services.InvestissementService.FrankfurterService;
import pi.services.InvestissementService.InvestissementService;
import pi.services.InvestissementService.ObjectifService;
import pi.controllers.UserTransactionController.AboutController;
import pi.controllers.UserTransactionController.ContactController;
import pi.controllers.UserTransactionController.SalaryHomeController;
import pi.controllers.UserTransactionController.ServiceController;
import pi.mains.Main;
import pi.tools.AppSceneStyles;
import pi.tools.ThemeManager;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CryptoController {

    @FXML
    private ScrollPane rootScroll;

    @FXML
    private TableView<Crypto> table;

    @FXML
    private TableColumn<Crypto, String> colName;

    @FXML
    private TableColumn<Crypto, Number> colPrice;

    @FXML
    private TableColumn<Crypto, Void> colChart;

    @FXML
    private TableColumn<Crypto, Void> colNews;

    @FXML
    private TableColumn<Crypto, Void> colGroqAnalysis;

    @FXML
    private TableView<Investissement> investTable;

    @FXML
    private TableColumn<Investissement, String> colCrypto;

    @FXML
    private TableColumn<Investissement, Number> colAmount;

    @FXML
    private TableColumn<Investissement, Number> colQuantity;

    @FXML
    private TableColumn<Investissement, Void> colDelete;

    @FXML
    private TableColumn<Investissement, Number> colProfitLoss;

    @FXML
    private TableColumn<Investissement, Void> colModify;

    @FXML
    private TextField investSearch;

    @FXML
    private ProgressBar fearGreedMeter;

    @FXML
    private Label fearGreedValue;

    @FXML
    private Label fearGreedClassification;

    @FXML
    private Label fearGreedUpdated;

    @FXML
    private Label globalMarketCapLabel;

    @FXML
    private Label globalVolumeLabel;

    @FXML
    private Label globalBtcDomLabel;

    @FXML
    private Label globalMetaLabel;

    @FXML
    private Label fiatEurLabel;

    @FXML
    private Label fiatGbpLabel;

    @FXML
    private Label fiatDisclaimerLabel;

    @FXML
    private Label lastMarketRefreshLabel;

    @FXML
    private Label objectifsBadgeLabel;

    @FXML
    private Label summaryTotalLabel;

    @FXML
    private Label summaryPnLLabel;

    @FXML
    private Label summaryPositionsLabel;

    @FXML
    private Label summaryApproxEurLabel;

    @FXML
    private ComboBox<Crypto> calcCryptoCombo;

    @FXML
    private TextField calcAmountField;

    @FXML
    private Label calcResultLabel;

    @FXML
    private Button themeToggleButton;

    @FXML
    private TextArea investChatArea;

    @FXML
    private TextField investChatInput;

    @FXML
    private Button investChatSendButton;

    @FXML
    private ProgressIndicator investChatBusy;

    private final ObservableList<Investissement> investBackingList = FXCollections.observableArrayList();
    private FilteredList<Investissement> filteredInvestissements;
    private double lastEurPerUsd;
    private boolean globalRefreshKeysRegistered;
    private User currentUser;

    private static final List<String> FNG_METER_ZONES = List.of(
            "fng-meter-extreme-fear",
            "fng-meter-fear",
            "fng-meter-neutral",
            "fng-meter-greed",
            "fng-meter-extreme-greed"
    );

    private final CryptoNewsService cryptoNewsService = new CryptoNewsService();
    private final CryptoApiService apiService = new CryptoApiService();
    private final CryptoService cryptoService = new CryptoService();
    private final InvestissementService investissementService = new InvestissementService();
    private final CryptoChartService cryptoChartService = new CryptoChartService();
    private final FearGreedService fearGreedService = new FearGreedService();
    private final CoinGeckoGlobalService coinGeckoGlobalService = new CoinGeckoGlobalService();
    private final FrankfurterService frankfurterService = new FrankfurterService();
    private final InvestmentAssistantService investmentAssistant = new InvestmentAssistantService();
    private final GroqAiService groqAiService = new GroqAiService();

    @FXML
    public void initialize() {

        // Crypto table
        colName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        setupCryptoNameColumnWithLogo();

        colPrice.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getCurrentprice()));

        setupChartColumn();
        setupNewsColumn();
        setupGroqAnalysisColumn();


        // Investment table
        colCrypto.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCrypto().getName()));

        colAmount.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getAmountInvested()));

        colProfitLoss.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getProfitLoss()));

        colQuantity.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getQuantity()));

        setupDeleteColumn();
        setupModifyColumn();

        filteredInvestissements = new FilteredList<>(investBackingList, p -> true);
        investSearch.textProperty().addListener((obs, oldVal, newVal) ->
                filteredInvestissements.setPredicate(inv -> {
                    if (newVal == null || newVal.isEmpty()) {
                        return true;
                    }
                    return inv.getCrypto().getName().toLowerCase().contains(newVal.toLowerCase());
                }));
        investTable.setItems(filteredInvestissements);

        loadData();
        loadInvestissements();
        loadFearGreedIndex();
        loadGlobalMarketOverview();
        loadFiatRates();

        refreshObjectifsBadge();
        if (rootScroll != null) {
            rootScroll.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    ThemeManager.registerScene(newScene);
                    updateThemeToggleButton();
                    Platform.runLater(this::registerGlobalRefreshKeysOnce);
                }
            });
        }
        updateThemeToggleButton();
        Platform.runLater(this::registerGlobalRefreshKeysOnce);

        setupInvestmentAssistantChat();
        attachUserFromStageIfAvailable();
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadInvestissements();
        refreshObjectifsBadge();
    }

    private void attachUserFromStageIfAvailable() {
        if (investTable == null) {
            return;
        }
        investTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getWindow() instanceof Stage stage && stage.getUserData() instanceof User user) {
                setUser(user);
            }
        });
    }

    private void setupInvestmentAssistantChat() {
        if (investChatArea != null) {
            investChatArea.setText(
                    "Assistant : Bonjour. Demandez d’ajouter un investissement en USD sur une crypto du marché "
                            + "(ex. « Ajoute 150 USD sur Ethereum »).\n");
        }
        if (investChatInput != null) {
            investChatInput.setOnAction(e -> sendInvestmentAssistantMessage());
        }
    }

    private void appendInvestChat(String who, String text) {
        if (investChatArea == null) {
            return;
        }
        investChatArea.appendText(who + " : " + text + "\n");
    }

    /**
     * Table marché : prix/API sans id MySQL ; la base fournit l'id pour l'INSERT — on fusionne via apiid CoinGecko.
     */
    private Crypto resolveCryptoWithLivePrice(Crypto fromDb) {
        if (table != null && table.getItems() != null && fromDb.getApiid() != null) {
            for (Crypto c : table.getItems()) {
                if (fromDb.getApiid().equalsIgnoreCase(c.getApiid())) {
                    return new Crypto(fromDb.getId(), c.getName(), c.getSymbol(), c.getApiid(),
                            c.getCurrentprice(), c.getImageUrl());
                }
            }
        }
        return fromDb;
    }

    @FXML
    private void sendInvestmentAssistantMessage() {
        if (investChatInput == null) {
            return;
        }
        String msg = investChatInput.getText();
        if (msg == null || msg.isBlank()) {
            return;
        }
        investChatInput.clear();
        appendInvestChat("Vous", msg.trim());

        if (investChatSendButton != null) {
            investChatSendButton.setDisable(true);
        }
        if (investChatBusy != null) {
            investChatBusy.setVisible(true);
        }

        Task<InvestmentAssistantService.Result> task = new Task<>() {
            @Override
            protected InvestmentAssistantService.Result call() throws Exception {
                List<Crypto> catalog = cryptoService.getAll();
                return investmentAssistant.processUserMessage(msg.trim(), catalog);
            }
        };

        task.setOnSucceeded(e -> {
            if (investChatSendButton != null) {
                investChatSendButton.setDisable(false);
            }
            if (investChatBusy != null) {
                investChatBusy.setVisible(false);
            }
            InvestmentAssistantService.Result r = task.getValue();
            if (r == null) {
                appendInvestChat("Assistant", "Réponse vide.");
                return;
            }
            switch (r.type) {
                case INVESTMENT_ADDED -> {
                    try {
                        Crypto live = resolveCryptoWithLivePrice(r.crypto);
                        double price = live.getCurrentprice();
                        if (price <= 0) {
                            appendInvestChat("Assistant",
                                    "Prix spot invalide pour " + live.getName() + ". Rafraîchissez le marché (Rafraîchir ou F5).");
                            return;
                        }
                        double qty = r.amountUsd / price;
                        Investissement inv = new Investissement(live, null, currentUser, r.amountUsd, price, qty, LocalDate.now());
                        investissementService.add(inv);
                        loadInvestissements();
                        appendInvestChat("Assistant", r.text);
                    } catch (Exception ex) {
                        appendInvestChat("Assistant", "Erreur en base : " + ex.getMessage());
                    }
                }
                case MESSAGE -> appendInvestChat("Assistant", r.text);
                case ERROR -> appendInvestChat("Assistant", r.text);
            }
        });

        task.setOnFailed(e -> {
            if (investChatSendButton != null) {
                investChatSendButton.setDisable(false);
            }
            if (investChatBusy != null) {
                investChatBusy.setVisible(false);
            }
            Throwable ex = task.getException();
            appendInvestChat("Assistant", "Erreur : " + (ex != null ? ex.getMessage() : "inconnue"));
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void setupCryptoNameColumnWithLogo() {
        colName.setCellFactory(column -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            private final Label nameLabel = new Label();
            private final HBox box = new HBox(10, imageView, nameLabel);

            {
                box.setAlignment(Pos.CENTER_LEFT);

                imageView.setFitWidth(26);
                imageView.setFitHeight(26);
                imageView.setPreserveRatio(true);

                nameLabel.setStyle("-fx-font-weight: bold;");
            }

            @Override
            protected void updateItem(String name, boolean empty) {
                super.updateItem(name, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                Crypto crypto = getTableRow().getItem();

                nameLabel.setText(crypto.getName());

                if (crypto.getImageUrl() != null && !crypto.getImageUrl().isBlank()) {
                    imageView.setImage(new Image(crypto.getImageUrl(), true));
                } else {
                    imageView.setImage(null);
                }

                setGraphic(box);
            }
        });
    }
    private void setupChartColumn() {
        colChart.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Show Chart");

            {
                btn.getStyleClass().add("table-edit-button");

                btn.setOnAction(event -> {
                    Crypto crypto = getTableView().getItems().get(getIndex());
                    openCryptoChartWindow(crypto);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void setupNewsColumn() {
        colNews.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("News");

            {
                btn.getStyleClass().add("table-edit-button");

                btn.setOnAction(event -> {
                    Crypto crypto = getTableView().getItems().get(getIndex());
                    openCryptoNewsWindow(crypto);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void setupGroqAnalysisColumn() {
        colGroqAnalysis.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Analyse");

            {
                btn.getStyleClass().add("table-edit-button");
                btn.setOnAction(event -> {
                    Crypto crypto = getTableView().getItems().get(getIndex());
                    openCryptoGroqAnalysisWindow(crypto);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void openCryptoGroqAnalysisWindow(Crypto crypto) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Analyse IA — " + crypto.getName());

        ProgressIndicator progressIndicator = new ProgressIndicator();
        Label loadingLabel = new Label("Analyse en cours (Groq)…");

        VBox loadingBox = new VBox(12, progressIndicator, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setStyle("-fx-padding: 20; -fx-background-color: white;");

        BorderPane root = new BorderPane();
        root.setCenter(loadingBox);

        Scene scene = new Scene(root, 900, 650);
        ThemeManager.registerScene(scene);
        stage.setScene(scene);
        stage.show();

        double exposureUsd = sumInvestedForCryptoApiId(crypto.getApiid());

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return groqAiService.analyzeCryptoSpot(crypto, exposureUsd);
            }
        };

        task.setOnSucceeded(event -> {
            TextArea textArea = new TextArea(task.getValue());
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setStyle("-fx-font-size: 14px;");
            ScrollPane scrollPane = new ScrollPane(textArea);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: white;");
            root.setCenter(scrollPane);
        });

        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            String message = exception != null ? exception.getMessage() : "Erreur inconnue";
            TextArea errorArea = new TextArea(
                    "Impossible de générer l'analyse.\n\nDétails :\n" + message
                            + "\n\nVérifiez GROQ_API_KEY (et GROQ_MODEL) dans .env.local à la racine du projet.");
            errorArea.setEditable(false);
            errorArea.setWrapText(true);
            root.setCenter(errorArea);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private double sumInvestedForCryptoApiId(String apiid) {
        if (apiid == null || apiid.isBlank()) {
            return 0;
        }
        return investBackingList.stream()
                .filter(inv -> inv.getCrypto().getApiid() != null
                        && inv.getCrypto().getApiid().equalsIgnoreCase(apiid))
                .mapToDouble(Investissement::getAmountInvested)
                .sum();
    }

    private void openCryptoNewsWindow(Crypto crypto) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(crypto.getName() + " News");

        ProgressIndicator progressIndicator = new ProgressIndicator();
        Label loadingLabel = new Label("Loading latest news for " + crypto.getName() + "...");

        VBox loadingBox = new VBox(12, progressIndicator, loadingLabel);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setStyle("-fx-padding: 20; -fx-background-color: white;");

        BorderPane root = new BorderPane();
        root.setCenter(loadingBox);

        Scene scene = new Scene(root, 900, 650);
        ThemeManager.registerScene(scene);
        stage.setScene(scene);
        stage.show();

        Task<List<CryptoNewsArticle>> task = new Task<>() {
            @Override
            protected List<CryptoNewsArticle> call() throws Exception {
                return cryptoNewsService.getNewsForCrypto(crypto);
            }
        };

        task.setOnSucceeded(event -> {
            List<CryptoNewsArticle> articles = task.getValue();

            if (articles == null || articles.isEmpty()) {
                root.setCenter(new Label("No news found for " + crypto.getName()));
                return;
            }

            VBox newsContainer = new VBox(18);
            newsContainer.setPadding(new Insets(20));
            newsContainer.setStyle("-fx-background-color: white;");

            Label title = new Label("Latest News About " + crypto.getName());
            title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

            newsContainer.getChildren().add(title);

            for (CryptoNewsArticle article : articles) {
                newsContainer.getChildren().add(createNewsCard(article));
            }

            ScrollPane scrollPane = new ScrollPane(newsContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: white;");

            root.setCenter(scrollPane);
        });

        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            String message = exception != null ? exception.getMessage() : "Unknown error";

            TextArea errorArea = new TextArea(
                    "Failed to load news.\n\nDetails:\n" + message
            );
            errorArea.setEditable(false);
            errorArea.setWrapText(true);

            root.setCenter(errorArea);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    private HBox createNewsCard(CryptoNewsArticle article) {
        HBox card = new HBox(16);
        card.setPadding(new Insets(14));
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("""
            -fx-background-color: #f8fbff;
            -fx-background-radius: 14;
            -fx-border-radius: 14;
            -fx-border-color: #d8e6f3;
            -fx-border-width: 1;
            """);

        ImageView imageView = new ImageView();
        imageView.setFitWidth(180);
        imageView.setFitHeight(110);
        imageView.setPreserveRatio(false);

        if (article.getImageUrl() != null && !article.getImageUrl().isBlank()) {
            Image image = new Image(article.getImageUrl(), true);
            imageView.setImage(image);
        } else {
            imageView.setStyle("-fx-background-color: #d9e6f2;");
        }

        VBox textBox = new VBox(8);
        textBox.setAlignment(Pos.TOP_LEFT);

        Label sourceLabel = new Label(article.getSourceName() + " • " + article.getPublishedAt());
        sourceLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6b7280;");

        Label titleLabel = new Label(article.getTitle());
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #172554;");

        Label descriptionLabel = new Label(article.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");

        Button openButton = new Button("Open Article");
        openButton.getStyleClass().add("table-edit-button");
        openButton.setOnAction(event -> openArticleInBrowser(article.getUrl()));

        textBox.getChildren().addAll(sourceLabel, titleLabel, descriptionLabel, openButton);

        card.getChildren().addAll(imageView, textBox);

        return card;
    }
    private void openArticleInBrowser(String url) {
        try {
            if (url == null || url.isBlank()) {
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void setupDeleteColumn() {
        colDelete.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");

            {
                btn.getStyleClass().add("table-delete-button");

                btn.setOnAction(e -> {
                    Investissement inv = getTableView().getItems().get(getIndex());

                    try {
                        investissementService.delete(inv.getId());
                        loadInvestissements();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void setupModifyColumn() {
        colModify.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Modifier");

            {
                btn.getStyleClass().add("table-edit-button");

                btn.setOnAction(e -> {
                    Investissement inv = getTableView().getItems().get(getIndex());

                    TextInputDialog dialog = new TextInputDialog(String.valueOf(inv.getAmountInvested()));
                    dialog.setTitle("Modifier investissement");
                    dialog.setHeaderText(null);
                    dialog.setContentText("Nouveau montant (USD):");

                    dialog.showAndWait().ifPresent(input -> {
                        try {
                            double newAmount = Double.parseDouble(input);
                            double newQuantity = newAmount / inv.getBuyPrice();

                            investissementService.update(inv.getId(), newAmount, newQuantity);
                            loadInvestissements();

                        } catch (NumberFormatException ex) {
                            System.out.println("Invalid amount.");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void openCryptoChartWindow(Crypto crypto) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(crypto.getName() + " Price Chart");

        ProgressIndicator progressIndicator = new ProgressIndicator();

        Label loadingLabel = new Label("Loading 30-day chart for " + crypto.getName() + "...");

        VBox loadingBox = new VBox(12, progressIndicator, loadingLabel);
        loadingBox.setStyle("-fx-padding: 20; -fx-background-color: white;");

        BorderPane root = new BorderPane();
        root.setCenter(loadingBox);

        Scene scene = new Scene(root, 850, 550);
        ThemeManager.registerScene(scene);
        stage.setScene(scene);
        stage.show();

        Task<List<CryptoPricePoint>> task = new Task<>() {
            @Override
            protected List<CryptoPricePoint> call() throws Exception {
                return cryptoChartService.getMarketChart(crypto.getApiid(), 30, "usd");
            }
        };

        task.setOnSucceeded(event -> {
            List<CryptoPricePoint> points = task.getValue();

            if (points == null || points.isEmpty()) {
                root.setCenter(new Label("No chart data found for " + crypto.getName()));
                return;
            }

            CategoryAxis xAxis = new CategoryAxis();
            NumberAxis yAxis = new NumberAxis();

            xAxis.setLabel("Date");
            yAxis.setLabel("Price USD");

            LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
            lineChart.setTitle(crypto.getName() + " price over the last 30 days");
            lineChart.setCreateSymbols(false);
            lineChart.setAnimated(false);
            lineChart.setLegendVisible(true);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(crypto.getSymbolUpper() + " / USD");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

            int step = Math.max(1, points.size() / 30);

            for (int i = 0; i < points.size(); i += step) {
                CryptoPricePoint point = points.get(i);

                series.getData().add(new XYChart.Data<>(
                        point.getDateTime().format(formatter),
                        point.getPrice()
                ));
            }

            lineChart.getData().add(series);

            BorderPane chartPane = new BorderPane();
            chartPane.setCenter(lineChart);
            chartPane.setStyle("-fx-padding: 20; -fx-background-color: white;");

            root.setCenter(chartPane);
        });

        task.setOnFailed(event -> {
            Throwable exception = task.getException();
            String message = exception != null ? exception.getMessage() : "Unknown error";

            TextArea errorArea = new TextArea(
                    "Failed to load chart.\n\nDetails:\n" + message
            );
            errorArea.setEditable(false);
            errorArea.setWrapText(true);

            root.setCenter(errorArea);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    public void refresh() {
        loadData();
        loadInvestissements();
        loadFearGreedIndex();
        loadGlobalMarketOverview();
        loadFiatRates();
        refreshObjectifsBadge();
    }

    private void loadGlobalMarketOverview() {
        globalMarketCapLabel.setText("Chargement…");
        globalVolumeLabel.setText("");
        globalBtcDomLabel.setText("");
        globalMetaLabel.setText("");

        Task<GlobalMarketSnapshot> task = new Task<>() {
            @Override
            protected GlobalMarketSnapshot call() throws Exception {
                return coinGeckoGlobalService.fetchGlobal();
            }
        };

        task.setOnSucceeded(event -> {
            GlobalMarketSnapshot g = task.getValue();
            globalMarketCapLabel.setText("Capitalisation globale : " + formatUsdShort(g.getTotalMarketCapUsd()));
            globalVolumeLabel.setText("Volume 24h : " + formatUsdShort(g.getTotalVolumeUsd()));
            globalBtcDomLabel.setText(String.format(Locale.US, "Dominance BTC : %.2f %%", g.getBtcDominancePercent()));
            globalMetaLabel.setText(String.format(Locale.FRENCH,
                    "%d cryptos actives — %d marchés spot (CoinGecko)",
                    g.getActiveCryptocurrencies(), g.getMarkets()));
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            String message = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erreur inconnue";
            globalMarketCapLabel.setText("Indisponible");
            globalVolumeLabel.setText("");
            globalBtcDomLabel.setText("");
            globalMetaLabel.setText(message.length() > 200 ? message.substring(0, 197) + "…" : message);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadFiatRates() {
        fiatEurLabel.setText("Chargement…");
        fiatGbpLabel.setText("");
        fiatDisclaimerLabel.setText("");

        Task<FiatRatesSnapshot> task = new Task<>() {
            @Override
            protected FiatRatesSnapshot call() throws Exception {
                return frankfurterService.fetchUsdRates();
            }
        };

        task.setOnSucceeded(event -> {
            FiatRatesSnapshot s = task.getValue();
            fiatEurLabel.setText(String.format(Locale.FRENCH, "1 USD = %.4f EUR", s.getEurPerUsd()));
            fiatGbpLabel.setText(String.format(Locale.FRENCH, "1 USD = %.4f GBP", s.getGbpPerUsd()));
            fiatDisclaimerLabel.setText(
                    "Données Frankfurter (BCE), cours au " + s.getRateDateIso()
                            + ". Le TND n'est pas disponible dans cette API.");
            lastEurPerUsd = s.getEurPerUsd();
            updatePortfolioSummaryFromBacking();
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            String message = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erreur inconnue";
            fiatEurLabel.setText("Indisponible");
            fiatGbpLabel.setText("");
            fiatDisclaimerLabel.setText(message.length() > 220 ? message.substring(0, 217) + "…" : message);
            lastEurPerUsd = 0;
            updatePortfolioSummaryFromBacking();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private static String formatUsdShort(double usd) {
        if (usd >= 1e12) {
            return String.format(Locale.US, "%.2f T USD", usd / 1e12);
        }
        if (usd >= 1e9) {
            return String.format(Locale.US, "%.2f B USD", usd / 1e9);
        }
        if (usd >= 1e6) {
            return String.format(Locale.US, "%.2f M USD", usd / 1e6);
        }
        return String.format(Locale.US, "%.0f USD", usd);
    }

    private void loadFearGreedIndex() {
        fearGreedValue.setText("…");
        fearGreedClassification.setText("Chargement…");
        fearGreedUpdated.setText("");
        fearGreedMeter.setProgress(0);
        applyFearGreedMeterStyle(50);

        Task<FearGreedSnapshot> task = new Task<>() {
            @Override
            protected FearGreedSnapshot call() throws Exception {
                return fearGreedService.fetchLatest();
            }
        };

        task.setOnSucceeded(event -> {
            FearGreedSnapshot snapshot = task.getValue();
            fearGreedValue.setText(String.valueOf(snapshot.getValue()));
            fearGreedClassification.setText(snapshot.getClassification());
            fearGreedMeter.setProgress(snapshot.getValue() / 100.0);
            applyFearGreedMeterStyle(snapshot.getValue());
            if (snapshot.getTimestampSeconds() > 0) {
                var zoned = Instant.ofEpochSecond(snapshot.getTimestampSeconds())
                        .atZone(ZoneId.systemDefault());
                fearGreedUpdated.setText("Index du "
                        + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(zoned));
            } else {
                fearGreedUpdated.setText("");
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            String message = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erreur inconnue";
            fearGreedValue.setText("—");
            fearGreedClassification.setText("Indisponible");
            fearGreedUpdated.setText(message.length() > 140 ? message.substring(0, 137) + "…" : message);
            fearGreedMeter.setProgress(0);
            applyFearGreedMeterStyle(50);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void applyFearGreedMeterStyle(int value) {
        fearGreedMeter.getStyleClass().removeAll(FNG_METER_ZONES);
        String zone;
        if (value <= 24) {
            zone = "fng-meter-extreme-fear";
        } else if (value <= 44) {
            zone = "fng-meter-fear";
        } else if (value <= 55) {
            zone = "fng-meter-neutral";
        } else if (value <= 74) {
            zone = "fng-meter-greed";
        } else {
            zone = "fng-meter-extreme-greed";
        }
        fearGreedMeter.getStyleClass().add(zone);
    }

    private void loadData() {
        try {
            List<Crypto> cryptos = apiService.getCryptos();

            try {
                cryptoService.saveAllOrUpdate(cryptos);
            } catch (Exception e) {
                System.out.println("DB save failed: " + e.getMessage());
            }

            table.setItems(FXCollections.observableArrayList(cryptos));
            repopulateCalculatorCryptos();
            touchMarketDataRefreshed();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadInvestissements() {
        try {
            List<Investissement> list = (currentUser != null && currentUser.getId() > 0)
                    ? investissementService.getAllByUser(currentUser.getId())
                    : List.of();
            investBackingList.setAll(list);
            updatePortfolioSummaryFromBacking();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void touchMarketDataRefreshed() {
        String ts = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss").format(LocalDateTime.now());
        lastMarketRefreshLabel.setText("Marché actualisé : " + ts);
    }

    private void refreshObjectifsBadge() {
        try {
            int n = (currentUser != null && currentUser.getId() > 0)
                    ? new ObjectifService().getAllByUser(currentUser.getId()).size()
                    : 0;
            objectifsBadgeLabel.setText("Objectifs : " + n);
        } catch (Exception e) {
            objectifsBadgeLabel.setText("Objectifs : —");
        }
    }

    private void updatePortfolioSummaryFromBacking() {
        double totalInv = investBackingList.stream().mapToDouble(Investissement::getAmountInvested).sum();
        double totalPl = investBackingList.stream().mapToDouble(Investissement::getProfitLoss).sum();
        summaryTotalLabel.setText(String.format(Locale.US, "%.2f USD", totalInv));
        summaryPnLLabel.setText(String.format(Locale.US, "%+.2f USD", totalPl));
        summaryPositionsLabel.setText(String.valueOf(investBackingList.size()));
        if (lastEurPerUsd > 0) {
            summaryApproxEurLabel.setText(String.format(Locale.FRENCH,
                    "≈ %.2f EUR investi (indicatif)", totalInv * lastEurPerUsd));
        } else {
            summaryApproxEurLabel.setText("EUR indicatif : chargez les taux (Frankfurter).");
        }
    }

    private void repopulateCalculatorCryptos() {
        ObservableList<Crypto> items = table.getItems();
        if (items == null) {
            return;
        }
        Crypto keep = calcCryptoCombo.getSelectionModel().getSelectedItem();
        calcCryptoCombo.setItems(FXCollections.observableArrayList(items));
        if (keep != null && items.contains(keep)) {
            calcCryptoCombo.getSelectionModel().select(keep);
        }
    }

    private void registerGlobalRefreshKeysOnce() {
        Scene scene = rootScroll != null ? rootScroll.getScene() : investTable.getScene();
        if (scene == null || globalRefreshKeysRegistered) {
            return;
        }
        globalRefreshKeysRegistered = true;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F5) {
                refresh();
                event.consume();
            } else if (event.isShortcutDown() && event.getCode() == KeyCode.R) {
                refresh();
                event.consume();
            }
        });
    }
    @FXML
    private void toggleTheme() {
        Scene scene = themeToggleButton != null ? themeToggleButton.getScene() : investTable.getScene();
        if (scene == null) {
            return;
        }
        ThemeManager.toggleTheme(scene);
        updateThemeToggleButton();
    }

    private void updateThemeToggleButton() {
        if (themeToggleButton != null) {
            themeToggleButton.setText(ThemeManager.isDarkSelected() ? "Light Mode" : "Dark Mode");
        }
    }

    @FXML
    private void clearInvestSearch() {
        investSearch.clear();
    }

    @FXML
    private void exportPortfolioCsv() {
        Stage stage = (Stage) investTable.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le portefeuille (CSV)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier CSV", "*.csv"));
        chooser.setInitialFileName("decides-portefeuille.csv");
        java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("crypto,montant_usd,prix_achat,quantite,gain_perte_usd,date_ajout\n");
        DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE;
        for (Investissement inv : investBackingList) {
            sb.append(csvEscape(inv.getCrypto().getName())).append(',');
            sb.append(inv.getAmountInvested()).append(',');
            sb.append(inv.getBuyPrice()).append(',');
            sb.append(inv.getQuantity()).append(',');
            sb.append(inv.getProfitLoss()).append(',');
            sb.append(inv.getCreatedAt() != null ? df.format(inv.getCreatedAt()) : "").append('\n');
        }
        try {
            Files.writeString(file.toPath(), sb.toString(), StandardCharsets.UTF_8);
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Export");
            a.setHeaderText(null);
            a.setContentText("Fichier enregistré :\n" + file.getAbsolutePath());
            a.showAndWait();
        } catch (IOException ex) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Export");
            a.setHeaderText(null);
            a.setContentText("Échec : " + ex.getMessage());
            a.showAndWait();
        }
    }

    private static String csvEscape(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s + "\"";
        }
        return s;
    }

    @FXML
    private void copyPortfolioSummary() {
        double totalInv = investBackingList.stream().mapToDouble(Investissement::getAmountInvested).sum();
        double totalPl = investBackingList.stream().mapToDouble(Investissement::getProfitLoss).sum();
        StringBuilder sb = new StringBuilder();
        sb.append("Decide$ — Résumé portefeuille\n");
        sb.append("Total investi : ").append(String.format(Locale.US, "%.2f USD\n", totalInv));
        sb.append("Gain / perte : ").append(String.format(Locale.US, "%+.2f USD\n", totalPl));
        sb.append("Positions : ").append(investBackingList.size()).append("\n\n");
        for (Investissement inv : investBackingList) {
            sb.append("- ")
                    .append(inv.getCrypto().getName())
                    .append(" | ")
                    .append(String.format(Locale.US, "%.2f USD", inv.getAmountInvested()))
                    .append(" | P/L ")
                    .append(String.format(Locale.US, "%+.2f", inv.getProfitLoss()))
                    .append("\n");
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(sb.toString());
        Clipboard.getSystemClipboard().setContent(content);
        calcResultLabel.setText("Résumé copié dans le presse-papiers.");
    }

    @FXML
    private void runQuickCalculator() {
        Crypto crypto = calcCryptoCombo.getSelectionModel().getSelectedItem();
        if (crypto == null) {
            calcResultLabel.setText("Choisissez une crypto dans la liste.");
            return;
        }
        double price = crypto.getCurrentprice();
        if (price <= 0) {
            calcResultLabel.setText("Prix spot invalide pour cette crypto.");
            return;
        }
        String amtText = calcAmountField.getText() != null ? calcAmountField.getText().trim() : "";
        if (amtText.isEmpty()) {
            calcResultLabel.setText("Entrez un montant en USD.");
            return;
        }
        try {
            double usd = Double.parseDouble(amtText.replace(',', '.'));
            if (usd <= 0) {
                calcResultLabel.setText("Le montant doit être positif.");
                return;
            }
            double qty = usd / price;
            String line = String.format(Locale.FRENCH,
                    "≈ %.8f unités au prix %.2f USD · montant %.2f USD",
                    qty, price, usd);
            if (lastEurPerUsd > 0) {
                line += String.format(Locale.FRENCH, " · ≈ %.2f EUR", usd * lastEurPerUsd);
            }
            calcResultLabel.setText(line);
        } catch (NumberFormatException ex) {
            calcResultLabel.setText("Montant invalide.");
        }
    }

    @FXML
    public void goToInvest() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Invest/investissement.fxml"));
            Scene scene = new Scene(loader.load());
            AppSceneStyles.apply(scene, "/Invest/admin-style.css");

            Stage stage = new Stage();
            Stage owner = currentStage();
            if (owner != null) {
                stage.initOwner(owner);
                stage.initModality(Modality.WINDOW_MODAL);
            }
            stage.setTitle("Investir");
            stage.setScene(scene);
            stage.setUserData(currentUser);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openHomePage() {
        loadApplicationPage("/pi/mains/salary-home-view.fxml", "/pi/styles/salary-home.css", "Salary Home");
    }

    @FXML
    private void openAboutPage() {
        loadApplicationPage("/pi/mains/about-view.fxml", "/pi/styles/about.css", "About Us");
    }

    @FXML
    private void openServicePage() {
        loadApplicationPage("/pi/mains/service-view.fxml", "/pi/styles/service.css", "Services");
    }

    @FXML
    private void openContactPage() {
        loadApplicationPage("/pi/mains/contact-view.fxml", "/pi/styles/contact.css", "Contact");
    }

    @FXML
    private void openDashboardPage() {
        openHomePage();
    }

    @FXML
    private void openLoginPage() {
        loadApplicationPage("/pi/mains/login-view.fxml", "/pi/styles/login.css", "User Secure Login", false);
    }

    private void loadApplicationPage(String fxmlPath, String cssPath, String title) {
        loadApplicationPage(fxmlPath, cssPath, title, true);
    }

    private void loadApplicationPage(String fxmlPath, String cssPath, String title, boolean preserveUser) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Parent root = loader.load();
            if (preserveUser) {
                applyUser(loader.getController());
            }
            Stage stage = currentStage();
            if (stage == null) {
                return;
            }
            Scene scene = new Scene(root, 1460, 780);
            AppSceneStyles.apply(scene, cssPath);
            stage.setTitle(title);
            stage.setUserData(preserveUser ? currentUser : null);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyUser(Object controller) {
        if (currentUser == null || controller == null) {
            return;
        }
        if (controller instanceof SalaryHomeController salaryHomeController) {
            salaryHomeController.setUser(currentUser);
        } else if (controller instanceof AboutController aboutController) {
            aboutController.setUser(currentUser);
        } else if (controller instanceof ServiceController serviceController) {
            serviceController.setUser(currentUser);
        } else if (controller instanceof ContactController contactController) {
            contactController.setUser(currentUser);
        }
    }

    private Stage currentStage() {
        Scene scene = rootScroll != null ? rootScroll.getScene() : investTable.getScene();
        return scene == null ? null : (Stage) scene.getWindow();
    }

    @FXML
    public void goToAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Invest/admin.fxml"));
            Scene scene = new Scene(loader.load());
            AdminController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            AppSceneStyles.apply(scene, "/Invest/admin-style.css");
            Stage stage = currentStage();
            if (stage == null) {
                return;
            }
            stage.setTitle("Admin");
            stage.setScene(scene);
            stage.setUserData(currentUser);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToObjectifs() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Invest/objectif.fxml"));
            Scene scene = new Scene(loader.load());
            ObjectifController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            AppSceneStyles.apply(scene, "/Invest/admin-style.css");
            Stage stage = currentStage();
            if (stage == null) {
                return;
            }
            stage.setTitle("Objectifs");
            stage.setScene(scene);
            stage.setUserData(currentUser);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

