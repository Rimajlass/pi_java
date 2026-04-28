package pi.savings.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRiskDTO(
        String goalName,
        int priority,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal remainingAmount,
        BigDecimal progressPercentage,
        LocalDate deadline,
        int daysLeft,
        BigDecimal requiredMonthlyContribution,
        LocalDate predictedCompletionDate,
        String riskLevel,
        String status,
        BigDecimal totalContributionAmount,
        BigDecimal averageMonthlyContribution,
        LocalDate lastContributionDate,
        String contributionFrequency
) {
}

