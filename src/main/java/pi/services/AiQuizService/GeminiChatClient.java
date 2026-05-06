package pi.services.AiQuizService;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Minimal REST client for Google Gemini API (AI Studio API key).
 * Endpoint: POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent
 * Auth: x-goog-api-key header.
 */
public final class GeminiChatClient {

    private final HttpClient httpClient;
    private final String apiKey;

    public GeminiChatClient(String apiKey) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.apiKey = apiKey;
    }

    public String generateContentJson(String model, String requestJson) throws IOException, InterruptedException {
        String pickedModel = model == null || model.isBlank() ? "gemini-2.5-flash" : model.trim();
        String encodedModel = URLEncoder.encode(pickedModel, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/" + encodedModel + ":generateContent"))
                .timeout(Duration.ofSeconds(60))
                .header("x-goog-api-key", apiKey)
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return resp.body();
        }
        throw new IOException("Gemini API error HTTP " + resp.statusCode() + ": " + resp.body());
    }
}

