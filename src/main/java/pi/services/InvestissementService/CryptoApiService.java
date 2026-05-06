package pi.services.InvestissementService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.entities.Crypto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class CryptoApiService {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public List<Crypto> getCryptos() throws IOException, InterruptedException {
        String url = "https://api.coingecko.com/api/v3/coins/markets"
                + "?vs_currency=usd"
                + "&ids=bitcoin,ethereum,binancecoin,solana,ripple,cardano,dogecoin,tron";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("CoinGecko API returned status "
                    + response.statusCode() + ": " + response.body());
        }

        JsonNode array = mapper.readTree(response.body());

        if (!array.isArray()) {
            throw new IOException("Unexpected JSON response: " + response.body());
        }

        List<Crypto> list = new ArrayList<>();

        for (JsonNode node : array) {
            String name = node.path("name").asText("");
            String symbol = node.path("symbol").asText("");
            String id = node.path("id").asText("");
            double currentPrice = node.path("current_price").asDouble(0.0);
            String imageUrl = node.path("image").asText();
            if (!id.isEmpty()) {
                Crypto c = new Crypto(name, symbol, id, currentPrice,imageUrl);
                list.add(c);
            }
        }

        return list;
    }
}