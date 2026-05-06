package pi.assistant;

public class CommandResult {
    private final boolean success;
    private final String message;
    private final CommandIntent intent;

    public CommandResult(boolean success, String message, CommandIntent intent) {
        this.success = success;
        this.message = message;
        this.intent = intent;
    }

    public static CommandResult success(String message, CommandIntent intent) {
        return new CommandResult(true, message, intent);
    }

    public static CommandResult error(String message, CommandIntent intent) {
        return new CommandResult(false, message, intent);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public CommandIntent getIntent() {
        return intent;
    }
}
