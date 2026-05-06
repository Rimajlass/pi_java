package pi.services.InvestissementService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.entities.FearGreedSnapshot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class FearGreedService {

    private static final String FNG_URL = "https://api.alternative.me/fng/?limit=1";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FearGreedService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public FearGreedSnapshot fetchLatest() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FNG_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Fear & Greed API returned status "
                    + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode data = root.path("data");

        if (!data.isArray() || data.isEmpty()) {
            throw new IOException("Unexpected Fear & Greed response: " + response.body());
        }

        JsonNode row = data.get(0);
        int value = row.path("value").asInt(-1);
        String classification = row.path("value_classification").asText("").trim();
        long ts = row.path("timestamp").asLong(0L);

        if (value < 0 || value > 100) {
            throw new IOException("Invalid index value in response: " + response.body());
        }

        return new FearGreedSnapshot(value, classification, ts);
    }
}
