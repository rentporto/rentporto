package pt.rentporto.booking.payment.api.model;

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
