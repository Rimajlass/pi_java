package pi.savings.service;

import pi.entities.CalendarEvent;
import pi.savings.repository.FinancialGoalRepository;
import pi.savings.repository.SavingAccountRepository;
import pi.savings.repository.SavingsTransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class SavingsStatsService {

    private final SavingAccountRepository savingAccountRepository;
    private final FinancialGoalRepository financialGoalRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;
    private final CurrencyRateService currencyRateService;
    private final SavingsCalendarService calendarService;

    public SavingsStatsService() {
        this(
                new SavingAccountRepository(),
                new FinancialGoalRepository(),
                new SavingsTransactionRepository(),
                new CurrencyRateService(),
                new SavingsCalendarService()
        );
    }

    SavingsStatsService(
            SavingAccountRepository savingAccountRepository,
            FinancialGoalRepository financialGoalRepository,
            SavingsTransactionRepository savingsTransactionRepository,
            CurrencyRateService currencyRateService,
            SavingsCalendarService calendarService
    ) {
        this.savingAccountRepository = savingAccountRepository;
        this.financialGoalRepository = financialGoalRepository;
        this.savingsTransactionRepository = savingsTransactionRepository;
        this.currencyRateService = currencyRateService;
        this.calendarService = calendarService;
    }

    public FrontStatsSnapshot loadFrontStats(int userId, String currency) {
        try {
            SavingAccountRepository.SavingAccountDetails account = savingAccountRepository.findLatestDetailsByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Compte savings introuvable."));
            List<GoalDetails> goals = mapGoals(financialGoalRepository.findDetailedByUserId(userId));
            List<SavingsTransactionRepository.TransactionRow> transactions = savingsTransactionRepository.findSavingsHistoryByUserId(userId);
            CurrencyRateService.RateSnapshot rateSnapshot = currencyRateService.getLatestRateSnapshot(currency);
            List<CalendarEvent> holidays = calendarService.getPublicHolidayEvents(LocalDate.now().getYear());

            return calculateFrontStats(account, goals, transactions, rateSnapshot, holidays);
        } catch (SQLException exception) {
            throw new SavingsStatsException("Impossible de charger les statistiques front-office.", exception);
        }
    }

    public BackOfficeStatsSnapshot loadBackOfficeStats(String currency) {
        try {
            List<SavingAccountRepository.SavingAccountDetails> accounts = savingAccountRepository.findAllDetailedAccounts();
            List<GoalDetails> goals = mapGoals(financialGoalRepository.findAllDetailedGoals());
            CurrencyRateService.RateSnapshot rateSnapshot = currencyRateService.getLatestRateSnapshot(currency);
            List<CalendarEvent> holidays = calendarService.getPublicHolidayEvents(LocalDate.now().getYear());
            return calculateBackOfficeStats(accounts, goals, rateSnapshot, holidays);
        } catch (SQLException exception) {
            throw new SavingsStatsException("Impossible de charger les statistiques back-office.", exception);
        }
    }

    FrontStatsSnapshot calculateFrontStats(
            SavingAccountRepository.SavingAccountDetails account,
            List<GoalDetails> goals,
            List<SavingsTransactionRepository.TransactionRow> transactions,
            CurrencyRateService.RateSnapshot rateSnapshot,
            List<CalendarEvent> holidays
    ) {
        LocalDate today = LocalDate.now();
        GoalCounts counts = countGoals(goals, holidays, today);
        BigDecimal totalTarget = sumGoals(goals, GoalDetails::targetAmount);
        BigDecimal totalCurrent = sumGoals(goals, GoalDetails::currentAmount);
        BigDecimal remaining = totalTarget.subtract(totalCurrent).max(BigDecimal.ZERO);
        BigDecimal progressAverage = averageProgress(goals);
        GoalDetails urgentGoal = goals.stream()
                .filter(goal -> "URGENT".equals(goalStatus(goal, today)))
                .min(Comparator.comparing(GoalDetails::deadline, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        GoalDetails nearestGoal = goals.stream()
                .filter(goal -> goal.deadline() != null)
                .min(Comparator.comparing(GoalDetails::deadline))
                .orElse(null);
        GoalDetails bestProgressGoal = goals.stream()
                .max(Comparator.comparing(goal -> progress(goal).doubleValue()))
                .orElse(null);

        BigDecimal convertedBalance = convert(account.balance(), rateSnapshot);
        Map<String, Double> historicalRates = currencyRateService.getHistoricalRatesToTnd(
                rateSnapshot.currency(),
                LocalDate.now().minusDays(6),
                LocalDate.now()
        );

        return new FrontStatsSnapshot(
                account,
                goals,
                rateSnapshot,
                counts.totalGoals(),
                counts.activeGoals(),
                counts.completedGoals(),
                counts.lateGoals(),
                counts.nearHolidayGoals(),
                account.balance(),
                convertedBalance,
                totalTarget,
                totalCurrent,
                remaining,
                progressAverage,
                nearestGoal == null ? "--/--/----" : nearestGoal.deadline().toString(),
                urgentGoal == null ? "None" : urgentGoal.name(),
                bestProgressGoal == null ? "None" : bestProgressGoal.name(),
                simpleForecast(transactions, remaining),
                historicalRates
        );
    }

    BackOfficeStatsSnapshot calculateBackOfficeStats(
            List<SavingAccountRepository.SavingAccountDetails> accounts,
            List<GoalDetails> goals,
            CurrencyRateService.RateSnapshot rateSnapshot,
            List<CalendarEvent> holidays
    ) {
        LocalDate today = LocalDate.now();
        GoalCounts counts = countGoals(goals, holidays, today);
        BigDecimal totalBalance = accounts.stream().map(SavingAccountRepository.SavingAccountDetails::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgInterest = accounts.isEmpty()
                ? BigDecimal.ZERO
                : accounts.stream().map(SavingAccountRepository.SavingAccountDetails::interestRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(accounts.size()), 2, RoundingMode.HALF_UP);
        BigDecimal totalTarget = sumGoals(goals, GoalDetails::targetAmount);
        BigDecimal totalCurrent = sumGoals(goals, GoalDetails::currentAmount);
        BigDecimal remaining = totalTarget.subtract(totalCurrent).max(BigDecimal.ZERO);

        List<UserBalanceRow> topUsers = accounts.stream()
                .sorted(Comparator.comparing(SavingAccountRepository.SavingAccountDetails::balance).reversed())
                .limit(5)
                .map(account -> new UserBalanceRow(
                        account.userName(),
                        account.userEmail(),
                        account.balance(),
                        convert(account.balance(), rateSnapshot)
                ))
                .toList();

        List<GoalLeaderRow> topGoals = goals.stream()
                .sorted(Comparator.comparing((GoalDetails goal) -> progress(goal).doubleValue()).reversed())
                .limit(5)
                .map(goal -> new GoalLeaderRow(
                        goal.name(),
                        goal.userName(),
                        progress(goal),
                        goal.deadline() == null ? "--/--/----" : goal.deadline().toString(),
                        goalStatus(goal, today)
                ))
                .toList();

        return new BackOfficeStatsSnapshot(
                accounts,
                goals,
                rateSnapshot,
                accounts.size(),
                totalBalance,
                convert(totalBalance, rateSnapshot),
                avgInterest,
                counts.totalGoals(),
                counts.activeGoals(),
                counts.completedGoals(),
                counts.lateGoals(),
                counts.nearHolidayGoals(),
                totalTarget,
                totalCurrent,
                remaining,
                averageProgress(goals),
                topUsers,
                topGoals
        );
    }

    public ExportBundle loadFrontExportBundle(int userId, String currency) {
        try {
            SavingAccountRepository.SavingAccountDetails account = savingAccountRepository.findLatestDetailsByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Compte savings introuvable."));
            List<GoalDetails> goals = mapGoals(financialGoalRepository.findDetailedByUserId(userId));
            List<SavingsTransactionRepository.TransactionRow> transactions =
                    savingsTransactionRepository.findSavingsHistoryByUserId(userId);
            CurrencyRateService.RateSnapshot rateSnapshot = currencyRateService.getLatestRateSnapshot(currency);
            List<CalendarEvent> holidays = calendarService.getPublicHolidayEvents(LocalDate.now().getYear());
            return new ExportBundle(List.of(account), goals, transactions, rateSnapshot, holidays);
        } catch (SQLException exception) {
            throw new SavingsStatsException("Impossible de charger les donnees d'export front-office.", exception);
        }
    }

    public ExportBundle loadBackOfficeExportBundle(String currency) {
        try {
            List<SavingAccountRepository.SavingAccountDetails> accounts = savingAccountRepository.findAllDetailedAccounts();
            List<GoalDetails> goals = mapGoals(financialGoalRepository.findAllDetailedGoals());
            CurrencyRateService.RateSnapshot rateSnapshot = currencyRateService.getLatestRateSnapshot(currency);
            List<CalendarEvent> holidays = calendarService.getPublicHolidayEvents(LocalDate.now().getYear());
            return new ExportBundle(accounts, goals, List.of(), rateSnapshot, holidays);
        } catch (SQLException exception) {
            throw new SavingsStatsException("Impossible de charger les donnees d'export back-office.", exception);
        }
    }

    static String goalStatus(GoalDetails goal, LocalDate today) {
        if (goal == null) {
            return "UNKNOWN";
        }
        if (goal.currentAmount().compareTo(goal.targetAmount()) >= 0 && goal.targetAmount().compareTo(BigDecimal.ZERO) > 0) {
            return "COMPLETED";
        }
        if (goal.deadline() != null && goal.deadline().isBefore(today)) {
            return "LATE";
        }
        if (goal.priority() >= 4 || (goal.deadline() != null && ChronoUnit.DAYS.between(today, goal.deadline()) <= 7)) {
            return "URGENT";
        }
        return "ACTIVE";
    }

    static BigDecimal progress(GoalDetails goal) {
        if (goal == null || goal.targetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return goal.currentAmount()
                .divide(goal.targetAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .min(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal convert(BigDecimal amount, CurrencyRateService.RateSnapshot rateSnapshot) {
        return BigDecimal.valueOf(currencyRateService.convertToTnd(amount.doubleValue(), rateSnapshot.currency()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private GoalCounts countGoals(List<GoalDetails> goals, List<CalendarEvent> holidays, LocalDate today) {
        int active = 0;
        int completed = 0;
        int late = 0;
        int nearHoliday = 0;

        for (GoalDetails goal : goals) {
            String status = goalStatus(goal, today);
            if ("COMPLETED".equals(status)) {
                completed++;
            } else if ("LATE".equals(status)) {
                late++;
            } else {
                active++;
            }
            if (calendarService.isDeadlineNearHoliday(goal.deadline(), holidays)) {
                nearHoliday++;
            }
        }
        return new GoalCounts(goals.size(), active, completed, late, nearHoliday);
    }

    private BigDecimal averageProgress(List<GoalDetails> goals) {
        if (goals.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return goals.stream()
                .map(SavingsStatsService::progress)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(goals.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumGoals(List<GoalDetails> goals, java.util.function.Function<GoalDetails, BigDecimal> extractor) {
        return goals.stream()
                .map(extractor)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String simpleForecast(List<SavingsTransactionRepository.TransactionRow> transactions, BigDecimal remaining) {
        BigDecimal totalDeposits = transactions.stream()
                .map(SavingsTransactionRepository.TransactionRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (transactions.isEmpty() || totalDeposits.compareTo(BigDecimal.ZERO) <= 0 || remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return "Insufficient history for forecast";
        }

        long daysCovered = Math.max(1L, ChronoUnit.DAYS.between(
                transactions.get(transactions.size() - 1).date().toLocalDate(),
                transactions.get(0).date().toLocalDate()
        ) + 1L);
        BigDecimal dailyAverage = totalDeposits.divide(BigDecimal.valueOf(daysCovered), 4, RoundingMode.HALF_UP);
        if (dailyAverage.compareTo(BigDecimal.ZERO) <= 0) {
            return "Insufficient history for forecast";
        }

        BigDecimal days = remaining.divide(dailyAverage, 0, RoundingMode.UP);
        return "At current contribution pace: about " + days.toPlainString() + " days";
    }

    private List<GoalDetails> mapGoals(List<FinancialGoalRepository.FinancialGoalDetails> rows) {
        return rows.stream()
                .map(row -> new GoalDetails(
                        row.goalName(),
                        row.userName(),
                        row.userEmail(),
                        row.targetAmount(),
                        row.currentAmount(),
                        row.deadline(),
                        row.priority()
                ))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public record GoalDetails(
            String name,
            String userName,
            String userEmail,
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            LocalDate deadline,
            int priority
    ) {
    }

    private record GoalCounts(
            int totalGoals,
            int activeGoals,
            int completedGoals,
            int lateGoals,
            int nearHolidayGoals
    ) {
    }

    public record FrontStatsSnapshot(
            SavingAccountRepository.SavingAccountDetails account,
            List<GoalDetails> goals,
            CurrencyRateService.RateSnapshot rateSnapshot,
            int totalGoals,
            int activeGoals,
            int completedGoals,
            int lateGoals,
            int nearHolidayGoals,
            BigDecimal balance,
            BigDecimal convertedBalance,
            BigDecimal totalTarget,
            BigDecimal totalCurrent,
            BigDecimal remaining,
            BigDecimal averageProgress,
            String nearestDeadline,
            String urgentGoal,
            String bestProgressGoal,
            String forecast,
            Map<String, Double> historicalRates
    ) {
    }

    public record UserBalanceRow(
            String userName,
            String userEmail,
            BigDecimal balance,
            BigDecimal convertedBalance
    ) {
    }

    public record GoalLeaderRow(
            String goalName,
            String userName,
            BigDecimal progressPercent,
            String deadline,
            String status
    ) {
    }

    public record BackOfficeStatsSnapshot(
            List<SavingAccountRepository.SavingAccountDetails> accounts,
            List<GoalDetails> goals,
            CurrencyRateService.RateSnapshot rateSnapshot,
            int totalSavingsAccounts,
            BigDecimal totalBalance,
            BigDecimal convertedBalance,
            BigDecimal averageInterestRate,
            int totalGoals,
            int activeGoals,
            int completedGoals,
            int lateGoals,
            int nearHolidayGoals,
            BigDecimal totalTarget,
            BigDecimal totalCurrent,
            BigDecimal remainingAmount,
            BigDecimal averageProgress,
            List<UserBalanceRow> topUsers,
            List<GoalLeaderRow> topGoals
    ) {
    }

    public record ExportBundle(
            List<SavingAccountRepository.SavingAccountDetails> accounts,
            List<GoalDetails> goals,
            List<SavingsTransactionRepository.TransactionRow> transactions,
            CurrencyRateService.RateSnapshot rateSnapshot,
            List<CalendarEvent> holidays
    ) {
    }

    public static final class SavingsStatsException extends RuntimeException {
        public SavingsStatsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
