package courquizTests;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pi.entities.Cours;
import pi.entities.Quiz;
import pi.entities.User;
import pi.services.MailingService.EmailSender;
import pi.services.MailingService.LearningNotificationMailer;
import pi.services.UserTransactionService.UserService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LearningNotificationMailerTest {

    @Test
    void notifyCourseAdded_sendsBccToStudents() {
        UserService userService = mock(UserService.class);
        EmailSender emailSender = mock(EmailSender.class);
        when(userService.findLearningNotificationRecipients()).thenReturn(List.of("a@example.com", "b@example.com"));

        LearningNotificationMailer mailer = new LearningNotificationMailer(userService, emailSender);
        Cours cours = new Cours(new User(), "Java Basics", "Contenu du cours assez long pour être valide........", "pdf", "http://x");

        mailer.notifyCourseAdded(cours);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendBcc(anyList(), subjectCaptor.capture(), anyString());
        assertTrue(subjectCaptor.getValue().contains("Java Basics"));
    }

    @Test
    void notifyQuizAdded_usesCoursTitleFallback() {
        UserService userService = mock(UserService.class);
        EmailSender emailSender = mock(EmailSender.class);
        when(userService.findLearningNotificationRecipients()).thenReturn(List.of("a@example.com"));

        LearningNotificationMailer mailer = new LearningNotificationMailer(userService, emailSender);

        Cours cours = new Cours();
        cours.setId(12);
        Quiz quiz = new Quiz(cours, new User(), "Question ?", "A;B;C", "A", 5);

        mailer.notifyQuizAdded(quiz, "Cours Réseau");

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendBcc(anyList(), subjectCaptor.capture(), anyString());
        assertTrue(subjectCaptor.getValue().contains("Cours Réseau"));
    }
}
