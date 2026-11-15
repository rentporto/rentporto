package pt.rentporto.events.reservation;

import java.util.Objects;
import java.util.UUID;
import pt.rentporto.events.Money;

/**
 * A confirmed reservation was cancelled by the customer or an agent.
 *
 * <p>Published by the reservation module of the booking-service on {@code reservation.cancelled}; consumed by the
 * rental-service, which discards the pending agreement, and by the notification-service.
 *
 * @param reservationId   reservation identifier
 * @param customerId      customer who booked
 * @param cancellationFee penalty kept: zero up to 48 hours before pick-up, one day afterwards
 * @param refundAmount    amount refunded to the customer
 */
public record ReservationCancelled(UUID reservationId, UUID customerId, Money cancellationFee, Money refundAmount) {

    public ReservationCancelled {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(cancellationFee, "cancellationFee");
        Objects.requireNonNull(refundAmount, "refundAmount");
    }
}
