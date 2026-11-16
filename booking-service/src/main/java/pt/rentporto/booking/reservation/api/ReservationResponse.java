package pt.rentporto.booking.reservation.api;

import java.time.OffsetDateTime;
import java.util.UUID;
import pt.rentporto.events.Money;
import pt.rentporto.events.RentalPeriod;

/**
 * Reservation and its state.
 *
 * @param reservationId   reservation identifier
 * @param status          current state
 * @param customerId      customer who booked
 * @param quoteId         quote that was booked
 * @param categoryCode    ACRISS code of the vehicle category
 * @param pickupStation   code of the pick-up station
 * @param returnStation   code of the return station
 * @param rentalPeriod    pick-up and return date-times
 * @param total           amount paid in advance
 * @param paymentDeadline instant the reservation expires if not paid; set in {@code PENDING_PAYMENT}
 * @param cancellationFee penalty kept; set in {@code CANCELLED}
 * @param refundAmount    amount refunded; set in {@code CANCELLED} and {@code NO_SHOW}
 * @param createdAt       instant the reservation was created
 */
public record ReservationResponse(
        UUID reservationId,
        ReservationStatus status,
        UUID customerId,
        UUID quoteId,
        String categoryCode,
        String pickupStation,
        String returnStation,
        RentalPeriod rentalPeriod,
        Money total,
        OffsetDateTime paymentDeadline,
        Money cancellationFee,
        Money refundAmount,
        OffsetDateTime createdAt) {
}
