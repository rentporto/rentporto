package pt.rentporto.notification.application;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.notification.domain.Notification;
import pt.rentporto.notification.domain.NotificationLog;
import pt.rentporto.notification.domain.Recipient;
import pt.rentporto.notification.domain.Recipients;
import pt.rentporto.notification.domain.UnknownRecipientException;

/**
 * Emails the customers what the events tell them, once per event. The email is outside the transactions of the
 * database, so the log is its own idempotency (ADR-07): an event already sent is skipped, a failed attempt is recorded
 * and tried again, and the {@code Message-ID} comes from the event, so even an email sent twice is the same message
 * to the mail clients.
 */
@Service
public class CustomerEmails {

    private final Recipients recipients;
    private final NotificationLog log;
    private final Mailer mailer;
    private final Clock clock;
    private final TransactionTemplate transaction;

    CustomerEmails(
            Recipients recipients,
            NotificationLog log,
            Mailer mailer,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.recipients = recipients;
        this.log = log;
        this.mailer = mailer;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Records the name and the email of a customer, for the emails of the next events, and welcomes them.
     *
     * @param customerId the customer
     * @param fullName   full name
     * @param email      email address
     * @param welcome    the welcome notice
     * @return {@code true} if the welcome was sent now
     */
    public boolean customerRegistered(UUID customerId, String fullName, String email, Notice welcome) {
        transaction.executeWithoutResult(status -> {
            Optional<Recipient> known = recipients.withId(customerId);
            Recipient recipient = known.orElseGet(() -> Recipient.of(customerId, fullName, email));
            known.ifPresent(existing -> existing.change(fullName, email));
            recipients.save(recipient);
        });
        return send(welcome);
    }

    /**
     * Sends a notice to its customer, unless the event already sent it.
     *
     * @param notice the notice
     * @return {@code true} if it was sent now, {@code false} if it had been sent before
     * @throws UnknownRecipientException if the service does not know the customer yet
     * @throws EmailNotSentException     if the mail server did not accept the email
     */
    public boolean send(Notice notice) {
        Optional<Notification> logged = transaction.execute(status -> log.forEvent(notice.eventId()));
        if (logged.isPresent() && logged.get().wasSent()) {
            return false;
        }
        Recipient recipient = transaction.execute(status -> recipients.withId(notice.customerId()))
                .orElseThrow(() -> new UnknownRecipientException(notice.customerId()));
        Notification notification = logged.orElseGet(() -> Notification.of(
                notice.eventId(), notice.customerId(), notice.type(), notice.subject(), recipient.email()));
        try {
            mailer.send(new Mailer.Email(
                    recipient.email(), notice.subject(), notice.body(), "<" + notice.eventId() + "@rentporto.pt>"));
        } catch (EmailNotSentException e) {
            notification.failed(clock.instant(), e.getMessage());
            transaction.executeWithoutResult(status -> log.save(notification));
            throw e;
        }
        notification.sent(clock.instant());
        transaction.executeWithoutResult(status -> log.save(notification));
        return true;
    }

    /**
     * Lists the notifications of a customer.
     *
     * @param customerId the customer
     * @return the notifications, most recent attempt first
     */
    public List<Notification> history(UUID customerId) {
        return log.forCustomer(customerId);
    }
}
