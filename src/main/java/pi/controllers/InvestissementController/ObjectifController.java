package pi.controllers.InvestissementController;

import com.lowagie.text.DocumentException;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pi.entities.Investissement;
import pi.entities.Objectif;
import pi.entities.User;
import pi.services.InvestissementService.GroqAiService;
import pi.services.InvestissementService.ObjectifMetrics;
import pi.services.InvestissementService.ObjectifMonteCarloService;
import pi.services.InvestissementService.ObjectifPdfExportService;
import pi.services.InvestissementService.ObjectifService;
import pi.services.InvestissementService.ObjectifStatistics;
import pi.tools.ThemeManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ObjectifController {

    @FXML
    private TableView<Objectif> objectifTable;

    @FXML
    private TableColumn<Objectif, String> colName;

    @FXML
    private TableColumn<Objectif, String> colPriorite;

    @FXML
    private TableColumn<Objectif, Number> colInitial;

    @FXML
    private TableColumn<Objectif, Number> colTarget;

    @FXML
    private TableColumn<Objectif, Number> colCurrent;

    @FXML
    private TableColumn<Objectif, Number> colRemaining;

    @FXML
    private TableColumn<Objectif, Void> colProgress;

    @FXML
    private TableColumn<Objectif, Number> colRoi;

    @FXML
    private TableColumn<Objectif, String> colAlerte;

    @FXML
    private TableColumn<Objectif, String> colCompleted;

    @FXML
    private TableColumn<Objectif, Void> colAiAnalysis;

    @FXML
    private TableColumn<Objectif, Void> colMonteCarlo;

    @FXML
    private TableColumn<Objectif, Void> colModify;

    @FXML
    private TableColumn<Objectif, Void> colDelete;

    @FXML
    private TextField objectifSearch;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private ComboBox<String> prioriteFilter;

    @FXML
    private Label summaryLine1;

    @FXML
    private Label summaryLine2;

    @FXML
    private Label summaryLine3;

    @FXML
    private Label statCompletionPct;

    @FXML
    private Label statAvgProgress;

    @FXML
    private Label statWeightedRoi;

    @FXML
    private Label statSumInitial;

    @FXML
    private Label statSumRemaining;

    @FXML
    private PieChart statusPieChart;

    @FXML
    private BarChart<String, Number> prioriteBarChart;

    private final ObjectifService objectifService = new ObjectifService();
    private final GroqAiService groqAiService = new GroqAiService();

    private final ObservableList<Objectif> objectifBackingList = FXCollections.observableArrayList();
    private FilteredList<Objectif> filteredObjectifs;
    private Map<Integer, Double> currentValueCache = new HashMap<>();
    private User currentUser;

    @FXML
    public void initialize() {
        colName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getName()));

        colPriorite.setCellValueFactory(data ->
                new SimpleStringProperty(ObjectifMetrics.prioriteLabel(data.getValue().getPriorite())));

        colInitial.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getInitialAmount()));

        colTarget.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getTargetAmount()));

        colCurrent.setCellValueFactory(data ->
                new SimpleDoubleProperty(currentFor(data.getValue())));

        colRemaining.setCellValueFactory(data ->
                new SimpleDoubleProperty(remainingFor(data.getValue())));

        colRoi.setCellValueFactory(data ->
                new SimpleDoubleProperty(ObjectifMetrics.roiPercent(data.getValue(), currentFor(data.getValue()))));

        colAlerte.setCellValueFactory(data ->
                new SimpleStringProperty(ObjectifMetrics.alerteMetier(data.getValue(), currentFor(data.getValue()))));

        setupProgressColumn();

        colCompleted.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().isCompleted() ? "✅ Complété" : "⏳ En cours"));

        setupAiAnalysisColumn();
        setupMonteCarloColumn();
        setupModifyColumn();
        setupDeleteColumn();

        statusFilter.setItems(FXCollections.observableArrayList("Tous", "En cours", "Complétés"));
        statusFilter.getSelectionModel().selectFirst();

        prioriteFilter.setItems(FXCollections.observableArrayList(
                "Toutes", ObjectifMetrics.prioriteLabel(Objectif.P_BASSE),
                ObjectifMetrics.prioriteLabel(Objectif.P_NORMALE),
                ObjectifMetrics.prioriteLabel(Objectif.P_HAUTE),
                ObjectifMetrics.prioriteLabel(Objectif.P_CRITIQUE)));
        prioriteFilter.getSelectionModel().selectFirst();

        filteredObjectifs = new FilteredList<>(objectifBackingList, p -> true);
        Runnable updatePred = () -> {
            String q = objectifSearch.getText();
            String st = statusFilter.getSelectionModel().getSelectedItem();
            if (st == null) {
                st = "Tous";
            }
            final String status = st;
            String pr = prioriteFilter.getSelectionModel().getSelectedItem();
            if (pr == null) {
                pr = "Toutes";
            }
            final String prioFilterLabel = pr;

            filteredObjectifs.setPredicate(obj -> {
                boolean nameOk = q == null || q.isBlank()
                        || obj.getName().toLowerCase().contains(q.toLowerCase());
                boolean statusOk = switch (status) {
                    case "Complétés" -> obj.isCompleted();
                    case "En cours" -> !obj.isCompleted();
                    default -> true;
                };
                boolean prioOk = switch (prioFilterLabel) {
                    case "Basse" -> Objectif.P_BASSE.equals(obj.getPriorite());
                    case "Normale" -> Objectif.P_NORMALE.equals(obj.getPriorite());
                    case "Haute" -> Objectif.P_HAUTE.equals(obj.getPriorite());
                    case "Critique" -> Objectif.P_CRITIQUE.equals(obj.getPriorite());
                    default -> true;
                };
                return nameOk && statusOk && prioOk;
            });
        };
        objectifSearch.textProperty().addListener((o, a, b) -> updatePred.run());
        statusFilter.getSelectionModel().selectedItemProperty().addListener((o, a, bar) -> updatePred.run());
        prioriteFilter.getSelectionModel().selectedItemProperty().addListener((o, a, bar) -> updatePred.run());
        objectifTable.setItems(filteredObjectifs);

        if (statusPieChart != null) {
            statusPieChart.setAnimated(false);
            statusPieChart.setLabelsVisible(true);
        }
        if (prioriteBarChart != null) {
            prioriteBarChart.setAnimated(false);
            prioriteBarChart.setLegendVisible(false);
        }

        attachUserFromStageIfAvailable();
        loadObjectifs();
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadObjectifs();
    }

    private void attachUserFromStageIfAvailable() {
        if (objectifTable == null) {
            return;
        }
        objectifTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getWindow() instanceof Stage stage && stage.getUserData() instanceof User user) {
                setUser(user);
            }
        });
    }

    private double currentFor(Objectif o) {
        return currentValueCache.getOrDefault(o.getId(), 0.0);
    }

    private double remainingFor(Objectif o) {
        if (o.isCompleted()) {
            return 0;
        }
        double cur = currentFor(o);
        return Math.max(0, o.getTargetAmount() - cur);
    }

    private void setupProgressColumn() {
        colProgress.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            private final Label pct = new Label();
            private final HBox box = new HBox(8, bar, pct);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                bar.setPrefWidth(90);
                bar.setMaxHeight(10);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Objectif o = getTableRow().getItem();
                double target = o.getTargetAmount();
                double cur = currentFor(o);
                double ratio = target > 0 ? Math.min(1.0, cur / target) : 0;
                bar.setProgress(ratio);
                double pctVal = target > 0 ? Math.min(100.0, cur / target * 100.0) : 0;
                pct.setText(String.format(Locale.FRENCH, "%.0f %%", pctVal));
                setGraphic(box);
            }
        });
    }

    private void setupAiAnalysisColumn() {
        colAiAnalysis.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("AI Analysis");

            {
                btn.getStyleClass().add("table-edit-button");

                btn.setOnAction(event -> {
                    Objectif objectif = getTableView().getItems().get(getIndex());
                    openAiAnalysisWindow(objectif);
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
                    Objectif obj = getTableView().getItems().get(getIndex());

                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Invest/modifyObjectif.fxml"));
                        Scene scene = new Scene(loader.load());

                        ModifyObjectifController controller = loader.getController();
                        if (currentUser != null) {
                            controller.setUser(currentUser);
                        }
                        controller.setObjectif(obj);

                        Stage stage = new Stage();
                        stage.setTitle("Modifier l'objectif");
                        ThemeManager.registerScene(scene);
                        stage.setScene(scene);
                        stage.initModality(Modality.APPLICATION_MODAL);
                        stage.showAndWait();

                        loadObjectifs();
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

    private void setupDeleteColumn() {
        colDelete.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Supprimer");

            {
                btn.getStyleClass().add("table-delete-button");

                btn.setOnAction(e -> {
                    Objectif obj = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Supprimer l'objectif");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Supprimer « " + obj.getName()
                            + " » ? Les investissements liés seront déplacés hors objectif (non supprimés).");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isEmpty() || result.get() != ButtonType.OK) {
                        return;
                    }

                    try {
                        if (currentUser != null && currentUser.getId() > 0) {
                            objectifService.deleteForUser(obj.getId(), currentUser.getId());
                        } else {
                            objectifService.delete(obj.getId());
                        }
                        loadObjectifs();
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

    private void setupMonteCarloColumn() {
        colMonteCarlo.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("MC");

            {
                btn.getStyleClass().add("table-edit-button");
                btn.setTooltip(new Tooltip("Simulation Monte Carlo (GBM)"));
                btn.setOnAction(event -> {
                    Objectif objectif = getTableView().getItems().get(getIndex());
                    openMonteCarloWindow(objectif);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void openMonteCarloWindow(Objectif objectif) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Monte Carlo — " + objectif.getName());

        double vMarket = currentFor(objectif);
        double v0 = vMarket > 0 ? vMarket : objectif.getInitialAmount();

        Label v0Explain = new Label();
        if (vMarket > 0) {
            v0Explain.setText(String.format(Locale.FRENCH,
                    "Valeur de marché actuelle (V₀) : %.2f USD", vMarket));
        } else if (objectif.getInitialAmount() > 0) {
            v0Explain.setText(String.format(Locale.FRENCH,
                    "Pas de positions liées ou prix nuls — V₀ = montant initial déclaré : %.2f USD",
                    objectif.getInitialAmount()));
        } else {
            v0Explain.setText("Impossible de définir V₀ : ajoutez un montant initial ou des investissements liés.");
        }

        Label targetLbl = new Label(String.format(Locale.FRENCH,
                "Cible : %.2f USD", objectif.getTargetAmount()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(4, 0, 4, 0));

        TextField horizonField = new TextField(String.format(Locale.FRENCH, "%.2f",
                ObjectifMonteCarloService.DEFAULT_HORIZON_YEARS));
        TextField volField = new TextField(String.format(Locale.FRENCH, "%.0f",
                ObjectifMonteCarloService.DEFAULT_ANNUAL_VOLATILITY * 100));
        TextField nField = new TextField(String.valueOf(ObjectifMonteCarloService.DEFAULT_SIMULATIONS));

        int row = 0;
        grid.add(new Label("Horizon (années)"), 0, row);
        grid.add(horizonField, 1, row++);
        grid.add(new Label("Volatilité annuelle σ (%)"), 0, row);
        grid.add(volField, 1, row++);
        grid.add(new Label("Nombre de simulations"), 0, row);
        grid.add(nField, 1, row++);

        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setText("Modèle : mouvement brownien géométrique (une étape à l’horizon). "
                + "Réglez l’horizon, la volatilité et le nombre de simulations, puis lancez.\n"
                + "La tendance annuelle (μ) est estimée automatiquement à partir de votre objectif "
                + "(montant initial → valeur actuelle, bornée) — vous n’avez pas à la saisir.");

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Valeur finale V_T (tranches USD)");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Fréquence");
        BarChart<String, Number> histChart = new BarChart<>(xAxis, yAxis);
        histChart.setLegendVisible(false);
        histChart.setAnimated(false);
        histChart.setPrefHeight(280);

        Button runBtn = new Button("Lancer la simulation");
        runBtn.getStyleClass().add("cyan-pill-button");
        ProgressIndicator busy = new ProgressIndicator();
        busy.setMaxSize(28, 28);
        busy.setVisible(false);

        HBox actions = new HBox(12, runBtn, busy);
        actions.setAlignment(Pos.CENTER_LEFT);

        runBtn.setOnAction(e -> {
            double vSim = currentFor(objectif) > 0 ? currentFor(objectif) : objectif.getInitialAmount();
            if (vSim <= 0) {
                resultArea.setText("V₀ doit être strictement positif pour lancer la simulation.");
                return;
            }
            try {
                double horizon = parseDoubleLoose(horizonField.getText());
                double volPct = parseDoubleLoose(volField.getText());
                int nSims = (int) Math.round(parseDoubleLoose(nField.getText()));
                double muAnnual = ObjectifMonteCarloService.impliedAnnualDriftCapped(objectif,
                        Math.max(vSim, 1e-6));

                runBtn.setDisable(true);
                busy.setVisible(true);

                Task<ObjectifMonteCarloService.SimulationResult> task = new Task<>() {
                    @Override
                    protected ObjectifMonteCarloService.SimulationResult call() {
                        return ObjectifMonteCarloService.simulate(
                                vSim,
                                objectif.getTargetAmount(),
                                objectif.isCompleted(),
                                horizon,
                                muAnnual,
                                volPct / 100.0,
                                nSims,
                                System.nanoTime()
                        );
                    }
                };

                task.setOnSucceeded(ev -> {
                    runBtn.setDisable(false);
                    busy.setVisible(false);
                    ObjectifMonteCarloService.SimulationResult res = task.getValue();
                    resultArea.setText(ObjectifMonteCarloService.formatSummary(res, Locale.FRENCH));
                    histChart.getData().clear();
                    if (!res.objectiveAlreadyCompleted && res.histogramCategories.length > 0) {
                        XYChart.Series<String, Number> series = new XYChart.Series<>();
                        series.setName("Simulations");
                        for (int i = 0; i < res.histogramCategories.length; i++) {
                            series.getData().add(new XYChart.Data<>(
                                    res.histogramCategories[i], res.histogramCounts[i]));
                        }
                        histChart.getData().add(series);
                    }
                });

                task.setOnFailed(ev -> {
                    runBtn.setDisable(false);
                    busy.setVisible(false);
                    Throwable ex = task.getException();
                    resultArea.setText(ex != null ? ex.getMessage() : "Erreur inconnue.");
                });

                Thread t = new Thread(task);
                t.setDaemon(true);
                t.start();
            } catch (NumberFormatException ex) {
                resultArea.setText("Vérifiez les nombres (utilisez le point ou la virgule pour les décimales).");
            }
        });

        VBox top = new VBox(8, v0Explain, targetLbl, grid, actions);
        VBox.setVgrow(resultArea, Priority.ALWAYS);
        VBox rootBox = new VBox(12, top, resultArea, histChart);
        rootBox.setPadding(new Insets(16));
        rootBox.setStyle("-fx-background-color: white;");

        Scene scene = new Scene(rootBox, 720, 720);
        ThemeManager.registerScene(scene);
        stage.setScene(scene);
        stage.show();
    }

    private static double parseDoubleLoose(String s) {
        if (s == null || s.isBlank()) {
            throw new NumberFormatException("vide");
        }
        return Double.parseDouble(s.trim().replace(',', '.'));
    }

    private void openAiAnalysisWindow(Objectif objectif) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("AI Analysis - " + objectif.getName());

        TextArea resultArea = new TextArea();
        resultArea.setEditable(false);
        resultArea.setWrapText(true);
        resultArea.setPrefWidth(650);
        resultArea.setPrefHeight(450);
        resultArea.setText("Analyzing objective with AI...");

        ProgressIndicator progressIndicator = new ProgressIndicator();

        VBox content = new VBox(12, progressIndicator, resultArea);
        content.setStyle("-fx-padding: 20; -fx-background-color: white;");

        BorderPane root = new BorderPane();
        root.setCenter(content);

        Scene scene = new Scene(root, 700, 520);
        ThemeManager.registerScene(scene);
        stage.setScene(scene);
        stage.show();

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                List<Investissement> linkedInvestments =
                        (currentUser != null && currentUser.getId() > 0)
                                ? objectifService.getLinkedByUser(objectif.getId(), currentUser.getId())
                                : objectifService.getLinked(objectif.getId());
                return groqAiService.analyzeObjective(objectif, linkedInvestments);
            }
        };

        task.setOnSucceeded(event -> {
            progressIndicator.setVisible(false);
            resultArea.setText(task.getValue());
        });

        task.setOnFailed(event -> {
            progressIndicator.setVisible(false);

            Throwable exception = task.getException();
            String message = exception != null ? exception.getMessage() : "Unknown error";

            resultArea.setText("""
                    Failed to generate AI analysis.

                    Details:
                    """ + message);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadObjectifs() {
        try {
            List<Objectif> list = (currentUser != null && currentUser.getId() > 0)
                    ? objectifService.getAllByUser(currentUser.getId())
                    : List.of();

            for (Objectif obj : list) {
                if (currentUser != null && currentUser.getId() > 0) {
                    objectifService.checkAndMarkCompletedForUser(obj.getId(), currentUser.getId());
                } else {
                    objectifService.checkAndMarkCompleted(obj.getId());
                }
            }

            list = (currentUser != null && currentUser.getId() > 0)
                    ? objectifService.getAllByUser(currentUser.getId())
                    : List.of();
            currentValueCache = (currentUser != null && currentUser.getId() > 0)
                    ? objectifService.getCurrentValuesAllByUser(currentUser.getId())
                    : Map.of();
            objectifBackingList.setAll(list);
            objectifTable.refresh();
            updateExecutiveSummary();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateExecutiveSummary() {
        ObjectifStatistics.Snapshot snap =
                ObjectifStatistics.compute(objectifBackingList, currentValueCache);

        if (summaryLine1 != null) {
            summaryLine1.setText(String.format(Locale.FRENCH,
                    "Objectifs : %d — Complétés : %d — En cours : %d",
                    snap.total, snap.completedCount, snap.total - snap.completedCount));
        }
        if (summaryLine2 != null) {
            summaryLine2.setText(String.format(Locale.FRENCH,
                    "Valeur de marché cumulée : %.2f USD  ·  Cible cumulée (non complétés) : %.2f USD",
                    snap.sumMarketUsd, snap.sumTargetOpenUsd));
        }
        if (summaryLine3 != null) {
            summaryLine3.setText(String.format(Locale.FRENCH,
                    "Objectifs critiques en retard fort (< 35 %% de la cible) : %d",
                    snap.critiquesRetard));
        }

        if (statCompletionPct != null) {
            statCompletionPct.setText(String.format(Locale.FRENCH, "%.0f %%", snap.completionRatePercent));
        }
        if (statAvgProgress != null) {
            statAvgProgress.setText(String.format(Locale.FRENCH, "%.1f %%", snap.avgProgressOpenPercent));
        }
        if (statWeightedRoi != null) {
            statWeightedRoi.setText(String.format(Locale.FRENCH, "%+.1f %%", snap.weightedAvgRoiPercent));
        }
        if (statSumInitial != null) {
            statSumInitial.setText(String.format(Locale.FRENCH, "%.2f USD", snap.sumInitialUsd));
        }
        if (statSumRemaining != null) {
            statSumRemaining.setText(String.format(Locale.FRENCH, "%.2f USD", snap.sumRemainingOpenUsd));
        }

        updateCharts(snap);
    }

    private void updateCharts(ObjectifStatistics.Snapshot s) {
        if (statusPieChart != null) {
            statusPieChart.getData().clear();
            if (s.total == 0) {
                statusPieChart.getData().add(new PieChart.Data("Aucun objectif", 1));
            } else {
                statusPieChart.getData().add(new PieChart.Data("Complété", s.completedCount));
                statusPieChart.getData().add(new PieChart.Data("En cours", s.total - s.completedCount));
            }
        }
        if (prioriteBarChart != null) {
            prioriteBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Objectifs");
            series.getData().add(new XYChart.Data<>("Basse", s.countBasse));
            series.getData().add(new XYChart.Data<>("Normale", s.countNormale));
            series.getData().add(new XYChart.Data<>("Haute", s.countHaute));
            series.getData().add(new XYChart.Data<>("Critique", s.countCritique));
            prioriteBarChart.getData().add(series);
        }
    }

    @FXML
    private void refreshObjectifs() {
        loadObjectifs();
    }

    @FXML
    private void exportObjectifsPdf() {
        Stage stage = (Stage) objectifTable.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter les objectifs (PDF)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("decides-objectifs.pdf");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            List<Objectif> rows = List.copyOf(objectifTable.getItems());
            ObjectifPdfExportService.export(file, rows, currentValueCache);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("PDF");
            ok.setHeaderText(null);
            ok.setContentText("Fichier enregistré :\n" + file.getAbsolutePath());
            ok.showAndWait();
        } catch (DocumentException | IOException ex) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("PDF");
            err.setHeaderText(null);
            err.setContentText(ex.getMessage());
            err.showAndWait();
        }
    }

    @FXML
    public void goToCreate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Invest/createObjectif.fxml"));
            Scene scene = new Scene(loader.load());
            CreateObjectifController controller = loader.getController();
            if (currentUser != null) {
                controller.setUser(currentUser);
            }

            Stage stage = new Stage();
            stage.setTitle("Créer un objectif");
            ThemeManager.registerScene(scene);
            stage.setScene(scene);
            stage.setUserData(currentUser);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            loadObjectifs();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
