package pi.savings.ui;

import pi.entities.CalendarEvent;
import pi.savings.dto.CalendarEventDTO;
import pi.savings.dto.GoalAnalyticsDTO;
import pi.savings.dto.WhatIfScenarioDTO;
import pi.savings.service.AiFinancialInsightService;
import pi.savings.service.CsvExportService;
import pi.savings.service.GoalsAnalyticsService;
import pi.savings.service.PdfExportService;
import pi.savings.service.SavingsCalendarService;
import pi.savings.service.SavingsModuleService;
import pi.savings.service.SavingsModuleService.DashboardSnapshot;
import pi.savings.service.SavingsModuleService.GoalSnapshot;
import pi.savings.service.SavingsModuleService.SavingsModuleException;
import pi.savings.service.SavingsStatsService;
import pi.savings.service.SavingsValidation.SavingsValidationException;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

final class SavingsUiController {

    private static final int DEFAULT_USER_ID = 1;
    private static final int DEFAULT_PAGE_SIZE = 5;
    private SavingsModuleService moduleService;
    private SavingsStatsController statsController;
    private SavingsCalendarController calendarController;
    private CsvExportService csvExportService;
    private PdfExportService pdfExportService;
    private GoalsAnalyticsService goalsAnalyticsService;
    private AiFinancialInsightService aiFinancialInsightService;
    private DashboardSnapshot snapshot;
    private String initializationFailureMessage;
    private String selectedCurrency = "TND";

    SavingsUiController() {
    }

    SavingsUiController(SavingsModuleService moduleService) {
        this.moduleService = moduleService;
    }

    OperationResult initialize() {
        try {
            snapshot = getModuleService().loadDashboard(DEFAULT_USER_ID);
            initializationFailureMessage = null;
            return OperationResult.success("Module charge avec succes.");
        } catch (RuntimeException ex) {
            snapshot = emptySnapshot();
            initializationFailureMessage = resolveMessage(ex, "Impossible de charger le module Savings & Goals.");
            return OperationResult.error(initializationFailureMessage);
        }
    }

    DashboardSnapshot getSnapshot() {
        if (snapshot == null) {
            initialize();
        }
        return snapshot;
    }

