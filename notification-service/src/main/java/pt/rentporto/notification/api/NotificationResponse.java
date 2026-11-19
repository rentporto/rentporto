package pt.rentporto.notification.api;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import pt.rentporto.notification.domain.Notification;

/**
 * Notification sent to a customer.
 *
 * @param notificationId notification identifier
 * @param customerId     customer the notification was sent to
 * @param type           event that triggered it, e.g. {@code ReservationConfirmed}
 * @param subject        subject of the email
 * @param sentAt         date-time of the last attempt to send it
 * @param status         outcome of the sending
 */
public record NotificationResponse(
        UUID notificationId,
        UUID customerId,
        String type,
        String subject,
        OffsetDateTime sentAt,
        NotificationStatus status) {

    static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.id(),
                notification.customerId(),
                notification.type(),
                notification.subject(),
                notification.attemptedAt().atOffset(ZoneOffset.UTC),
                NotificationStatus.valueOf(notification.status().name()));
    }
}
