package pi.savings.service;

import com.fasterxml.jackson.databind.JsonNode;
import pi.savings.dto.CalendarEventDTO;
import pi.tools.ApiClient;
import pi.tools.ConfigLoader;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HolidayApiService {

    private final ApiClient apiClient;
    private final String baseUrl;
    private final String defaultCountryCode;
    private final Map<String, List<CalendarEventDTO>> cache = new ConcurrentHashMap<>();

    public HolidayApiService() {
        this(
                new ApiClient(),
                ConfigLoader.get("CALENDAR_API_BASE_URL", "https://date.nager.at/api/v3"),
                ConfigLoader.get("CALENDAR_API_COUNTRY", "TN")
        );
    }

    HolidayApiService(ApiClient apiClient, String baseUrl, String defaultCountryCode) {
        this.apiClient = apiClient;
        this.baseUrl = baseUrl;
        this.defaultCountryCode = defaultCountryCode;
    }

    public List<CalendarEventDTO> loadPublicHolidays(int year) {
        return loadPublicHolidays(year, defaultCountryCode);
    }

    public List<CalendarEventDTO> loadPublicHolidays(int year, String countryCode) {
        String normalizedCountry = (countryCode == null || countryCode.isBlank())
                ? defaultCountryCode
                : countryCode.trim().toUpperCase(Locale.ROOT);
        String cacheKey = normalizedCountry + "-" + year;
        List<CalendarEventDTO> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            JsonNode root = apiClient.getJson(baseUrl + "/PublicHolidays/" + year + "/" + normalizedCountry);
            List<CalendarEventDTO> events = new ArrayList<>();
            for (JsonNode item : root) {
                LocalDate date = LocalDate.parse(item.path("date").asText());
                String localName = item.path("localName").asText(item.path("name").asText("Public Holiday"));
                String description = item.path("name").asText(localName);
                events.add(new CalendarEventDTO(
                        date,
                        "Holiday: " + localName,
                        "PUBLIC_HOLIDAY",
                        "Nager.Date API",
                        null,
                        null,
                        description
                ));
            }
            events.sort(Comparator.comparing(CalendarEventDTO::date));
            cache.put(cacheKey, events);
            return events;
        } catch (Exception exception) {
            throw new HolidayApiException("Unable to load public holidays.", exception);
        }
    }

    public void clearCache() {
        cache.clear();
    }

    public static final class HolidayApiException extends RuntimeException {
        public HolidayApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
