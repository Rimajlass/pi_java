package pi.services.RevenueExpenseService;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIExpenseCategorizationService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4.1-mini";
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final List<String> ALLOWED_CATEGORIES = List.of(
            "Alimentation",
            "Transport",
            "Loyer",
            "Sante",
            "Education",
            "Loisirs",
            "Other"
    );
    private static final Pattern MESSAGE_CONTENT_PATTERN = Pattern.compile(
            "\"message\"\\s*:\\s*\\{.*?\"content\"\\s*:\\s*\"((?:\\\\.|[^\\\\\"])*)\"",
            Pattern.DOTALL
    );

    private final OkHttpClient httpClient;
    private final String apiKey;
    private final String model;
    private final Map<String, String> categoryCache = new ConcurrentHashMap<>();

    public AIExpenseCategorizationService() {
        this(new OkHttpClient());
    }

    public AIExpenseCategorizationService(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.apiKey = System.getenv("OPENAI_API_KEY");
        this.model = readModel();
    }

    public String categorizeExpense(String description) {
        String normalizedDescription = normalizeDescription(description);
        if (normalizedDescription.isBlank()) {
            return "Other";
        }

        String heuristicCategory = categorizeHeuristically(normalizedDescription);
        if (!"Other".equals(heuristicCategory)) {
            return heuristicCategory;
        }

        if (apiKey == null || apiKey.isBlank()) {
            return "Other";
        }

        return categoryCache.computeIfAbsent(normalizedDescription.toLowerCase(Locale.ROOT), key -> {
            try {
                return requestCategoryFromOpenAI(normalizedDescription);
            } catch (Exception exception) {
                return "Other";
            }
        });
    }

    private String requestCategoryFromOpenAI(String description) throws IOException {
        String prompt = """
                Classify this expense description into exactly one of these categories:
                Alimentation, Transport, Loyer, Sante, Education, Loisirs, Other.
                Return only the category name.
                Description: %s
                """.formatted(description);

        String requestJson = """
                {
                  "model": "%s",
                  "temperature": 0,
                  "messages": [
                    {
                      "role": "system",
                      "content": "You classify expense descriptions. Return exactly one category from this list: Alimentation, Transport, Loyer, Sante, Education, Loisirs, Other."
                    },
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ]
                }
                """.formatted(escapeJson(model), escapeJson(prompt));

        Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey.trim())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestJson, JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Other";
            }

            String responseBody = response.body() != null ? response.body().string() : "";
            String rawCategory = extractAssistantContent(responseBody);
            return validateCategory(rawCategory);
        }
    }

    private String extractAssistantContent(String responseBody) {
        Matcher matcher = MESSAGE_CONTENT_PATTERN.matcher(responseBody);
        String lastMatch = null;
        while (matcher.find()) {
            lastMatch = matcher.group(1);
        }
        if (lastMatch == null || lastMatch.isBlank()) {
            return "Other";
        }
        return unescapeJson(lastMatch).trim();
    }

    private String validateCategory(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return "Other";
        }

        String firstLine = candidate.lines()
                .findFirst()
                .orElse("")
                .trim();
        String normalizedCandidate = normalizeToken(firstLine);

        for (String allowedCategory : ALLOWED_CATEGORIES) {
            if (normalizeToken(allowedCategory).equals(normalizedCandidate)) {
                return allowedCategory;
            }
        }

        return "Other";
    }

    private String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }

    private String normalizeToken(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9\\s]", " ")
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized;
    }

    private String categorizeHeuristically(String description) {
        String normalized = normalizeToken(description);
        if (normalized.isBlank()) {
            return "Other";
        }

        if (containsAny(normalized, "taxi", "uber", "bolt", "bus", "metro", "tram", "train", "transport",
                "essence", "fuel", "carburant", "parking", "peage", "flight", "airport", "avion")) {
            return "Transport";
        }

        if (containsAny(normalized, "lunch", "dinner", "breakfast", "restaurant", "food", "meal", "snack",
                "groceries", "grocery", "supermarket", "carrefour", "monoprix", "mg", "pizza", "burger",
                "sandwich", "coffee", "cafe", "dejeuner", "repas", "alimentation")) {
            return "Alimentation";
        }

        if (containsAny(normalized, "medicament", "medicaments", "medicine", "pharmacy", "pharmacie", "doctor",
                "dentist", "hospital", "clinique", "consultation", "analyse", "health", "sante", "soin")) {
            return "Sante";
        }

        if (containsAny(normalized, "rent", "loyer", "apartment", "appartement", "lease", "landlord", "syndic")) {
            return "Loyer";
        }

        if (containsAny(normalized, "school", "university", "college", "course", "formation", "education",
                "tuition", "book", "books", "livre", "livres", "certification")) {
            return "Education";
        }

        if (containsAny(normalized, "cinema", "movie", "netflix", "spotify", "gym", "sport", "game", "gaming",
                "concert", "trip", "vacation", "loisir", "loisirs")) {
            return "Loisirs";
        }

        return "Other";
    }

    private boolean containsAny(String normalizedDescription, String... keywords) {
        return Arrays.stream(keywords).anyMatch(keyword -> normalizedDescription.contains(normalizeToken(keyword)));
    }

    private String readModel() {
        String envModel = System.getenv("OPENAI_MODEL");
        return envModel == null || envModel.isBlank() ? DEFAULT_MODEL : envModel.trim();
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (char ch : value.toCharArray()) {
            switch (ch) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.toString();
    }

    private String unescapeJson(String value) {
        return value
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
