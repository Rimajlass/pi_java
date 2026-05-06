package pi.assistant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CommandInterpreterService {

    private final CommandGateway gateway;
    private final AiIntentExtractionService extractionService;
    private String pendingDeleteGoalName;

    public CommandInterpreterService(CommandGateway gateway) {
        this.gateway = gateway;
        this.extractionService = new AiIntentExtractionService();
    }

    public CommandResult interpret(String rawCommand) {
        String command = normalize(rawCommand);
        if (command.isBlank()) {
            return CommandResult.error("Command not understood.", CommandIntent.UNKNOWN);
        }

        if (pendingDeleteGoalName != null) {
            return handleDeleteConfirmation(command);
        }

        AiIntentExtractionService.ExtractionResult extraction = extractionService.extract(command);
        return switch (extraction.intent()) {
            case HELP -> CommandResult.success("Help requested.", CommandIntent.HELP);
            case SHOW_BALANCE -> showBalance();
            case LIST_GOALS -> listGoals();
            case SHOW_OVERDUE_GOALS -> showOverdueGoals();
            case SHOW_COMPLETED_GOALS -> showCompletedGoals();
            case SHOW_AT_RISK_GOALS -> showOverdueGoals();
            case CREATE_GOAL -> createGoal(extraction);
            case UPDATE_GOAL -> updateGoal(extraction);
            case CONTRIBUTE_TO_GOAL -> contributeToGoal(extraction);
            case DELETE_GOAL -> requestDeleteConfirmation(extraction.goalName());
            case UNKNOWN -> CommandResult.error("Command not understood.", CommandIntent.UNKNOWN);
        };
    }

    private CommandResult showBalance() {
        CommandGatewaySnapshot snapshot = gateway.loadSnapshot();
        if (snapshot == null || snapshot.accountId() <= 0) {
            return CommandResult.error("Savings account not found.", CommandIntent.SHOW_BALANCE);
        }
        return CommandResult.success(
                "Your current savings balance is " + formatMoney(snapshot.balance()) + ".",
                CommandIntent.SHOW_BALANCE
        );
    }

    private CommandResult listGoals() {
        CommandGatewaySnapshot snapshot = gateway.loadSnapshot();
        List<CommandGoalSnapshot> goals = snapshot == null ? List.of() : snapshot.goals();
        if (goals.isEmpty()) {
            return CommandResult.success("No goals found.", CommandIntent.LIST_GOALS);
        }

        List<CommandGoalSnapshot> sortedGoals = new ArrayList<>(goals);
        sortedGoals.sort(
                Comparator.comparing(CommandGoalSnapshot::deadline, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CommandGoalSnapshot::name, String.CASE_INSENSITIVE_ORDER)
        );

        StringBuilder builder = new StringBuilder("Goals:\n");
        for (CommandGoalSnapshot goal : sortedGoals) {
            builder.append("- ")
                    .append(goal.name())
                    .append(" | current ")
                    .append(formatMoney(goal.currentAmount()))
                    .append(" / target ")
                    .append(formatMoney(goal.targetAmount()))
                    .append(" | deadline ")
                    .append(goal.deadline() == null ? "-" : goal.deadline())
                    .append('\n');
        }
        return CommandResult.success(builder.toString().trim(), CommandIntent.LIST_GOALS);
    }

    private CommandResult showOverdueGoals() {
        CommandGatewaySnapshot snapshot = gateway.loadSnapshot();
        LocalDate today = LocalDate.now();
        List<CommandGoalSnapshot> overdueGoals = snapshot == null
                ? List.of()
                : snapshot.goals().stream()
                .filter(goal -> goal.deadline() != null && goal.deadline().isBefore(today))
                .filter(goal -> goal.currentAmount().compareTo(goal.targetAmount()) < 0)
                .sorted(Comparator.comparing(CommandGoalSnapshot::deadline))
                .toList();

        if (overdueGoals.isEmpty()) {
            return CommandResult.success("No overdue goals found.", CommandIntent.SHOW_OVERDUE_GOALS);
        }

        StringBuilder builder = new StringBuilder("Overdue goals:\n");
        for (CommandGoalSnapshot goal : overdueGoals) {
            builder.append("- ")
                    .append(goal.name())
                    .append(" (deadline ")
                    .append(goal.deadline())
                    .append(", remaining ")
                    .append(formatMoney(goal.targetAmount().subtract(goal.currentAmount()).max(BigDecimal.ZERO)))
                    .append(")\n");
        }
        return CommandResult.success(builder.toString().trim(), CommandIntent.SHOW_OVERDUE_GOALS);
    }

    private CommandResult showCompletedGoals() {
        CommandGatewaySnapshot snapshot = gateway.loadSnapshot();
        List<CommandGoalSnapshot> completedGoals = snapshot == null
                ? List.of()
                : snapshot.goals().stream()
                .filter(goal -> goal.currentAmount().compareTo(goal.targetAmount()) >= 0)
                .sorted(Comparator.comparing(CommandGoalSnapshot::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (completedGoals.isEmpty()) {
            return CommandResult.success("No completed goals found.", CommandIntent.SHOW_COMPLETED_GOALS);
        }

        StringBuilder builder = new StringBuilder("Completed goals:\n");
        for (CommandGoalSnapshot goal : completedGoals) {
            builder.append("- ")
                    .append(goal.name())
                    .append(" (")
                    .append(formatMoney(goal.targetAmount()))
                    .append(")\n");
        }
        return CommandResult.success(builder.toString().trim(), CommandIntent.SHOW_COMPLETED_GOALS);
    }

    private CommandResult createGoal(AiIntentExtractionService.ExtractionResult extraction) {
        if (extraction.goalName() == null || extraction.goalName().isBlank()) {
            return CommandResult.error("Goal name is required.", CommandIntent.CREATE_GOAL);
        }

        BigDecimal targetAmount = CommandValidator.parseAmount(extraction.targetAmount());
        if (targetAmount == null) {
            return CommandResult.error("Invalid amount.", CommandIntent.CREATE_GOAL);
        }

        LocalDate deadline = null;
        if (extraction.deadline() != null) {
            deadline = CommandValidator.parseDate(extraction.deadline());
            if (deadline == null) {
                return CommandResult.error("Invalid date format. Use YYYY-MM-DD.", CommandIntent.CREATE_GOAL);
            }
        }

        CommandGatewayResult result = gateway.createGoal(extraction.goalName(), targetAmount, deadline);
        if (!result.success()) {
            return CommandResult.error(result.message(), CommandIntent.CREATE_GOAL);
        }
        return CommandResult.success(result.message(), CommandIntent.CREATE_GOAL);
    }

    private CommandResult updateGoal(AiIntentExtractionService.ExtractionResult extraction) {
        if (extraction.goalName() == null || extraction.goalName().isBlank()) {
            return CommandResult.error("Goal name is required.", CommandIntent.UPDATE_GOAL);
        }

        CommandGatewaySnapshot snapshot = gateway.loadSnapshot();
        Optional<CommandGoalSnapshot> goalOptional = findGoalByName(snapshot, extraction.goalName());
        if (goalOptional.isEmpty()) {
            return CommandResult.error("Goal not found.", CommandIntent.UPDATE_GOAL);
        }

        BigDecimal targetAmount = null;
        if (extraction.targetAmount() != null) {
            targetAmount = CommandValidator.parseAmount(extraction.targetAmount());
            if (targetAmount == null) {
                return CommandResult.error("Invalid amount.", CommandIntent.UPDATE_GOAL);
            }
        }

        LocalDate deadline = null;
        if (extraction.deadline() != null) {
            deadline = CommandValidator.parseDate(extraction.deadline());
            if (deadline == null) {
                return CommandResult.error("Invalid date format. Use YYYY-MM-DD.", CommandIntent.UPDATE_GOAL);
            }
        }

        if (targetAmount == null && deadline == null) {
            return CommandResult.error("No fields to update.", CommandIntent.UPDATE_GOAL);
        }

        CommandGatewayResult result = gateway.updateGoal(extraction.goalName(), targetAmount, deadline);
        if (!result.success()) {
            return CommandResult.error(result.message(), CommandIntent.UPDATE_GOAL);
        }
        return CommandResult.success(result.message(), CommandIntent.UPDATE_GOAL);
    }

    private CommandResult contributeToGoal(AiIntentExtractionService.ExtractionResult extraction) {
        if (extraction.goalName() == null || extraction.goalName().isBlank()) {
            return CommandResult.error("Goal name is required.", CommandIntent.CONTRIBUTE_TO_GOAL);
        }

        BigDecimal amount = CommandValidator.parseAmount(extraction.amount());
        if (amount == null) {
            return CommandResult.error("Invalid amount.", CommandIntent.CONTRIBUTE_TO_GOAL);
        }

        CommandGatewaySnapshot snapshot = gateway.loadSnapshot();
        if (snapshot == null || snapshot.accountId() <= 0) {
            return CommandResult.error("Savings account not found.", CommandIntent.CONTRIBUTE_TO_GOAL);
        }

        Optional<CommandGoalSnapshot> goalOptional = findGoalByName(snapshot, extraction.goalName());
        if (goalOptional.isEmpty()) {
            return CommandResult.error("Goal not found.", CommandIntent.CONTRIBUTE_TO_GOAL);
        }
        CommandGoalSnapshot goal = goalOptional.get();

        if (snapshot.balance().compareTo(amount) < 0) {
            return CommandResult.error("Insufficient savings balance.", CommandIntent.CONTRIBUTE_TO_GOAL);
        }

        BigDecimal remaining = goal.targetAmount().subtract(goal.currentAmount()).max(BigDecimal.ZERO);
        if (remaining.compareTo(BigDecimal.ZERO) == 0 || amount.compareTo(remaining) > 0) {
            return CommandResult.error("Contribution exceeds remaining goal amount.", CommandIntent.CONTRIBUTE_TO_GOAL);
        }

        CommandGatewayResult result = gateway.contributeToGoal(goal.name(), amount);
        if (!result.success()) {
            return CommandResult.error(result.message(), CommandIntent.CONTRIBUTE_TO_GOAL);
        }
        return CommandResult.success(result.message(), CommandIntent.CONTRIBUTE_TO_GOAL);
    }

    private CommandResult requestDeleteConfirmation(String goalName) {
        if (goalName == null || goalName.isBlank()) {
            return CommandResult.error("Goal name is required.", CommandIntent.DELETE_GOAL);
        }

        CommandGatewaySnapshot snapshot = gateway.loadSnapshot();
        Optional<CommandGoalSnapshot> goalOptional = findGoalByName(snapshot, goalName);
        if (goalOptional.isEmpty()) {
            return CommandResult.error("Goal not found.", CommandIntent.DELETE_GOAL);
        }

        pendingDeleteGoalName = goalOptional.get().name();
        return CommandResult.success(
                "Are you sure you want to delete goal " + pendingDeleteGoalName + "? Type yes to confirm.",
                CommandIntent.DELETE_GOAL
        );
    }

    private CommandResult handleDeleteConfirmation(String normalizedCommand) {
        if ("yes".equals(normalizedCommand) || "confirm".equals(normalizedCommand)) {
            String goalToDelete = pendingDeleteGoalName;
            pendingDeleteGoalName = null;
            CommandGatewayResult result = gateway.deleteGoal(goalToDelete);
            if (!result.success()) {
                return CommandResult.error(result.message(), CommandIntent.DELETE_GOAL);
            }
            return CommandResult.success(result.message(), CommandIntent.DELETE_GOAL);
        }

        if ("no".equals(normalizedCommand) || "cancel".equals(normalizedCommand)) {
            pendingDeleteGoalName = null;
            return CommandResult.success("Delete action canceled.", CommandIntent.DELETE_GOAL);
        }

        return CommandResult.error("Please confirm this action.", CommandIntent.DELETE_GOAL);
    }

    private Optional<CommandGoalSnapshot> findGoalByName(CommandGatewaySnapshot snapshot, String goalName) {
        if (snapshot == null || snapshot.goals() == null || goalName == null) {
            return Optional.empty();
        }

        String normalizedGoalName = normalize(goalName);
        return snapshot.goals().stream()
                .filter(goal -> normalize(goal.name()).equals(normalizedGoalName))
                .findFirst();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String formatMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " TND";
    }

    public interface CommandGateway {
        CommandGatewaySnapshot loadSnapshot();

        CommandGatewayResult createGoal(String goalName, BigDecimal targetAmount, LocalDate deadline);

        CommandGatewayResult updateGoal(String goalName, BigDecimal targetAmount, LocalDate deadline);

        CommandGatewayResult contributeToGoal(String goalName, BigDecimal amount);

        CommandGatewayResult deleteGoal(String goalName);
    }

    public record CommandGatewaySnapshot(
            int accountId,
            BigDecimal balance,
            List<CommandGoalSnapshot> goals
    ) {
    }

    public record CommandGoalSnapshot(
            int id,
            String name,
            BigDecimal targetAmount,
            BigDecimal currentAmount,
            LocalDate deadline
    ) {
    }

    public record CommandGatewayResult(
            boolean success,
            String message
    ) {
        public static CommandGatewayResult success(String message) {
            return new CommandGatewayResult(true, message);
        }

        public static CommandGatewayResult failure(String message) {
            return new CommandGatewayResult(false, message);
        }
    }
}
