package pi.services.InvestissementService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.entities.GlobalMarketSnapshot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class CoinGeckoGlobalService {

    private static final String GLOBAL_URL = "https://api.coingecko.com/api/v3/global";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CoinGeckoGlobalService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public GlobalMarketSnapshot fetchGlobal() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GLOBAL_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("CoinGecko global API returned status "
                    + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode data = root.path("data");

        if (data.isMissingNode() || !data.isObject()) {
            throw new IOException("Unexpected CoinGecko global response: " + response.body());
        }

        double mcapUsd = data.path("total_market_cap").path("usd").asDouble(0);
        double volUsd = data.path("total_volume").path("usd").asDouble(0);
        double btcDom = data.path("market_cap_percentage").path("btc").asDouble(0);
        int active = data.path("active_cryptocurrencies").asInt(0);
        int markets = data.path("markets").asInt(0);

        return new GlobalMarketSnapshot(mcapUsd, volUsd, btcDom, active, markets);
    }
}
