package pt.rentporto.events.reservation;

import java.util.Objects;
import java.util.UUID;

/**
 * A reservation expired before its payment was completed.
 *
 * <p>Published by the reservation module of the booking-service on {@code reservation.expired}; consumed by the
 * notification-service.
 *
 * @param reservationId reservation identifier
 * @param customerId    customer who booked
 * @param reason        why the reservation expired, e.g. {@code PAYMENT_TIMEOUT}
 */
public record ReservationExpired(UUID reservationId, UUID customerId, String reason) {

    public ReservationExpired {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(reason, "reason");
    }
}
