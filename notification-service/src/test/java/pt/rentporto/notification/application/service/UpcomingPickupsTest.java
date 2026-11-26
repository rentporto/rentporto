package pt.rentporto.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.notification.application.model.Notice;
import pt.rentporto.notification.application.port.Mailer;
import pt.rentporto.notification.application.port.ReminderTemplate;
import pt.rentporto.notification.domain.model.Notification;
import pt.rentporto.notification.domain.model.NotificationStatus;
import pt.rentporto.notification.domain.model.PickupReminder;
import pt.rentporto.notification.domain.model.PickupReminderStatus;
import pt.rentporto.notification.domain.model.Recipient;

class UpcomingPickupsTest {

    private static final UUID ANA = UUID.fromString("3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b");
    private static final Instant CONFIRMED_AT = Instant.parse("2026-11-20T10:00:00Z");
    private static final Instant PICKUP_AT = Instant.parse("2026-11-23T10:00:00Z");
    private static final Duration DAY = Duration.ofHours(24);

    private final InMemoryRecipients recipients = new InMemoryRecipients();
    private final InMemoryLog log = new InMemoryLog();
    private final FakeMailer mailer = new FakeMailer();
    private final InMemoryPickupReminders reminders = new InMemoryPickupReminders();
    private final MovingClock clock = new MovingClock(CONFIRMED_AT);
    private final UUID reservation = UUID.randomUUID();

    UpcomingPickupsTest() {
        recipients.save(Recipient.of(ANA, "Ana Ferreira", "ana@rentporto.pt"));
    }

    @Test
    void aConfirmedReservationIsRemindedOnceWhenTheConfiguredTimeIsLeft() {
        UpcomingPickups pickups = pickups(DAY);
        pickups.reservationConfirmed(reservation, ANA, "OPO", PICKUP_AT, CONFIRMED_AT);

        clock.at(PICKUP_AT.minus(DAY).minusSeconds(1));
        assertThat(pickups.remindDue()).isZero();
        clock.at(PICKUP_AT.minus(DAY));
        assertThat(pickups.remindDue()).isEqualTo(1);
        clock.at(PICKUP_AT.minus(DAY).plusSeconds(60));
        assertThat(pickups.remindDue()).isZero();

        PickupReminder reminder = reminders.forReservation(reservation).orElseThrow();
        assertThat(reminder.status()).isEqualTo(PickupReminderStatus.SENT);
        assertThat(mailer.sent).singleElement().satisfies(email -> {
            assertThat(email.to()).isEqualTo("ana@rentporto.pt");
            assertThat(email.subject()).isEqualTo("Reminder of " + reservation);
            assertThat(email.messageId()).isEqualTo("<" + reminder.noticeId() + "@rentporto.pt>");
        });
        assertThat(log.forEvent(reminder.noticeId())).get()
                .extracting(Notification::type, Notification::status)
                .containsExactly("PickupReminder", NotificationStatus.SENT);
    }

    @Test
    void theTimeBeforeThePickUpIsConfigured() {
        UpcomingPickups pickups = pickups(Duration.ofHours(2));
        pickups.reservationConfirmed(reservation, ANA, "OPO", PICKUP_AT, CONFIRMED_AT);

        clock.at(PICKUP_AT.minus(DAY));
        assertThat(pickups.remindDue()).isZero();
        clock.at(PICKUP_AT.minus(Duration.ofHours(2)));
        assertThat(pickups.remindDue()).isEqualTo(1);
    }

    @Test
    void aReservationCancelledOrPickedUpBeforeItsReminderIsNotReminded() {
        UpcomingPickups pickups = pickups(DAY);
        UUID pickedUp = UUID.randomUUID();
        pickups.reservationConfirmed(reservation, ANA, "OPO", PICKUP_AT, CONFIRMED_AT);
        pickups.reservationConfirmed(pickedUp, ANA, "OPO", PICKUP_AT, CONFIRMED_AT);

        pickups.cancel(reservation, ANA);
        pickups.cancel(pickedUp, ANA);
        clock.at(PICKUP_AT.minus(DAY));

        assertThat(pickups.remindDue()).isZero();
        assertThat(mailer.sent).isEmpty();
        assertThat(reminders.forReservation(reservation)).get()
                .extracting(PickupReminder::status).isEqualTo(PickupReminderStatus.CANCELLED);
    }

    @Test
    void aCancellationHeardBeforeTheConfirmationStillCallsOffTheReminder() {
        UpcomingPickups pickups = pickups(DAY);

        pickups.cancel(reservation, ANA);
        pickups.reservationConfirmed(reservation, ANA, "OPO", PICKUP_AT, CONFIRMED_AT);
        clock.at(PICKUP_AT.minus(DAY));

        assertThat(pickups.remindDue()).isZero();
        assertThat(mailer.sent).isEmpty();
    }

