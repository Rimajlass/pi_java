package pi.services.InvestissementService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pi.entities.Crypto;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interprète une phrase (FR/EN) pour ajouter un investissement sur les cryptos présentes en base.
 * Utilise Groq si {@code GROQ_API_KEY} est défini ; sinon heuristique locale.
 */
public final class InvestmentAssistantService {

    private static final Pattern ADD_INTENT = Pattern.compile(
            "\\b(ajoute|ajouter|investis|investir|place|mets|mettre|achète|acheter|buy|add|invest)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final GroqAiService groqAiService = new GroqAiService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public enum OutcomeType {
        INVESTMENT_ADDED,
        MESSAGE,
        ERROR
    }

    public static final class Result {
        public final OutcomeType type;
        public final Crypto crypto;
        public final double amountUsd;
        public final String text;

        private Result(OutcomeType type, Crypto crypto, double amountUsd, String text) {
            this.type = type;
            this.crypto = crypto;
            this.amountUsd = amountUsd;
            this.text = text;
        }

        public static Result added(Crypto c, double amount, String text) {
            return new Result(OutcomeType.INVESTMENT_ADDED, c, amount, text);
        }

        public static Result message(String text) {
            return new Result(OutcomeType.MESSAGE, null, 0, text);
        }

        public static Result error(String text) {
            return new Result(OutcomeType.ERROR, null, 0, text);
        }
    }

    public Result processUserMessage(String userMessage, List<Crypto> catalogFromDb) {
        if (userMessage == null || userMessage.isBlank()) {
            return Result.message("Écris une demande, par exemple : « Ajoute 150 USD sur Ethereum ».");
        }
        if (catalogFromDb == null || catalogFromDb.isEmpty()) {
            return Result.error("Aucune crypto disponible en base.");
        }

        try {
            Result groq = tryParseWithGroq(userMessage.trim(), catalogFromDb);
            if (groq != null) {
                return groq;
            }
        } catch (IllegalStateException ignored) {
            // Pas de clé API : repli local
        } catch (Exception e) {
            Result fallback = parseFallback(userMessage.trim(), catalogFromDb);
            if (fallback.type == OutcomeType.INVESTMENT_ADDED) {
                return fallback;
            }
            return Result.message("Je n'ai pas pu analyser la phrase (API). Essaie le mode hors-ligne : "
                    + formatCatalogHint(catalogFromDb) + "\nExemple : « Ajoute 200 en bitcoin ».\nDétail : "
                    + e.getMessage());
        }

        return parseFallback(userMessage.trim(), catalogFromDb);
    }

    private Result tryParseWithGroq(String userMessage, List<Crypto> catalogFromDb) throws Exception {
        StringBuilder catalog = new StringBuilder();
        for (Crypto c : catalogFromDb) {
            catalog.append("- id=").append(c.getId())
                    .append(" name=\"").append(escape(c.getName())).append("\"")
                    .append(" symbol=\"").append(escape(c.getSymbol())).append("\"\n");
        }

        String system = """
                Tu es un extracteur d'intentions pour une application JavaFX de finances.
                Réponds UNIQUEMENT par un objet JSON valide (pas de markdown, pas de texte autour).
                Schéma :
                {"intent":"add_investment"|"chat","crypto_id":null ou nombre,"amount_usd":null ou nombre,"message_fr":"texte court en français"}
                Règles :
                - intent=add_investment seulement si l'utilisateur demande clairement d'ajouter un investissement/acheter/déposer un montant en USD sur une crypto listée ci-dessous.
                - crypto_id doit être exactement l'un des id listés ; si incertain ou crypto hors liste, intent=chat et message_fr explique.
                - amount_usd : montant en dollars US ; si l'utilisateur dit EUR approximatif, convertis avec 1 EUR ≈ 1.08 USD pour l'estimation ou demande clarification en chat.
                - Pour toute autre question ou salutation, intent=chat.
                """;

        String user = "Cryptos AUTORISÉES (seules valeurs valides pour crypto_id):\n"
                + catalog
                + "\nMessage utilisateur:\n"
                + userMessage;

        String raw = groqAiService.completeChat(system, user, 0.1, 400);
        JsonNode root = parseJsonFromAssistantText(raw);
        if (root == null || root.isMissingNode()) {
            return null;
        }

        String intent = root.path("intent").asText("");
        String messageFr = root.path("message_fr").asText("").trim();

        if ("add_investment".equals(intent)) {
            int cryptoId = root.path("crypto_id").asInt(-1);
            double amount = root.path("amount_usd").asDouble(-1);

            Crypto matched = catalogFromDb.stream()
                    .filter(c -> c.getId() == cryptoId)
                    .findFirst()
                    .orElse(null);

            if (matched != null && amount > 0 && Double.isFinite(amount) && amount <= 1_000_000_000) {
                String confirm = messageFr.isBlank()
                        ? String.format(Locale.FRENCH,
                        "C'est noté : j'ajoute %.2f USD sur %s.", amount, matched.getName())
                        : messageFr;
                return Result.added(matched, amount, confirm);
            }

            return Result.message(messageFr.isBlank()
                    ? "Je n'ai pas pu relier la crypto ou le montant. " + formatCatalogHint(catalogFromDb)
                    : messageFr);
        }

        return Result.message(messageFr.isBlank()
                ? "Je peux ajouter un investissement si tu indiques une crypto de la liste et un montant en USD."
                : messageFr);
    }

