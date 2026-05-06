package pi.savings.service;

import pi.savings.dto.CalendarEventDTO;
import pi.savings.repository.SavingsTransactionRepository;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ContributionCalendarService {

    public List<CalendarEventDTO> toContributionEvents(
            YearMonth month,
            List<SavingsTransactionRepository.TransactionRow> transactions
    ) {
        List<CalendarEventDTO> events = new ArrayList<>();
        for (SavingsTransactionRepository.TransactionRow transaction : transactions) {
            if (transaction == null || transaction.date() == null) {
                continue;
            }

            if (!YearMonth.from(transaction.date().toLocalDate()).equals(month)) {
                continue;
            }

            String type = "GOAL_CONTRIBUTION".equalsIgnoreCase(transaction.type())
                    ? "GOAL_CONTRIBUTION"
                    : "SAVINGS_EVENT";
            String titlePrefix = "GOAL_CONTRIBUTION".equals(type) ? "Contribution" : "Savings";
            events.add(new CalendarEventDTO(
                    transaction.date().toLocalDate(),
                    titlePrefix + ": " + transaction.amount(),
                    type,
                    "MySQL",
                    transaction.amount(),
                    extractGoalName(transaction.description()),
                    transaction.description()
            ));
        }
        return events;
    }

    private String extractGoalName(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        String prefix = "Contribution to goal:";
        if (description.startsWith(prefix)) {
            return description.substring(prefix.length()).trim();
        }
        return null;
    }
}
