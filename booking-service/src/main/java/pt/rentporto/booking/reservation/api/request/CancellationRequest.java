package pt.rentporto.booking.reservation.api.request;

/**
 * Cancellation of a reservation.
 *
 * @param reason why the reservation is cancelled; optional
 */
public record CancellationRequest(String reason) {
}
