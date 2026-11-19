package pt.rentporto.notification.application;

import java.util.ArrayList;
import java.util.List;

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
