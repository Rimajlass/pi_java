package pi.savings.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WhatIfScenarioDTO(
        String scenarioName,
        BigDecimal monthlyContribution,
        List<GoalProjectionDTO> projections
) {
    public record GoalProjectionDTO(
            String goalName,
            BigDecimal monthlyAllocation,
            LocalDate predictedCompletionDate,
            boolean completesBeforeDeadline,
            int daysDifferenceFromDeadline,
            String statusMessage
    ) {
    }
}

