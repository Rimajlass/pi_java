package pi.savings.ui;

import pi.entities.CalendarEvent;
import pi.savings.dto.CalendarEventDTO;
import pi.savings.repository.SavingsTransactionRepository;
import pi.savings.service.SavingsCalendarService;
import pi.savings.service.SavingsStatsService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

final class SavingsCalendarController {

    private final SavingsCalendarService calendarService;

    SavingsCalendarController() {
        this(new SavingsCalendarService());
    }

    SavingsCalendarController(SavingsCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    List<CalendarEvent> loadMonthEvents(YearMonth month, List<SavingsStatsService.GoalDetails> goals) {
        return loadMonthEvents(month, goals, List.of());
    }

    List<CalendarEvent> loadMonthEvents(
            YearMonth month,
            List<SavingsStatsService.GoalDetails> goals,
            List<SavingsTransactionRepository.TransactionRow> transactions
    ) {
        return calendarService.buildMonthEvents(month, goals, transactions);
    }

    SavingsCalendarService.CalendarData loadCalendarData(
            YearMonth month,
            List<SavingsStatsService.GoalDetails> goals,
            List<SavingsTransactionRepository.TransactionRow> transactions,
            String baseCurrency,
            String targetCurrency
    ) {
        return calendarService.loadCalendarData(month, goals, transactions, baseCurrency, targetCurrency);
    }

    void refreshApiCaches() {
        calendarService.refreshApiCaches();
    }
}
