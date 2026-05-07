package pi.services.MailingService;

import pi.entities.Cours;
import pi.entities.Quiz;
import pi.services.UserTransactionService.UserService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LearningNotificationMailer {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "learning-mailer");
        t.setDaemon(true);
        return t;
    });

    private final UserService userService;
    private final EmailSender emailSender;
    private final boolean popupEnabled;

    public LearningNotificationMailer(UserService userService, EmailSender emailSender) {
        this(userService, emailSender, false);
    }

    public LearningNotificationMailer(UserService userService, EmailSender emailSender, boolean popupEnabled) {
        this.userService = userService;
        this.emailSender = emailSender;
        this.popupEnabled = popupEnabled;
    }

    public static LearningNotificationMailer defaultInstance() {
        MailSettings settings = MailSettingsLoader.load();
        return new LearningNotificationMailer(
                new UserService(),
                new JakartaMailEmailSender(settings),
                settings.isDebugPopup()
        );
    }

    public CompletableFuture<MailSendResult> notifyCourseAddedAsync(Cours cours) {
        return CompletableFuture.supplyAsync(() -> notifyCourseAdded(cours), EXECUTOR)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.out.println("[MAIL] Echec notification cours: " + ex.getMessage());
                    } else if (result != null) {
                        System.out.println("[MAIL] Notification cours status=" + result.getStatus() + " message=" + result.getMessage());
                    }
                    if (!popupEnabled) {
                        return;
                    }
                    if (ex != null) {
                        MailFxPopup.showResult(MailSendResult.failed("Erreur email cours: " + ex.getMessage()));
                    } else {
                        MailFxPopup.showResult(result);
                    }
                });
    }

    public CompletableFuture<MailSendResult> notifyQuizAddedAsync(Quiz quiz, String coursTitreFallback) {
        return CompletableFuture.supplyAsync(() -> notifyQuizAdded(quiz, coursTitreFallback), EXECUTOR)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        System.out.println("[MAIL] Echec notification quiz: " + ex.getMessage());
                    } else if (result != null) {
                        System.out.println("[MAIL] Notification quiz status=" + result.getStatus() + " message=" + result.getMessage());
                    }
                    if (!popupEnabled) {
                        return;
                    }
                    if (ex != null) {
                        MailFxPopup.showResult(MailSendResult.failed("Erreur email quiz: " + ex.getMessage()));
                    } else {
                        MailFxPopup.showResult(result);
                    }
                });
    }

    public MailSendResult notifyCourseAdded(Cours cours) {
        if (cours == null) {
            return MailSendResult.failed("Cours null.");
        }

        List<String> recipients = userService.findLearningNotificationRecipients();
        if (recipients == null || recipients.isEmpty()) {
            System.out.println("[MAIL] Aucun destinataire (email non vide + is_blocked=0).");
            return MailSendResult.noRecipients();
        }

        System.out.println("[MAIL] Notification cours -> " + recipients.size() + " destinataire(s).");
        String subject = "Nouveau cours ajouté : " + safe(cours.getTitre());
        String body = """
                Bonjour,

                Un nouveau cours vient d’être ajouté sur l’application.

                Titre : %s
                Type média : %s
                Date : %s

                À bientôt.
                """.formatted(
                safe(cours.getTitre()),
                safe(cours.getTypeMedia()),
                now()
        );

        return emailSender.sendBcc(recipients, subject, body);
    }

    public MailSendResult notifyQuizAdded(Quiz quiz, String coursTitreFallback) {
        if (quiz == null) {
            return MailSendResult.failed("Quiz null.");
        }

        List<String> recipients = userService.findLearningNotificationRecipients();
        if (recipients == null || recipients.isEmpty()) {
            System.out.println("[MAIL] Aucun destinataire (email non vide + is_blocked=0).");
            return MailSendResult.noRecipients();
        }

        System.out.println("[MAIL] Notification quiz -> " + recipients.size() + " destinataire(s).");
        String coursTitre = safe(quiz.getCours() != null ? quiz.getCours().getTitre() : null);
        if (coursTitre.isBlank()) {
            coursTitre = safe(coursTitreFallback);
        }

        String subject = coursTitre.isBlank()
                ? "Nouveau quiz ajouté"
                : "Nouveau quiz ajouté (" + coursTitre + ")";

        String body = """
                Bonjour,

                Un nouveau quiz vient d’être ajouté sur l’application.

                Cours : %s
                Question : %s
                Points : %s
                Date : %s

                À bientôt.
                """.formatted(
                coursTitre.isBlank() ? "—" : coursTitre,
                safe(quiz.getQuestion()),
                quiz.getPointsValeur(),
                now()
        );

        return emailSender.sendBcc(recipients, subject, body);
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
