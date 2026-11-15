package pt.rentporto.events.payment;

import java.util.Objects;
import java.util.UUID;
import pt.rentporto.events.Money;

/**
 * Part or all of the advance payment of a reservation was refunded.
 *
 * <p>Published by the payment module of the booking-service on {@code payment.refunded}; consumed by the
 * notification-service.
 *
 * @param paymentId     payment identifier
 * @param reservationId reservation the payment belongs to
 * @param customerId    customer who booked
 * @param amount        amount refunded
 */
public record PaymentRefunded(UUID paymentId, UUID reservationId, UUID customerId, Money amount) {

    public PaymentRefunded {
        Objects.requireNonNull(paymentId, "paymentId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(amount, "amount");
    }
}
