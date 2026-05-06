package pi.assistant;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiIntentExtractionService {

    private static final Pattern CREATE_GOAL_PATTERN = Pattern.compile(
            "^create\\s+goal\\s+(.+?)(?:\\s+target\\s+([0-9]+(?:[.,][0-9]+)?))?(?:\\s+deadline\\s+(\\d{4}-\\d{2}-\\d{2}))?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern UPDATE_GOAL_PATTERN = Pattern.compile(
            "^update\\s+goal\\s+(.+?)(?:\\s+target\\s+([0-9]+(?:[.,][0-9]+)?))?(?:\\s+deadline\\s+(\\d{4}-\\d{2}-\\d{2}))?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DELETE_GOAL_PATTERN = Pattern.compile(
            "^(?:delete|remove)\\s+goal\\s+(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CONTRIBUTE_PATTERN = Pattern.compile(
            "^(?:contribute|add|put)\\s+([0-9]+(?:[.,][0-9]+)?)\\s*(?:tnd)?\\s*(?:to\\s+goal|to|in|into)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE
    );
    private final GeminiIntentExtractionService geminiService = new GeminiIntentExtractionService();
    private boolean geminiUnavailableLogged;

    public ExtractionResult extract(String command) {
        if (geminiService.isConfigured()) {
            try {
                ExtractionResult extracted = geminiService.extract(command);
                if (extracted.intent() != CommandIntent.UNKNOWN) {
                    return extracted;
                }
            } catch (Exception exception) {
                if (!geminiUnavailableLogged) {
                    System.err.println("[SmartVoiceAgent] Gemini extraction unavailable, fallback to local parser: " + exception.getMessage());
                    geminiUnavailableLogged = true;
                }
            }
        }
        return extractLocal(command);
    }

    public boolean isGeminiConfigured() {
        return geminiService.isConfigured();
    }

    public ExtractionResult extractLocal(String command) {
        String normalized = normalize(command);
        if (normalized.isBlank()) {
            return ExtractionResult.unknown();
        }

        if (containsAny(normalized, "/help", "help", "show commands", "what can i say")) {
            return new ExtractionResult(CommandIntent.HELP, null, null, null, null);
        }

        if (containsAny(normalized, "show my savings balance", "show savings balance", "show balance", "what is my balance", "balance")) {
            return new ExtractionResult(CommandIntent.SHOW_BALANCE, null, null, null, null);
        }

        if (containsAny(normalized, "show overdue goals", "list overdue goals", "overdue goals")) {
            return new ExtractionResult(CommandIntent.SHOW_OVERDUE_GOALS, null, null, null, null);
        }

        if (containsAny(normalized, "show completed goals", "list completed goals", "completed goals")) {
            return new ExtractionResult(CommandIntent.SHOW_COMPLETED_GOALS, null, null, null, null);
        }

        if (containsAny(normalized, "show at risk goals", "at risk goals")) {
            return new ExtractionResult(CommandIntent.SHOW_AT_RISK_GOALS, null, null, null, null);
        }

        if (containsAny(normalized, "list my goals", "list goals", "show my goals", "show goals")) {
            return new ExtractionResult(CommandIntent.LIST_GOALS, null, null, null, null);
        }

        Matcher contributeMatcher = CONTRIBUTE_PATTERN.matcher(normalized);
        if (contributeMatcher.matches()) {
            return new ExtractionResult(
                    CommandIntent.CONTRIBUTE_TO_GOAL,
                    cleanGoalName(contributeMatcher.group(2)),
                    contributeMatcher.group(1),
                    null,
                    null
            );
        }

        Matcher createMatcher = CREATE_GOAL_PATTERN.matcher(normalized);
        if (createMatcher.matches()) {
            return new ExtractionResult(
                    CommandIntent.CREATE_GOAL,
                    cleanGoalName(createMatcher.group(1)),
                    null,
                    createMatcher.group(2),
                    createMatcher.group(3)
            );
        }

        Matcher updateMatcher = UPDATE_GOAL_PATTERN.matcher(normalized);
        if (updateMatcher.matches()) {
            return new ExtractionResult(
                    CommandIntent.UPDATE_GOAL,
                    cleanGoalName(updateMatcher.group(1)),
                    null,
                    updateMatcher.group(2),
                    updateMatcher.group(3)
            );
        }

        Matcher deleteMatcher = DELETE_GOAL_PATTERN.matcher(normalized);
        if (deleteMatcher.matches()) {
            return new ExtractionResult(
                    CommandIntent.DELETE_GOAL,
                    cleanGoalName(deleteMatcher.group(1)),
                    null,
                    null,
                    null
            );
        }

        return ExtractionResult.unknown();
    }

    private String normalize(String command) {
        if (command == null) {
            return "";
        }
        return command.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (value.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String cleanGoalName(String rawName) {
        if (rawName == null) {
            return null;
        }
        String cleaned = rawName.trim().replaceAll("\\s+", " ");
        return cleaned.isBlank() ? null : cleaned;
    }

    public record ExtractionResult(
            CommandIntent intent,
            String goalName,
            String amount,
            String targetAmount,
            String deadline
    ) {
        static ExtractionResult unknown() {
            return new ExtractionResult(CommandIntent.UNKNOWN, null, null, null, null);
        }
    }
}