    List<SavingsModuleService.GoalSnapshot> filterAndSortGoals(String queryText, String sortAttributeText, String sortDirectionText) {
        String query = normalizeQuery(queryText);
        String attribute = normalizeSort(sortAttributeText);
        String direction = normalizeSort(sortDirectionText);
        boolean ascending = !"descending".equals(direction);

        Comparator<SavingsModuleService.GoalSnapshot> comparator = switch (attribute) {
            case "id" -> Comparator.comparingInt(SavingsModuleService.GoalSnapshot::id);
            case "target" -> Comparator.comparing(SavingsModuleService.GoalSnapshot::target);
            case "current" -> Comparator.comparing(SavingsModuleService.GoalSnapshot::current);
            case "deadline" -> Comparator.comparing(
                    (SavingsModuleService.GoalSnapshot goal) -> goal.deadline() == null ? LocalDate.MAX : goal.deadline()
            );
            case "priority" -> Comparator.comparingInt(SavingsModuleService.GoalSnapshot::priority);
            case "progress" -> Comparator.comparingDouble(SavingsModuleService.GoalSnapshot::progressPercent);
            case "all", "name", "" -> Comparator.comparing(
                    SavingsModuleService.GoalSnapshot::name,
                    String.CASE_INSENSITIVE_ORDER
            );
            default -> Comparator.comparing(
                    SavingsModuleService.GoalSnapshot::name,
                    String.CASE_INSENSITIVE_ORDER
            );
        };
        comparator = applyDirection(comparator, ascending)
                .thenComparing(applyDirection(Comparator.comparingInt(SavingsModuleService.GoalSnapshot::priority), ascending))
                .thenComparing(applyDirection(Comparator.comparingInt(SavingsModuleService.GoalSnapshot::id), ascending));

        return getSnapshot().goals().stream()
                .filter(goal -> matchesGoal(goal, query))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    List<pi.savings.repository.SavingsTransactionRepository.TransactionRow> filterAndSortHistory(
            String queryText,
            String sortAttributeText,
            String sortDirectionText
    ) {
        String query = normalizeQuery(queryText);
        String attribute = normalizeSort(sortAttributeText);
        String direction = normalizeSort(sortDirectionText);
        boolean ascending = !"descending".equals(direction);

        Comparator<pi.savings.repository.SavingsTransactionRepository.TransactionRow> comparator = switch (attribute) {
            case "id" -> Comparator.comparingInt(pi.savings.repository.SavingsTransactionRepository.TransactionRow::id);
            case "amount" -> Comparator.comparing(pi.savings.repository.SavingsTransactionRepository.TransactionRow::amount);
            case "type" -> Comparator.comparing(
                    (pi.savings.repository.SavingsTransactionRepository.TransactionRow row) -> safeLower(row.type()),
                    String.CASE_INSENSITIVE_ORDER
            );
            case "description" -> Comparator.comparing(
                    (pi.savings.repository.SavingsTransactionRepository.TransactionRow row) -> safeLower(row.description()),
                    String.CASE_INSENSITIVE_ORDER
            );
            case "module_source" -> Comparator.comparing(
                    (pi.savings.repository.SavingsTransactionRepository.TransactionRow row) -> safeLower(row.moduleSource()),
                    String.CASE_INSENSITIVE_ORDER
            );
            case "user_id" -> Comparator.comparingInt(pi.savings.repository.SavingsTransactionRepository.TransactionRow::userId);
            case "all", "date", "" -> Comparator.comparing(pi.savings.repository.SavingsTransactionRepository.TransactionRow::date);
            default -> Comparator.comparing(pi.savings.repository.SavingsTransactionRepository.TransactionRow::date);
        };
        comparator = applyDirection(comparator, ascending)
                .thenComparing(applyDirection(Comparator.comparing(
                        pi.savings.repository.SavingsTransactionRepository.TransactionRow::date
                ), ascending))
                .thenComparing(applyDirection(Comparator.comparingInt(
                        pi.savings.repository.SavingsTransactionRepository.TransactionRow::id
                ), ascending));

        return getSnapshot().transactions().stream()
                .filter(row -> matchesHistory(row, query))
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    OperationResult safeDeposit(String amountText, String descriptionText) {
        try {
            snapshot = getModuleService().saveDeposit(DEFAULT_USER_ID, amountText, descriptionText);
            initializationFailureMessage = null;
            return OperationResult.success("Depot enregistre avec succes.");
        } catch (SavingsValidationException ex) {
            return OperationResult.error(ex.getMessage());
        } catch (RuntimeException ex) {
            return OperationResult.error(resolveMessage(ex, "Impossible d'enregistrer le depot."));
        }
    }

    OperationResult safeUpdateInterestRate(String rateText) {
        try {
            snapshot = getModuleService().updateInterestRate(DEFAULT_USER_ID, rateText);
            initializationFailureMessage = null;
            return OperationResult.success("Taux d'interet mis a jour.");
        } catch (SavingsValidationException ex) {
            return OperationResult.error(ex.getMessage());
        } catch (RuntimeException ex) {
            return OperationResult.error(resolveMessage(ex, "Impossible de mettre a jour le taux d'interet."));
        }
    }

    OperationResult safeCreateGoal(
            String nameText,
            String targetText,
            String currentText,
            String deadlineText,
            String priorityText
    ) {
        try {
            snapshot = getModuleService().createGoal(DEFAULT_USER_ID, nameText, targetText, currentText, deadlineText, priorityText);
            initializationFailureMessage = null;
            return OperationResult.success("Goal cree avec succes.");
        } catch (SavingsValidationException ex) {
            return OperationResult.error(ex.getMessage());
        } catch (RuntimeException ex) {
            return OperationResult.error(resolveMessage(ex, "Impossible de creer le goal."));
        }
    }

    OperationResult safeUpdateGoal(
            int goalId,
            String nameText,
            String targetText,
            String currentText,
            String deadlineText,
            String priorityText
    ) {
        try {
            snapshot = getModuleService().updateGoal(DEFAULT_USER_ID, goalId, nameText, targetText, currentText, deadlineText, priorityText);
            initializationFailureMessage = null;
            return OperationResult.success("Goal modifie avec succes.");
        } catch (SavingsValidationException ex) {
            return OperationResult.error(ex.getMessage());
        } catch (RuntimeException ex) {
            return OperationResult.error(resolveMessage(ex, "Impossible de modifier le goal."));
        }
    }

    OperationResult safeDeleteGoal(int goalId) {
        try {
            snapshot = getModuleService().deleteGoal(DEFAULT_USER_ID, goalId);
            initializationFailureMessage = null;
            return OperationResult.success("Goal supprime avec succes.");
        } catch (RuntimeException ex) {
            return OperationResult.error(resolveMessage(ex, "Impossible de supprimer le goal."));
        }
    }

    OperationResult safeContributeToGoal(int goalId, String amountText) {
        try {
            snapshot = getModuleService().contributeToGoal(DEFAULT_USER_ID, goalId, amountText);
            initializationFailureMessage = null;
            return OperationResult.success("Contribution enregistree avec succes.");
        } catch (SavingsValidationException ex) {
            return OperationResult.error(ex.getMessage());
        } catch (RuntimeException ex) {
            return OperationResult.error(resolveMessage(ex, "Impossible d'enregistrer la contribution."));
        }
    }

    SavingsModuleService.HistoryStats getHistoryStats(String queryText, String sortAttributeText, String sortDirectionText) {
        return getModuleService().calculateHistoryStats(
                filterAndSortHistory(queryText, sortAttributeText, sortDirectionText)
        );
    }

    SavingsModuleService.HistoryStats calculateHistoryStats(
            List<pi.savings.repository.SavingsTransactionRepository.TransactionRow> rows
    ) {
        return getModuleService().calculateHistoryStats(rows);
    }

    SavingsModuleService.GoalStats getGoalStats(String queryText, String sortAttributeText, String sortDirectionText) {
        return getModuleService().calculateGoalStats(
                filterAndSortGoals(queryText, sortAttributeText, sortDirectionText)
        );
    }

    SavingsModuleService.GoalStats calculateGoalStats(List<SavingsModuleService.GoalSnapshot> goals) {
        return getModuleService().calculateGoalStats(goals);
    }

    SavingsStatsService.FrontStatsSnapshot loadFrontStats() {
        return getStatsController().loadFrontStats(selectedCurrency);
    }

    SavingsStatsService.BackOfficeStatsSnapshot loadBackOfficeStats() {
        return getStatsController().loadBackOfficeStats(selectedCurrency);
    }

    List<CalendarEvent> loadCalendarEvents(YearMonth month) {
        SavingsStatsService.ExportBundle bundle = getStatsController().loadFrontExportBundle(selectedCurrency);
        return getCalendarController().loadMonthEvents(month, bundle.goals(), bundle.transactions());
    }

    CalendarViewData loadCalendarViewData(YearMonth month, String baseCurrency, String targetCurrency) {
        SavingsStatsService.ExportBundle bundle = getStatsController().loadFrontExportBundle(selectedCurrency);
        SavingsCalendarService.CalendarData data = getCalendarController().loadCalendarData(
                month,
                bundle.goals(),
                bundle.transactions(),
                baseCurrency,
                targetCurrency
        );
        return new CalendarViewData(
                data.events(),
                data.dailyRates(),
                data.holidayStatus(),
                data.currencyStatus(),
                data.mysqlStatus(),
                data.refreshedAt()
        );
    }

    void refreshCalendarApiCaches() {
        getCalendarController().refreshApiCaches();
    }

    OperationResult selectCurrency(String currency) {
        try {
            selectedCurrency = currency == null || currency.isBlank() ? "TND" : currency.toUpperCase(Locale.ROOT);
            loadFrontStats();
            return OperationResult.success("Devise active: " + selectedCurrency);
        } catch (RuntimeException exception) {
            return OperationResult.error(resolveMessage(exception, "Impossible de changer la devise."));
        }
    }

    String getSelectedCurrency() {
        return selectedCurrency;
    }

    List<String> getSupportedCurrencies() {
        return pi.savings.service.CurrencyRateService.SUPPORTED_CURRENCIES.stream().sorted().toList();
    }

    <T> PageSlice<T> paginate(List<T> items, int pageIndex, int pageSize) {
        int resolvedPageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : pageSize;
        List<T> safeItems = items == null ? List.of() : items;
        int totalItems = safeItems.size();
        int pageCount = Math.max(1, (int) Math.ceil(totalItems / (double) resolvedPageSize));
        int resolvedPageIndex = Math.min(Math.max(pageIndex, 0), pageCount - 1);
        int fromIndex = Math.min(resolvedPageIndex * resolvedPageSize, totalItems);
        int toIndex = Math.min(fromIndex + resolvedPageSize, totalItems);
        return new PageSlice<>(
                safeItems.subList(fromIndex, toIndex),
                resolvedPageIndex,
                pageCount,
                totalItems,
                resolvedPageSize
        );
    }

    OperationResult safeExportHistoryCsv() {
        return safeExportHistoryCsv("", "Date", "Descending", defaultExportDirectory());
    }

    OperationResult safeExportHistoryPdf() {
        return safeExportHistoryPdf("", "Date", "Descending", defaultExportDirectory());
    }

    OperationResult safeExportHistoryCsv(Path exportDirectory) {
        return safeExportHistoryCsv("", "Date", "Descending", exportDirectory);
    }

    OperationResult safeExportHistoryPdf(Path exportDirectory) {
        return safeExportHistoryPdf("", "Date", "Descending", exportDirectory);
    }

    OperationResult safeExportHistoryCsv(
            String queryText,
            String sortAttributeText,
            String sortDirectionText,
            Path exportDirectory
    ) {
        try {
            Path exportPath = getModuleService().exportHistoryCsv(
                    filterAndSortHistory(queryText, sortAttributeText, sortDirectionText),
                    exportDirectory
            );
            return OperationResult.success("CSV exporte: " + exportPath.toAbsolutePath());
        } catch (RuntimeException ex) {
            return OperationResult.error(resolveMessage(ex, "Impossible d'exporter l'historique en CSV."));
        }
    }

    OperationResult safeExportHistoryPdf(
            String queryText,
            String sortAttributeText,
            String sortDirectionText,
            Path exportDirectory
    ) {
        try {
            Path exportPath = getModuleService().exportHistoryPdf(
                    filterAndSortHistory(queryText, sortAttributeText, sortDirectionText),
                    exportDirectory
            );
            return OperationResult.success("PDF exporte: " + exportPath.toAbsolutePath());
        } catch (RuntimeException ex) {
            return OperationResult.error(resolveMessage(ex, "Impossible d'exporter l'historique en PDF."));
        }
    }

    OperationResult safeExportGoalsCsv(
            String queryText,
            String sortAttributeText,
            String sortDirectionText,
            Path exportDirectory
    ) {
        try {
            SavingsStatsService.ExportBundle bundle = getStatsController().loadFrontExportBundle(selectedCurrency);
            List<String> goalNames = filterAndSortGoals(queryText, sortAttributeText, sortDirectionText)
                    .stream()
                    .map(GoalSnapshot::name)
                    .toList();
            Path exportPath = getCsvExportService().exportGoals(
                    bundle.goals().stream().filter(goal -> goalNames.contains(goal.name())).toList(),
                    bundle.rateSnapshot(),
                    bundle.holidays(),
                    exportDirectory
            );
            return OperationResult.success("CSV goals exporte: " + exportPath.toAbsolutePath());
        } catch (RuntimeException ex) {
            try {
                Path exportPath = getModuleService().exportGoalsCsv(
                        filterAndSortGoals(queryText, sortAttributeText, sortDirectionText),
                        exportDirectory
                );
                return OperationResult.success("CSV goals exporte: " + exportPath.toAbsolutePath());
            } catch (RuntimeException fallback) {
                return OperationResult.error(resolveMessage(fallback, "Impossible d'exporter les goals en CSV."));
            }
        }
    }

    OperationResult safeExportGoalsPdf(
            String queryText,
            String sortAttributeText,
            String sortDirectionText,
            Path exportDirectory
    ) {
        try {
            SavingsStatsService.ExportBundle bundle = getStatsController().loadFrontExportBundle(selectedCurrency);
            List<String> goalNames = filterAndSortGoals(queryText, sortAttributeText, sortDirectionText)
                    .stream()
                    .map(GoalSnapshot::name)
                    .toList();
            Path exportPath = getPdfExportService().exportGoalsPdf(
                    bundle.goals().stream().filter(goal -> goalNames.contains(goal.name())).toList(),
                    bundle.rateSnapshot(),
                    bundle.holidays(),
                    exportDirectory
            );
            if (!Files.exists(exportPath) || Files.size(exportPath) == 0) {
                return OperationResult.error("Le PDF goals n'a pas ete genere.");
            }
            return OperationResult.success("PDF goals exporte: " + exportPath.toAbsolutePath());
        } catch (RuntimeException ex) {
            try {
                Path exportPath = getModuleService().exportGoalsPdf(
                        filterAndSortGoals(queryText, sortAttributeText, sortDirectionText),
                        exportDirectory
                );
                if (!Files.exists(exportPath) || Files.size(exportPath) == 0) {
                    return OperationResult.error("Le PDF goals n'a pas ete genere.");
                }
                return OperationResult.success("PDF goals exporte: " + exportPath.toAbsolutePath());
            } catch (RuntimeException fallback) {
                return OperationResult.error(resolveMessage(fallback, "Impossible d'exporter les goals en PDF."));
            } catch (Exception fallback) {
                return OperationResult.error("Impossible d'exporter les goals en PDF.");
            }
        } catch (Exception ex) {
            return OperationResult.error("Impossible d'exporter les goals en PDF.");
        }
    }

    record OperationResult(boolean success, String message) {
        static OperationResult success(String message) {
            return new OperationResult(true, message);
        }

        static OperationResult error(String message) {
            return new OperationResult(false, message);
        }
    }

    OperationResult safeExportSavingAccountsCsv(Path exportDirectory) {
        try {
            SavingsStatsService.ExportBundle bundle = getStatsController().loadFrontExportBundle(selectedCurrency);
            Path exportPath = getCsvExportService().exportSavingAccounts(bundle.accounts(), bundle.rateSnapshot(), exportDirectory);
            return OperationResult.success("CSV saving accounts exporte: " + exportPath.toAbsolutePath());
        } catch (RuntimeException exception) {
            return OperationResult.error(resolveMessage(exception, "Impossible d'exporter les saving accounts en CSV."));
        }
    }

    OperationResult safeExportSavingAccountsPdf(Path exportDirectory) {
        try {
            SavingsStatsService.ExportBundle bundle = getStatsController().loadFrontExportBundle(selectedCurrency);
            Path exportPath = getPdfExportService().exportSavingAccountsPdf(bundle.accounts(), bundle.rateSnapshot(), exportDirectory);
            return OperationResult.success("PDF saving accounts exporte: " + exportPath.toAbsolutePath());
        } catch (RuntimeException exception) {
            return OperationResult.error(resolveMessage(exception, "Impossible d'exporter les saving accounts en PDF."));
        }
    }

    OperationResult safeExportAllSavingAccountsCsv(Path exportDirectory) {
        try {
            SavingsStatsService.ExportBundle bundle = getStatsController().loadBackOfficeExportBundle(selectedCurrency);
            Path exportPath = getCsvExportService().exportSavingAccounts(bundle.accounts(), bundle.rateSnapshot(), exportDirectory);
            return OperationResult.success("CSV back-office savings exporte: " + exportPath.toAbsolutePath());
        } catch (RuntimeException exception) {
            return OperationResult.error(resolveMessage(exception, "Impossible d'exporter les savings du back-office en CSV."));
        }
    }

    OperationResult safeExportAllSavingAccountsPdf(Path exportDirectory) {
        try {
            SavingsStatsService.ExportBundle bundle = getStatsController().loadBackOfficeExportBundle(selectedCurrency);
            Path exportPath = getPdfExportService().exportSavingAccountsPdf(bundle.accounts(), bundle.rateSnapshot(), exportDirectory);
            return OperationResult.success("PDF back-office savings exporte: " + exportPath.toAbsolutePath());
        } catch (RuntimeException exception) {
            return OperationResult.error(resolveMessage(exception, "Impossible d'exporter les savings du back-office en PDF."));
        }
    }

    OperationResult safeExportAllGoalsCsv(Path exportDirectory) {
        try {
            SavingsStatsService.ExportBundle bundle = getStatsController().loadBackOfficeExportBundle(selectedCurrency);
            Path exportPath = getCsvExportService().exportGoals(bundle.goals(), bundle.rateSnapshot(), bundle.holidays(), exportDirectory);
            return OperationResult.success("CSV back-office goals exporte: " + exportPath.toAbsolutePath());
        } catch (RuntimeException exception) {
            return OperationResult.error(resolveMessage(exception, "Impossible d'exporter les goals du back-office en CSV."));
        }
    }

    OperationResult safeExportAllGoalsPdf(Path exportDirectory) {
        try {
            SavingsStatsService.ExportBundle bundle = getStatsController().loadBackOfficeExportBundle(selectedCurrency);
            Path exportPath = getPdfExportService().exportGoalsPdf(bundle.goals(), bundle.rateSnapshot(), bundle.holidays(), exportDirectory);
            return OperationResult.success("PDF back-office goals exporte: " + exportPath.toAbsolutePath());
        } catch (RuntimeException exception) {
            return OperationResult.error(resolveMessage(exception, "Impossible d'exporter les goals du back-office en PDF."));
        }
    }

    GoalAnalyticsDTO loadGoalsAnalytics(
            String analyzeBy,
            String queryText,
            String sortAttributeText,
            String sortDirectionText
    ) {
        List<SavingsModuleService.GoalSnapshot> filteredGoals = filterAndSortGoals(queryText, sortAttributeText, sortDirectionText);
        GoalsAnalyticsService.AnalyzeAttribute attribute = GoalsAnalyticsService.AnalyzeAttribute.fromLabel(analyzeBy);
        return getGoalsAnalyticsService().analyze(filteredGoals, getSnapshot().transactions(), attribute);
    }

    List<String> getGoalsAnalyzeByOptions() {
        boolean hasContributions = getSnapshot().transactions().stream()
                .anyMatch(row -> "GOAL_CONTRIBUTION".equalsIgnoreCase(row.type()));
        return getGoalsAnalyticsService().listAnalyzeByAttributes(hasContributions);
    }

    WhatIfScenarioDTO runWhatIfScenario(
            GoalAnalyticsDTO analytics,
            String scenarioName,
            BigDecimal monthlyContribution
    ) {
        return getGoalsAnalyticsService().simulateScenario(
                analytics == null ? List.of() : analytics.goalRisks(),
                scenarioName,
                monthlyContribution == null ? BigDecimal.ZERO : monthlyContribution
        );
    }

    String loadAiFinancialInsight(GoalAnalyticsDTO analytics) {
        try {
            return getAiFinancialInsightService().generateInsight(analytics);
        } catch (AiFinancialInsightService.AiInsightException exception) {
            return AiFinancialInsightService.FALLBACK_MESSAGE;
        }
    }

    record PageSlice<T>(List<T> items, int pageIndex, int pageCount, int totalItems, int pageSize) {
    }

    record CalendarViewData(
            List<CalendarEventDTO> events,
            Map<LocalDate, Double> dailyRates,
            SavingsCalendarService.ApiConnectionStatus holidayStatus,
            SavingsCalendarService.ApiConnectionStatus currencyStatus,
            SavingsCalendarService.ApiConnectionStatus mysqlStatus,
            java.time.LocalDateTime refreshedAt
    ) {
    }

    private DashboardSnapshot emptySnapshot() {
        return new DashboardSnapshot(
                0,
                DEFAULT_USER_ID,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDate.now(),
                0,
                0,
                "--/--/----",
                List.<GoalSnapshot>of(),
                List.of()
        );
    }

    private SavingsModuleService getModuleService() {
        if (moduleService == null) {
            moduleService = new SavingsModuleService();
        }
        return moduleService;
    }

    private SavingsStatsController getStatsController() {
        if (statsController == null) {
            statsController = new SavingsStatsController();
        }
        return statsController;
    }

    private SavingsCalendarController getCalendarController() {
        if (calendarController == null) {
            calendarController = new SavingsCalendarController();
        }
        return calendarController;
    }

    private CsvExportService getCsvExportService() {
        if (csvExportService == null) {
            csvExportService = new CsvExportService();
        }
        return csvExportService;
    }

    private PdfExportService getPdfExportService() {
        if (pdfExportService == null) {
            pdfExportService = new PdfExportService();
        }
        return pdfExportService;
    }

    private GoalsAnalyticsService getGoalsAnalyticsService() {
        if (goalsAnalyticsService == null) {
            goalsAnalyticsService = new GoalsAnalyticsService();
        }
        return goalsAnalyticsService;
    }

    private AiFinancialInsightService getAiFinancialInsightService() {
        if (aiFinancialInsightService == null) {
            aiFinancialInsightService = new AiFinancialInsightService();
        }
        return aiFinancialInsightService;
    }

    private String resolveMessage(RuntimeException exception, String fallbackMessage) {
        if (exception instanceof SavingsModuleException savingsModuleException
                && savingsModuleException.getMessage() != null
                && !savingsModuleException.getMessage().isBlank()) {
            return savingsModuleException.getMessage();
        }

        if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            return exception.getMessage();
        }

        return initializationFailureMessage != null ? initializationFailureMessage : fallbackMessage;
    }

    private boolean matchesGoal(SavingsModuleService.GoalSnapshot goal, String query) {
        if (query.isEmpty()) {
            return true;
        }

        return String.valueOf(goal.id()).contains(query)
                || safeLower(goal.name()).contains(query)
                || String.valueOf(goal.priority()).contains(query)
                || goal.target().toPlainString().contains(query)
                || goal.current().toPlainString().contains(query)
                || (goal.deadline() != null && goal.deadline().toString().contains(query))
                || String.valueOf((int) Math.round(goal.progressPercent())).contains(query);
    }

    private boolean matchesHistory(pi.savings.repository.SavingsTransactionRepository.TransactionRow row, String query) {
        if (query.isEmpty()) {
            return true;
        }

        return String.valueOf(row.id()).contains(query)
                || safeLower(row.type()).contains(query)
                || row.date().toLocalDate().toString().contains(query)
                || row.amount().toPlainString().contains(query)
                || safeLower(row.description()).contains(query)
                || safeLower(row.moduleSource()).contains(query)
                || String.valueOf(row.userId()).contains(query);
    }

    private String normalizeQuery(String rawValue) {
        return safeLower(rawValue).trim();
    }

    private String normalizeSort(String rawValue) {
        return safeLower(rawValue).trim().replace(' ', '_');
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private <T> Comparator<T> applyDirection(Comparator<T> comparator, boolean ascending) {
        return ascending ? comparator : comparator.reversed();
    }

    private Path defaultExportDirectory() {
        return Paths.get("target", "exports");
    }
}
