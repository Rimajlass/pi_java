package pi.services.InvestissementService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.entities.CryptoPricePoint;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class CryptoChartService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CryptoChartService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public List<CryptoPricePoint> getMarketChart(String apiId, int days, String currency) throws Exception {
        if (apiId == null || apiId.isBlank()) {
            throw new IllegalArgumentException("Crypto API id is missing.");
        }

        if (currency == null || currency.isBlank()) {
            currency = "usd";
        }

        String safeApiId = URLEncoder.encode(apiId, StandardCharsets.UTF_8);
        String safeCurrency = URLEncoder.encode(currency.toLowerCase(), StandardCharsets.UTF_8);

        String url = "https://api.coingecko.com/api/v3/coins/"
                + safeApiId
                + "/market_chart?vs_currency="
                + safeCurrency
                + "&days="
                + days;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("CoinGecko chart API error "
                    + response.statusCode()
                    + ": "
                    + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode prices = root.path("prices");

        if (!prices.isArray()) {
            throw new RuntimeException("Invalid CoinGecko response: prices array not found.");
        }

        List<CryptoPricePoint> points = new ArrayList<>();

        for (JsonNode item : prices) {
            if (item.isArray() && item.size() >= 2) {
                long timestampMillis = item.get(0).asLong();
                double price = item.get(1).asDouble();

                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(timestampMillis),
                        ZoneId.systemDefault()
                );

                points.add(new CryptoPricePoint(dateTime, price));
            }
        }

        return points;
    }
}