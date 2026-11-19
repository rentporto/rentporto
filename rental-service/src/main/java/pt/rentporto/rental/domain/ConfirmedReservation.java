package pt.rentporto.rental.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Reservation confirmed by the booking-service, with everything the rental agreement keeps from it.
 *
 * @param reservationId reservation identifier
 * @param customerId    customer who booked
 * @param pickupStation code of the pick-up station
 * @param returnStation code of the return station; another one on a one-way rental
 * @param categoryCode  ACRISS code of the category booked
 * @param period        pick-up and return instants
 * @param terms         conditions frozen at booking
 * @param total         amount paid in advance
 */
public record ConfirmedReservation(
        UUID reservationId,
        UUID customerId,
        String pickupStation,
        String returnStation,
        String categoryCode,
        RentalPeriod period,
        ContractTerms terms,
        Money total) {

    public ConfirmedReservation {
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(pickupStation, "pickupStation");
        Objects.requireNonNull(returnStation, "returnStation");
        Objects.requireNonNull(categoryCode, "categoryCode");
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(terms, "terms");
        Objects.requireNonNull(total, "total");
    }
}
