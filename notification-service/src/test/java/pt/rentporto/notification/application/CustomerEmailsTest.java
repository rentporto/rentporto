package pt.rentporto.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.notification.domain.Notification;
import pt.rentporto.notification.domain.NotificationLog;
import pt.rentporto.notification.domain.NotificationStatus;
import pt.rentporto.notification.domain.Recipient;
import pt.rentporto.notification.domain.Recipients;
import pt.rentporto.notification.domain.UnknownRecipientException;

class CustomerEmailsTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneId.of("Europe/Lisbon"));
    private static final UUID ANA = UUID.fromString("3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b");

    private final InMemoryRecipients recipients = new InMemoryRecipients();
    private final InMemoryLog log = new InMemoryLog();
    private final FakeMailer mailer = new FakeMailer();
    private final CustomerEmails emails =
            new CustomerEmails(recipients, log, mailer, CLOCK, mock(PlatformTransactionManager.class));

    @Test
    void aRegisteredCustomerIsWelcomedAndRemembered() {
        Notice welcome = notice("CustomerRegistered");

        assertThat(emails.customerRegistered(ANA, "Ana Ferreira", "ana@rentporto.pt", welcome)).isTrue();

        assertThat(recipients.withId(ANA)).get().extracting(Recipient::email).isEqualTo("ana@rentporto.pt");
        assertThat(mailer.sent).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo("ana@rentporto.pt");
            assertThat(email.subject()).isEqualTo("Subject of CustomerRegistered");
            assertThat(email.messageId()).isEqualTo("<" + welcome.eventId() + "@rentporto.pt>");
        });
    }

    @Test
    void aNewRegistrationUpdatesTheAddress() {
        emails.customerRegistered(ANA, "Ana Ferreira", "ana@rentporto.pt", notice("CustomerRegistered"));
        emails.customerRegistered(ANA, "Ana Sofia Ferreira", "ana.sofia@rentporto.pt", notice("CustomerRegistered"));

        emails.send(notice("ReservationConfirmed"));

        assertThat(mailer.sent.getLast().to()).isEqualTo("ana.sofia@rentporto.pt");
    }

    @Test
    void anEventIsSentOnceAndLoggedAsSent() {
        recipients.save(Recipient.of(ANA, "Ana Ferreira", "ana@rentporto.pt"));
        Notice confirmed = notice("ReservationConfirmed");

        assertThat(emails.send(confirmed)).isTrue();
        assertThat(emails.send(confirmed)).isFalse();

        assertThat(mailer.sent).hasSize(1);
        assertThat(log.forEvent(confirmed.eventId())).get()
                .extracting(Notification::status, Notification::attempts, Notification::attemptedAt)
                .containsExactly(NotificationStatus.SENT, 1, CLOCK.instant());
    }

    @Test
    void aFailedEmailIsLoggedAsFailedAndSentWhenTheEventIsTriedAgain() {
        recipients.save(Recipient.of(ANA, "Ana Ferreira", "ana@rentporto.pt"));
        Notice confirmed = notice("ReservationConfirmed");
        mailer.failing = true;

        assertThatThrownBy(() -> emails.send(confirmed)).isInstanceOf(EmailNotSentException.class);
        assertThat(log.forEvent(confirmed.eventId())).get()
                .extracting(Notification::status, Notification::lastError)
                .containsExactly(NotificationStatus.FAILED, "Mail server down");

        mailer.failing = false;
        assertThat(emails.send(confirmed)).isTrue();
        assertThat(log.forEvent(confirmed.eventId())).get()
                .extracting(Notification::status, Notification::attempts, Notification::lastError)
                .containsExactly(NotificationStatus.SENT, 2, null);
        assertThat(log.forCustomer(ANA)).hasSize(1);
    }

    @Test
    void aCustomerTheServiceDoesNotKnowYetIsNotSentAnything() {
        assertThatThrownBy(() -> emails.send(notice("PaymentFailed")))
                .isInstanceOf(UnknownRecipientException.class);
        assertThat(mailer.sent).isEmpty();
        assertThat(log.forCustomer(ANA)).isEmpty();
    }

    private static Notice notice(String type) {
        return new Notice(UUID.randomUUID(), ANA, type, "Subject of " + type, "Text of " + type);
    }

    /**
     * Recipients in memory.
     */
    private static final class InMemoryRecipients implements Recipients {

        private final Map<UUID, Recipient> byId = new HashMap<>();

        @Override
        public Optional<Recipient> withId(UUID customerId) {
            return Optional.ofNullable(byId.get(customerId));
        }

        @Override
        public void save(Recipient recipient) {
            byId.put(recipient.id(), recipient);
        }
    }

    /**
     * Notification log in memory.
     */
    private static final class InMemoryLog implements NotificationLog {

        private final Map<UUID, Notification> byEvent = new HashMap<>();

        @Override
        public Optional<Notification> forEvent(UUID eventId) {
            return Optional.ofNullable(byEvent.get(eventId));
        }

        @Override
        public List<Notification> forCustomer(UUID customerId) {
            return byEvent.values().stream()
                    .filter(notification -> notification.customerId().equals(customerId))
                    .sorted(Comparator.comparing(Notification::attemptedAt).reversed())
                    .toList();
        }

        @Override
        public void save(Notification notification) {
            byEvent.put(notification.eventId(), notification);
        }
    }

    /**
     * Mailer that keeps what it sends, or fails while {@code failing}.
     */
    private static final class FakeMailer implements Mailer {

        private final List<Email> sent = new ArrayList<>();
        private boolean failing;

        @Override
        public void send(Email email) {
            if (failing) {
                throw new EmailNotSentException("Mail server down", null);
            }
            sent.add(email);
        }
    }
}
