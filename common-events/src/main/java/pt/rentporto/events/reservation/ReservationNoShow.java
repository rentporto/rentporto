package pt.rentporto.events.reservation;

import java.util.Objects;
import java.util.UUID;
import pt.rentporto.events.Money;

/**
 * The customer did not pick up the vehicle within 2 hours of the pick-up time.
 *
 * <p>Published by the reservation module of the booking-service on {@code reservation.no-show}; consumed by the
 * rental-service, which discards the pending agreement, and by the notification-service.
 *
 * @param reservationId  reservation identifier
 * @param customerId     customer who booked
 * @param retainedAmount amount kept by RentPorto; the rest is refunded
 */
public record ReservationNoShow(UUID reservationId, UUID customerId, Money retainedAmount) {

    public ReservationNoShow {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(retainedAmount, "retainedAmount");
    }
}