    @Test
    void aReservationConfirmedWhenLessThanTheConfiguredTimeIsLeftHasNoReminder() {
        UpcomingPickups pickups = pickups(DAY);
        UUID justInTime = UUID.randomUUID();

        pickups.reservationConfirmed(reservation, ANA, "OPO", PICKUP_AT, PICKUP_AT.minus(DAY));
        pickups.reservationConfirmed(justInTime, ANA, "OPO", PICKUP_AT, PICKUP_AT.minus(DAY).minusSeconds(1));

        assertThat(reminders.forReservation(reservation)).isEmpty();
        assertThat(reminders.forReservation(justInTime)).isPresent();
    }

    @Test
    void aReminderNotSentBeforeThePickUpIsNotSentAfterIt() {
        UpcomingPickups pickups = pickups(DAY);
        pickups.reservationConfirmed(reservation, ANA, "OPO", PICKUP_AT, CONFIRMED_AT);

        clock.at(PICKUP_AT.plusSeconds(1));

        assertThat(pickups.remindDue()).isZero();
        assertThat(mailer.sent).isEmpty();
    }

    @Test
    void aReminderTheMailServerRefusesIsLoggedAsFailedAndSentOnTheNextRun() {
        UpcomingPickups pickups = pickups(DAY);
        pickups.reservationConfirmed(reservation, ANA, "OPO", PICKUP_AT, CONFIRMED_AT);
        clock.at(PICKUP_AT.minus(DAY));
        mailer.failing = true;

        assertThat(pickups.remindDue()).isZero();
        PickupReminder reminder = reminders.forReservation(reservation).orElseThrow();
        assertThat(reminder.status()).isEqualTo(PickupReminderStatus.PENDING);
        assertThat(log.forEvent(reminder.noticeId())).get()
                .extracting(Notification::status).isEqualTo(NotificationStatus.FAILED);

        mailer.failing = false;
        assertThat(pickups.remindDue()).isEqualTo(1);
        assertThat(log.forEvent(reminder.noticeId())).get()
                .extracting(Notification::status, Notification::attempts)
                .containsExactly(NotificationStatus.SENT, 2);
    }

    @Test
    void theOtherRemindersAreSentWhileOneWaitsForItsCustomer() {
        UpcomingPickups pickups = pickups(DAY);
        UUID stranger = UUID.randomUUID();
        pickups.reservationConfirmed(UUID.randomUUID(), stranger, "OPO", PICKUP_AT.minusSeconds(60), CONFIRMED_AT);
        pickups.reservationConfirmed(reservation, ANA, "OPO", PICKUP_AT, CONFIRMED_AT);
        clock.at(PICKUP_AT.minus(DAY));

        assertThat(pickups.remindDue()).isEqualTo(1);

        recipients.save(Recipient.of(stranger, "Rui Costa", "rui@rentporto.pt"));
        assertThat(pickups.remindDue()).isEqualTo(1);
        assertThat(mailer.sent).extracting(Mailer.Email::to)
                .containsExactly("ana@rentporto.pt", "rui@rentporto.pt");
    }

    @Test
    void aConfirmationDeliveredAgainKeepsTheReminderAsItIs() {
        UpcomingPickups pickups = pickups(DAY);
        pickups.reservationConfirmed(reservation, ANA, "OPO", PICKUP_AT, CONFIRMED_AT);
        clock.at(PICKUP_AT.minus(DAY));
        pickups.remindDue();

        pickups.reservationConfirmed(reservation, ANA, "OPO", PICKUP_AT, CONFIRMED_AT);
        pickups.cancel(reservation, ANA);

        assertThat(pickups.remindDue()).isZero();
        assertThat(reminders.forReservation(reservation)).get()
                .extracting(PickupReminder::status).isEqualTo(PickupReminderStatus.SENT);
        assertThat(mailer.sent).hasSize(1);
    }

    private UpcomingPickups pickups(Duration before) {
        CustomerEmails emails = new CustomerEmails(recipients, log, mailer, clock, mock(PlatformTransactionManager.class));
        ReminderTemplate template = reminder -> new Notice(reminder.noticeId(), reminder.customerId(),
                "PickupReminder", "Reminder of " + reminder.reservationId(), "Pick-up at " + reminder.pickupAt());
        return new UpcomingPickups(
                reminders, emails, template, clock, before, mock(PlatformTransactionManager.class));
    }

    /**
     * Clock the test moves.
     */
    private static final class MovingClock extends Clock {

        private Instant now;

        MovingClock(Instant now) {
            this.now = now;
        }

        void at(Instant instant) {
            now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("Europe/Lisbon");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
