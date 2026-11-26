package pt.rentporto.notification.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.notification.application.exception.EmailNotSentException;
import pt.rentporto.notification.application.port.ReminderTemplate;
import pt.rentporto.notification.domain.exception.UnknownRecipientException;
import pt.rentporto.notification.domain.model.PickupReminder;
import pt.rentporto.notification.domain.repository.PickupReminders;

/**
 * Reminds the customers of their pick-up, a configured time before it. A reservation confirmed when less than that
 * time is left has no reminder, since its confirmation was just sent with the station and the time. A reservation
 * cancelled or picked up before its reminder is not reminded, even if the service hears of it before the
 * confirmation. A reminder that could not be sent is tried again on the next run, while the pick-up is ahead.
 */
public class UpcomingPickups {

    private static final Logger log = LoggerFactory.getLogger(UpcomingPickups.class);

    private final PickupReminders reminders;
    private final CustomerEmails emails;
    private final ReminderTemplate template;
    private final Clock clock;
    private final Duration before;
    private final TransactionTemplate transaction;

    /**
     * Creates the reminders of the pick-ups.
     *
     * @param reminders          stored reminders
     * @param emails             sends the emails and logs them
     * @param template           text of the reminder
     * @param clock              clock of the service
     * @param before             how long before the pick-up the reminder is sent
     * @param transactionManager transaction manager of the service
     */
    public UpcomingPickups(
            PickupReminders reminders,
            CustomerEmails emails,
            ReminderTemplate template,
            Clock clock,
            Duration before,
            PlatformTransactionManager transactionManager) {
        this.reminders = reminders;
        this.emails = emails;
        this.template = template;
        this.clock = clock;
        this.before = Objects.requireNonNull(before, "before");
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Keeps the reminder of a confirmed reservation, unless less than the configured time was left when it was
     * confirmed, or the reservation was already cancelled or picked up.
     *
     * @param reservationId the reservation
     * @param customerId    the customer who booked
     * @param pickupStation code of the pick-up station
     * @param pickupAt      when the customer picks up the vehicle
     * @param confirmedAt   when the reservation was confirmed
     */
    public void reservationConfirmed(
            UUID reservationId, UUID customerId, String pickupStation, Instant pickupAt, Instant confirmedAt) {
        if (!confirmedAt.isBefore(pickupAt.minus(before))) {
            return;
        }
        transaction.executeWithoutResult(status -> {
            if (reminders.forReservation(reservationId).isEmpty()) {
                reminders.save(PickupReminder.pending(reservationId, customerId, pickupStation, pickupAt));
            }
        });
    }

    /**
     * Calls off the reminder of a reservation cancelled or picked up, keeping it cancelled if the confirmation was not
     * heard of yet.
     *
     * @param reservationId the reservation
     * @param customerId    the customer who booked
     */
    public void cancel(UUID reservationId, UUID customerId) {
        transaction.executeWithoutResult(status -> {
            PickupReminder reminder = reminders.forReservation(reservationId)
                    .orElseGet(() -> PickupReminder.cancelled(reservationId, customerId));
            reminder.cancel();
            reminders.save(reminder);
        });
    }

    /**
     * Sends the reminders whose pick-up is within the configured time and still ahead. One that cannot be sent now is
     * left pending for the next run.
     *
     * @return how many reminders were sent
     */
    public int remindDue() {
        Instant now = clock.instant();
        List<PickupReminder> due = transaction.execute(status ->
                reminders.pendingWithPickupBetween(now, now.plus(before)));
        int sent = 0;
        for (PickupReminder reminder : due) {
            try {
                emails.send(template.pickupReminder(reminder));
            } catch (UnknownRecipientException | EmailNotSentException e) {
                log.warn("Pick-up reminder of reservation {} not sent, trying again on the next run: {}",
                        reminder.reservationId(), e.getMessage());
                continue;
            }
            reminder.sent();
            transaction.executeWithoutResult(status -> reminders.save(reminder));
            sent++;
        }
        return sent;
    }
}
