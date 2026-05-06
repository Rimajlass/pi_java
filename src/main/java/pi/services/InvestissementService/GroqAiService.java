package pi.services.InvestissementService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.entities.Crypto;
import pi.entities.Investissement;
import pi.entities.Objectif;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroqAiService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GroqAiService() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public String analyzeObjective(Objectif objectif, List<Investissement> investments) throws Exception {
        String apiKey = readEnvValue("GROQ_API_KEY");
        String model = readEnvValue("GROQ_MODEL");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing GROQ_API_KEY in .env.local");
        }

        if (model == null || model.isBlank()) {
            model = "llama-3.3-70b-versatile";
        }

        String prompt = buildObjectivePrompt(objectif, investments);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", 0.4);
        requestBody.put("max_tokens", 900);
        requestBody.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content", """
                                You are a financial analysis assistant inside a student personal finance JavaFX app.
                                Analyze crypto-linked investment objectives clearly and cautiously.
                                Do not give guaranteed financial advice.
                                Explain risk in simple English.
                                """
                ),
                Map.of(
                        "role", "user",
                        "content", prompt
                )
        ));

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Groq API error " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.path("choices").get(0).path("message").path("content");

        if (contentNode == null || contentNode.isMissingNode()) {
            throw new RuntimeException("Invalid Groq response: " + response.body());
        }

        return contentNode.asText();
    }

    /**
     * Generic chat completion (same auth/env as {@link #analyzeObjective}).
     */
    public String completeChat(String systemPrompt, String userPrompt, double temperature, int maxTokens) throws Exception {
        String apiKey = readEnvValue("GROQ_API_KEY");
        String model = readEnvValue("GROQ_MODEL");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing GROQ_API_KEY in .env.local");
        }

        if (model == null || model.isBlank()) {
            model = "llama-3.3-70b-versatile";
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Groq API error " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.path("choices").get(0).path("message").path("content");

        if (contentNode == null || contentNode.isMissingNode()) {
            throw new RuntimeException("Invalid Groq response: " + response.body());
        }

        return contentNode.asText();
    }

    /**
     * Analyse pédagogique d'un actif (prix affiché dans l'app), en français.
     *
     * @param userInvestedTotalUsd somme des montants investis sur cet actif dans le portefeuille (0 si aucune ligne).
     */
    public String analyzeCryptoSpot(Crypto crypto, double userInvestedTotalUsd) throws Exception {
        String system = """
                Tu es un analyste crypto pour une application JavaFX à vocation pédagogique (projet étudiant).
                Réponds en français. Pas de promesse de rendement. Pas de conseil d'achat ou de vente catégorique.
                Style clair, structuré, quelques emojis pertinents. Reste factuel sur les risques du marché crypto.
                """;

        StringBuilder user = new StringBuilder();
        user.append("Analyse l'actif suivant à partir des données ci-dessous (prix indicatif depuis l'app, pas un cours officiel).\n\n");
        user.append("- Nom : ").append(crypto.getName()).append("\n");
        user.append("- Symbole : ").append(crypto.getSymbolUpper()).append("\n");
        user.append("- Identifiant CoinGecko : ").append(crypto.getApiid()).append("\n");
        user.append("- Prix spot affiché dans l'application (USD) : ").append(formatMoney(crypto.getCurrentprice())).append("\n");
        if (userInvestedTotalUsd > 0) {
            user.append("- Exposition de l'utilisateur dans le portefeuille (somme des montants investis sur cet actif, USD) : ")
                    .append(formatMoney(userInvestedTotalUsd)).append("\n");
        } else {
            user.append("- L'utilisateur n'a pas d'investissement enregistré sur cet actif dans l'application.\n");
        }

        user.append("""
                
                Propose une analyse structurée avec :
                1. Synthèse de l'actif et rôle typique dans un portefeuille (information générale).
                2. Principaux risques (réglementaire, technique, volatilité, concentration…).
                3. Liquidité / volatilité : commentaires qualitatifs (sans inventer de chiffres de marché non fournis).
                4. Si exposition > 0 : comment penser la diversification et la taille de position (sans ordre d'achat/vente).
                5. Questions à se poser avant d'exposer son capital.
                
                Rappel final : ce texte est informatif et ne constitue pas un conseil en investissement.
                """);

        return completeChat(system, user.toString(), 0.35, 1400);
    }

    private String buildObjectivePrompt(Objectif objectif, List<Investissement> investments) {
        StringBuilder sb = new StringBuilder();

        sb.append("Analyze this investment objective.\n\n");

        sb.append("Objective information:\n");
        sb.append("- Name: ").append(objectif.getName()).append("\n");
        sb.append("- Initial amount: ").append(formatMoney(objectif.getInitialAmount())).append("\n");
        sb.append("- Target amount: ").append(formatMoney(objectif.getTargetAmount())).append("\n");
        sb.append("- Completed: ").append(objectif.isCompleted() ? "Yes" : "No").append("\n\n");

        sb.append("Linked investments:\n");

        if (investments == null || investments.isEmpty()) {
            sb.append("No investments are linked to this objective.\n\n");
        } else {
            double totalInvested = 0;
            double totalCurrentValue = 0;
            double totalProfitLoss = 0;

            for (Investissement inv : investments) {
                double currentValue = inv.getCrypto().getCurrentprice() * inv.getQuantity();
                double profitLoss = inv.getProfitLoss();

                totalInvested += inv.getAmountInvested();
                totalCurrentValue += currentValue;
                totalProfitLoss += profitLoss;

                sb.append("\nInvestment ID: ").append(inv.getId()).append("\n");
                sb.append("- Crypto: ").append(inv.getCrypto().getName())
                        .append(" / ").append(inv.getCrypto().getSymbolUpper()).append("\n");
                sb.append("- Amount invested: ").append(formatMoney(inv.getAmountInvested())).append("\n");
                sb.append("- Buy price: ").append(formatMoney(inv.getBuyPrice())).append("\n");
                sb.append("- Quantity: ").append(formatNumber(inv.getQuantity())).append("\n");
                sb.append("- Current price: ").append(formatMoney(inv.getCrypto().getCurrentprice())).append("\n");
                sb.append("- Current value: ").append(formatMoney(currentValue)).append("\n");
                sb.append("- Profit/Loss: ").append(formatMoney(profitLoss)).append("\n");
            }

            sb.append("\nPortfolio summary for this objective:\n");
            sb.append("- Total invested: ").append(formatMoney(totalInvested)).append("\n");
            sb.append("- Total current value: ").append(formatMoney(totalCurrentValue)).append("\n");
            sb.append("- Total profit/loss: ").append(formatMoney(totalProfitLoss)).append("\n");
        }

        sb.append("""
                
                
                Please provide:
                1. A short overview of how the investments are divided inside this objective.
                2. Your point of view on whether the division looks balanced or concentrated.
                3. Whether this objective looks low risk, medium risk, or high risk, and why.
                4. Main strengths.
                5. Main weaknesses.
                6. A short final recommendation.
                
                Answer in English.
                Keep the response clear and useful for a beginner investor.use emojis 
                """);

        return sb.toString();
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

    private String formatMoney(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String formatNumber(double value) {
        return BigDecimal.valueOf(value)
                .setScale(8, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}