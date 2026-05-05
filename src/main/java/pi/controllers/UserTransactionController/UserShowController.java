package pi.controllers.UserTransactionController;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import pi.entities.User;
import pi.mains.Main;
import pi.services.UserTransactionService.TransactionService;
import pi.tools.AdminNavigation;
import pi.tools.FxmlResources;
import pi.tools.UiDialog;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class UserShowController {

    @FXML private Label sidebarNameLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageSubtitleLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label balanceLabel;
    @FXML private Label idValueLabel;
    @FXML private Label nameDetailsValueLabel;
    @FXML private Label emailDetailsValueLabel;
    @FXML private Label rolesValueLabel;
    @FXML private Label balanceDetailsValueLabel;
    @FXML private Label registrationValueLabel;
    @FXML private Label geoCountryValueLabel;
    @FXML private Label geoRegionValueLabel;
    @FXML private Label ipValueLabel;
    @FXML private Label vpnValueLabel;
    @FXML private HBox roleBadgesBox;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label totalRevenuesLabel;
    @FXML private Label totalExpensesLabel;
    @FXML private Label savingCountLabel;
    @FXML private Label expenseCountLabel;
    @FXML private Label investmentCountLabel;
    @FXML private Label lastActivityLabel;
    @FXML private Label netCashFlowLabel;
    @FXML private Label financialHealthLabel;
    @FXML private Label savingsRateLabel;
    @FXML private VBox insightsBox;
    @FXML private LineChart<String, Number> trendChart;
    @FXML private PieChart distributionChart;

    @FXML private Label behaviorScoreLabel;
    @FXML private Label behaviorTypeLabel;
    @FXML private Label scoreTrendLabel;
    @FXML private Label currentWeekExpenseLabel;
    @FXML private Label previousWeekExpenseLabel;
    @FXML private Label expenseDeltaLabel;
    @FXML private Label regularityLabel;
    @FXML private Label impulsivityLabel;
    @FXML private Label disciplineLabel;
    @FXML private Label stabilityLabel;
    @FXML private VBox strengthsBox;
    @FXML private VBox weaknessesBox;
    @FXML private VBox actionsBox;
    @FXML private Label aiNoteLabel;

    @FXML private Label btcPriceLabel;
    @FXML private Label ethPriceLabel;
    @FXML private Label usdtPriceLabel;
    @FXML private ImageView profileImageView;
    @FXML private Label profilePlaceholderLabel;
    @FXML private Button changePhotoButton;

    private final UserController userController = new UserController();
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat usdFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat usd4Format = new DecimalFormat("#,##0.0000");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);
    private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg");
    private static final Path PROFILE_IMAGES_DIR = Path.of(System.getProperty("user.home"), ".decide-profile-images");

    private User adminUser;
    private User viewedUser;

    public void setContext(User adminUser, User viewedUser) {
        this.adminUser = adminUser;
        this.viewedUser = viewedUser;
        loadDashboard();
    }

    @FXML
    public void initialize() {
        if (trendChart != null) {
            trendChart.setLegendVisible(true);
            trendChart.setAnimated(false);
        }
        configureProfileImageView();
    }

    @FXML
    private void handleBackToList() {
        Stage stage = (Stage) pageTitleLabel.getScene().getWindow();
        AdminNavigation.showUsersManagement(stage, adminUser);
    }

    @FXML
    private void handleSignOut() {
        try {
            FXMLLoader loader = FxmlResources.load(Main.class, "/pi/mains/login-view.fxml");
            Parent root = (Parent) loader.getRoot();

            Stage stage = (Stage) pageTitleLabel.getScene().getWindow();
            Scene scene = new Scene(root, 1460, 780);
            FxmlResources.addStylesheet(scene, Main.class, "/pi/styles/login.css");

            stage.setTitle("User Secure Login");
            stage.setUserData(null);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError("Navigation error", "Unable to sign out: " + e.getMessage());
        }
    }

    @FXML
    private void handleTransactionsHistory() {
        if (viewedUser == null) {
            return;
        }
        TransactionService.UserTransactionSummary summary = userController.transactionsHistory(viewedUser.getId());
        String message = "Transactions: " + summary.count() +
                "\nTotal savings: " + moneyFormat.format(summary.totalSavings()) + " TND" +
                "\nTotal expenses: " + moneyFormat.format(summary.totalExpenses()) + " TND" +
                "\nNet: " + moneyFormat.format(summary.net()) + " TND";
        showInfo("Transactions History", message);
    }

    @FXML
    private void handleEditUser() {
        if (viewedUser == null || pageTitleLabel == null || pageTitleLabel.getScene() == null) {
            return;
        }
        Stage stage = (Stage) pageTitleLabel.getScene().getWindow();
        AdminNavigation.showUserEdit(stage, adminUser, viewedUser);
    }

    @FXML
    private void handleDeleteUser() {
        if (viewedUser == null) {
            return;
        }
        Stage stage = resolveStage();
        boolean confirmed = UiDialog.showConfirm(
                stage,
                "Delete user",
                "Delete user #" + viewedUser.getId() + " ?",
                "This action is permanent."
        );
        if (!confirmed) {
            return;
        }

        try {
            userController.delete(viewedUser.getId());
            handleBackToList();
        } catch (Exception e) {
            showError("Delete error", e.getMessage());
        }
    }

    @FXML
    private void handleChangePhoto() {
        if (viewedUser == null) {
            return;
        }
        Stage stage = resolveStage();
        if (stage == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une photo de profil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = chooser.showOpenDialog(stage);
        if (selectedFile == null) {
            return;
        }

        String extension = resolveExtension(selectedFile.getName());
        if (!SUPPORTED_IMAGE_EXTENSIONS.contains(extension)) {
            showError("Photo", "Format non supporté. Utilisez PNG, JPG ou JPEG.");
            return;
        }

        try {
            Files.createDirectories(PROFILE_IMAGES_DIR);
            String targetName = "user-" + viewedUser.getId() + "-" + UUID.randomUUID() + "." + extension;
            Path targetPath = PROFILE_IMAGES_DIR.resolve(targetName);
            Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            userController.updateProfileImage(viewedUser.getId(), targetPath.toString());
            viewedUser.setImage(targetPath.toString());
            applyProfileImage(viewedUser);
            showInfo("Photo", "Photo de profil mise à jour.");
        } catch (Exception e) {
            showError("Photo", "Impossible de sauvegarder la photo: " + e.getMessage());
        }
    }

    private void loadDashboard() {
        if (viewedUser == null) {
            return;
        }

        try {
            TransactionService.UserDashboardStats stats = userController.showDetails(viewedUser.getId());
            User user = stats.user();
            this.viewedUser = user;

            if (sidebarNameLabel != null) {
                sidebarNameLabel.setText(valueOrDash(adminUser != null ? adminUser.getNom() : user.getNom()));
            }
            pageTitleLabel.setText("User Profile #" + user.getId());
            pageSubtitleLabel.setText("Detailed account information and role overview");

            String name = valueOrDash(user.getNom());
            String email = valueOrDash(user.getEmail());
            String balance = moneyFormat.format(user.getSoldeTotal()) + " TND";
            nameLabel.setText(name);
            emailLabel.setText(email);
            balanceLabel.setText(balance);
            applyProfileImage(user);

            idValueLabel.setText(String.valueOf(user.getId()));
            nameDetailsValueLabel.setText(name);
            emailDetailsValueLabel.setText(email);
            rolesValueLabel.setText(resolveRoles(user));
            balanceDetailsValueLabel.setText(balance);
            registrationValueLabel.setText(user.getDateInscription() == null ? "-" : user.getDateInscription().format(dateFormatter));
            geoCountryValueLabel.setText(firstNonBlank(user.getGeoCountryName(), user.getGeoCountryCode(), "-"));

            String region = valueOrDash(user.getGeoRegionName());
            String city = valueOrDash(user.getGeoCityName());
            geoRegionValueLabel.setText("-".equals(city) ? region : region + " / " + city);

            ipValueLabel.setText(valueOrDash(user.getGeoDetectedIp()));
            vpnValueLabel.setText(user.isGeoVpnSuspected() ? "Suspected" : "No");
            vpnValueLabel.getStyleClass().removeAll("badge-good", "badge-warn");
            vpnValueLabel.getStyleClass().add(user.isGeoVpnSuspected() ? "badge-warn" : "badge-good");

            renderRoleBadges(user);

            totalTransactionsLabel.setText(String.valueOf(stats.totalTransactions()));
            totalRevenuesLabel.setText(moneyFormat.format(stats.totalRevenues()) + " TND");
            totalExpensesLabel.setText(moneyFormat.format(stats.totalExpenses()) + " TND");
            savingCountLabel.setText(String.valueOf(stats.savingCount()));
            expenseCountLabel.setText(String.valueOf(stats.expenseTxCount()));
            investmentCountLabel.setText(String.valueOf(stats.investmentCount()));
            lastActivityLabel.setText(stats.lastActivityDate() == null ? "-" : stats.lastActivityDate().format(dateFormatter));

            netCashFlowLabel.setText(moneyFormat.format(stats.netCashFlow()) + " TND");
            netCashFlowLabel.getStyleClass().removeAll("text-good", "text-bad");
            netCashFlowLabel.getStyleClass().add(stats.netCashFlow() >= 0 ? "text-good" : "text-bad");

            financialHealthLabel.setText(valueOrDash(stats.financialHealth()));
            savingsRateLabel.setText(String.format(Locale.US, "%.1f%%", stats.savingsRate()));

            renderInsights(insightsBox, stats.insights(), "- ");
            renderCharts(stats);
            fillBehaviorSection(stats);
            fillApiSection(stats);
        } catch (Exception e) {
            showError("Load error", "Unable to load user profile: " + e.getMessage());
        }
    }

    private void configureProfileImageView() {
        if (profileImageView == null) {
            return;
        }
        profileImageView.setPreserveRatio(true);
        profileImageView.setSmooth(true);
        profileImageView.setCache(true);

        Circle clip = new Circle(46, 46, 46);
        profileImageView.setClip(clip);
    }

    private void applyProfileImage(User user) {
        if (profileImageView == null || profilePlaceholderLabel == null || user == null) {
            return;
        }

        profilePlaceholderLabel.setText(resolveInitial(user.getNom()));

        Image image = loadUserImage(user.getImage());
        if (image == null) {
            profileImageView.setImage(null);
            profileImageView.setVisible(false);
            profileImageView.setManaged(false);
            profilePlaceholderLabel.setVisible(true);
            profilePlaceholderLabel.setManaged(true);
            return;
        }

        profileImageView.setImage(image);
        profileImageView.setVisible(true);
        profileImageView.setManaged(true);
        profilePlaceholderLabel.setVisible(false);
        profilePlaceholderLabel.setManaged(false);
    }

    private Image loadUserImage(String imageRef) {
        if (imageRef == null || imageRef.isBlank()) {
            return null;
        }

        try {
            Path imagePath = Path.of(imageRef);
            if (Files.exists(imagePath)) {
                return new Image(imagePath.toUri().toString(), 92, 92, true, true, false);
            }
        } catch (Exception ignored) {
            // Not a local path, fallback to URI handling below.
        }

        try {
            if (imageRef.startsWith("/") && Main.class.getResource(imageRef) != null) {
                return new Image(Main.class.getResource(imageRef).toExternalForm(), 92, 92, true, true, false);
            }
            return new Image(imageRef, 92, 92, true, true, false);
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveInitial(String name) {
        if (name == null || name.isBlank()) {
            return "U";
        }
        return name.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private String resolveExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private void renderCharts(TransactionService.UserDashboardStats stats) {
        XYChart.Series<String, Number> revenuesSeries = new XYChart.Series<>();
        revenuesSeries.setName("Revenues");
        XYChart.Series<String, Number> expensesSeries = new XYChart.Series<>();
        expensesSeries.setName("Expenses");

        for (int i = 0; i < stats.monthLabels().size(); i++) {
            String month = stats.monthLabels().get(i);
            revenuesSeries.getData().add(new XYChart.Data<>(month, stats.monthlyRevenue().get(i)));
            expensesSeries.getData().add(new XYChart.Data<>(month, stats.monthlyExpense().get(i)));
        }
        trendChart.getData().setAll(revenuesSeries, expensesSeries);

        List<Integer> d = stats.distribution();
        distributionChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Saving", d.get(0)),
                new PieChart.Data("Expense", d.get(1)),
                new PieChart.Data("Investment", d.get(2))
        ));
    }

    private void fillBehaviorSection(TransactionService.UserDashboardStats stats) {
        int total = Math.max(1, stats.totalTransactions());
        int behaviorScore = clamp((int) Math.round(100 - stats.expenseRatio() + (stats.savingsRate() * 0.25)), 0, 100);
        int regularity = clamp((int) Math.round((double) (stats.expenseTxCount() + stats.savingCount()) / total * 100), 0, 100);
        int impulsivity = clamp(100 - (stats.expenseTxCount() * 8), 0, 100);
        int discipline = clamp((int) Math.round(stats.savingsRate()), 0, 100);
        int stability = clamp((regularity + impulsivity + discipline) / 3, 0, 100);
        int scoreTrend = stats.netCashFlow() >= 0 ? 2 : -2;

        String profileType = behaviorScore >= 75 ? "Stable Saver"
                : behaviorScore >= 55 ? "Variable but Controlled"
                : "High Volatility";

        double currentWeekExpense = stats.totalExpenses() * 0.5;
        double previousWeekExpense = stats.totalExpenses() * 0.3;
        double deltaPct = previousWeekExpense == 0
                ? (currentWeekExpense > 0 ? 100 : 0)
                : ((currentWeekExpense - previousWeekExpense) / previousWeekExpense) * 100.0;

        behaviorScoreLabel.setText(behaviorScore + "/100");
        behaviorTypeLabel.setText(profileType);
        scoreTrendLabel.setText((scoreTrend >= 0 ? "+" : "") + scoreTrend);
        scoreTrendLabel.getStyleClass().removeAll("text-good", "text-bad");
        scoreTrendLabel.getStyleClass().add(scoreTrend >= 0 ? "text-good" : "text-bad");

        currentWeekExpenseLabel.setText(moneyFormat.format(currentWeekExpense) + " TND");
        previousWeekExpenseLabel.setText(moneyFormat.format(previousWeekExpense) + " TND");
        expenseDeltaLabel.setText((deltaPct >= 0 ? "+" : "") + String.format(Locale.US, "%.0f%%", deltaPct));
        expenseDeltaLabel.getStyleClass().removeAll("text-good", "text-bad");
        expenseDeltaLabel.getStyleClass().add(deltaPct <= 0 ? "text-good" : "text-bad");

        regularityLabel.setText(regularity + "/100");
        impulsivityLabel.setText(impulsivity + "/100");
        disciplineLabel.setText(discipline + "/100");
        stabilityLabel.setText(stability + "/100");

        renderInsights(strengthsBox, buildStrengths(regularity, impulsivity, stability), "- ");
        renderInsights(weaknessesBox, buildWeaknesses(discipline, stats.netCashFlow()), "- ");
        renderInsights(actionsBox, List.of(
                "1. Set an automatic transfer of 10% of each income to savings.",
                "2. Review your top 3 expenses weekly and cut one recurring cost.",
                "3. Define one weekly micro-goal and mark it as completed."
        ), "");

        aiNoteLabel.setText("Diagnostic:\n- Score actuel: " + behaviorScore + "/100 (" + profileType + ").\n" +
                "- Evolution hebdomadaire: " + (scoreTrend >= 0 ? "+" : "") + scoreTrend + " point(s).\n\n" +
                "Risque principal: " + (stats.netCashFlow() >= 0
                ? "surconfiance malgré une bonne trajectoire."
                : "dépenses supérieures aux revenus sur la période.") + "\n\n" +
                "Plan 7 jours:\n- Appliquer les 3 actions adaptatives ci-dessus.");
    }

    private void fillApiSection(TransactionService.UserDashboardStats stats) {
        double baseline = Math.max(1, stats.totalTransactions());
        double btc = 70000 + (baseline * 420);
        double eth = 2200 + (baseline * 18);
        double usdt = 1.0;

        btcPriceLabel.setText("$" + usdFormat.format(btc));
        ethPriceLabel.setText("$" + usdFormat.format(eth));
        usdtPriceLabel.setText("$" + usd4Format.format(usdt));
    }

    private List<String> buildStrengths(int regularity, int impulsivity, int stability) {
        return List.of(
                "Impulsivity control (" + impulsivity + "/100)",
                "Monthly stability (" + stability + "/100)",
                "Expense regularity (" + regularity + "/100)"
        );
    }

    private List<String> buildWeaknesses(int discipline, double netCashFlow) {
        String disciplineLine = "Saving discipline (" + discipline + "/100)";
        String cashFlowLine = netCashFlow >= 0
                ? "Potential complacency in expense tracking."
                : "Negative cash flow pressure detected.";
        return List.of(disciplineLine, cashFlowLine);
    }

    private void renderInsights(VBox box, List<String> lines, String prefix) {
        box.getChildren().clear();
        for (String insight : lines) {
            Label line = new Label(prefix + insight);
            line.getStyleClass().add("insight-line");
            line.setWrapText(true);
            box.getChildren().add(line);
        }
    }

    private void renderRoleBadges(User user) {
        roleBadgesBox.getChildren().clear();
        addRoleBadgeIf(user.hasRole("ROLE_ADMIN"), "Admin", "role-admin");
        addRoleBadgeIf(user.hasRole("ROLE_SALARY"), "Salary", "role-salary");
        addRoleBadgeIf(user.hasRole("ROLE_ETUDIANT"), "Student", "role-student");
        if (roleBadgesBox.getChildren().isEmpty()) {
            Label label = new Label("No role");
            label.getStyleClass().addAll("role-badge", "role-none");
            roleBadgesBox.getChildren().add(label);
        }
    }

    private void addRoleBadgeIf(boolean condition, String text, String style) {
        if (!condition) {
            return;
        }
        Label label = new Label(text);
        label.getStyleClass().addAll("role-badge", style);
        roleBadgesBox.getChildren().add(label);
    }

    private String resolveRoles(User user) {
        String roles = user.getRoles();
        if (roles == null || roles.isBlank() || "[]".equals(roles.trim())) {
            return "-";
        }
        return roles.replace("[", "").replace("]", "").replace("\"", "");
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String valueOrDash(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }

    private void showInfo(String title, String content) {
        UiDialog.info(resolveStage(), title, content);
    }

    private void showError(String title, String content) {
        UiDialog.error(resolveStage(), title, content);
    }

    private Stage resolveStage() {
        if (pageTitleLabel != null && pageTitleLabel.getScene() != null && pageTitleLabel.getScene().getWindow() instanceof Stage stage) {
            return stage;
        }
        return null;
    }
}
