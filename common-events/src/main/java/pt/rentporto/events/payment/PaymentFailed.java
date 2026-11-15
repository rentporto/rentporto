package pt.rentporto.events.payment;

import java.util.Objects;
import java.util.UUID;

/**
 * The advance payment of a reservation failed.
 *
 * <p>Published by the payment module of the booking-service on {@code payment.failed}; consumed by the
 * notification-service. The reservation module reacts to an internal event, not to this message.
 *
 * @param paymentId     payment identifier
 * @param reservationId reservation being paid
 * @param customerId    customer who booked
 * @param reason        why the payment failed, e.g. {@code CARD_DECLINED}
 */
public record PaymentFailed(UUID paymentId, UUID reservationId, UUID customerId, String reason) {

    public PaymentFailed {
        Objects.requireNonNull(paymentId, "paymentId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(reason, "reason");
    }
}
