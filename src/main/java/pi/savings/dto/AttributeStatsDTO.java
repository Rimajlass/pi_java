package pi.savings.dto;

import java.math.BigDecimal;

public record AttributeStatsDTO(
        String attributeValue,
        int goalCount,
        BigDecimal totalTargetAmount,
        BigDecimal totalCurrentAmount,
        BigDecimal totalRemainingAmount,
        BigDecimal averageProgressPercentage,
        BigDecimal averageRequiredMonthlyContribution,
        int lowRiskCount,
        int mediumRiskCount,
        int highRiskCount
) {
}

