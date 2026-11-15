package pt.rentporto.events.rental;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The customer returned the vehicle.
 *
 * <p>Published by the rental-service on {@code rental.vehicle-returned}; consumed by the fleet-service, which
 * updates the vehicle, and by the reservation module of the booking-service, which stops counting a one-way
 * occupation.
 *
 * @param rentalId         rental agreement identifier
 * @param reservationId    reservation fulfilled by this rental
 * @param licensePlate     licence plate of the vehicle returned
 * @param returnStation    code of the station where the vehicle was returned
 * @param odometerKm       odometer reading in kilometres
 * @param fuelLevelEighths fuel level in eighths of a tank, from 0 to 8
 * @param damages          new damages found at the return inspection, possibly none
 */
public record VehicleReturned(
        UUID rentalId,
        UUID reservationId,
        String licensePlate,
        String returnStation,
        long odometerKm,
        int fuelLevelEighths,
        List<Damage> damages) {

    public VehicleReturned {
        Objects.requireNonNull(rentalId, "rentalId");
        Objects.requireNonNull(reservationId, "reservationId");
        Objects.requireNonNull(licensePlate, "licensePlate");
        Objects.requireNonNull(returnStation, "returnStation");
        if (odometerKm < 0) {
            throw new IllegalArgumentException("odometerKm must be >= 0, was " + odometerKm);
        }
        if (fuelLevelEighths < 0 || fuelLevelEighths > 8) {
            throw new IllegalArgumentException("fuelLevelEighths must be between 0 and 8, was " + fuelLevelEighths);
        }
        damages = List.copyOf(Objects.requireNonNull(damages, "damages"));
    }
}
