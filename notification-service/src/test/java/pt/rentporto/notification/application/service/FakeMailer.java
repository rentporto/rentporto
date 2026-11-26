package pt.rentporto.notification.application.service;

import java.util.ArrayList;
import java.util.List;
import pt.rentporto.notification.application.exception.EmailNotSentException;
import pt.rentporto.notification.application.port.Mailer;

/**
 * Mailer that keeps what it sends, or fails while {@code failing}.
 */
final class FakeMailer implements Mailer {

    final List<Email> sent = new ArrayList<>();
    boolean failing;

    @Override
    public void send(Email email) {
        if (failing) {
            throw new EmailNotSentException("Mail server down", null);
        }
        sent.add(email);
    }
}
