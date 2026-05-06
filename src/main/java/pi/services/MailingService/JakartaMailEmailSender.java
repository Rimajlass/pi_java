package pi.services.MailingService;

import jakarta.mail.Authenticator;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class JakartaMailEmailSender implements EmailSender {

    private final MailSettings settings;

    public JakartaMailEmailSender(MailSettings settings) {
        this.settings = settings;

        // 🔥 Force Java à utiliser IPv4 (corrige bugs réseau JVM)
        System.setProperty("java.net.preferIPv4Stack", "true");

        // 🔥 Force résolution DNS Java (corrige UnknownHostException)
        System.setProperty("sun.net.spi.nameservice.provider.1", "dns,sun");
    }

    @Override
    public MailSendResult sendBcc(List<String> recipients, String subject, String bodyText) {

        System.out.println("=== DEBUG MAIL CONFIG ===");
        System.out.println("HOST = " + settings.getHost());
        System.out.println("USERNAME = " + settings.getUsername());
        System.out.println("PASSWORD = " + (settings.getPassword() != null ? "****" : "NULL"));
        System.out.println("=========================");

        if (!settings.isEnabled()) {
            System.out.println("[MAIL] Envoi désactivé.");
            return MailSendResult.disabled();
        }

        List<String> sanitized = recipients == null ? List.of() : recipients.stream()
                                                                  .filter(email -> email != null && !email.isBlank())
                                                                  .map(String::trim)
                                                                  .distinct()
                                                                  .collect(Collectors.toList());

        if (sanitized.isEmpty()) {
            return MailSendResult.noRecipients();
        }

        // 🔴 Vérifications
        if (settings.getHost() == null || settings.getHost().isBlank()) {
            return MailSendResult.configError("mail.smtp.host manquant.");
        }
        if (settings.getUsername() == null || !settings.getUsername().contains("@")) {
            return MailSendResult.configError("Username invalide (doit être email complet).");
        }
        if (settings.getPassword() == null || settings.getPassword().isBlank()) {
            return MailSendResult.configError("Mot de passe manquant.");
        }
        if (settings.getFrom() == null || settings.getFrom().isBlank()) {
            return MailSendResult.configError("mail.from manquant.");
        }

        Properties props = new Properties();

        // ✅ Config SMTP Gmail
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        props.put("mail.smtp.host", settings.getHost());
        props.put("mail.smtp.port", "587");

        // 🔐 Sécurité / SSL
        props.put("mail.smtp.ssl.enable", "false");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        // ⏱️ Timeouts
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        settings.getUsername(),
                        settings.getPassword()
                );
            }
        });

        session.setDebug(settings.isDebug());

        try {
            MimeMessage message = new MimeMessage(session);

            // Expéditeur
            if (settings.getFromName() != null && !settings.getFromName().isBlank()) {
                message.setFrom(new InternetAddress(
                        settings.getFrom(),
                        settings.getFromName(),
                        StandardCharsets.UTF_8.name()
                ));
            } else {
                message.setFrom(new InternetAddress(settings.getFrom()));
            }

            message.setSubject(subject, StandardCharsets.UTF_8.name());
            message.setText(bodyText, StandardCharsets.UTF_8.name());

            // Destinataires BCC
            InternetAddress[] bcc = sanitized.stream().map(email -> {
                try {
                    return new InternetAddress(email);
                } catch (Exception e) {
                    return null;
                }
            }).filter(addr -> addr != null).toArray(InternetAddress[]::new);

            if (bcc.length == 0) {
                return MailSendResult.failed("Aucune adresse valide.");
            }

            message.setRecipients(Message.RecipientType.BCC, bcc);

            // 🚀 Envoi
            Transport transport = session.getTransport("smtp");

            transport.connect(
                    settings.getHost(),
                    settings.getUsername(),
                    settings.getPassword()
            );

            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

            System.out.println("[MAIL] ✅ Email envoyé à " + bcc.length + " destinataire(s)");
            return MailSendResult.sent("Email envoyé avec succès.");

        } catch (AuthenticationFailedException e) {
            String msg = """
❌ Authentification Gmail échouée.

✔️ Solution :
- Active la validation en 2 étapes
- Génère un App Password
- Utilise ce mot de passe ici
""";
            System.out.println(msg);
            e.printStackTrace();
            return MailSendResult.failed(msg);

        } catch (MessagingException e) {
            String msg = "[MAIL] Erreur SMTP: " + e.getMessage();
            System.out.println(msg);
            e.printStackTrace();
            return MailSendResult.failed(msg);

        } catch (Exception e) {
            String msg = "[MAIL] Erreur générale: " + e.getMessage();
            System.out.println(msg);
            e.printStackTrace();
            return MailSendResult.failed(msg);
        }
    }
}