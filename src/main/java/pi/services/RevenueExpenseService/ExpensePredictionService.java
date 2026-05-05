package pi.services.RevenueExpenseService;

import pi.entities.Expense;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExpensePredictionService {

    private static final int PREDICTION_WINDOW_DAYS = 30;

    public double calculateDailyAverage(List<Expense> expenses) {
        List<Expense> recentExpenses = filterRecentExpenses(expenses);
        if (recentExpenses.isEmpty()) {
            return 0.0;
        }

        double totalExpenses = recentExpenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();
        long observedDays = calculateObservedDays(recentExpenses);
        if (observedDays <= 0) {
            return 0.0;
        }

        return totalExpenses / observedDays;
    }

    public double predictMonthlyTotal(List<Expense> expenses) {
        return calculateDailyAverage(expenses) * PREDICTION_WINDOW_DAYS;
    }

    public double estimateDaysToExceedBudget(List<Expense> expenses, double budget) {
        if (budget <= 0) {
            return 0.0;
        }

        double dailyAverage = calculateDailyAverage(expenses);
        if (dailyAverage <= 0) {
            return Double.POSITIVE_INFINITY;
        }

        double currentMonthSpent = calculateCurrentMonthSpent(expenses);
        if (currentMonthSpent >= budget) {
            return 0.0;
        }

        double remainingBudget = budget - currentMonthSpent;
        return remainingBudget / dailyAverage;
    }

    private List<Expense> filterRecentExpenses(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(PREDICTION_WINDOW_DAYS - 1L);

        return expenses.stream()
                .filter(Objects::nonNull)
                .filter(expense -> expense.getExpenseDate() != null)
                .filter(expense -> !expense.getExpenseDate().isBefore(startDate) && !expense.getExpenseDate().isAfter(today))
                .sorted(Comparator.comparing(Expense::getExpenseDate))
                .collect(Collectors.toList());
    }

    private long calculateObservedDays(List<Expense> expenses) {
        if (expenses.isEmpty()) {
            return 0;
        }

        LocalDate earliestDate = expenses.stream()
                .map(Expense::getExpenseDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        long observedDays = ChronoUnit.DAYS.between(earliestDate, LocalDate.now()) + 1;
        return Math.max(1, Math.min(PREDICTION_WINDOW_DAYS, observedDays));
    }

    private double calculateCurrentMonthSpent(List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return 0.0;
        }

        LocalDate now = LocalDate.now();
        return expenses.stream()
                .filter(Objects::nonNull)
                .filter(expense -> expense.getExpenseDate() != null)
                .filter(expense -> expense.getExpenseDate().getYear() == now.getYear()
                        && expense.getExpenseDate().getMonth() == now.getMonth())
                .mapToDouble(Expense::getAmount)
                .sum();
    }
}
