package pi.services.ImprevusCasreelService;

import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import pi.entities.CasRelles;
import pi.entities.User;
import pi.tools.AppEnv;

import java.util.List;
import java.util.Properties;

public class CaseNotificationService {

    private final LocationSuggestionService locationSuggestionService = new LocationSuggestionService();
    private final AppointmentSuggestionService appointmentSuggestionService = new AppointmentSuggestionService();

    public record EmailSendResult(boolean sent, String failureReason) {
        public static EmailSendResult ok() {
            return new EmailSendResult(true, null);
        }

        public static EmailSendResult fail(String reason) {
            return new EmailSendResult(false, reason);
        }
    }

    public boolean sendDecisionEmail(CasRelles cas, User user) {
        return sendDecisionEmailDetailed(cas, user).sent();
    }

    /**
     * Sends the decision email and returns a structured result (useful to show admin-facing errors).
     */
    /**
     * Proactive email when similar risk events repeat (health, car, home): suggests a monthly follow-up and nearby places.
     */
    public EmailSendResult sendRecurrenceRiskEmail(User user, String riskCategory, int occurrencesInWindow,
                                                   String monthlySuggestion, List<String> nearbyPlaces) {
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return EmailSendResult.fail("missing recipient email");
        }
        MailConfig config = MailConfig.load();
        if (!config.isComplete()) {
            return EmailSendResult.fail("SMTP config incomplete");
        }
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", config.tls ? "true" : "false");
            if (config.tls) {
                properties.put("mail.smtp.starttls.required", "true");
            }
            properties.put("mail.smtp.host", config.host);
            properties.put("mail.smtp.port", String.valueOf(config.port));
            properties.put("mail.smtp.ssl.trust", config.host);

