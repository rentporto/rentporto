package pt.rentporto.booking.payment.api;

/**
 * State of a payment.
 */
public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    REFUNDED
}
