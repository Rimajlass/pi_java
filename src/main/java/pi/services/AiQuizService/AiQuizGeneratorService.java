package pi.services.AiQuizService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.entities.Cours;
import pi.services.AiQuizService.QuizGenerationModels.GeneratedQuizBundle;
import pi.services.AiQuizService.QuizGenerationModels.GeminiGenerateContentResponse;
import pi.services.AiQuizService.QuizGenerationModels.GeneratedQuestion;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class AiQuizGeneratorService {

    private final ObjectMapper mapper;

    public AiQuizGeneratorService() {
        mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public GeneratedQuizBundle generateQuiz(String apiKey, String model, Cours cours, String topic, String difficulty, int questionCount)
            throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Clé Gemini manquante. Définissez GEMINI_API_KEY ou -Dgemini.api.key, ou créez ai.properties.");
        }
        if (cours == null || cours.getId() <= 0) {
            throw new IllegalArgumentException("Veuillez selectionner un cours valide.");
        }
        int count = Math.max(1, Math.min(20, questionCount));
        String level = safe(difficulty);
        if (level.isBlank()) {
            level = "Moyen";
        }

        String courseTitle = safe(cours.getTitre());
        String courseText = safe(cours.getContenuTexte());
        if (courseText.length() > 8000) {
            courseText = courseText.substring(0, 8000);
        }
        String theme = safe(topic);
        if (theme.isBlank()) {
            theme = "finance personnelle, investissement, gestion d'argent";
        }

        String system = """
                Tu es un generateur de quiz pedagogiques.
                Tu dois retourner uniquement du JSON (pas de texte hors JSON).
                Le JSON doit respecter exactement ce schema:
                {
                  "title": string,
                  "questions": [
                    {
                      "question": string,
                      "choices": [string, string, string, string],
                      "correct": string,
                      "points": integer,
                      "explanation": string
                    }
                  ]
                }
                Contraintes:
                - "choices" contient exactement 4 choix.
                - "correct" est exactement l'un des choix.
                - Questions en francais.
                """;

        String user = ("Genere " + count + " questions (" + level + ") sur le theme: " + theme + ".\n\n"
                + "Cours (titre): " + courseTitle + "\n"
                + "Cours (contenu): " + courseText).trim();

        String requestJson = buildGeminiGenerateContentRequest(system, user);
        GeminiChatClient client = new GeminiChatClient(apiKey);
        String responseJson;
        try {
            responseJson = client.generateContentJson(pickModel(model), requestJson);
        } catch (IOException e) {
            String msg = safe(e.getMessage());
            String lower = msg.toLowerCase(Locale.ROOT);
            if (msg.contains("HTTP 429") || lower.contains("resource_exhausted") || lower.contains("quota")) {
                throw new InsufficientQuotaException(msg);
            }
            throw e;
        }

        GeminiGenerateContentResponse resp = mapper.readValue(responseJson, GeminiGenerateContentResponse.class);
        String content = resp.candidates.stream()
                .filter(Objects::nonNull)
                .map(c -> c.content)
                .filter(Objects::nonNull)
                .flatMap(c -> (c.parts == null ? java.util.stream.Stream.empty() : c.parts.stream()))
                .filter(Objects::nonNull)
                .map(p -> p.text)
                .filter(s -> s != null && !s.isBlank())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Reponse Gemini vide."));

        GeneratedQuizBundle bundle = parseGeneratedBundle(content);
        validateBundle(bundle);
        return bundle;
    }

    public GeneratedQuizBundle generateDemoQuiz(Cours cours, String topic, String difficulty, int questionCount) {
        if (cours == null || cours.getId() <= 0) {
            throw new IllegalArgumentException("Veuillez selectionner un cours valide.");
        }
        int count = Math.max(1, Math.min(20, questionCount));
        String theme = safe(topic).trim();
        if (theme.isBlank()) {
            theme = "investissement et gestion d'argent";
        }

        GeneratedQuizBundle bundle = new GeneratedQuizBundle();
        bundle.title = "Quiz (Demo) - " + (safe(cours.getTitre()).isBlank() ? "Cours " + cours.getId() : safe(cours.getTitre()));

        for (int i = 0; i < count; i++) {
            bundle.questions.add(buildDemoQuestion(theme, safe(difficulty), i));
        }
        validateBundle(bundle);
        return bundle;
    }

    public String toPrettyJson(Object obj) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    }

    private GeneratedQuizBundle parseGeneratedBundle(String jsonContent) throws JsonProcessingException {
        String cleaned = cleanupJson(jsonContent);
        return mapper.readValue(cleaned, GeneratedQuizBundle.class);
    }

    private static String cleanupJson(String value) {
        String v = safe(value).trim();
        if (v.isBlank()) {
            return v;
        }

        // Gemini renvoie parfois du JSON dans un bloc Markdown ```json ... ```
        if (v.startsWith("```")) {
            int firstNewline = v.indexOf('\n');
            if (firstNewline > 0) {
                v = v.substring(firstNewline + 1);
            } else {
                v = v.substring(3);
            }
            int fenceEnd = v.lastIndexOf("```");
            if (fenceEnd >= 0) {
                v = v.substring(0, fenceEnd);
            }
            v = v.trim();
        }

        // Si un texte parasite encadre le JSON, on extrait le premier objet JSON.
        int start = v.indexOf('{');
        int end = v.lastIndexOf('}');
        if (start >= 0 && end > start) {
            v = v.substring(start, end + 1).trim();
        }
        return v;
    }

    private void validateBundle(GeneratedQuizBundle bundle) {
        if (bundle == null || bundle.questions == null || bundle.questions.isEmpty()) {
            throw new IllegalArgumentException("Aucune question generee.");
        }
        for (var q : bundle.questions) {
            if (q == null) {
                throw new IllegalArgumentException("Question invalide.");
            }
            if (safe(q.question).isBlank()) {
                throw new IllegalArgumentException("Question vide.");
            }
            if (q.choices == null || q.choices.size() != 4) {
                throw new IllegalArgumentException("Chaque question doit avoir exactement 4 choix.");
            }
            String correct = safe(q.correct);
            boolean match = q.choices.stream().anyMatch(c -> safe(c).equalsIgnoreCase(correct));
            if (!match) {
                throw new IllegalArgumentException("La reponse correcte doit etre un des choix.");
            }
            q.points = Math.max(1, Math.min(10, q.points));
        }
    }

    private static String pickModel(String model) {
        String pickedModel = safe(model);
        if (pickedModel.isBlank()) {
            return "gemini-2.5-flash";
        }
        return pickedModel.trim();
    }

    private String buildGeminiGenerateContentRequest(String system, String user) {
        String prompt = (safe(system).trim() + "\n\n" + safe(user).trim()).trim();
        return """
                {
                  "contents": [
                    {
                      "role": "user",
                      "parts": [
                        {"text": %s}
                      ]
                    }
                  ]
                }
                """.formatted(
                jsonString(prompt)
        );
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private GeneratedQuestion buildDemoQuestion(String theme, String difficulty, int index) {
        String level = safe(difficulty).trim();
        if (level.isBlank()) {
            level = "Moyen";
        }
        int variant = ThreadLocalRandom.current().nextInt(6);

        GeneratedQuestion q = new GeneratedQuestion();
        q.points = "Difficile".equalsIgnoreCase(level) ? 3 : ("Facile".equalsIgnoreCase(level) ? 1 : 2);
        q.explanation = "Mode demo: question generee localement sur le theme \"" + theme + "\".";

        switch (variant) {
            case 0 -> {
                q.question = "Pourquoi diversifier un portefeuille d'investissement ?";
                q.choices = java.util.List.of(
                        "Reduire le risque global",
                        "Garantir un rendement eleve",
                        "Eviter de suivre un budget",
                        "Supprimer tout frais"
                );
                q.correct = "Reduire le risque global";
            }
            case 1 -> {
                q.question = "Quel est le meilleur premier pas pour gerer son argent efficacement ?";
                q.choices = java.util.List.of(
                        "Etablir un budget",
                        "Tout investir en crypto",
                        "Ignorer ses depenses",
                        "Faire un pret sans plan"
                );
                q.correct = "Etablir un budget";
            }
            case 2 -> {
                q.question = "A quoi sert un fonds d'urgence ?";
                q.choices = java.util.List.of(
                        "Couvrir les depenses imprevues",
                        "Acheter des actions tous les jours",
                        "Payer plus d'impots",
                        "Remplacer l'assurance"
                );
                q.correct = "Couvrir les depenses imprevues";
            }
            case 3 -> {
                q.question = "Que signifie 'interets composes' ?";
                q.choices = java.util.List.of(
                        "Les interets produisent eux-memes des interets",
                        "Les interets sont fixes a vie",
                        "Les interets sont toujours negatifs",
                        "Les interets n'existent pas en epargne"
                );
                q.correct = "Les interets produisent eux-memes des interets";
            }
            case 4 -> {
                q.question = "Quel est l'effet principal de l'inflation sur ton argent ?";
                q.choices = java.util.List.of(
                        "Elle reduit le pouvoir d'achat",
                        "Elle supprime le risque",
                        "Elle augmente automatiquement le salaire",
                        "Elle rend les dettes impossibles"
                );
                q.correct = "Elle reduit le pouvoir d'achat";
            }
            default -> {
                q.question = "Quel est un objectif raisonnable d'epargne mensuelle (en general) ?";
                q.choices = java.util.List.of(
                        "Un pourcentage de revenu adapte a ta situation",
                        "Toujours 90% du revenu",
                        "0% pour maximiser les loisirs",
                        "Seulement quand il reste de l'argent"
                );
                q.correct = "Un pourcentage de revenu adapte a ta situation";
            }
        }

        // Add slight variety for repeated questions
        if (index > 0 && q.question != null) {
            q.question = q.question;
        }
        return q;
    }

    private static String jsonString(String s) {
        String v = safe(s);
        v = v.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
        return "\"" + v + "\"";
    }
}
