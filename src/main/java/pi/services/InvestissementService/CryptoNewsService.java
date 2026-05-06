package pi.services.InvestissementService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.entities.Crypto;
import pi.entities.CryptoNewsArticle;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CryptoNewsService {

    private static final String GNEWS_URL = "https://gnews.io/api/v4/search";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CryptoNewsService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public List<CryptoNewsArticle> getNewsForCrypto(Crypto crypto) throws Exception {
        String apiKey = readEnvValue("GNEWS_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing GNEWS_API_KEY in .env.local");
        }

        String query = crypto.getName() + " cryptocurrency";
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

        String url = GNEWS_URL
                + "?q=" + encodedQuery
                + "&lang=en"
                + "&max=6"
                + "&sortby=publishedAt"
                + "&apikey=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException(
                    "GNews API error " + response.statusCode() + ": " + response.body()
            );
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode articlesNode = root.path("articles");

        if (!articlesNode.isArray()) {
            throw new RuntimeException("Invalid GNews response: articles array not found.");
        }

        List<CryptoNewsArticle> articles = new ArrayList<>();

        for (JsonNode articleNode : articlesNode) {
            String title = articleNode.path("title").asText("");
            String description = articleNode.path("description").asText("");
            String articleUrl = articleNode.path("url").asText("");
            String imageUrl = articleNode.path("image").asText("");
            String publishedAt = articleNode.path("publishedAt").asText("");
            String sourceName = articleNode.path("source").path("name").asText("Unknown source");

            if (title.isBlank()) {
                continue;
            }

            articles.add(new CryptoNewsArticle(
                    title,
                    description,
                    articleUrl,
                    imageUrl,
                    publishedAt,
                    sourceName
            ));
        }

        return articles;
    }

    private String readEnvValue(String key) throws IOException {
        String fromSystemEnv = System.getenv(key);

        if (fromSystemEnv != null && !fromSystemEnv.isBlank()) {
            return fromSystemEnv;
        }

        Path envPath = Path.of(".env.local");

        if (!Files.exists(envPath)) {
            return null;
        }

        List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            int equalsIndex = trimmed.indexOf("=");

            if (equalsIndex <= 0) {
                continue;
            }

            String envKey = trimmed.substring(0, equalsIndex).trim();
            String envValue = trimmed.substring(equalsIndex + 1).trim();

            if (envKey.equals(key)) {
                return removeQuotes(envValue);
            }
        }

        return null;
    }

    private String removeQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }

        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }
}