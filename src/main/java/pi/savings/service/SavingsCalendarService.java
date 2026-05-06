package pi.savings.service;

import pi.entities.CalendarEvent;
import pi.savings.dto.CalendarEventDTO;
import pi.savings.repository.SavingsTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SavingsCalendarService {

    private final HolidayApiService holidayApiService;
    private final CurrencyApiService currencyApiService;
    private final GoalDeadlineService goalDeadlineService;
    private final ContributionCalendarService contributionCalendarService;

    public SavingsCalendarService() {
        this(
                new HolidayApiService(),
                new CurrencyApiService(),
                new GoalDeadlineService(),
                new ContributionCalendarService()
        );
    }

    SavingsCalendarService(
            HolidayApiService holidayApiService,
            CurrencyApiService currencyApiService,
            GoalDeadlineService goalDeadlineService,
            ContributionCalendarService contributionCalendarService
    ) {
        this.holidayApiService = holidayApiService;
        this.currencyApiService = currencyApiService;
        this.goalDeadlineService = goalDeadlineService;
        this.contributionCalendarService = contributionCalendarService;
    }

    public CalendarData loadCalendarData(
            YearMonth month,
            List<SavingsStatsService.GoalDetails> goals,
            List<SavingsTransactionRepository.TransactionRow> transactions,
            String baseCurrency,
            String targetCurrency
    ) {
        List<CalendarEventDTO> allEvents = new ArrayList<>();
        ApiConnectionStatus holidayStatus;
        ApiConnectionStatus currencyStatus;
        ApiConnectionStatus mysqlStatus;
        Map<LocalDate, Double> dailyRates = new LinkedHashMap<>();

        try {
            List<CalendarEventDTO> holidayEvents = holidayApiService.loadPublicHolidays(month.getYear()).stream()
                    .filter(event -> YearMonth.from(event.date()).equals(month))
                    .toList();
            allEvents.addAll(holidayEvents);
            holidayStatus = ApiConnectionStatus.connected("Nager.Date API");
        } catch (RuntimeException exception) {
            holidayStatus = ApiConnectionStatus.error("Nager.Date API", "Unable to load public holidays.");
        }

        try {
            if (baseCurrency != null
                    && targetCurrency != null
                    && !baseCurrency.equalsIgnoreCase(targetCurrency)) {
                Map<String, Double> rates = currencyApiService.loadMonthRates(month, baseCurrency, targetCurrency);
                for (Map.Entry<String, Double> entry : rates.entrySet()) {
                    LocalDate date = LocalDate.parse(entry.getKey());
                    double rate = entry.getValue();
                    dailyRates.put(date, rate);
                    allEvents.add(new CalendarEventDTO(
                            date,
                            "Rate: 1 " + baseCurrency.toUpperCase(Locale.ROOT) + " = "
                                    + String.format(Locale.US, "%.4f", rate) + " "
                                    + targetCurrency.toUpperCase(Locale.ROOT),
                            "CURRENCY_RATE",
                            "Currency API",
                            BigDecimal.valueOf(rate),
                            null,
                            "Exchange rates from Currency API"
                    ));
                }
            }
            currencyStatus = ApiConnectionStatus.connected("Currency API");
        } catch (RuntimeException exception) {
            currencyStatus = ApiConnectionStatus.error("Currency API", "Currency service unavailable. Please try again later.");
        }

        try {
            List<SavingsStatsService.GoalDetails> safeGoals = goals == null ? List.of() : goals;
            List<SavingsTransactionRepository.TransactionRow> safeTransactions = transactions == null ? List.of() : transactions;
            allEvents.addAll(goalDeadlineService.toDeadlineEvents(month, safeGoals));
            allEvents.addAll(contributionCalendarService.toContributionEvents(month, safeTransactions));
            mysqlStatus = ApiConnectionStatus.connected("MySQL");
        } catch (RuntimeException exception) {
            mysqlStatus = ApiConnectionStatus.error("MySQL", "Unable to load goals and contributions.");
        }

        allEvents.sort(Comparator.comparing(CalendarEventDTO::date).thenComparing(CalendarEventDTO::title));
        return new CalendarData(
                allEvents,
                dailyRates,
                holidayStatus,
                currencyStatus,
                mysqlStatus,
                LocalDateTime.now()
        );
    }

    public void refreshApiCaches() {
        holidayApiService.clearCache();
        currencyApiService.clearCache();
    }

    // Backward compatibility for existing module methods.
    public List<CalendarEvent> getPublicHolidayEvents(int year) {
        return toLegacyEvents(holidayApiService.loadPublicHolidays(year));
    }

    public List<CalendarEvent> getPublicHolidayEvents(int year, String countryCode) {
        return toLegacyEvents(holidayApiService.loadPublicHolidays(year, countryCode));
    }

    public List<CalendarEvent> buildMonthEvents(
            YearMonth month,
            List<SavingsStatsService.GoalDetails> goals,
            List<SavingsTransactionRepository.TransactionRow> transactions
    ) {
        return toLegacyEvents(loadCalendarData(month, goals, transactions, "EUR", "TND").events());
    }

    public boolean isDeadlineNearHoliday(LocalDate deadline, List<CalendarEvent> holidays) {
        if (deadline == null) {
            return false;
        }
        return holidays.stream()
                .map(CalendarEvent::getDate)
                .anyMatch(holidayDate -> Math.abs(ChronoUnit.DAYS.between(deadline, holidayDate)) <= 1);
    }

    public String describeHolidayProximity(LocalDate deadline, List<CalendarEvent> holidays) {
        if (deadline == null) {
            return "No deadline";
        }
        return holidays.stream()
                .map(CalendarEvent::getDate)
                .filter(holidayDate -> Math.abs(ChronoUnit.DAYS.between(deadline, holidayDate)) <= 1)
                .findFirst()
                .map(holidayDate -> "Deadline near holiday on " + holidayDate)
                .orElse("No nearby holiday");
    }

    private List<CalendarEvent> toLegacyEvents(List<CalendarEventDTO> events) {
        List<CalendarEvent> legacy = new ArrayList<>();
        for (CalendarEventDTO event : events) {
            legacy.add(new CalendarEvent(
                    event.date(),
                    event.title(),
                    event.type(),
                    event.description(),
                    colorTagForType(event.type())
            ));
        }
        return legacy;
    }

    private String colorTagForType(String type) {
        return switch (type) {
            case "PUBLIC_HOLIDAY" -> "holiday-blue";
            case "GOAL_DEADLINE" -> "goal-red";
            case "GOAL_CONTRIBUTION", "SAVINGS_EVENT" -> "contribution-green";
            case "CURRENCY_RATE" -> "rate-orange";
            default -> "default-gray";
        };
    }

    public record ApiConnectionStatus(String name, boolean connected, String message) {
        static ApiConnectionStatus connected(String name) {
            return new ApiConnectionStatus(name, true, "Connected");
        }

        static ApiConnectionStatus error(String name, String message) {
            return new ApiConnectionStatus(name, false, message);
        }
    }

    public record CalendarData(
            List<CalendarEventDTO> events,
            Map<LocalDate, Double> dailyRates,
            ApiConnectionStatus holidayStatus,
            ApiConnectionStatus currencyStatus,
            ApiConnectionStatus mysqlStatus,
            LocalDateTime refreshedAt
    ) {
    }

    public static final class CalendarServiceException extends RuntimeException {
        public CalendarServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
