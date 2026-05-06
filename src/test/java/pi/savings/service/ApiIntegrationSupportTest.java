package pi.savings.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pi.entities.CalendarEvent;
import pi.savings.repository.SavingAccountRepository;
import pi.tools.ApiClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiIntegrationSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldParseCurrencyApiLatestAndHistoricalRates() {
        FakeApiClient apiClient = new FakeApiClient(Map.of(
                "latest://eur", "{\"date\":\"2026-04-26\",\"eur\":{\"tnd\":3.4112}}",
                "history://2026-04-24/eur", "{\"eur\":{\"tnd\":3.40}}",
                "history://2026-04-25/eur", "{\"eur\":{\"tnd\":3.41}}",
                "history://2026-04-26/eur", "{\"eur\":{\"tnd\":3.42}}"
        ));
        CurrencyRateService service = new CurrencyRateService(apiClient, "latest://%s", "history://%s/%s");

        CurrencyRateService.RateSnapshot snapshot = service.getLatestRateSnapshot("EUR");
        Map<String, Double> history = service.getHistoricalRatesToTnd("EUR", LocalDate.of(2026, 4, 24), LocalDate.of(2026, 4, 26));

        assertEquals(3.4112d, snapshot.rateToTnd());
        assertEquals(LocalDate.of(2026, 4, 26), snapshot.rateDate());
        assertEquals(3, history.size());
        assertEquals(3.42d, history.get("2026-04-26"));
    }

    @Test
    void shouldParseHolidayApiAndDetectNearbyDeadline() {
        FakeApiClient apiClient = new FakeApiClient(Map.of(
                "https://calendar/PublicHolidays/2026/TN",
                "[{\"date\":\"2026-04-09\",\"localName\":\"Aid\",\"name\":\"Aid Holiday\"}]"
        ));
        SavingsCalendarService service = new SavingsCalendarService(
                new HolidayApiService(apiClient, "https://calendar", "TN"),
                new CurrencyApiService(),
                new GoalDeadlineService(),
                new ContributionCalendarService()
        );

        List<CalendarEvent> holidays = service.getPublicHolidayEvents(2026);

        assertEquals(1, holidays.size());
        assertEquals("PUBLIC_HOLIDAY", holidays.get(0).getType());
        assertTrue(service.isDeadlineNearHoliday(LocalDate.of(2026, 4, 10), holidays));
        assertFalse(service.isDeadlineNearHoliday(LocalDate.of(2026, 4, 15), holidays));
    }

    @Test
    void shouldExportSavingsAndGoalsWithoutTechnicalIds() throws Exception {
        CsvExportService csvExportService = new CsvExportService();
        PdfExportService pdfExportService = new PdfExportService();
        CurrencyRateService.RateSnapshot rateSnapshot =
                new CurrencyRateService.RateSnapshot("EUR", 3.40d, LocalDate.of(2026, 4, 26), "test", false, null);

        List<SavingAccountRepository.SavingAccountDetails> accounts = List.of(
                new SavingAccountRepository.SavingAccountDetails(
                        9,
                        3,
                        "Rahma",
                        "rahma@example.com",
                        new BigDecimal("1250.00"),
                        LocalDate.of(2026, 4, 1),
                        new BigDecimal("4.50")
                )
        );
        List<SavingsStatsService.GoalDetails> goals = List.of(
                new SavingsStatsService.GoalDetails(
                        "Trip",
                        "Rahma",
                        "rahma@example.com",
                        new BigDecimal("2200.00"),
                        new BigDecimal("700.00"),
                        LocalDate.of(2026, 5, 10),
                        4
                )
        );
        List<CalendarEvent> holidays = List.of(
                new CalendarEvent(LocalDate.of(2026, 5, 9), "Holiday", "PUBLIC_HOLIDAY", "Holiday", "holiday-blue")
        );

        Path savingsCsv = csvExportService.exportSavingAccounts(accounts, rateSnapshot, tempDir);
        Path goalsCsv = csvExportService.exportGoals(goals, rateSnapshot, holidays, tempDir);
        Path savingsPdf = pdfExportService.exportSavingAccountsPdf(accounts, rateSnapshot, tempDir);
        Path goalsPdf = pdfExportService.exportGoalsPdf(goals, rateSnapshot, holidays, tempDir);

        String savingsCsvText = Files.readString(savingsCsv, StandardCharsets.UTF_8).toLowerCase();
        String goalsCsvText = Files.readString(goalsCsv, StandardCharsets.UTF_8).toLowerCase();
        String savingsPdfText = Files.readString(savingsPdf, StandardCharsets.ISO_8859_1).toLowerCase();
        String goalsPdfText = Files.readString(goalsPdf, StandardCharsets.ISO_8859_1).toLowerCase();

        assertFalse(savingsCsvText.contains("user_id"));
        assertFalse(savingsCsvText.contains("account_id"));
        assertFalse(goalsCsvText.contains("goal_id"));
        assertFalse(goalsCsvText.contains("saving_account_id"));
        assertFalse(savingsPdfText.contains("user_id"));
        assertFalse(goalsPdfText.contains("goal_id"));
        assertTrue(goalsCsvText.contains("holiday_deadline_warning"));
        assertTrue(goalsPdfText.contains("near holiday"));
    }

    private static final class FakeApiClient extends ApiClient {
        private final Map<String, String> responses;
        private final ObjectMapper objectMapper = new ObjectMapper();

        private FakeApiClient(Map<String, String> responses) {
            this.responses = responses;
        }

        @Override
        public JsonNode getJson(String url) throws java.io.IOException {
            String payload = responses.get(url);
            if (payload == null) {
                throw new java.io.IOException("Missing fake payload for " + url);
            }
            return objectMapper.readTree(payload);
        }
    }
}
