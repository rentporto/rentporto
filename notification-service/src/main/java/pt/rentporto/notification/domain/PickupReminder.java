package pt.rentporto.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Reminder of the pick-up of a confirmed reservation, one per reservation. A reservation cancelled or picked up before
 * the service hears of its confirmation gets a cancelled reminder without a pick-up, so that the confirmation that
 * comes later does not schedule one.
 */
@Entity
@Table(name = "pickup_reminders")
public class PickupReminder {

    @Id
    @Column(name = "reservation_id")
    private UUID reservationId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "pickup_station", updatable = false)
    private String pickupStation;

    @Column(name = "pickup_at", updatable = false)
    private Instant pickupAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PickupReminderStatus status;

    protected PickupReminder() {
    }

    /**
     * Creates the reminder of a confirmed reservation, still to be sent.
     *
     * @param reservationId the reservation
     * @param customerId    the customer who booked
     * @param pickupStation code of the pick-up station
     * @param pickupAt      when the customer picks up the vehicle
     * @return the reminder
     */
    public static PickupReminder pending(UUID reservationId, UUID customerId, String pickupStation, Instant pickupAt) {
        PickupReminder reminder = new PickupReminder(reservationId, customerId, PickupReminderStatus.PENDING);
        reminder.pickupStation = Objects.requireNonNull(pickupStation, "pickupStation");
        reminder.pickupAt = Objects.requireNonNull(pickupAt, "pickupAt");
        return reminder;
    }

    /**
     * Creates the reminder of a reservation cancelled or picked up before its confirmation was heard of.
     *
     * @param reservationId the reservation
     * @param customerId    the customer who booked
     * @return the reminder, cancelled
     */
    public static PickupReminder cancelled(UUID reservationId, UUID customerId) {
        return new PickupReminder(reservationId, customerId, PickupReminderStatus.CANCELLED);
    }

    private PickupReminder(UUID reservationId, UUID customerId, PickupReminderStatus status) {
        this.reservationId = Objects.requireNonNull(reservationId, "reservationId");
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.status = status;
    }

    /**
     * Calls off the reminder, if it was not sent yet.
     */
    public void cancel() {
        if (status == PickupReminderStatus.PENDING) {
            status = PickupReminderStatus.CANCELLED;
        }
    }

    /**
     * Records that the reminder was sent.
     *
     * @throws IllegalStateException if it was not pending
     */
    public void sent() {
        if (status != PickupReminderStatus.PENDING) {
            throw new IllegalStateException("The reminder of reservation " + reservationId + " is " + status);
        }
        status = PickupReminderStatus.SENT;
    }

    /**
     * Identifies the email of the reminder in the notification log, as the event does for the other emails.
     *
     * @return an id derived from the reservation, the same on every run
     */
    public UUID noticeId() {
        return UUID.nameUUIDFromBytes(("pickup-reminder:" + reservationId).getBytes(StandardCharsets.UTF_8));
    }

    public UUID reservationId() {
        return reservationId;
    }

    public UUID customerId() {
        return customerId;
    }

    public String pickupStation() {
        return pickupStation;
    }

    public Instant pickupAt() {
        return pickupAt;
    }

    public PickupReminderStatus status() {
        return status;
    }
}
