package pi.assistant;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AgentResponseFormatter {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:[\\.,]\\d+)?)\\s*(tnd)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern GOAL_PATTERN = Pattern.compile("goal\\s+([\\p{L}\\d _\\-]+)", Pattern.CASE_INSENSITIVE);

    private AgentResponseFormatter() {
    }

    public static AgentResponseCard from(String command, CommandResult result) {
        String safeCommand = command == null ? "" : command.trim();
        CommandIntent intent = result == null || result.getIntent() == null ? CommandIntent.UNKNOWN : result.getIntent();
        String message = result == null || result.getMessage() == null ? "Command not understood. Please try again." : result.getMessage();
        boolean success = result != null && result.isSuccess();

        Map<String, String> params = extractParameters(safeCommand, intent);
        String action = actionLabel(intent, success);

        return new AgentResponseCard(
                null,
                null,
                safeCommand,
                intentLabel(intent),
                params,
                Map.of(),
                List.of(),
                action,
                message,
                success
        );
    }

    private static String intentLabel(CommandIntent intent) {
        return switch (intent) {
            case HELP -> "help";
            case SHOW_BALANCE -> "get_savings_balance";
            case LIST_GOALS -> "list_goals";
            case CREATE_GOAL -> "create_goal";
            case DELETE_GOAL -> "delete_goal";
            case CONTRIBUTE_TO_GOAL -> "contribute_to_goal";
            case UPDATE_GOAL -> "update_goal";
            case SHOW_OVERDUE_GOALS -> "show_overdue_goals";
            case SHOW_COMPLETED_GOALS -> "show_completed_goals";
            case SHOW_AT_RISK_GOALS -> "show_at_risk_goals";
            case UNKNOWN -> "unknown";
        };
    }

    private static String actionLabel(CommandIntent intent, boolean success) {
        if (!success) {
            return "Execution failed";
        }
        return switch (intent) {
            case HELP -> "Help displayed";
            case CREATE_GOAL -> "Goal created";
            case UPDATE_GOAL -> "Goal updated";
            case DELETE_GOAL -> "Goal delete workflow executed";
            case CONTRIBUTE_TO_GOAL -> "Contribution executed";
            case SHOW_BALANCE, LIST_GOALS, SHOW_OVERDUE_GOALS, SHOW_COMPLETED_GOALS -> "Information fetched";
            case SHOW_AT_RISK_GOALS -> "Information fetched";
            case UNKNOWN -> "No action";
        };
    }

    private static Map<String, String> extractParameters(String command, CommandIntent intent) {
        Map<String, String> params = new LinkedHashMap<>();
        if (command == null || command.isBlank()) {
            return params;
        }

        Matcher amountMatcher = AMOUNT_PATTERN.matcher(command);
        if (amountMatcher.find()) {
            String amount = amountMatcher.group(1).replace(',', '.');
            params.put("Amount", formatMoney(amount));
        }

        Matcher goalMatcher = GOAL_PATTERN.matcher(command);
        if (goalMatcher.find()) {
            params.put("Goal", goalMatcher.group(1).trim());
        }

        if (intent == CommandIntent.DELETE_GOAL && !params.containsKey("Goal")) {
            String lower = command.toLowerCase(Locale.ROOT);
            int idx = lower.indexOf("delete");
            if (idx >= 0) {
                params.put("Goal", command.substring(Math.min(command.length(), idx + 6)).trim());
            }
        }

        return params;
    }

    private static String formatMoney(String amount) {
        try {
            return new BigDecimal(amount).setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " TND";
        } catch (NumberFormatException exception) {
            return amount + " TND";
        }
    }
}
