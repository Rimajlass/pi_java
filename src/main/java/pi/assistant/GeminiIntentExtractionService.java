package pi.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.tools.ConfigLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

public class GeminiIntentExtractionService {

    private static final String DEFAULT_MODEL = "gemini-1.5-flash";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiIntentExtractionService() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = ConfigLoader.get("GEMINI_API_KEY", "");
        this.model = ConfigLoader.get("GEMINI_MODEL", DEFAULT_MODEL);
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.contains("your_gemini_api_key_here");
    }

    public AiIntentExtractionService.ExtractionResult extract(String command) throws IOException, InterruptedException {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        String prompt = buildPrompt(command);
        String payload = "{\"contents\":[{\"parts\":[{\"text\":" + quote(prompt) + "}]}]}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Gemini API request failed with status " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
        String jsonBlock = extractJsonBlock(text);
        if (jsonBlock.isBlank()) {
            return AiIntentExtractionService.ExtractionResult.unknown();
        }
        JsonNode parsed = objectMapper.readTree(jsonBlock);
        return toExtractionResult(parsed);
    }

    private AiIntentExtractionService.ExtractionResult toExtractionResult(JsonNode json) {
        String intentRaw = json.path("intent").asText("UNKNOWN").trim().toUpperCase(Locale.ROOT);
        CommandIntent intent;
        try {
            intent = CommandIntent.valueOf(intentRaw);
        } catch (IllegalArgumentException ex) {
            intent = CommandIntent.UNKNOWN;
        }

        String goalName = asNullableText(json, "goalName");
        String amount = asNullableText(json, "amount");
        String targetAmount = asNullableText(json, "targetAmount");
        String deadline = asNullableText(json, "deadline");
        return new AiIntentExtractionService.ExtractionResult(intent, goalName, amount, targetAmount, deadline);
    }

    private String asNullableText(JsonNode json, String field) {
        JsonNode node = json.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String extractJsonBlock(String text) {
        if (text == null) {
            return "";
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return "";
        }
        return text.substring(start, end + 1);
    }

    private String buildPrompt(String command) {
        return """
                You are an intent extractor for a savings and goals assistant.
                Return JSON only with fields:
                intent, amount, currency, goalName, deadline, targetAmount.
                Allowed intents:
                HELP, SHOW_BALANCE, LIST_GOALS, CREATE_GOAL, DELETE_GOAL, CONTRIBUTE_TO_GOAL, UPDATE_GOAL,
                SHOW_OVERDUE_GOALS, SHOW_COMPLETED_GOALS, SHOW_AT_RISK_GOALS, UNKNOWN.
                If a field is unknown use null.
                User command:
                """ + command;
    }

    private String quote(String value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            return "\"\"";
        }
    }
}
