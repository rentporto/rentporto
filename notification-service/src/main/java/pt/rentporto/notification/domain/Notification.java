package pt.rentporto.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entry of the notification log: the email an event sent to a customer, or failed to send. There is one entry per
 * event, which keeps a redelivered event from sending a second email.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Column(name = "type", nullable = false, updatable = false)
    private String type;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationStatus status;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected Notification() {
    }

    /**
     * Creates the entry of an event, before its first attempt.
     *
     * @param eventId    the event
     * @param customerId the customer
     * @param type       type of the event, e.g. {@code ReservationConfirmed}
     * @param subject    subject of the email
     * @param email      address it goes to
     * @return the entry
     */
    public static Notification of(UUID eventId, UUID customerId, String type, String subject, String email) {
        Notification notification = new Notification();
        notification.id = UUID.randomUUID();
        notification.eventId = Objects.requireNonNull(eventId, "eventId");
        notification.customerId = Objects.requireNonNull(customerId, "customerId");
        notification.type = Objects.requireNonNull(type, "type");
        notification.subject = Objects.requireNonNull(subject, "subject");
        notification.email = Objects.requireNonNull(email, "email");
        return notification;
    }

    /**
     * Records an attempt the mail server accepted.
     *
     * @param at when
     */
    public void sent(Instant at) {
        attempt(at);
        status = NotificationStatus.SENT;
        lastError = null;
    }

    /**
     * Records an attempt that failed.
     *
     * @param at    when
     * @param error why
     */
    public void failed(Instant at, String error) {
        attempt(at);
        status = NotificationStatus.FAILED;
        lastError = error;
    }

    /**
     * Tells whether the email was sent, so the event must not send it again.
     *
     * @return {@code true} if an attempt was accepted
     */
    public boolean wasSent() {
        return status == NotificationStatus.SENT;
    }

    private void attempt(Instant at) {
        attempts++;
        attemptedAt = Objects.requireNonNull(at, "at");
    }

    public UUID id() {
        return id;
    }

    public UUID eventId() {
        return eventId;
    }

    public UUID customerId() {
        return customerId;
    }

    public String type() {
        return type;
    }

    public String subject() {
        return subject;
    }

    public String email() {
        return email;
    }

    public NotificationStatus status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }

    public String lastError() {
        return lastError;
    }

    public Instant attemptedAt() {
        return attemptedAt;
    }
}