    private JsonNode parseJsonFromAssistantText(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            t = t.substring(start, end + 1);
        }
        try {
            return objectMapper.readTree(t);
        } catch (Exception e) {
            return null;
        }
    }

    private Result parseFallback(String userMessage, List<Crypto> catalogFromDb) {
        String lower = userMessage.toLowerCase(Locale.FRENCH);
        boolean wantsAdd = ADD_INTENT.matcher(lower).find();

        Crypto matched = bestMatchCrypto(lower, catalogFromDb);

        double amount = extractAmount(userMessage);
        if (wantsAdd && matched != null && amount > 0 && amount <= 1_000_000_000) {
            return Result.added(matched, amount, String.format(Locale.FRENCH,
                    "Ajout local : %.2f USD sur %s (prix courant du tableau).", amount, matched.getName()));
        }

        if (!wantsAdd) {
            return Result.message("Je peux enregistrer un investissement si tu écris par exemple : "
                    + "« Ajoute 250 USD en Ethereum ». " + formatCatalogHint(catalogFromDb));
        }

        if (matched == null) {
            return Result.message("Je n'ai pas reconnu la crypto. " + formatCatalogHint(catalogFromDb));
        }

        return Result.message("Indique un montant positif en USD (ex. 100 ou 99,50).");
    }

    private static Crypto bestMatchCrypto(String lower, List<Crypto> catalogFromDb) {
        Crypto matched = null;
        int bestLen = 0;
        for (Crypto c : catalogFromDb) {
            String name = c.getName() != null ? c.getName().toLowerCase(Locale.FRENCH) : "";
            if (!name.isEmpty() && lower.contains(name) && name.length() > bestLen) {
                matched = c;
                bestLen = name.length();
            }
        }
        for (Crypto c : catalogFromDb) {
            String sym = c.getSymbol() != null ? c.getSymbol().toLowerCase(Locale.ROOT) : "";
            if (!sym.isEmpty() && lower.contains(sym) && sym.length() > bestLen) {
                matched = c;
                bestLen = sym.length();
            }
        }
        return matched;
    }

    private static double extractAmount(String text) {
        Matcher m = Pattern.compile("(\\d+(?:[.,]\\d+)?)").matcher(text);
        double best = -1;
        while (m.find()) {
            double v = Double.parseDouble(m.group(1).replace(',', '.'));
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    private static String formatCatalogHint(List<Crypto> cryptos) {
        StringBuilder sb = new StringBuilder("Cryptos disponibles : ");
        for (int i = 0; i < cryptos.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(cryptos.get(i).getName());
        }
        sb.append(".");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
