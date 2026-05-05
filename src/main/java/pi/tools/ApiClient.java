package pi.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    public ApiClient() {
        this(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(ConfigLoader.getInt("API_CONNECT_TIMEOUT_SECONDS", 8)))
                        .build(),
                new ObjectMapper(),
                Duration.ofSeconds(ConfigLoader.getInt("API_READ_TIMEOUT_SECONDS", 12))
        );
    }

    ApiClient(HttpClient httpClient, ObjectMapper objectMapper, Duration timeout) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.timeout = timeout;
    }

    public JsonNode getJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(timeout)
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " for " + url);
        }
        return objectMapper.readTree(response.body());
    }
}
