package pi.services.MailingService;

import java.util.List;

public interface EmailSender {
    MailSendResult sendBcc(List<String> recipients, String subject, String bodyText);
}
