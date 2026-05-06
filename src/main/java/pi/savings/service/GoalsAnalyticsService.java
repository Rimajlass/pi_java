package pi.savings.service;

import pi.savings.dto.AttributeStatsDTO;
import pi.savings.dto.GoalAnalyticsDTO;
import pi.savings.dto.GoalRiskDTO;
import pi.savings.dto.WhatIfScenarioDTO;
import pi.savings.repository.SavingsTransactionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class GoalsAnalyticsService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String GOAL_PREFIX = "Contribution to goal:";

    public GoalAnalyticsDTO analyze(
            List<SavingsModuleService.GoalSnapshot> goals,
            List<SavingsTransactionRepository.TransactionRow> transactions,
            AnalyzeAttribute selectedAttribute
    ) {
        List<SavingsModuleService.GoalSnapshot> safeGoals = goals == null ? List.of() : goals;
        List<SavingsTransactionRepository.TransactionRow> safeTransactions = transactions == null ? List.of() : transactions;
        AnalyzeAttribute resolvedAttribute = selectedAttribute == null ? AnalyzeAttribute.PRIORITY : selectedAttribute;

        Map<String, List<SavingsTransactionRepository.TransactionRow>> contributionsByGoal =
                groupContributionsByGoalName(safeTransactions);
        boolean hasContributionData = safeTransactions.stream()
                .anyMatch(row -> "GOAL_CONTRIBUTION".equalsIgnoreCase(row.type()));

        List<GoalRiskDTO> risks = safeGoals.stream()
                .map(goal -> toGoalRisk(goal, contributionsByGoal.getOrDefault(normalize(goal.name()), List.of())))
                .sorted(Comparator.comparingInt(GoalRiskDTO::priority).thenComparing(GoalRiskDTO::goalName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int completed = (int) risks.stream().filter(goal -> "Completed".equalsIgnoreCase(goal.status())).count();
        int overdue = (int) risks.stream().filter(goal -> "Late".equalsIgnoreCase(goal.status())).count();
        int atRisk = (int) risks.stream()
                .filter(goal -> "High".equalsIgnoreCase(goal.riskLevel()) || "Critical".equalsIgnoreCase(goal.riskLevel()))
                .count();
        int active = Math.max(0, risks.size() - completed);

        BigDecimal totalTarget = risks.stream().map(GoalRiskDTO::targetAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal totalCurrent = risks.stream().map(GoalRiskDTO::currentAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal totalRemaining = risks.stream().map(GoalRiskDTO::remainingAmount).reduce(ZERO, BigDecimal::add);
        BigDecimal avgProgress = risks.isEmpty()
                ? ZERO
                : risks.stream().map(GoalRiskDTO::progressPercentage).reduce(ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(risks.size()), 2, RoundingMode.HALF_UP);
        BigDecimal totalRequiredMonthly = risks.stream()
                .map(GoalRiskDTO::requiredMonthlyContribution)
                .reduce(ZERO, BigDecimal::add);

        LocalDate nearestDeadline = risks.stream()
                .map(GoalRiskDTO::deadline)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

        Map<String, Integer> riskDistribution = countBy(risks, GoalRiskDTO::riskLevel, List.of("Low", "Medium", "High", "Critical"));
        Map<String, Integer> statusDistribution = countBy(risks, GoalRiskDTO::status, List.of("On Track", "Watch", "At Risk", "Critical", "Late", "Completed"));
        Map<String, AttributeStatsDTO> attributeStats = buildAttributeStats(risks, resolvedAttribute);

        int financialHealthScore = calculateFinancialHealthScore(avgProgress, overdue, atRisk, riskDistribution);
        String healthStatus = healthLabel(financialHealthScore);

        return new GoalAnalyticsDTO(
                resolvedAttribute.label(),
                risks.size(),
                completed,
                active,
                overdue,
                atRisk,
                scale(totalTarget),
                scale(totalCurrent),
                scale(totalRemaining),
                scale(avgProgress),
                scale(totalRequiredMonthly),
                financialHealthScore,
                healthStatus,
                nearestDeadline,
                riskDistribution,
                statusDistribution,
                attributeStats,
                risks,
                hasContributionData
        );
    }

    public List<String> listAnalyzeByAttributes(boolean hasContributionData) {
        List<String> attributes = new ArrayList<>(List.of(
                AnalyzeAttribute.GOAL_NAME.label(),
                AnalyzeAttribute.TARGET_AMOUNT.label(),
                AnalyzeAttribute.CURRENT_AMOUNT.label(),
                AnalyzeAttribute.REMAINING_AMOUNT.label(),
                AnalyzeAttribute.DEADLINE.label(),
                AnalyzeAttribute.PRIORITY.label(),
                AnalyzeAttribute.PROGRESS_PERCENTAGE.label(),
                AnalyzeAttribute.STATUS.label(),
                AnalyzeAttribute.RISK_LEVEL.label(),
                AnalyzeAttribute.REQUIRED_MONTHLY_CONTRIBUTION.label(),
                AnalyzeAttribute.PREDICTED_COMPLETION_DATE.label()
        ));
        if (hasContributionData) {
            attributes.add(AnalyzeAttribute.CONTRIBUTION_AMOUNT.label());
            attributes.add(AnalyzeAttribute.CONTRIBUTION_DATE.label());
            attributes.add(AnalyzeAttribute.CONTRIBUTION_MONTH.label());
            attributes.add(AnalyzeAttribute.GOAL_CONTRIBUTION_FREQUENCY.label());
        }
        return attributes;
    }

    public WhatIfScenarioDTO simulateScenario(
            List<GoalRiskDTO> goalRisks,
            String scenarioName,
            BigDecimal monthlyContribution
    ) {
        List<GoalRiskDTO> actionableGoals = (goalRisks == null ? List.<GoalRiskDTO>of() : goalRisks).stream()
                .filter(goal -> !"Completed".equalsIgnoreCase(goal.status()))
                .toList();

        BigDecimal budget = scale(monthlyContribution);
        if (budget.compareTo(BigDecimal.ZERO) <= 0 || actionableGoals.isEmpty()) {
            return new WhatIfScenarioDTO(
                    scenarioName,
                    budget,
                    List.of()
            );
        }

        BigDecimal totalWeight = actionableGoals.stream()
                .map(goal -> scenarioWeight(goal.priority()).multiply(goal.remainingAmount().max(BigDecimal.ONE)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            totalWeight = BigDecimal.ONE;
        }

        List<WhatIfScenarioDTO.GoalProjectionDTO> projections = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (GoalRiskDTO goal : actionableGoals) {
            BigDecimal weight = scenarioWeight(goal.priority()).multiply(goal.remainingAmount().max(BigDecimal.ONE));
            BigDecimal allocation = budget.multiply(weight).divide(totalWeight, 2, RoundingMode.HALF_UP);
            if (allocation.compareTo(BigDecimal.ZERO) <= 0) {
                allocation = new BigDecimal("0.01");
            }

            int months = goal.remainingAmount().compareTo(BigDecimal.ZERO) <= 0
                    ? 0
                    : goal.remainingAmount().divide(allocation, 0, RoundingMode.CEILING).intValue();
            LocalDate predictedDate = now.plusDays((long) months * 30L);

            boolean beforeDeadline = goal.deadline() == null || !predictedDate.isAfter(goal.deadline());
            int dayDifference = goal.deadline() == null ? 0 : (int) ChronoUnit.DAYS.between(goal.deadline(), predictedDate);
            String status = beforeDeadline
                    ? "Can finish before deadline"
                    : "Likely to miss deadline by " + Math.abs(dayDifference) + " days";

            projections.add(new WhatIfScenarioDTO.GoalProjectionDTO(
                    goal.goalName(),
                    scale(allocation),
                    predictedDate,
                    beforeDeadline,
                    dayDifference,
                    status
            ));
        }

        projections.sort(Comparator.comparing(WhatIfScenarioDTO.GoalProjectionDTO::goalName, String.CASE_INSENSITIVE_ORDER));
        return new WhatIfScenarioDTO(scenarioName, budget, projections);
    }

    public enum AnalyzeAttribute {
        GOAL_NAME("Goal Name"),
        TARGET_AMOUNT("Target Amount"),
        CURRENT_AMOUNT("Current Amount"),
        REMAINING_AMOUNT("Remaining Amount"),
        DEADLINE("Deadline"),
        PRIORITY("Priority"),
        PROGRESS_PERCENTAGE("Progress Percentage"),
        STATUS("Status"),
        RISK_LEVEL("Risk Level"),
        REQUIRED_MONTHLY_CONTRIBUTION("Required Monthly Contribution"),
        PREDICTED_COMPLETION_DATE("Predicted Completion Date"),
        CONTRIBUTION_AMOUNT("Contribution Amount"),
        CONTRIBUTION_DATE("Contribution Date"),
        CONTRIBUTION_MONTH("Contribution Month"),
        GOAL_CONTRIBUTION_FREQUENCY("Goal Contribution Frequency");

        private final String label;

        AnalyzeAttribute(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        public static AnalyzeAttribute fromLabel(String value) {
            if (value == null || value.isBlank()) {
                return PRIORITY;
            }
            for (AnalyzeAttribute attribute : values()) {
                if (attribute.label.equalsIgnoreCase(value.trim())) {
                    return attribute;
                }
            }
            return PRIORITY;
        }
    }

    private GoalRiskDTO toGoalRisk(
            SavingsModuleService.GoalSnapshot goal,
            List<SavingsTransactionRepository.TransactionRow> contributions
    ) {
        BigDecimal target = scale(goal.target());
        BigDecimal current = scale(goal.current());
        BigDecimal remaining = scale(target.subtract(current).max(BigDecimal.ZERO));
        BigDecimal progress = target.compareTo(BigDecimal.ZERO) <= 0
                ? ZERO
                : scale(current.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100)));

        LocalDate now = LocalDate.now();
        int daysLeft = goal.deadline() == null ? Integer.MAX_VALUE : (int) ChronoUnit.DAYS.between(now, goal.deadline());
        BigDecimal requiredMonthly = scale(calculateRequiredMonthlyContribution(remaining, goal.deadline(), now));
        BigDecimal totalContribution = contributions.stream()
                .map(SavingsTransactionRepository.TransactionRow::amount)
                .filter(Objects::nonNull)
                .map(this::scale)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal avgMonthlyContribution = scale(calculateAverageMonthlyContribution(contributions));
        LocalDate lastContributionDate = contributions.stream()
                .map(SavingsTransactionRepository.TransactionRow::date)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(java.time.LocalDateTime::toLocalDate)
                .orElse(null);
        String frequency = detectContributionFrequency(contributions);

        LocalDate predictedCompletionDate = predictCompletionDate(remaining, avgMonthlyContribution, requiredMonthly, now);
        int riskScore = calculateRiskScore(goal.priority(), progress, daysLeft, requiredMonthly, avgMonthlyContribution, remaining);
        String riskLevel = riskScore >= 85 ? "Critical" : riskScore >= 60 ? "High" : riskScore >= 35 ? "Medium" : "Low";
        String status = resolveStatus(current, target, goal.deadline(), predictedCompletionDate, riskLevel, now);

        return new GoalRiskDTO(
                goal.name(),
                goal.priority(),
                target,
                current,
                remaining,
                progress,
                goal.deadline(),
                daysLeft == Integer.MAX_VALUE ? 9999 : daysLeft,
                requiredMonthly,
                predictedCompletionDate,
                riskLevel,
                status,
                totalContribution,
                avgMonthlyContribution,
                lastContributionDate,
                frequency
        );
    }

    private Map<String, List<SavingsTransactionRepository.TransactionRow>> groupContributionsByGoalName(
            List<SavingsTransactionRepository.TransactionRow> transactions
    ) {
        return transactions.stream()
                .filter(row -> "GOAL_CONTRIBUTION".equalsIgnoreCase(row.type()))
                .filter(row -> row.description() != null && row.description().toLowerCase(Locale.ROOT).startsWith(GOAL_PREFIX.toLowerCase(Locale.ROOT)))
                .collect(Collectors.groupingBy(
                        row -> normalize(parseGoalName(row.description())),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    private String parseGoalName(String description) {
        if (description == null) {
            return "";
        }
        int index = description.indexOf(':');
        if (index < 0 || index + 1 >= description.length()) {
            return description.trim();
        }
        return description.substring(index + 1).trim();
    }

    private BigDecimal calculateRequiredMonthlyContribution(BigDecimal remaining, LocalDate deadline, LocalDate now) {
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (deadline == null) {
            return remaining.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        }
        long days = ChronoUnit.DAYS.between(now, deadline);
        if (days <= 0) {
            return remaining;
        }
        int months = Math.max(1, (int) Math.ceil(days / 30.0));
        return remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageMonthlyContribution(List<SavingsTransactionRepository.TransactionRow> contributions) {
        if (contributions == null || contributions.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = contributions.stream()
                .map(SavingsTransactionRepository.TransactionRow::amount)
                .filter(Objects::nonNull)
                .map(this::scale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        YearMonth minMonth = contributions.stream()
                .map(SavingsTransactionRepository.TransactionRow::date)
                .filter(Objects::nonNull)
                .map(YearMonth::from)
                .min(Comparator.naturalOrder())
                .orElse(YearMonth.now());
        YearMonth maxMonth = contributions.stream()
                .map(SavingsTransactionRepository.TransactionRow::date)
                .filter(Objects::nonNull)
                .map(YearMonth::from)
                .max(Comparator.naturalOrder())
                .orElse(minMonth);
        long months = ChronoUnit.MONTHS.between(minMonth, maxMonth) + 1;
        return total.divide(BigDecimal.valueOf(Math.max(1, months)), 2, RoundingMode.HALF_UP);
    }

    private String detectContributionFrequency(List<SavingsTransactionRepository.TransactionRow> contributions) {
        if (contributions == null || contributions.isEmpty()) {
            return "No contributions";
        }
        if (contributions.size() == 1) {
            return "Low frequency";
        }

        List<LocalDate> dates = contributions.stream()
                .map(SavingsTransactionRepository.TransactionRow::date)
                .filter(Objects::nonNull)
                .map(java.time.LocalDateTime::toLocalDate)
                .sorted()
                .toList();
        if (dates.size() <= 1) {
            return "Low frequency";
        }

        long totalGaps = 0;
        for (int i = 1; i < dates.size(); i++) {
            totalGaps += Math.max(1, ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i)));
        }
        double avgGap = totalGaps / (double) (dates.size() - 1);
        if (avgGap <= 10) {
            return "Weekly";
        }
        if (avgGap <= 35) {
            return "Monthly";
        }
        return "Low frequency";
    }

    private LocalDate predictCompletionDate(
            BigDecimal remaining,
            BigDecimal averageMonthlyContribution,
            BigDecimal requiredMonthlyContribution,
            LocalDate startDate
    ) {
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return startDate;
        }
        BigDecimal pace = averageMonthlyContribution.compareTo(BigDecimal.ZERO) > 0
                ? averageMonthlyContribution
                : requiredMonthlyContribution;
        if (pace.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        int months = remaining.divide(pace, 0, RoundingMode.CEILING).intValue();
        return startDate.plusDays((long) months * 30L);
    }

    private int calculateRiskScore(
            int priority,
            BigDecimal progressPercentage,
            int daysLeft,
            BigDecimal requiredMonthlyContribution,
            BigDecimal averageMonthlyContribution,
            BigDecimal remaining
    ) {
        int score = 0;
        double progress = progressPercentage.doubleValue();
        if (daysLeft <= 0 && remaining.compareTo(BigDecimal.ZERO) > 0) {
            score += 45;
        } else if (daysLeft <= 30 && progress < 70) {
            score += 28;
        } else if (daysLeft <= 60 && progress < 50) {
            score += 22;
        } else if (daysLeft <= 90 && progress < 35) {
            score += 14;
        }

        if (averageMonthlyContribution.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal ratio = requiredMonthlyContribution.divide(averageMonthlyContribution, 2, RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("2.0")) >= 0) {
                score += 22;
            } else if (ratio.compareTo(new BigDecimal("1.4")) >= 0) {
                score += 14;
            }
        } else if (requiredMonthlyContribution.compareTo(new BigDecimal("100")) >= 0) {
            score += 16;
        }

        if (priority <= 2 && progress < 45) {
            score += 12;
        }
        return Math.min(100, score);
    }

    private String resolveStatus(
            BigDecimal current,
            BigDecimal target,
            LocalDate deadline,
            LocalDate predictedCompletionDate,
            String riskLevel,
            LocalDate now
    ) {
        if (target.compareTo(BigDecimal.ZERO) > 0 && current.compareTo(target) >= 0) {
            return "Completed";
        }
        if (deadline != null && deadline.isBefore(now)) {
            return "Late";
        }
        if ("Critical".equalsIgnoreCase(riskLevel)) {
            return "Critical";
        }
        if ("High".equalsIgnoreCase(riskLevel)) {
            return "At Risk";
        }
        if ("Medium".equalsIgnoreCase(riskLevel)) {
            return "Watch";
        }
        return "On Track";
    }

    private Map<String, Integer> countBy(
            List<GoalRiskDTO> goals,
            java.util.function.Function<GoalRiskDTO, String> classifier,
            List<String> preferredOrder
    ) {
        Map<String, Integer> countMap = new LinkedHashMap<>();
        for (String key : preferredOrder) {
            countMap.put(key, 0);
        }
        for (GoalRiskDTO goal : goals) {
            String key = classifier.apply(goal);
            countMap.put(key, countMap.getOrDefault(key, 0) + 1);
        }
        return countMap;
    }

    private Map<String, AttributeStatsDTO> buildAttributeStats(
            List<GoalRiskDTO> goals,
            AnalyzeAttribute selectedAttribute
    ) {
        Map<String, List<GoalRiskDTO>> grouped = goals.stream()
                .collect(Collectors.groupingBy(goal -> resolveAttributeValue(goal, selectedAttribute), LinkedHashMap::new, Collectors.toList()));

        Map<String, AttributeStatsDTO> stats = new LinkedHashMap<>();
        grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> {
                    List<GoalRiskDTO> bucket = entry.getValue();
                    BigDecimal totalTarget = bucket.stream().map(GoalRiskDTO::targetAmount).reduce(ZERO, BigDecimal::add);
                    BigDecimal totalCurrent = bucket.stream().map(GoalRiskDTO::currentAmount).reduce(ZERO, BigDecimal::add);
                    BigDecimal totalRemaining = bucket.stream().map(GoalRiskDTO::remainingAmount).reduce(ZERO, BigDecimal::add);
                    BigDecimal averageProgress = bucket.isEmpty() ? ZERO : bucket.stream()
                            .map(GoalRiskDTO::progressPercentage)
                            .reduce(ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(bucket.size()), 2, RoundingMode.HALF_UP);
                    BigDecimal averageRequiredMonthly = bucket.isEmpty() ? ZERO : bucket.stream()
                            .map(GoalRiskDTO::requiredMonthlyContribution)
                            .reduce(ZERO, BigDecimal::add)
                            .divide(BigDecimal.valueOf(bucket.size()), 2, RoundingMode.HALF_UP);

                    int low = (int) bucket.stream().filter(goal -> "Low".equalsIgnoreCase(goal.riskLevel())).count();
                    int medium = (int) bucket.stream().filter(goal -> "Medium".equalsIgnoreCase(goal.riskLevel())).count();
                    int high = (int) bucket.stream().filter(goal -> "High".equalsIgnoreCase(goal.riskLevel()) || "Critical".equalsIgnoreCase(goal.riskLevel())).count();

                    stats.put(entry.getKey(), new AttributeStatsDTO(
                            entry.getKey(),
                            bucket.size(),
                            scale(totalTarget),
                            scale(totalCurrent),
                            scale(totalRemaining),
                            scale(averageProgress),
                            scale(averageRequiredMonthly),
                            low,
                            medium,
                            high
                    ));
                });

        return stats;
    }

    private int calculateFinancialHealthScore(
            BigDecimal averageProgress,
            int overdueGoals,
            int atRiskGoals,
            Map<String, Integer> riskDistribution
    ) {
        int highRisk = riskDistribution.getOrDefault("High", 0) + riskDistribution.getOrDefault("Critical", 0);
        int mediumRisk = riskDistribution.getOrDefault("Medium", 0);
        int penalty = (overdueGoals * 18) + (atRiskGoals * 10) + (highRisk * 8) + (mediumRisk * 4);
        int progressBoost = (int) Math.round(averageProgress.doubleValue() * 0.3);
        int score = 55 + progressBoost - penalty;
        return Math.max(0, Math.min(100, score));
    }

    private String healthLabel(int score) {
        if (score >= 80) {
            return "Excellent";
        }
        if (score >= 60) {
            return "Good";
        }
        if (score >= 40) {
            return "Medium";
        }
        if (score >= 20) {
            return "At Risk";
        }
        return "Critical";
    }

    private String resolveAttributeValue(GoalRiskDTO goal, AnalyzeAttribute attribute) {
        return switch (attribute) {
            case GOAL_NAME -> goal.goalName();
            case TARGET_AMOUNT -> bucketAmount(goal.targetAmount());
            case CURRENT_AMOUNT -> bucketAmount(goal.currentAmount());
            case REMAINING_AMOUNT -> bucketAmount(goal.remainingAmount());
            case DEADLINE -> goal.deadline() == null
                    ? "No deadline"
                    : goal.deadline().getYear() + "-" + twoDigits(goal.deadline().getMonthValue());
            case PRIORITY -> "P" + goal.priority();
            case PROGRESS_PERCENTAGE -> bucketProgress(goal.progressPercentage());
            case STATUS -> goal.status();
            case RISK_LEVEL -> goal.riskLevel();
            case REQUIRED_MONTHLY_CONTRIBUTION -> bucketAmount(goal.requiredMonthlyContribution());
            case PREDICTED_COMPLETION_DATE -> goal.predictedCompletionDate() == null
                    ? "No prediction"
                    : goal.predictedCompletionDate().getYear() + "-" + twoDigits(goal.predictedCompletionDate().getMonthValue());
            case CONTRIBUTION_AMOUNT -> bucketAmount(goal.totalContributionAmount());
            case CONTRIBUTION_DATE -> goal.lastContributionDate() == null ? "No contribution" : goal.lastContributionDate().toString();
            case CONTRIBUTION_MONTH -> goal.lastContributionDate() == null
                    ? "No contribution"
                    : goal.lastContributionDate().getYear() + "-" + twoDigits(goal.lastContributionDate().getMonthValue());
            case GOAL_CONTRIBUTION_FREQUENCY -> goal.contributionFrequency();
        };
    }

    private String bucketAmount(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        if (value.compareTo(new BigDecimal("500")) < 0) {
            return "0-499";
        }
        if (value.compareTo(new BigDecimal("1000")) < 0) {
            return "500-999";
        }
        if (value.compareTo(new BigDecimal("3000")) < 0) {
            return "1000-2999";
        }
        if (value.compareTo(new BigDecimal("7000")) < 0) {
            return "3000-6999";
        }
        return "7000+";
    }

    private String bucketProgress(BigDecimal progress) {
        BigDecimal value = progress == null ? BigDecimal.ZERO : progress;
        if (value.compareTo(new BigDecimal("25")) < 0) {
            return "0-24%";
        }
        if (value.compareTo(new BigDecimal("50")) < 0) {
            return "25-49%";
        }
        if (value.compareTo(new BigDecimal("75")) < 0) {
            return "50-74%";
        }
        if (value.compareTo(new BigDecimal("100")) < 0) {
            return "75-99%";
        }
        return "100%";
    }

    private BigDecimal scenarioWeight(int priority) {
        int normalized = Math.max(1, Math.min(5, priority));
        int inverse = 6 - normalized;
        return BigDecimal.valueOf(Math.max(1, inverse));
    }

    private String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return ZERO;
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
