package pi.savings.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record GoalAnalyticsDTO(
        String selectedAttribute,
        int totalGoals,
        int completedGoals,
        int activeGoals,
        int overdueGoals,
        int atRiskGoals,
        BigDecimal totalTargetAmount,
        BigDecimal totalCurrentAmount,
        BigDecimal totalRemainingAmount,
        BigDecimal averageProgressPercentage,
        BigDecimal requiredMonthlyContribution,
        int financialHealthScore,
        String financialHealthStatus,
        LocalDate nearestDeadline,
        Map<String, Integer> riskDistribution,
        Map<String, Integer> statusDistribution,
        Map<String, AttributeStatsDTO> attributeStats,
        List<GoalRiskDTO> goalRisks,
        boolean hasContributionData
) {
}

