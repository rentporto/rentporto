package pt.rentporto.events.rental;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

/**
 * The customer picked up the vehicle and the rental agreement became active.
 *
 * <p>Published by the rental-service on {@code rental.started}; consumed by the reservation module of the
 * booking-service, which marks the reservation as fulfilled, and by the notification-service.
 *
 * @param rentalId      rental agreement identifier
 * @param reservationId reservation fulfilled by this rental
 * @param customerId    customer who booked
 * @param licensePlate  licence plate of the vehicle assigned
 * @param pickedUpAt    pick-up date-time, normalised to UTC
 */
public record RentalStarted(
        UUID rentalId,
        UUID reservationId,
        UUID customerId,
        String licensePlate,
        OffsetDateTime pickedUpAt) {

    public RentalStarted {
        Objects.requireNonNull(rentalId, "rentalId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(licensePlate, "licensePlate");
        pickedUpAt = Objects.requireNonNull(pickedUpAt, "pickedUpAt").withOffsetSameInstant(ZoneOffset.UTC);
    }
}
