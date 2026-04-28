package pi.controllers.ImprevusCasreelController;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pi.entities.CasRelles;
import pi.services.ImprevusCasreelService.CasReelService;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ImprevusStatsController {

    @FXML private PieChart typePieChart;
    @FXML private PieChart paymentPieChart;
    @FXML private PieChart statusPieChart;
    @FXML private PieChart monthlyRiskPieChart;

    @FXML private BarChart<String, Number> budgetBarChart;
    @FXML private BarChart<String, Number> caseTypeBarChart;
    @FXML private BarChart<String, Number> monthlyOccurrenceBarChart;

    @FXML private VBox totalCard;
    @FXML private VBox budgetCard;
    @FXML private VBox monthSafetyCard;
    @FXML private VBox occurrenceCard;

    private final CasReelService casReelService = new CasReelService();

    @FXML
    public void initialize() {
        List<CasRelles> cases = casReelService.afficher();

        int total = cases.size();
        int gains = countByType(cases, "Gain");
        int depenses = countByType(cases, "Depense");

        double totalGain = sumByType(cases, "Gain");
        double totalDepense = sumByType(cases, "Depense");
        double balance = totalGain - totalDepense;

        LocalDate now = LocalDate.now();

        List<CasRelles> thisMonthCases = cases.stream()
                .filter(c -> c.getDateEffet() != null)
                .filter(c -> c.getDateEffet().getMonth() == now.getMonth())
                .filter(c -> c.getDateEffet().getYear() == now.getYear())
                .toList();

        int monthlyOccurrences = thisMonthCases.size();
        String safety = monthlyOccurrences <= 3 ? "Safe" :
                monthlyOccurrences <= 7 ? "Medium Risk" : "High Risk";

        fillCard(totalCard, "Total cas", String.valueOf(total));
        fillCard(budgetCard, "Balance budget", formatMoney(balance));
        fillCard(monthSafetyCard, "Safety ce mois", safety);
        fillCard(occurrenceCard, "Occurrences ce mois", String.valueOf(monthlyOccurrences));

        setupTypePie(gains, depenses);
        setupPaymentPie(cases);
        setupStatusPie(cases);
        setupMonthlyRiskPie(monthlyOccurrences);

        setupBudgetBar(totalGain, totalDepense, balance);
        setupCaseTypeBar(gains, depenses);
        setupMonthlyOccurrenceBar(thisMonthCases);
    }

    private void setupTypePie(int gains, int depenses) {
        typePieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Gains", gains),
                new PieChart.Data("Dépenses", depenses)
        ));
    }

    private void setupPaymentPie(List<CasRelles> cases) {
        Map<String, Integer> map = new LinkedHashMap<>();

        for (CasRelles c : cases) {
            String payment = c.getPaymentMethod() == null || c.getPaymentMethod().isBlank()
                    ? "Non défini"
                    : c.getPaymentMethod();

            map.put(payment, map.getOrDefault(payment, 0) + 1);
        }

        paymentPieChart.setData(FXCollections.observableArrayList(
                map.entrySet().stream()
                        .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                        .toList()
        ));
    }

    private void setupStatusPie(List<CasRelles> cases) {
        Map<String, Integer> map = new LinkedHashMap<>();

        for (CasRelles c : cases) {
            String status = c.getResultat() == null || c.getResultat().isBlank()
                    ? "En attente"
                    : c.getResultat();

            map.put(status, map.getOrDefault(status, 0) + 1);
        }

        statusPieChart.setData(FXCollections.observableArrayList(
                map.entrySet().stream()
                        .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                        .toList()
        ));
    }

    private void setupMonthlyRiskPie(int monthlyOccurrences) {
        int safe = monthlyOccurrences <= 3 ? 1 : 0;
        int medium = monthlyOccurrences > 3 && monthlyOccurrences <= 7 ? 1 : 0;
        int high = monthlyOccurrences > 7 ? 1 : 0;

        monthlyRiskPieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Safe", safe),
                new PieChart.Data("Medium Risk", medium),
                new PieChart.Data("High Risk", high)
        ));
    }

    private void setupBudgetBar(double totalGain, double totalDepense, double balance) {
        budgetBarChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Gains", totalGain));
        series.getData().add(new XYChart.Data<>("Dépenses", totalDepense));
        series.getData().add(new XYChart.Data<>("Balance", balance));

        budgetBarChart.getData().add(series);
    }

    private void setupCaseTypeBar(int gains, int depenses) {
        caseTypeBarChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Gains", gains));
        series.getData().add(new XYChart.Data<>("Dépenses", depenses));

        caseTypeBarChart.getData().add(series);
    }

    private void setupMonthlyOccurrenceBar(List<CasRelles> monthCases) {
        monthlyOccurrenceBarChart.setLegendVisible(false);

        Map<String, Integer> map = new LinkedHashMap<>();

        for (CasRelles c : monthCases) {
            String category = c.getCategorie() == null || c.getCategorie().isBlank()
                    ? "Autres"
                    : c.getCategorie();

            map.put(category, map.getOrDefault(category, 0) + 1);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        map.forEach((category, count) ->
                series.getData().add(new XYChart.Data<>(category, count))
        );

        monthlyOccurrenceBarChart.getData().add(series);
    }

    private int countByType(List<CasRelles> cases, String type) {
        return (int) cases.stream()
                .filter(c -> type.equalsIgnoreCase(c.getType()))
                .count();
    }

    private double sumByType(List<CasRelles> cases, String type) {
        return cases.stream()
                .filter(c -> type.equalsIgnoreCase(c.getType()))
                .mapToDouble(CasRelles::getMontant)
                .sum();
    }

    private void fillCard(VBox card, String title, String value) {
        card.getChildren().clear();

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stats-card-label");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stats-card-value");

        card.getChildren().addAll(titleLabel, valueLabel);
    }

    private String formatMoney(double value) {
        return String.format("%.2f DT", value);
    }


    @FXML
    private void handleBack(ActionEvent event) {
        try {
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/imprevus-view.fxml"));
            Scene scene = new Scene(loader.load());

            stage.setTitle("Unexpected Events & Real Cases");
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}