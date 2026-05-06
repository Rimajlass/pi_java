package pi.services.MailingService;

public class MailSendResult {

    public enum Status {
        SENT,
        DISABLED,
        NO_RECIPIENTS,
        CONFIG_ERROR,
        FAILED
    }

    private final Status status;
    private final String message;

    public MailSendResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSent() {
        return status == Status.SENT;
    }

    public static MailSendResult sent(String message) {
        return new MailSendResult(Status.SENT, message);
    }

    public static MailSendResult disabled() {
        return new MailSendResult(Status.DISABLED, "Envoi désactivé (mail.enabled=false).");
    }

    public static MailSendResult noRecipients() {
        return new MailSendResult(Status.NO_RECIPIENTS, "Aucun destinataire (email non vide + is_blocked=0).");
    }

    public static MailSendResult configError(String message) {
        return new MailSendResult(Status.CONFIG_ERROR, message);
    }

    public static MailSendResult failed(String message) {
        return new MailSendResult(Status.FAILED, message);
    }
}
