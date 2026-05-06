package pi.services.InvestissementService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.entities.FiatRatesSnapshot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class FrankfurterService {

    private static final String LATEST_USD = "https://api.frankfurter.app/latest?from=USD&to=EUR,GBP";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FrankfurterService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public FiatRatesSnapshot fetchUsdRates() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(LATEST_USD))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Frankfurter API returned status "
                    + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode rates = root.path("rates");

        if (!rates.isObject()) {
            throw new IOException("Unexpected Frankfurter response: " + response.body());
        }

        double eur = rates.path("EUR").asDouble(0);
        double gbp = rates.path("GBP").asDouble(0);
        String date = root.path("date").asText("");

        if (eur <= 0 || gbp <= 0) {
            throw new IOException("Missing EUR/GBP rates in response: " + response.body());
        }

        return new FiatRatesSnapshot(eur, gbp, date);
    }
}
