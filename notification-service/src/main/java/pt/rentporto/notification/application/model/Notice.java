package pt.rentporto.notification.application.model;

import java.util.Objects;
import java.util.UUID;

/**
 * What an event tells a customer.
 *
 * @param eventId    the event, which identifies the notice
 * @param customerId the customer it is for
 * @param type       type of the event, e.g. {@code ReservationConfirmed}
 * @param subject    subject of the email
 * @param body       text of the email
 */
public record Notice(UUID eventId, UUID customerId, String type, String subject, String body) {

    public Notice {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(body, "body");
    }
}
