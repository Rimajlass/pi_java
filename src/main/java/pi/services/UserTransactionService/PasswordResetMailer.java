package pi.services.UserTransactionService;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Properties;

public class PasswordResetMailer {

    private final SmtpConfig config;

    public PasswordResetMailer(SmtpConfig config) {
        this.config = config;
    }

    public void sendResetMail(String toEmail, String resetLink) {
        if (!config.isReady()) {
            throw new IllegalStateException("Configuration SMTP manquante: " + buildMissingConfigMessage());
        }

        try {
            Session session = createSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.from()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Reset your password");
            message.setText(buildPlainText(resetLink));
            Transport.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Erreur lors de l'envoi de l'email de r\u00e9initialisation.", e);
        }
    }

    private String buildMissingConfigMessage() {
        StringBuilder missing = new StringBuilder();
        if (config.host() == null || config.host().isBlank()) {
            appendMissing(missing, "SMTP_HOST");
        }
        if (config.from() == null || config.from().isBlank()) {
            appendMissing(missing, "SMTP_FROM");
        }
        if (config.auth()) {
            if (config.username() == null || config.username().isBlank()) {
                appendMissing(missing, "SMTP_USERNAME");
            }
            if (config.password() == null || config.password().isBlank()) {
                appendMissing(missing, "SMTP_PASSWORD");
            }
        }
        if (missing.length() == 0) {
            return "valeurs invalides";
        }
        return missing.toString();
    }

    private void appendMissing(StringBuilder builder, String key) {
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(key);
    }

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.host());
        props.put("mail.smtp.port", String.valueOf(config.port()));
        props.put("mail.smtp.auth", String.valueOf(config.auth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(config.startTls()));

        if (config.auth()) {
            return Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.username(), config.password());
                }
            });
        }
        return Session.getInstance(props);
    }

    private String buildPlainText(String resetLink) {
        return "A password reset has been requested for your account.\n\n"
                + "Use the following link to reset your password:\n"
                + resetLink + "\n\n"
                + "If you did not request this change, ignore this email.";
    }
}
