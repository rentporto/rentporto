package pt.rentporto.notification.api;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Notification sent to a customer.
 *
 * @param notificationId notification identifier
 * @param customerId     customer the notification was sent to
 * @param type           event that triggered it, e.g. {@code ReservationConfirmed}
 * @param subject        subject of the email
 * @param sentAt         date-time the notification was sent
 * @param status         outcome of the sending
 */
public record NotificationResponse(
        UUID notificationId,
        UUID customerId,
        String type,
        String subject,
        OffsetDateTime sentAt,
        NotificationStatus status) {
}
