package pi.services.CurrencyService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CurrencyConverterService {

    private static final String API_KEY_ENV = "EXCHANGE_RATE_API_KEY";
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6";

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CurrencyConverterService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public double convert(double amount, String fromCurrency, String toCurrency) {
        validateAmount(amount);
        String from = normalizeCurrency(fromCurrency);
        String to = normalizeCurrency(toCurrency);

        if (from.equals(to)) {
            return amount;
        }

        String apiKey = resolveApiKey();
        String url = buildUrl(apiKey, from, to, amount);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody body = response.body();
            String responseBody = body == null ? "" : body.string();
            if (!response.isSuccessful()) {
                throw new CurrencyConversionException("Exchange rate API failed with HTTP " + response.code() + ".");
            }
            return parseConvertedAmount(responseBody);
        } catch (IOException exception) {
            throw new CurrencyConversionException("Network error while calling exchange rate API.", exception);
        }
    }

    private double parseConvertedAmount(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        String result = root.path("result").asText("");
        if (!"success".equalsIgnoreCase(result)) {
            String errorType = root.path("error-type").asText("unknown error");
            throw new CurrencyConversionException("Exchange rate API error: " + errorType + ".");
        }

        JsonNode conversionResult = root.get("conversion_result");
        if (conversionResult == null || !conversionResult.isNumber()) {
            throw new CurrencyConversionException("Exchange rate API response did not include a converted amount.");
        }
        return conversionResult.asDouble();
    }

    private String buildUrl(String apiKey, String fromCurrency, String toCurrency, double amount) {
        return BASE_URL + "/"
                + urlEncode(apiKey) + "/pair/"
                + urlEncode(fromCurrency) + "/"
                + urlEncode(toCurrency) + "/"
                + urlEncode(BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString());
    }

    private String resolveApiKey() {
        String apiKey = normalizeText(System.getenv(API_KEY_ENV));
        if (apiKey.isBlank()) {
            apiKey = readApiKeyFromDotEnv();
        }
        if (apiKey.isBlank()) {
            throw new CurrencyConversionException("Missing EXCHANGE_RATE_API_KEY. Set it in the environment or .env file.");
        }
        return apiKey;
    }

    private String readApiKeyFromDotEnv() {
        Path envFile = Path.of(".env");
        if (!Files.isRegularFile(envFile)) {
            return "";
        }

        try {
            List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.startsWith(API_KEY_ENV + "=")) {
                    continue;
                }
                return normalizeText(trimmed.substring((API_KEY_ENV + "=").length()));
            }
        } catch (IOException ignored) {
            return "";
        }
        return "";
    }

    private void validateAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Conversion amount must be zero or greater.");
        }
    }

    private String normalizeCurrency(String value) {
        String currency = normalizeText(value).toUpperCase(Locale.ROOT);
        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a valid 3-letter code.");
        }
        return currency;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("\"", "").replace("'", "");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static class CurrencyConversionException extends RuntimeException {
        public CurrencyConversionException(String message) {
            super(message);
        }

        public CurrencyConversionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
