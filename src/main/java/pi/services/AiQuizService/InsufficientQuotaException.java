package pi.services.AiQuizService;

public final class InsufficientQuotaException extends Exception {

    public InsufficientQuotaException(String message) {
        super(message);
    }
}

