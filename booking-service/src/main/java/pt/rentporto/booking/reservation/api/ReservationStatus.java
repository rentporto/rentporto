package pt.rentporto.booking.reservation.api;

/**
 * State of a reservation; it is also the state of the booking saga.
 */
public enum ReservationStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PAYMENT_FAILED,
    EXPIRED,
    CANCELLED,
    NO_SHOW,
    FULFILLED
}
