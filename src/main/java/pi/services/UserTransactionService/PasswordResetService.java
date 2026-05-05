package pi.services.UserTransactionService;

import pi.entities.User;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PasswordResetService {

    private final UserService userService;
    private final PasswordResetMailer mailer;
    private final SmtpConfig smtpConfig;

    public PasswordResetService() {
        this.userService = new UserService();
        this.smtpConfig = SmtpConfig.fromEnvironment();
        this.mailer = new PasswordResetMailer(smtpConfig);
    }

    public void requestReset(String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        if (email.isBlank()) {
            return;
        }

        User user = userService.findByEmail(email);
        if (user == null) {
            return;
        }

        String token = UUID.randomUUID().toString();
        String link = buildResetLink(token);
        mailer.sendResetMail(email, link);
    }

    private String buildResetLink(String token) {
        String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
        String separator = smtpConfig.appBaseUrl().contains("?") ? "&" : "?";
        return smtpConfig.appBaseUrl() + separator + "token=" + encodedToken;
    }
}