            Session session = Session.getInstance(properties, new jakarta.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.username, config.password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.fromAddress, config.fromName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user.getEmail()));
            message.setSubject("Decide$ - suivi preventif (" + riskCategory + ")");
            message.setText(buildRecurrenceBody(user, riskCategory, occurrencesInWindow, monthlySuggestion, nearbyPlaces));
            Transport.send(message);
            return EmailSendResult.ok();
        } catch (Exception exception) {
            return EmailSendResult.fail(exception.getMessage());
        }
    }

    private String buildRecurrenceBody(User user, String riskCategory, int occurrences, String monthlySuggestion, List<String> nearbyPlaces) {
        String name = user.getNom() == null || user.getNom().isBlank() ? user.getEmail() : user.getNom();
        AppointmentSuggestionService.AppointmentSuggestion appointment = appointmentSuggestionService.suggest(riskCategory, "risque " + riskCategory, monthlySuggestion, user);
        String places = nearbyPlaces == null || nearbyPlaces.isEmpty()
                ? "(Activez LOCATIONIQ_API_KEY dans .env pour des suggestions de lieux pres de vous.)"
                : String.join("\n  - ", nearbyPlaces);
        return """
                Bonjour %s,

                Nous constatons que plusieurs evenements lies a "%s" sont survenus recemment sur votre compte (%d occurrences sur la periode analysee).

                D'apres la topologie des risques et votre historique, nous vous proposons immediatement :
                %s

                Un rendez-vous est recommande :
                %s
                Pourquoi : %s
                Types de lieux utiles : %s
                Ajouter au calendrier : %s

                Exemples de lieux / prestataires a proximite (selon vos APIs) :
                - %s

                Un rendez-vous mensuel (medical, entretien, inspection selon le cas) peut reduire les surprises et mieux lisser vos depenses.

                L'equipe Decide$
                """.formatted(name, riskCategory, occurrences, monthlySuggestion, appointment.description(), appointment.reason(), String.join(", ", appointment.placeTypesNeeded()), appointment.calendarUrl(), places);
    }

    public EmailSendResult sendDecisionEmailDetailed(CasRelles cas, User user) {
        if (cas == null || user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return EmailSendResult.fail("missing recipient email on case user");
        }

        MailConfig config = MailConfig.load();
        boolean mailDebug = AppEnv.has("MAIL_DEBUG");
        if (!config.isComplete()) {
            if (mailDebug) {
                System.err.println("[MAIL] incomplete SMTP config. Check MAILER_DSN (must include password) + MAILER_FROM_ADDRESS.");
                System.err.println("[MAIL] parsed host=" + config.host + " port=" + config.port + " tls=" + config.tls
                        + " user=" + config.username
                        + " passwordBlank=" + (config.password == null || config.password.isBlank()));
            }
            return EmailSendResult.fail("SMTP config incomplete (MAILER_DSN / MAILER_FROM_ADDRESS)");
        }

        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", config.tls ? "true" : "false");
            if (config.tls) {
                properties.put("mail.smtp.starttls.required", "true");
            }
            properties.put("mail.smtp.host", config.host);
            properties.put("mail.smtp.port", String.valueOf(config.port));
            properties.put("mail.smtp.ssl.trust", config.host);
            if (mailDebug) {
                properties.put("mail.debug", "true");
            }

            Session session = Session.getInstance(properties, new jakarta.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.username, config.password);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.fromAddress, config.fromName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(user.getEmail()));
            message.setSubject("Update on your real case: " + cas.getTitre());
            message.setText(buildBody(cas, user));
            Transport.send(message);
            return EmailSendResult.ok();
        } catch (Exception exception) {
            if (mailDebug) {
                System.err.println("[MAIL] failed to send decision email: " + exception.getMessage());
                exception.printStackTrace(System.err);
            }
            return EmailSendResult.fail(exception.getMessage());
        }
    }

    private String buildBody(CasRelles cas, User user) {
        String userName = user.getNom() == null || user.getNom().isBlank() ? user.getEmail() : user.getNom();
        String status = cas.getResultat() == null ? CasReelService.STATUT_EN_ATTENTE : cas.getResultat();
        String processedBy = cas.getConfirmedBy() == null
                ? "Admin"
                : (cas.getConfirmedBy().getNom() == null || cas.getConfirmedBy().getNom().isBlank()
                ? cas.getConfirmedBy().getEmail()
                : cas.getConfirmedBy().getNom());
        String city = user.getGeoCityName() == null || user.getGeoCityName().isBlank()
                ? defaultSuggestionCity()
                : user.getGeoCityName();
        String riskCategory = new CasReelService().inferRiskCategory(cas.getTitre(), cas.getDescription(), cas.getImprevus());
        AppointmentSuggestionService.AppointmentSuggestion appointment = appointmentSuggestionService.suggest(riskCategory, cas.getTitre(), cas.getDescription(), user);
        List<String> places = locationSuggestionService.suggestNearbyPlacesForNeeds(appointment.placeTypesNeeded(), city);
        String placesText = places == null || places.isEmpty()
                ? "Aucun lieu recommande pour le moment (verifie LOCATIONIQ_API_KEY dans .env)."
                : String.join(" | ", places);

        return """
                Bonjour %s,

                Votre cas reel "%s" a ete traite.

                Statut: %s
                Affectation: %s
                Traite par: %s
                Raison de refus: %s
                Note admin: %s

                Un rendez-vous est recommande :
                %s
                Pourquoi: %s
                Types de lieux utiles: %s
                Ajouter au calendrier : %s

                Meilleurs lieux suggeres :
                %s

                Decide$ Finance Bot
                """.formatted(
                userName,
                cas.getTitre(),
                status,
                cas.getPaymentMethod() == null ? "-" : cas.getPaymentMethod(),
                processedBy == null ? "Admin" : processedBy,
                cas.getRaisonRefus() == null || cas.getRaisonRefus().isBlank() ? "-" : cas.getRaisonRefus(),
                cas.getAdminNote() == null || cas.getAdminNote().isBlank() ? "-" : cas.getAdminNote(),
                appointment.description(),
                appointment.reason(),
                String.join(", ", appointment.placeTypesNeeded()),
                appointment.calendarUrl(),
                placesText
        );
    }

    private String defaultSuggestionCity() {
        String city = AppEnv.get("DEFAULT_SUGGESTION_CITY");
        return city == null || city.isBlank() ? "Tunis" : city.trim();
    }

    private static final class MailConfig {
        private final String host;
        private final int port;
        private final boolean tls;
        private final String username;
        private final String password;
        private final String fromAddress;
        private final String fromName;

        private MailConfig(String host, int port, boolean tls, String username, String password, String fromAddress, String fromName) {
            this.host = host;
            this.port = port;
            this.tls = tls;
            this.username = username;
            this.password = password;
            this.fromAddress = fromAddress;
            this.fromName = fromName;
        }

        private static MailConfig load() {
            boolean mailDebug = AppEnv.has("MAIL_DEBUG");
            String dsn = AppEnv.get("MAILER_DSN");
            String fromAddress = AppEnv.get("MAILER_FROM_ADDRESS");
            String fromName = AppEnv.get("MAILER_FROM_NAME");
            if (dsn == null || dsn.isBlank()) {
                if (mailDebug) {
                    System.err.println("[MAIL] MAILER_DSN is missing/blank in environment/system properties/.env");
                    System.err.println("[MAIL] MAILER_FROM_ADDRESS present=" + (fromAddress != null && !fromAddress.isBlank()));
                }
                return new MailConfig(null, 0, true, null, null, fromAddress, fromName);
            }
            try {
                String normalized = dsn.trim();
                normalized = normalized.startsWith("smtp://") ? normalized.substring("smtp://".length()) : normalized;

                // Symfony MAILER_DSN allows an unencoded '@' inside the username (email).
                // Therefore we must split CREDENTIALS vs HOST using the LAST '@' before the query string.
                String base = normalized;
                String query = null;
                int q = normalized.indexOf('?');
                if (q >= 0) {
                    base = normalized.substring(0, q);
                    query = normalized.substring(q + 1);
                }

                int at = base.lastIndexOf('@');
                if (at <= 0 || at == base.length() - 1) {
                    if (mailDebug) {
                        System.err.println("[MAIL] MAILER_DSN parse failed: expected user:pass@host:port (missing '@' separator)");
                    }
                    return new MailConfig(null, 0, true, null, null, fromAddress, fromName);
                }

                String credentialsPart = base.substring(0, at);
                String hostPart = base.substring(at + 1);

                String[] credentials = credentialsPart.split(":", 2);
                String[] hostAndPort = hostPart.split(":", 2);
                String host = hostAndPort[0];
                int port = hostAndPort.length > 1 ? Integer.parseInt(hostAndPort[1]) : 587;
                boolean tls = query == null || query.contains("encryption=tls");
                String username = java.net.URLDecoder.decode(credentials[0], java.nio.charset.StandardCharsets.UTF_8);
                String password = credentials.length > 1
                        ? java.net.URLDecoder.decode(credentials[1], java.nio.charset.StandardCharsets.UTF_8)
                        : "";
                return new MailConfig(
                        host,
                        port,
                        tls,
                        username,
                        password,
                        fromAddress == null || fromAddress.isBlank() ? username : fromAddress,
                        fromName == null || fromName.isBlank() ? "Decide$" : fromName
                );
            } catch (Exception exception) {
                if (mailDebug) {
                    System.err.println("[MAIL] MAILER_DSN parse threw: " + exception.getMessage());
                }
                return new MailConfig(null, 0, true, null, null, fromAddress, fromName);
            }
        }

        private boolean isComplete() {
            return host != null && !host.isBlank()
                    && username != null && !username.isBlank()
                    && password != null && !password.isBlank()
                    && fromAddress != null && !fromAddress.isBlank();
        }
    }
}

