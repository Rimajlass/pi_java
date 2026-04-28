package pi.savings.service;

import com.fasterxml.jackson.databind.JsonNode;
import pi.tools.ApiClient;
import pi.tools.ConfigLoader;

import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CurrencyRateService {

    public static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "TND", "EUR", "USD", "AED", "GBP", "MAD", "CAD", "CHF", "SAR", "TRY"
    );

    private final ApiClient apiClient;
    private final String latestUrlTemplate;
    private final String historicalUrlTemplate;
    private final Map<String, RateSnapshot> latestCache = new ConcurrentHashMap<>();
    private final Map<String, RateSnapshot> historicalCache = new ConcurrentHashMap<>();

    public CurrencyRateService() {
        this(
                new ApiClient(),
                ConfigLoader.get("CURRENCY_API_LATEST_URL_TEMPLATE",
                        "https://latest.currency-api.pages.dev/v1/currencies/%s.json"),
                ConfigLoader.get("CURRENCY_API_HISTORICAL_URL_TEMPLATE",
                        "https://%s.currency-api.pages.dev/v1/currencies/%s.json")
        );
    }

    CurrencyRateService(ApiClient apiClient, String latestUrlTemplate, String historicalUrlTemplate) {
        this.apiClient = apiClient;
        this.latestUrlTemplate = latestUrlTemplate;
        this.historicalUrlTemplate = historicalUrlTemplate;
    }

    public RateSnapshot getLatestRateSnapshot(String currency) {
        String normalizedCurrency = normalizeCurrency(currency);
        if ("TND".equals(normalizedCurrency)) {
            return new RateSnapshot("TND", 1d, LocalDate.now(), "Identity", false, "No conversion needed.");
        }

        try {
            String url = String.format(Locale.ROOT, latestUrlTemplate, normalizedCurrency.toLowerCase(Locale.ROOT));
            JsonNode jsonNode = apiClient.getJson(url);
            double rate = readRate(jsonNode, normalizedCurrency, "tnd");
            LocalDate rateDate = jsonNode.hasNonNull("date")
                    ? LocalDate.parse(jsonNode.get("date").asText())
                    : LocalDate.now();

            RateSnapshot snapshot = new RateSnapshot(normalizedCurrency, rate, rateDate, "currency-api.pages.dev", false, null);
            latestCache.put(normalizedCurrency, snapshot);
            return snapshot;
        } catch (Exception exception) {
            RateSnapshot cached = latestCache.get(normalizedCurrency);
            if (cached != null) {
                return cached.asFallback("Latest live rate unavailable. Cached rate reused.");
            }
            throw new CurrencyServiceException("Impossible de recuperer le taux " + normalizedCurrency + " vers TND.", exception);
        }
    }

    public double getRateToTnd(String currency) {
        return getLatestRateSnapshot(currency).rateToTnd();
    }

    public double convertToTnd(double amount, String currency) {
        return amount * getRateToTnd(currency);
    }

    public Map<String, Double> getHistoricalRatesToTnd(String currency, LocalDate start, LocalDate end) {
        String normalizedCurrency = normalizeCurrency(currency);
        LocalDate safeStart = start == null ? LocalDate.now().minusDays(6) : start;
        LocalDate safeEnd = end == null ? LocalDate.now() : end;
        if (safeEnd.isBefore(safeStart)) {
            throw new CurrencyServiceException("Intervalle de dates invalide pour l'historique des taux.", null);
        }

        Map<String, Double> rates = new LinkedHashMap<>();
        for (LocalDate cursor = safeStart; !cursor.isAfter(safeEnd); cursor = cursor.plusDays(1)) {
            RateSnapshot snapshot = getHistoricalRateSnapshot(normalizedCurrency, cursor);
            rates.put(cursor.toString(), snapshot.rateToTnd());
        }
        return rates;
    }

    public Map<String, Double> getHistoricalRatesBetweenCurrencies(
            String baseCurrency,
            String targetCurrency,
            LocalDate start,
            LocalDate end
    ) {
        String normalizedBase = normalizeCurrency(baseCurrency);
        String normalizedTarget = normalizeCurrency(targetCurrency);
        LocalDate safeStart = start == null ? LocalDate.now().minusDays(6) : start;
        LocalDate safeEnd = end == null ? LocalDate.now() : end;
        if (safeEnd.isBefore(safeStart)) {
            throw new CurrencyServiceException("Intervalle de dates invalide pour la comparaison des devises.", null);
        }

        Map<String, Double> rates = new LinkedHashMap<>();
        for (LocalDate cursor = safeStart; !cursor.isAfter(safeEnd); cursor = cursor.plusDays(1)) {
            double baseToTnd = "TND".equals(normalizedBase)
                    ? 1d
                    : getHistoricalRateSnapshot(normalizedBase, cursor).rateToTnd();
            double targetToTnd = "TND".equals(normalizedTarget)
                    ? 1d
                    : getHistoricalRateSnapshot(normalizedTarget, cursor).rateToTnd();

            if (targetToTnd <= 0d) {
                throw new CurrencyServiceException("Taux cible invalide pour " + normalizedTarget + " le " + cursor + ".", null);
            }

            rates.put(cursor.toString(), baseToTnd / targetToTnd);
        }
        return rates;
    }

    RateSnapshot getHistoricalRateSnapshot(String currency, LocalDate date) {
        String normalizedCurrency = normalizeCurrency(currency);
        if ("TND".equals(normalizedCurrency)) {
            return new RateSnapshot("TND", 1d, date, "Identity", false, "No conversion needed.");
        }

        String cacheKey = normalizedCurrency + "@" + date;
        RateSnapshot cached = historicalCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            String url = String.format(
                    Locale.ROOT,
                    historicalUrlTemplate,
                    date,
                    normalizedCurrency.toLowerCase(Locale.ROOT)
            );
            JsonNode jsonNode = apiClient.getJson(url);
            double rate = readRate(jsonNode, normalizedCurrency, "tnd");
            RateSnapshot snapshot = new RateSnapshot(normalizedCurrency, rate, date, "currency-api.pages.dev", false, null);
            historicalCache.put(cacheKey, snapshot);
            latestCache.put(normalizedCurrency, snapshot);
            return snapshot;
        } catch (Exception exception) {
            RateSnapshot latest = latestCache.get(normalizedCurrency);
            if (latest != null) {
                RateSnapshot fallback = new RateSnapshot(
                        normalizedCurrency,
                        latest.rateToTnd(),
                        date,
                        latest.provider(),
                        true,
                        "Historical live rate unavailable. Cached latest rate reused."
                );
                historicalCache.put(cacheKey, fallback);
                return fallback;
            }
            throw new CurrencyServiceException("Impossible de recuperer l'historique du taux " + normalizedCurrency + " vers TND.", exception);
        }
    }

    double readRate(JsonNode root, String currencyKey, String targetKey) throws IOException {
        JsonNode currencyNode = root.path(currencyKey.toLowerCase(Locale.ROOT));
        JsonNode targetNode = currencyNode.path(targetKey.toLowerCase(Locale.ROOT));
        if (!targetNode.isNumber()) {
            throw new IOException("Rate " + currencyKey + " -> " + targetKey + " missing from response.");
        }
        return targetNode.asDouble();
    }

    private String normalizeCurrency(String currency) {
        String normalized = currency == null ? "TND" : currency.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CURRENCIES.contains(normalized)) {
            throw new CurrencyServiceException("Devise non supportee: " + currency, null);
        }
        return normalized;
    }

    public record RateSnapshot(
            String currency,
            double rateToTnd,
            LocalDate rateDate,
            String provider,
            boolean fallback,
            String message
    ) {
        RateSnapshot asFallback(String fallbackMessage) {
            return new RateSnapshot(currency, rateToTnd, rateDate, provider, true, fallbackMessage);
        }
    }

    public void clearCache() {
        latestCache.clear();
        historicalCache.clear();
    }

    public static final class CurrencyServiceException extends RuntimeException {
        public CurrencyServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
