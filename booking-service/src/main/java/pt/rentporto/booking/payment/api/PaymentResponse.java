package pt.rentporto.booking.payment.api;

import java.util.UUID;
import pt.rentporto.events.Money;

/**
 * Payment made for a reservation.
 *
 * @param paymentId     payment identifier
 * @param reservationId reservation the payment belongs to
 * @param type          what the payment is for
 * @param status        current state
 * @param amount        amount of the payment
 */
public record PaymentResponse(
        UUID paymentId, UUID reservationId, PaymentType type, PaymentStatus status, Money amount) {
}
