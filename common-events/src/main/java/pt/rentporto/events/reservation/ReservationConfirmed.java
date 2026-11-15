package pt.rentporto.events.reservation;

import java.util.Objects;
import java.util.UUID;
import pt.rentporto.events.Money;
import pt.rentporto.events.RentalPeriod;

/**
 * A reservation was paid and confirmed.
 *
 * <p>Published by the reservation module of the booking-service on {@code reservation.confirmed}; consumed by the
 * rental-service, which creates the pending agreement, and by the notification-service.
 *
 * @param reservationId reservation identifier
 * @param customerId    customer who booked
 * @param pickupStation code of the pick-up station
 * @param returnStation code of the return station; differs from {@code pickupStation} on a one-way rental
 * @param categoryCode  ACRISS code of the category booked
 * @param rentalPeriod  pick-up and return date-times
 * @param contractTerms conditions frozen at booking time
 * @param total         amount paid in advance
 */
public record ReservationConfirmed(
        UUID reservationId,
        UUID customerId,
        String pickupStation,
        String returnStation,
        String categoryCode,
        RentalPeriod rentalPeriod,
        ContractTerms contractTerms,
        Money total) {

    public ReservationConfirmed {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(pickupStation, "pickupStation");
        Objects.requireNonNull(returnStation, "returnStation");
        Objects.requireNonNull(categoryCode, "categoryCode");
        Objects.requireNonNull(rentalPeriod, "rentalPeriod");
        Objects.requireNonNull(contractTerms, "contractTerms");
        Objects.requireNonNull(total, "total");
    }
}
