package pi.savings.service;

import pi.savings.dto.CalendarEventDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class GoalDeadlineService {

    public List<CalendarEventDTO> toDeadlineEvents(YearMonth month, List<SavingsStatsService.GoalDetails> goals) {
        List<CalendarEventDTO> events = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (SavingsStatsService.GoalDetails goal : goals) {
            if (goal == null || goal.deadline() == null || !YearMonth.from(goal.deadline()).equals(month)) {
                continue;
            }

            BigDecimal remaining = goal.targetAmount().subtract(goal.currentAmount()).max(BigDecimal.ZERO);
            String status = SavingsStatsService.goalStatus(goal, today);
            BigDecimal progress = SavingsStatsService.progress(goal);
            events.add(new CalendarEventDTO(
                    goal.deadline(),
                    "Goal: " + goal.name(),
                    "GOAL_DEADLINE",
                    "MySQL",
                    null,
                    goal.name(),
                    "Target: " + goal.targetAmount().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " TND"
                            + " | Progress: " + progress.toPlainString() + "%"
                            + " | Remaining: " + remaining.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " TND"
                            + " | Status: " + status
            ));
        }
        return events;
    }
}
