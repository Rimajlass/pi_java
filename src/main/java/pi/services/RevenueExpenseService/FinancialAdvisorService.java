package pi.services.RevenueExpenseService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class FinancialAdvisorService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4.1-mini";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final Path DOTENV_PATH = Path.of(".env");
    private static final Map<String, String> DOTENV_VALUES = loadDotenv();
    private static final String FALLBACK_MESSAGE = """
            Le conseil IA est indisponible pour le moment.

            Vérifiez la variable d'environnement OPENAI_API_KEY puis réessayez.
            """;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public record AdviceResult(boolean success, String message) {
    }

    public FinancialAdvisorService() {
        this(new OkHttpClient(), new ObjectMapper());
    }

    public FinancialAdvisorService(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    public AdviceResult generateAdvice(double totalRevenue, double totalExpenses, double netBalance, String dominantExpenseCategory) {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return new AdviceResult(true, buildLocalAdvice(totalRevenue, totalExpenses, netBalance, dominantExpenseCategory));
        }

        String prompt = buildPrompt(totalRevenue, totalExpenses, netBalance, dominantExpenseCategory);
        try {
            return new AdviceResult(true, requestAdvice(apiKey.trim(), prompt));
        } catch (Exception exception) {
            return new AdviceResult(true, buildLocalAdvice(totalRevenue, totalExpenses, netBalance, dominantExpenseCategory));
        }
    }

    public String buildPrompt(double totalRevenue, double totalExpenses, double netBalance, String dominantExpenseCategory) {
        return """
                Tu es un conseiller financier personnel.
                Réponds uniquement en français, de manière concise et pratique.

                Données financières:
                - Revenus totaux: %s
                - Dépenses totales: %s
                - Solde net: %s
                - Catégorie de dépense dominante: %s

                Donne exactement:
                1. Un court résumé financier (2 phrases maximum)
                2. Un avertissement si les dépenses sont élevées ou si le solde est faible/négatif
                3. Trois conseils pratiques numérotés

                Le ton doit être clair, utile et concret.
                """.formatted(
                formatMoney(totalRevenue),
                formatMoney(totalExpenses),
                formatMoney(netBalance),
                dominantExpenseCategory == null || dominantExpenseCategory.isBlank() ? "None" : dominantExpenseCategory
        );
    }

    private String requestAdvice(String apiKey, String prompt) throws IOException {
        String payload = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("model", MODEL)
                        .put("temperature", 0.7)
                        .put("max_tokens", 350)
                        .set("messages", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode()
                                        .put("role", "system")
                                        .put("content", "Tu es un conseiller financier prudent et pédagogique."))
                                .add(objectMapper.createObjectNode()
                                        .put("role", "user")
                                        .put("content", prompt)))
        );

        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(payload, JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI API error: HTTP " + response.code() + " - " + responseBody);
            }
            return extractAdvice(responseBody);
        }
    }

    private String extractAdvice(String responseBody) throws IOException {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (!contentNode.isTextual() || contentNode.asText().isBlank()) {
            throw new IOException("Empty advice received from OpenAI.");
        }
        return contentNode.asText().trim();
    }

    private String formatMoney(double amount) {
        return String.format(Locale.US, "%.2f TND", amount);
    }

    private String mapApiFailureMessage(String rawMessage) {
        String normalized = rawMessage == null ? "" : rawMessage.toLowerCase(Locale.ROOT);

        if (normalized.contains("insufficient_quota")) {
            return """
                    Le conseil IA est indisponible car le quota OpenAI est épuisé.

                    Action requise :
                    1. Vérifiez votre facturation OpenAI
                    2. Rechargez ou activez le plan API
                    3. Réessayez ensuite depuis le bouton AI Advice
                    """;
        }

        if (normalized.contains("429")) {
            return """
                    Le service OpenAI est temporairement limité.

                    Réessayez dans quelques instants.
                    """;
        }

        if (normalized.contains("401") || normalized.contains("invalid_api_key")) {
            return """
                    La clé OpenAI est invalide ou non autorisée.

                    Vérifiez la valeur de OPENAI_API_KEY puis relancez l'application.
                    """;
        }

        return """
                Le conseil IA est temporairement indisponible.

                Détail technique :
                %s
                """.formatted(rawMessage == null || rawMessage.isBlank() ? "Unknown error." : rawMessage);
    }

    private String buildLocalAdvice(double totalRevenue, double totalExpenses, double netBalance, String dominantExpenseCategory) {
        double expenseRatio = totalRevenue <= 0 ? 1.0 : totalExpenses / totalRevenue;
        String category = dominantExpenseCategory == null || dominantExpenseCategory.isBlank() ? "Other" : dominantExpenseCategory;

        List<String> summaries = new ArrayList<>();
        if (netBalance < 0) {
            summaries.add("Summary: spending is above income.");
            summaries.add("Summary: your balance is negative now.");
        } else if (expenseRatio >= 0.85) {
            summaries.add("Summary: your balance is positive, but tight.");
            summaries.add("Summary: spending is high vs income.");
        } else {
            summaries.add("Summary: your finances look stable.");
            summaries.add("Summary: you still have a good margin.");
        }

        List<String> warnings = new ArrayList<>();
        if (netBalance < 0) {
            warnings.add("Warning: your budget is already exceeded.");
            warnings.add("Warning: the current pace is not sustainable.");
        } else if (expenseRatio >= 0.85) {
            warnings.add("Warning: one extra cost could hurt your balance.");
            warnings.add("Warning: reduce non-essential spending.");
        } else {
            warnings.add("Warning: keep watching your top expense category.");
            warnings.add("Warning: things are fine, stay consistent.");
        }

        List<String> practicalAdvice = new ArrayList<>();
        practicalAdvice.addAll(buildCategoryAdvice(category));
        if (netBalance < 0) {
            practicalAdvice.add("Set a weekly spending cap.");
            practicalAdvice.add("Delay non-essential purchases.");
            practicalAdvice.add("Cover fixed costs first.");
        } else if (expenseRatio >= 0.85) {
            practicalAdvice.add("Keep a small reserve.");
            practicalAdvice.add("Cut small daily expenses.");
            practicalAdvice.add("Compare this week with last week.");
        } else {
            practicalAdvice.add("Keep the current pace.");
            practicalAdvice.add("Watch your highest-cost category.");
            practicalAdvice.add("Review your numbers weekly.");
        }

        List<String> selectedAdvice = pickDistinctAdvice(practicalAdvice, 3);
        return """
                %s

                %s

                1. %s
                2. %s
                3. %s
                """.formatted(
                pickOne(summaries),
                pickOne(warnings),
                selectedAdvice.get(0),
                selectedAdvice.get(1),
                selectedAdvice.get(2)
        );
    }

    private List<String> buildCategoryAdvice(String dominantExpenseCategory) {
        String normalized = dominantExpenseCategory.toLowerCase(Locale.ROOT);
        List<String> advice = new ArrayList<>();

        if (normalized.contains("transport")) {
            advice.add("Group trips to reduce cost.");
            advice.add("Review routes for cheaper options.");
        } else if (normalized.contains("food") || normalized.contains("alimentation")) {
            advice.add("Prepare meals in advance.");
            advice.add("Set a weekly food budget.");
        } else if (normalized.contains("rent") || normalized.contains("loyer")) {
            advice.add("Prioritize essentials while rent stays high.");
            advice.add("Reserve rent money early.");
        } else if (normalized.contains("health") || normalized.contains("sante")) {
            advice.add("Track health costs in one place.");
            advice.add("Keep a small care budget.");
        } else if (normalized.contains("education")) {
            advice.add("Plan study costs ahead.");
            advice.add("Separate essential and optional learning costs.");
        } else if (normalized.contains("leisure") || normalized.contains("loisirs")) {
            advice.add("Set a weekly leisure limit.");
            advice.add("Pick one priority activity.");
        } else {
            advice.add("Find your top recurring expenses.");
            advice.add("Reduce non-essential costs first.");
        }

        return advice;
    }

    private List<String> pickDistinctAdvice(List<String> source, int count) {
        List<String> pool = new ArrayList<>(source);
        List<String> result = new ArrayList<>();

        while (!pool.isEmpty() && result.size() < count) {
            int index = ThreadLocalRandom.current().nextInt(pool.size());
            result.add(pool.remove(index));
        }

        while (result.size() < count) {
            result.add("Review your budget every week.");
        }

        return result;
    }

    private String pickOne(List<String> values) {
        return values.get(ThreadLocalRandom.current().nextInt(values.size()));
    }

    private String resolveApiKey() {
        String envValue = System.getenv("OPENAI_API_KEY");
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }
        String dotenvValue = DOTENV_VALUES.get("OPENAI_API_KEY");
        return dotenvValue == null || dotenvValue.isBlank() ? null : dotenvValue.trim();
    }

    private static Map<String, String> loadDotenv() {
        Map<String, String> values = new HashMap<>();
        if (!Files.exists(DOTENV_PATH)) {
            return values;
        }

        try {
            List<String> lines = Files.readAllLines(DOTENV_PATH);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                int separatorIndex = trimmed.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }

                String key = trimmed.substring(0, separatorIndex).trim();
                String value = trimmed.substring(separatorIndex + 1).trim();
                values.put(key, stripOptionalQuotes(value));
            }
        } catch (IOException ignored) {
            return values;
        }

        return values;
    }

    private static String stripOptionalQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
