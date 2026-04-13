package pi.services.InvestissementService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.entities.Crypto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class CryptoApiService {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public List<Crypto> getCryptos() throws Exception {

        String url = "https://api.coingecko.com/api/v3/coins/markets"
                + "?vs_currency=usd"
                + "&ids=bitcoin,ethereum,binancecoin,solana,ripple,cardano,dogecoin,tron";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode array = mapper.readTree(response.body());

        List<Crypto> list = new ArrayList<>();

        for (JsonNode node : array) {
            Crypto c = new Crypto(
                    node.get("name").asText(),
                    node.get("symbol").asText(),
                    node.get("id").asText(),
                    node.get("current_price").asDouble()
            );
            list.add(c);
        }

        return list;
    }
}