package pi.savings.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.savings.dto.GoalAnalyticsDTO;
import pi.tools.ConfigLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiFinancialInsightService {

    public static final String FALLBACK_MESSAGE = "AI insight unavailable. Local statistics are still available.";
    private static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String model;
    private final String apiKey;
    private final Duration timeout;

    public AiFinancialInsightService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.endpoint = ConfigLoader.get("OPENAI_TEXT_URL", DEFAULT_ENDPOINT);
        this.model = ConfigLoader.get("OPENAI_TEXT_MODEL", DEFAULT_MODEL);
        this.apiKey = ConfigLoader.get("OPENAI_API_KEY", "");
        this.timeout = Duration.ofSeconds(ConfigLoader.getInt("OPENAI_TEXT_TIMEOUT_SECONDS", 30));
    }

    public String generateInsight(GoalAnalyticsDTO analytics) throws AiInsightException {
        if (!isUsableApiKey(apiKey)) {
            throw new AiInsightException("OpenAI API key is missing.");
        }
        if (analytics == null) {
            throw new AiInsightException("Analytics payload is missing.");
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("model", model);
            payload.put("temperature", 0.2);
            payload.put("messages", List.of(
                    Map.of(
                            "role", "system",
                            "content", "You are a financial goals analyst. Return exactly 2 concise sentences with one actionable recommendation and one numeric estimate."
                    ),
                    Map.of(
                            "role", "user",
                            "content", "Analyze this goals analytics JSON and provide a short AI Financial Insight: "
                                    + compactAnalyticsJson(analytics)
                    )
            ));

            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiInsightException("AI API error (" + response.statusCode() + ").");
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            String text = contentNode.isMissingNode() ? "" : contentNode.asText("");
            if (text == null || text.isBlank()) {
                throw new AiInsightException("AI API returned empty insight.");
            }
            return text.trim();
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AiInsightException("AI insight request failed.", exception);
        }
    }

    private String compactAnalyticsJson(GoalAnalyticsDTO analytics) throws IOException {
        Map<String, Object> compact = new LinkedHashMap<>();
        compact.put("selectedAttribute", analytics.selectedAttribute());
        compact.put("totalGoals", analytics.totalGoals());
        compact.put("completedGoals", analytics.completedGoals());
        compact.put("activeGoals", analytics.activeGoals());
        compact.put("overdueGoals", analytics.overdueGoals());
        compact.put("atRiskGoals", analytics.atRiskGoals());
        compact.put("averageProgressPercentage", analytics.averageProgressPercentage());
        compact.put("requiredMonthlyContribution", analytics.requiredMonthlyContribution());
        compact.put("financialHealthScore", analytics.financialHealthScore());
        compact.put("riskDistribution", analytics.riskDistribution());
        compact.put("statusDistribution", analytics.statusDistribution());
        compact.put("attributeStats", analytics.attributeStats());
        return objectMapper.writeValueAsString(compact);
    }

    private boolean isUsableApiKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.trim();
        return !normalized.equalsIgnoreCase("your_openai_api_key_here")
                && !normalized.equalsIgnoreCase("sk-your-real-key-here")
                && !normalized.startsWith("sk-your-");
    }

    public static class AiInsightException extends Exception {
        public AiInsightException(String message) {
            super(message);
        }

        public AiInsightException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
