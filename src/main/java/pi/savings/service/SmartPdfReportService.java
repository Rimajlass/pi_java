package pi.savings.service;

import pi.savings.dto.GoalAnalyticsDTO;
import pi.savings.dto.GoalRiskDTO;
import pi.savings.dto.PdfExportResultDTO;
import pi.savings.dto.WhatIfScenarioDTO;
import pi.savings.repository.SavingAccountRepository;
import pi.tools.ConfigLoader;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SmartPdfReportService {

    private static final int DEFAULT_USER_ID = 1;
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final SavingsModuleService savingsModuleService;
    private final SavingsStatsService savingsStatsService;
    private final GoalsAnalyticsService goalsAnalyticsService;
    private final AiFinancialInsightService aiFinancialInsightService;
    private final PdfApiClientService pdfApiClientService;
    private final PdfReportHtmlBuilder pdfReportHtmlBuilder;
    private final PdfExportService pdfExportService;

    public SmartPdfReportService() {
        this(
                new SavingsModuleService(),
                new SavingsStatsService(),
                new GoalsAnalyticsService(),
                new AiFinancialInsightService(),
                new PdfApiClientService(),
                new PdfReportHtmlBuilder(),
                new PdfExportService()
        );
    }

    SmartPdfReportService(
            SavingsModuleService savingsModuleService,
            SavingsStatsService savingsStatsService,
            GoalsAnalyticsService goalsAnalyticsService,
            AiFinancialInsightService aiFinancialInsightService,
            PdfApiClientService pdfApiClientService,
            PdfReportHtmlBuilder pdfReportHtmlBuilder,
            PdfExportService pdfExportService
    ) {
        this.savingsModuleService = savingsModuleService;
        this.savingsStatsService = savingsStatsService;
        this.goalsAnalyticsService = goalsAnalyticsService;
        this.aiFinancialInsightService = aiFinancialInsightService;
        this.pdfApiClientService = pdfApiClientService;
        this.pdfReportHtmlBuilder = pdfReportHtmlBuilder;
        this.pdfExportService = pdfExportService;
    }

    public PdfExportResultDTO generateSmartGoalsReport(
            String currency,
            String queryText,
            String sortAttributeText,
            String sortDirectionText,
            Path exportDirectory
    ) {
        if (!pdfApiClientService.hasConfiguredApiKey()) {
            return PdfExportResultDTO.error(
                    "Le service Smart PDF n'est pas configure. Ajoutez HTML2PDF_API_KEY ou PDFSHIFT_API_KEY dans config.properties, .env ou vos variables d'environnement."
            );
        }

        try {
            SmartPdfReportData reportData = buildReportData(
                    DEFAULT_USER_ID,
                    currency == null || currency.isBlank() ? "TND" : currency,
                    queryText,
                    sortAttributeText,
                    sortDirectionText
            );
            Files.createDirectories(exportDirectory);

            String html = pdfReportHtmlBuilder.build(reportData);
            String fileName = "smart-goals-report-" + LocalDateTime.now().format(FILE_STAMP) + ".pdf";
            byte[] pdfBytes = pdfApiClientService.generatePdf(html, fileName);
            Path savedFile = exportDirectory.resolve(fileName);
            Files.write(savedFile, pdfBytes);
            return PdfExportResultDTO.success(
                    "Smart PDF report generated successfully: " + savedFile.toAbsolutePath(),
                    savedFile,
                    true,
                    false
            );
        } catch (PdfApiClientService.PdfApiException exception) {
            if (allowLocalFallback()) {
                try {
                    return generateLocalFallback(queryText, sortAttributeText, sortDirectionText, exportDirectory);
                } catch (RuntimeException fallbackException) {
                    return PdfExportResultDTO.error("La generation du Smart PDF a echoue. Verifiez votre configuration PDF puis reessayez.");
                }
            }
            return PdfExportResultDTO.error(exception.getMessage() == null || exception.getMessage().isBlank()
                    ? "La generation du Smart PDF a echoue. Verifiez votre configuration PDF puis reessayez."
                    : exception.getMessage());
        } catch (IOException | RuntimeException exception) {
            return PdfExportResultDTO.error("La generation du Smart PDF a echoue. Verifiez votre configuration PDF puis reessayez.");
        }
    }

    private SmartPdfReportData buildReportData(
            int userId,
            String currency,
            String queryText,
            String sortAttributeText,
            String sortDirectionText
    ) {
        SavingsModuleService.DashboardSnapshot dashboard = savingsModuleService.loadDashboard(userId);
        SavingsStatsService.ExportBundle exportBundle = savingsStatsService.loadFrontExportBundle(userId, currency);
        List<SavingsModuleService.GoalSnapshot> filteredGoals = filterAndSortGoals(
                dashboard.goals(),
                queryText,
                sortAttributeText,
                sortDirectionText
        );
        GoalAnalyticsDTO analytics = goalsAnalyticsService.analyze(
                filteredGoals,
                dashboard.transactions(),
                GoalsAnalyticsService.AnalyzeAttribute.PRIORITY
        );
        String aiInsight = loadAiInsight(analytics);
        WhatIfScenarioDTO whatIfScenario = buildScenario(analytics);
        SavingAccountRepository.SavingAccountDetails account = exportBundle.accounts().isEmpty()
                ? null
                : exportBundle.accounts().get(0);

        return new SmartPdfReportData(
                currency.toUpperCase(Locale.ROOT),
                account == null ? "Savings User" : safe(account.userName()),
                account == null ? BigDecimal.ZERO : account.balance(),
                exportBundle.rateSnapshot().provider(),
                analytics,
                analytics.goalRisks(),
                aiInsight,
                !AiFinancialInsightService.FALLBACK_MESSAGE.equals(aiInsight),
                whatIfScenario
        );
    }

    private List<SavingsModuleService.GoalSnapshot> filterAndSortGoals(
            List<SavingsModuleService.GoalSnapshot> goals,
            String queryText,
            String sortAttributeText,
            String sortDirectionText
    ) {
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

        Comparator<SavingsModuleService.GoalSnapshot> resolvedComparator = applyDirection(comparator, ascending)
                .thenComparing(applyDirection(Comparator.comparingInt(SavingsModuleService.GoalSnapshot::priority), ascending))
                .thenComparing(applyDirection(Comparator.comparingInt(SavingsModuleService.GoalSnapshot::id), ascending));

        return (goals == null ? List.<SavingsModuleService.GoalSnapshot>of() : goals).stream()
                .filter(goal -> matchesGoal(goal, query))
                .sorted(resolvedComparator)
                .toList();
    }

    private boolean matchesGoal(SavingsModuleService.GoalSnapshot goal, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return safeLower(goal.name()).contains(query)
                || String.valueOf(goal.priority()).contains(query)
                || goal.target().toPlainString().contains(query)
                || goal.current().toPlainString().contains(query)
                || (goal.deadline() != null && goal.deadline().toString().contains(query))
                || String.valueOf((int) Math.round(goal.progressPercent())).contains(query);
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

    private String safe(String value) {
        return value == null || value.isBlank() ? "Savings User" : value;
    }

    private <T> Comparator<T> applyDirection(Comparator<T> comparator, boolean ascending) {
        return ascending ? comparator : comparator.reversed();
    }

    private String loadAiInsight(GoalAnalyticsDTO analytics) {
        try {
            return aiFinancialInsightService.generateInsight(analytics);
        } catch (AiFinancialInsightService.AiInsightException exception) {
            return AiFinancialInsightService.FALLBACK_MESSAGE;
        }
    }

    private WhatIfScenarioDTO buildScenario(GoalAnalyticsDTO analytics) {
        BigDecimal baseline = analytics.requiredMonthlyContribution().compareTo(BigDecimal.ZERO) > 0
                ? analytics.requiredMonthlyContribution()
                : new BigDecimal("250");
        return goalsAnalyticsService.simulateScenario(analytics.goalRisks(), "Balanced smart scenario", baseline);
    }

    private PdfExportResultDTO generateLocalFallback(
            String queryText,
            String sortAttributeText,
            String sortDirectionText,
            Path exportDirectory
    ) {
        SavingsStatsService.ExportBundle bundle = savingsStatsService.loadFrontExportBundle(DEFAULT_USER_ID, "TND");
        List<String> goalNames = filterAndSortGoals(
                savingsModuleService.loadDashboard(DEFAULT_USER_ID).goals(),
                queryText,
                sortAttributeText,
                sortDirectionText
        ).stream().map(SavingsModuleService.GoalSnapshot::name).toList();
        Path exportPath = pdfExportService.exportGoalsPdf(
                bundle.goals().stream().filter(goal -> goalNames.contains(goal.name())).toList(),
                bundle.rateSnapshot(),
                bundle.holidays(),
                exportDirectory
        );
        return PdfExportResultDTO.success(
                "Smart PDF API unavailable. Local fallback PDF saved: " + exportPath.toAbsolutePath(),
                exportPath,
                false,
                true
        );
    }

    private boolean allowLocalFallback() {
        return Boolean.parseBoolean(ConfigLoader.get("SMART_PDF_ALLOW_LOCAL_FALLBACK", "false"));
    }

    public record SmartPdfReportData(
            String currency,
            String userName,
            BigDecimal savingsBalance,
            String rateProvider,
            GoalAnalyticsDTO analytics,
            List<GoalRiskDTO> goalRisks,
            String aiInsight,
            boolean aiInsightAvailable,
            WhatIfScenarioDTO whatIfScenario
    ) {
    }
}
