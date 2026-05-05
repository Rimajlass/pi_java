package pi.controllers.UserTransactionController;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import pi.entities.Transaction;
import pi.entities.User;
import pi.mains.Main;
import pi.services.UserTransactionService.TransactionService;
import pi.tools.FxmlResources;
import pi.tools.MyDatabase;
import pi.tools.ThemeManager;

import java.io.IOException;
import java.io.File;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AnalyticsController {

    @FXML
    private AnchorPane analyticsRoot;
    @FXML
    private BarChart<String, Number> incomeExpenseChart;
    @FXML
    private LineChart<String, Number> spendingTrendChart;
    @FXML
    private PieChart expensesPieChart;
    @FXML
    private Label kpiIncomeValueLabel;
    @FXML
    private Label kpiExpenseValueLabel;
    @FXML
    private Label kpiNetValueLabel;
    @FXML
    private Label kpiRateValueLabel;
    @FXML
    private Label kpiIncomeDeltaLabel;
    @FXML
    private Label kpiExpenseDeltaLabel;
    @FXML
    private Label kpiNetDeltaLabel;
    @FXML
    private Label kpiRateDeltaLabel;
    @FXML
    private Label donutTotalValueLabel;
    @FXML
    private Label healthScoreLabel;
    @FXML
    private Label merchant1NameLabel;
    @FXML
    private Label merchant2NameLabel;
    @FXML
    private Label merchant3NameLabel;
    @FXML
    private Label merchant4NameLabel;
    @FXML
    private Label merchant5NameLabel;
    @FXML
    private Label merchant1ValueLabel;
    @FXML
    private Label merchant2ValueLabel;
    @FXML
    private Label merchant3ValueLabel;
    @FXML
    private Label merchant4ValueLabel;
    @FXML
    private Label merchant5ValueLabel;
    @FXML
    private ProgressBar merchant1Bar;
    @FXML
    private ProgressBar merchant2Bar;
    @FXML
    private ProgressBar merchant3Bar;
    @FXML
    private ProgressBar merchant4Bar;
    @FXML
    private ProgressBar merchant5Bar;

    private final TransactionService transactionService = new TransactionService();
    private final DecimalFormat amountFormat = new DecimalFormat("#,##0");
    private final DecimalFormat oneDecimal = new DecimalFormat("0.0");

    private User currentUser;

    @FXML
    private void initialize() {
        loadAnalyticsData();
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadAnalyticsData();
    }

    @FXML
    private void handleGoProfile() {
        openView("/pi/mains/salary-profile-view.fxml", "/pi/styles/salary-profile.css", "My Salary Profile");
    }

    @FXML
    private void handleGoDashboard() {
        openView("/pi/mains/salary-home-view.fxml", "/pi/styles/salary-home.css", "Salary Home");
    }

    @FXML
    private void handleLogout() {
        openView("/pi/mains/login-view.fxml", "/pi/styles/login.css", "User Secure Login", false);
    }

    @FXML
    public void handleExportCsv() {
        User user = resolveCurrentUser();
        if (user == null || user.getId() <= 0) {
            showInfo("Export", "Aucune donnee utilisateur disponible pour l'export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Analytics CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        chooser.setInitialFileName("analytics-" + LocalDate.now() + ".csv");

        Stage stage = resolveStage();
        if (stage == null) {
            return;
        }
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        file = ensureExtension(file, ".csv");

        List<Transaction> transactions = transactionService.findByUserId(user.getId());
        double income = 0.0;
        double expense = 0.0;
        for (Transaction tx : transactions) {
            if ("SAVING".equalsIgnoreCase(tx.getType())) {
                income += Math.max(0.0, tx.getMontant());
            } else if ("EXPENSE".equalsIgnoreCase(tx.getType())) {
                expense += Math.max(0.0, tx.getMontant());
            }
        }
        double net = income - expense;
        double rate = income > 0 ? (net / income) * 100.0 : 0.0;

        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("\uFEFF");
            writer.write("Decide$ Analytics Export");
            writer.newLine();
            writer.write("Generated At," + LocalDate.now());
            writer.newLine();
            writer.write("User," + escapeCsv(user.getNom() == null ? ("user-" + user.getId()) : user.getNom()));
            writer.newLine();
            writer.write("Total Income," + formatAmount(income) + " TND");
            writer.newLine();
            writer.write("Total Expenses," + formatAmount(expense) + " TND");
            writer.newLine();
            writer.write("Net Savings," + formatAmount(net) + " TND");
            writer.newLine();
            writer.write("Savings Rate," + oneDecimal.format(rate) + "%");
            writer.newLine();
            writer.newLine();
            writer.write("Date,Type,Amount (TND),Description,Source");
            writer.newLine();
            for (Transaction tx : transactions) {
                String date = tx.getDate() == null ? "" : tx.getDate().toString();
                String type = tx.getType() == null ? "" : tx.getType();
                String amount = oneDecimal.format(Math.max(0.0, tx.getMontant()));
                String description = tx.getDescription() == null ? "" : tx.getDescription();
                String source = tx.getModuleSource() == null ? "" : tx.getModuleSource();
                writer.write(
                        escapeCsv(date) + "," +
                                escapeCsv(type) + "," +
                                escapeCsv(amount) + "," +
                                escapeCsv(description) + "," +
                                escapeCsv(source)
                );
                writer.newLine();
            }
            showInfo("Export", "Export CSV reussi:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            showError("Echec export CSV: " + e.getMessage());
        } catch (Exception e) {
            showError("Echec export CSV: " + e.getMessage());
        }
    }

    @FXML
    public void handleExportPdf() {
        User user = resolveCurrentUser();
        if (user == null || user.getId() <= 0) {
            showInfo("Export", "Aucune donnee utilisateur disponible pour l'export.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Analytics PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        chooser.setInitialFileName("analytics-summary-" + LocalDate.now() + ".pdf");

        Stage stage = resolveStage();
        if (stage == null) {
            return;
        }
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        file = ensureExtension(file, ".pdf");

        List<Transaction> transactions = transactionService.findByUserId(user.getId());
        try {
            writeSimplePdf(file, user, transactions);
            showInfo("Export", "Export PDF reussi:\n" + file.getAbsolutePath());
        } catch (IOException e) {
            showError("Echec export PDF: " + e.getMessage());
        } catch (Exception e) {
            showError("Echec export PDF: " + e.getMessage());
        }
    }

    private void openView(String fxmlPath, String cssPath, String title) {
        openView(fxmlPath, cssPath, title, true);
    }

    private void openView(String fxmlPath, String cssPath, String title, boolean keepUser) {
        Stage stage = resolveStage();
        if (stage == null) {
            return;
        }

        try {
            FXMLLoader loader = FxmlResources.load(Main.class, fxmlPath);
            Parent root = loader.getRoot();
            Object controller = loader.getController();

            if (keepUser && resolveCurrentUser() != null) {
                User user = resolveCurrentUser();
                if (controller instanceof SalaryProfileController salaryProfileController) {
                    salaryProfileController.setUser(user);
                } else if (controller instanceof SalaryHomeController salaryHomeController) {
                    salaryHomeController.setUser(user);
                }
                stage.setUserData(user);
            } else {
                stage.setUserData(null);
            }

            Scene scene = new Scene(root, 1460, 780);
            scene.getStylesheets().add(Main.class.getResource(cssPath).toExternalForm());
            ThemeManager.registerScene(scene);

            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Unable to open view: " + fxmlPath, e);
        }
    }

    private void loadAnalyticsData() {
        if (incomeExpenseChart == null || spendingTrendChart == null || expensesPieChart == null) {
            return;
        }
        User user = resolveCurrentUser();
        if (user == null || user.getId() <= 0) {
            initChartsWithFallback();
            return;
        }

        List<Transaction> transactions = transactionService.findByUserId(user.getId());
        Map<String, Double> incomeByDay = initSevenDayMap();
        Map<String, Double> expenseByDay = initSevenDayMap();

        double totalIncome = 0.0;
        double totalExpense = 0.0;
        YearMonth now = YearMonth.now();
        YearMonth prev = now.minusMonths(1);
        double thisIncome = 0.0;
        double thisExpense = 0.0;
        double prevIncome = 0.0;
        double prevExpense = 0.0;

        for (Transaction tx : transactions) {
            if (tx.getDate() == null) {
                continue;
            }
            String bucket = toBucketLabel(tx.getDate().getDayOfMonth());
            String type = tx.getType() == null ? "" : tx.getType().toUpperCase(Locale.ROOT);
            double amount = Math.max(0.0, tx.getMontant());
            YearMonth ym = YearMonth.from(tx.getDate());

            if ("SAVING".equals(type)) {
                totalIncome += amount;
                incomeByDay.merge(bucket, amount, Double::sum);
                if (ym.equals(now)) {
                    thisIncome += amount;
                } else if (ym.equals(prev)) {
                    prevIncome += amount;
                }
            } else if ("EXPENSE".equals(type)) {
                totalExpense += amount;
                expenseByDay.merge(bucket, amount, Double::sum);
                if (ym.equals(now)) {
                    thisExpense += amount;
                } else if (ym.equals(prev)) {
                    prevExpense += amount;
                }
            }
        }

        double net = totalIncome - totalExpense;
        double savingsRate = totalIncome > 0 ? (net / totalIncome) * 100.0 : 0.0;

        setLabel(kpiIncomeValueLabel, formatAmount(totalIncome) + " TND");
        setLabel(kpiExpenseValueLabel, formatAmount(totalExpense) + " TND");
        setLabel(kpiNetValueLabel, formatAmount(net) + " TND");
        setLabel(kpiRateValueLabel, oneDecimal.format(savingsRate) + "%");
        setLabel(kpiIncomeDeltaLabel, signedPct(thisIncome, prevIncome));
        setLabel(kpiExpenseDeltaLabel, signedPct(thisExpense, prevExpense));
        setLabel(kpiNetDeltaLabel, signedPct(thisIncome - thisExpense, prevIncome - prevExpense));
        setLabel(kpiRateDeltaLabel, signedPct(
                thisIncome > 0 ? ((thisIncome - thisExpense) / thisIncome) * 100.0 : 0.0,
                prevIncome > 0 ? ((prevIncome - prevExpense) / prevIncome) * 100.0 : 0.0
        ));
        setLabel(donutTotalValueLabel, formatAmount(totalExpense));

        int healthScore = computeHealthScore(savingsRate, totalIncome, totalExpense);
        setLabel(healthScoreLabel, String.valueOf(healthScore));

        fillIncomeExpenseChart(incomeByDay, expenseByDay);
        fillTrendChart(expenseByDay);
        fillExpensePieFromDb(user.getId(), totalExpense);
        fillTopMerchants(transactions);
    }

    private void initChartsWithFallback() {
        Map<String, Double> income = new LinkedHashMap<>();
        Map<String, Double> expense = new LinkedHashMap<>();
        for (String key : List.of("May 1", "May 6", "May 11", "May 16", "May 21", "May 26", "May 31")) {
            income.put(key, 0.0);
            expense.put(key, 0.0);
        }
        fillIncomeExpenseChart(income, expense);
        fillTrendChart(expense);
        expensesPieChart.setData(FXCollections.observableArrayList(new PieChart.Data("No data", 1)));
        setLabel(donutTotalValueLabel, "0");
    }

    private void fillIncomeExpenseChart(Map<String, Double> incomeByDay, Map<String, Double> expenseByDay) {
        XYChart.Series<String, Number> income = new XYChart.Series<>();
        income.setName("Income");
        XYChart.Series<String, Number> expenses = new XYChart.Series<>();
        expenses.setName("Expenses");

        for (String key : incomeByDay.keySet()) {
            income.getData().add(new XYChart.Data<>(key, incomeByDay.getOrDefault(key, 0.0)));
            expenses.getData().add(new XYChart.Data<>(key, expenseByDay.getOrDefault(key, 0.0)));
        }
        incomeExpenseChart.getData().setAll(income, expenses);
        applyBarSeriesStyle(incomeExpenseChart);
    }

    private void fillTrendChart(Map<String, Double> expenseByDay) {
        XYChart.Series<String, Number> trend = new XYChart.Series<>();
        trend.setName("Spending");
        for (String key : expenseByDay.keySet()) {
            trend.getData().add(new XYChart.Data<>(key, expenseByDay.getOrDefault(key, 0.0)));
        }
        spendingTrendChart.getData().setAll(trend);
        applyLineSeriesStyle(spendingTrendChart);
    }

    private void fillExpensePieFromDb(int userId, double totalExpense) {
        String sql = """
                SELECT COALESCE(NULLIF(TRIM(category), ''), 'Others') AS cat, SUM(amount) AS total
                FROM expense
                WHERE user_id = ?
                GROUP BY COALESCE(NULLIF(TRIM(category), ''), 'Others')
                ORDER BY total DESC
                """;

        List<PieChart.Data> data = new ArrayList<>();
        try (Connection cnx = MyDatabase.getInstance().getCnx();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.add(new PieChart.Data(rs.getString("cat"), rs.getDouble("total")));
                }
            }
        } catch (SQLException ignored) {
        }

        if (data.isEmpty()) {
            data.add(new PieChart.Data("Others", Math.max(totalExpense, 1)));
        }
        expensesPieChart.setData(FXCollections.observableArrayList(data));
        applyPieSliceColors(expensesPieChart);
    }

    private void fillTopMerchants(List<Transaction> transactions) {
        Map<String, Double> byMerchant = new LinkedHashMap<>();
        for (Transaction tx : transactions) {
            if (tx.getType() == null || !"EXPENSE".equalsIgnoreCase(tx.getType())) {
                continue;
            }
            String merchant = normalizeMerchant(tx.getDescription());
            byMerchant.merge(merchant, Math.max(0.0, tx.getMontant()), Double::sum);
        }

        List<Map.Entry<String, Double>> top = byMerchant.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .toList();

        if (top.isEmpty()) {
            top = List.of(
                    Map.entry("No merchant", 0.0),
                    Map.entry("No merchant", 0.0),
                    Map.entry("No merchant", 0.0),
                    Map.entry("No merchant", 0.0),
                    Map.entry("No merchant", 0.0)
            );
        } else if (top.size() < 5) {
            List<Map.Entry<String, Double>> pad = new ArrayList<>(top);
            while (pad.size() < 5) {
                pad.add(Map.entry("-", 0.0));
            }
            top = pad;
        }

        double max = Math.max(1.0, top.get(0).getValue());
        setMerchantRow(1, top.get(0), max);
        setMerchantRow(2, top.get(1), max);
        setMerchantRow(3, top.get(2), max);
        setMerchantRow(4, top.get(3), max);
        setMerchantRow(5, top.get(4), max);
    }

    private void setMerchantRow(int idx, Map.Entry<String, Double> row, double max) {
        String name = row.getKey();
        String value = formatAmount(row.getValue()) + " TND";
        double progress = Math.min(1.0, row.getValue() / max);

        switch (idx) {
            case 1 -> {
                setLabel(merchant1NameLabel, name);
                setLabel(merchant1ValueLabel, value);
                if (merchant1Bar != null) merchant1Bar.setProgress(progress);
            }
            case 2 -> {
                setLabel(merchant2NameLabel, name);
                setLabel(merchant2ValueLabel, value);
                if (merchant2Bar != null) merchant2Bar.setProgress(progress);
            }
            case 3 -> {
                setLabel(merchant3NameLabel, name);
                setLabel(merchant3ValueLabel, value);
                if (merchant3Bar != null) merchant3Bar.setProgress(progress);
            }
            case 4 -> {
                setLabel(merchant4NameLabel, name);
                setLabel(merchant4ValueLabel, value);
                if (merchant4Bar != null) merchant4Bar.setProgress(progress);
            }
            case 5 -> {
                setLabel(merchant5NameLabel, name);
                setLabel(merchant5ValueLabel, value);
                if (merchant5Bar != null) merchant5Bar.setProgress(progress);
            }
            default -> { }
        }
    }

    private Map<String, Double> initSevenDayMap() {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("May 1", 0.0);
        map.put("May 6", 0.0);
        map.put("May 11", 0.0);
        map.put("May 16", 0.0);
        map.put("May 21", 0.0);
        map.put("May 26", 0.0);
        map.put("May 31", 0.0);
        return map;
    }

    private String toBucketLabel(int day) {
        if (day <= 1) return "May 1";
        if (day <= 6) return "May 6";
        if (day <= 11) return "May 11";
        if (day <= 16) return "May 16";
        if (day <= 21) return "May 21";
        if (day <= 26) return "May 26";
        return "May 31";
    }

    private String normalizeMerchant(String description) {
        if (description == null || description.isBlank()) {
            return "Unknown";
        }
        String cleaned = description.trim().replaceAll("[^\\p{L}\\p{N} ]", " ");
        String[] parts = cleaned.split("\\s+");
        if (parts.length == 0) {
            return "Unknown";
        }
        return parts[0].substring(0, 1).toUpperCase(Locale.ROOT) + parts[0].substring(1).toLowerCase(Locale.ROOT);
    }

    private int computeHealthScore(double savingsRate, double income, double expense) {
        if (income <= 0) {
            return 35;
        }
        double ratio = expense / income;
        int score = (int) Math.round(100.0 - ratio * 70.0 + Math.max(0, savingsRate) * 0.3);
        return Math.max(0, Math.min(100, score));
    }

    private String formatAmount(double value) {
        return amountFormat.format(Math.max(0.0, value));
    }

    private String signedPct(double current, double previous) {
        if (Math.abs(previous) < 0.0001) {
            return "+0.0%";
        }
        double pct = ((current - previous) / Math.abs(previous)) * 100.0;
        String sign = pct >= 0 ? "+" : "";
        return sign + oneDecimal.format(pct) + "%";
    }

    private void setLabel(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private User resolveCurrentUser() {
        if (currentUser != null) {
            return currentUser;
        }
        Stage stage = resolveStage();
        if (stage != null && stage.getUserData() instanceof User user) {
            currentUser = user;
            return user;
        }
        return null;
    }

    private Stage resolveStage() {
        if (analyticsRoot != null && analyticsRoot.getScene() != null && analyticsRoot.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }

    private String buildAnalyticsSummary(User user, List<Transaction> transactions) {
        double income = 0;
        double expense = 0;
        for (Transaction tx : transactions) {
            if (tx.getType() == null) continue;
            if ("SAVING".equalsIgnoreCase(tx.getType())) {
                income += Math.max(0.0, tx.getMontant());
            } else if ("EXPENSE".equalsIgnoreCase(tx.getType())) {
                expense += Math.max(0.0, tx.getMontant());
            }
        }
        double net = income - expense;
        double rate = income > 0 ? (net / income) * 100.0 : 0.0;
        String userName = user.getNom() == null || user.getNom().isBlank() ? ("User #" + user.getId()) : user.getNom();
        return "Decide$ Analytics Report\n"
                + "Generated: " + LocalDate.now() + "\n"
                + "User: " + userName + "\n"
                + "----------------------------------------\n"
                + "Total Income:   " + formatAmount(income) + " TND\n"
                + "Total Expenses: " + formatAmount(expense) + " TND\n"
                + "Net Savings:    " + formatAmount(net) + " TND\n"
                + "Savings Rate:   " + oneDecimal.format(rate) + "%\n"
                + "Transactions:   " + transactions.size() + "\n"
                + "----------------------------------------\n";
    }

    private void writeSimplePdf(File file, User user, List<Transaction> transactions) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("BT /F1 18 Tf 50 800 Td (").append(pdfEscape("Decide$ Analytics Export")).append(") Tj ET\n");
        content.append("BT /F1 11 Tf 50 780 Td (").append(pdfEscape("Generated: " + LocalDate.now())).append(") Tj ET\n");

        String summary = buildAnalyticsSummary(user, transactions);
        String[] summaryLines = summary.split("\\n");
        int y = 760;
        for (String line : summaryLines) {
            content.append("BT /F1 11 Tf 50 ").append(y).append(" Td (").append(pdfEscape(line)).append(") Tj ET\n");
            y -= 15;
        }

        content.append("BT /F1 12 Tf 50 ").append(y - 8).append(" Td (").append(pdfEscape("Recent Transactions")).append(") Tj ET\n");
        y -= 28;
        content.append("BT /F1 10 Tf 50 ").append(y).append(" Td (")
                .append(pdfEscape(String.format("%-12s %-10s %-12s %-38s", "Date", "Type", "Amount", "Description")))
                .append(") Tj ET\n");
        y -= 14;
        content.append("BT /F1 10 Tf 50 ").append(y).append(" Td (")
                .append(pdfEscape("--------------------------------------------------------------------------"))
                .append(") Tj ET\n");
        y -= 16;

        int limit = Math.min(18, transactions.size());
        for (int i = 0; i < limit; i++) {
            Transaction tx = transactions.get(i);
            String date = tx.getDate() == null ? "-" : tx.getDate().toString();
            String type = tx.getType() == null ? "-" : tx.getType();
            String amount = oneDecimal.format(Math.max(0.0, tx.getMontant())) + " TND";
            String description = tx.getDescription() == null ? "-" : tx.getDescription().trim();
            if (description.length() > 36) {
                description = description.substring(0, 36) + "...";
            }
            String row = String.format("%-12s %-10s %-12s %-38s", date, type, amount, description);
            content.append("BT /F1 10 Tf 50 ").append(y).append(" Td (").append(pdfEscape(row)).append(") Tj ET\n");
            y -= 14;
            if (y < 60) {
                break;
            }
        }

        String stream = content.toString();
        byte[] streamBytes = stream.getBytes(StandardCharsets.UTF_8);

        String obj1 = "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n";
        String obj2 = "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n";
        String obj3 = "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n";
        String obj4 = "4 0 obj << /Length " + streamBytes.length + " >> stream\n" + stream + "\nendstream endobj\n";
        String obj5 = "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n";

        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        int xref1 = pdf.length(); pdf.append(obj1);
        int xref2 = pdf.length(); pdf.append(obj2);
        int xref3 = pdf.length(); pdf.append(obj3);
        int xref4 = pdf.length(); pdf.append(obj4);
        int xref5 = pdf.length(); pdf.append(obj5);
        int xrefStart = pdf.length();

        pdf.append("xref\n0 6\n");
        pdf.append("0000000000 65535 f \n");
        pdf.append(String.format("%010d 00000 n \n", xref1));
        pdf.append(String.format("%010d 00000 n \n", xref2));
        pdf.append(String.format("%010d 00000 n \n", xref3));
        pdf.append(String.format("%010d 00000 n \n", xref4));
        pdf.append(String.format("%010d 00000 n \n", xref5));
        pdf.append("trailer << /Size 6 /Root 1 0 R >>\nstartxref\n");
        pdf.append(xrefStart).append("\n%%EOF");

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(pdf.toString());
        }
    }

    private String escapeCsv(String value) {
        String v = value == null ? "" : value;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private String pdfEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private File ensureExtension(File file, String ext) {
        String lower = file.getName().toLowerCase(Locale.ROOT);
        if (!lower.endsWith(ext)) {
            return new File(file.getParentFile(), file.getName() + ext);
        }
        return file;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void applyBarSeriesStyle(BarChart<String, Number> chart) {
        Platform.runLater(() -> {
            if (chart.getData().size() > 0 && chart.getData().get(0).getNode() != null) {
                chart.getData().get(0).getNode().setStyle("-fx-stroke: transparent;");
            }
            if (chart.getData().size() > 1 && chart.getData().get(1).getNode() != null) {
                chart.getData().get(1).getNode().setStyle("-fx-stroke: transparent;");
            }
        });
    }

    private void applyLineSeriesStyle(LineChart<String, Number> chart) {
        Platform.runLater(() -> {
            if (!chart.getData().isEmpty() && chart.getData().get(0).getNode() != null) {
                chart.getData().get(0).getNode().setStyle("-fx-stroke: #2563EB; -fx-stroke-width: 2.4px;");
            }
        });
    }

    private void applyPieSliceColors(PieChart chart) {
        Platform.runLater(() -> {
            String[] colors = {"#2563EB", "#22C55E", "#F59E0B", "#8B5CF6", "#EF4444", "#CBD5E1"};
            int idx = 0;
            for (PieChart.Data slice : chart.getData()) {
                if (slice.getNode() != null) {
                    String color = colors[idx % colors.length];
                    slice.getNode().setStyle("-fx-pie-color: " + color + ";");
                }
                idx++;
            }
        });
    }
}
