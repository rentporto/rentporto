package pt.rentporto.rental.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * State of the vehicle the agent records at the return.
 *
 * @param odometerKm       odometer reading in kilometres
 * @param fuelLevelEighths fuel level in eighths of a tank, from 0 to 8, as found
 * @param litresToFill     litres the agent put in to fill the tank, zero when it came back full
 * @param damages          new damages, possibly none
 * @param notes            free notes of the agent, or {@code null}
 */
public record ReturnInspection(
        long odometerKm, int fuelLevelEighths, BigDecimal litresToFill, List<ReturnDamage> damages, String notes) {

    public ReturnInspection {
        if (fuelLevelEighths < 0 || fuelLevelEighths > RentalAgreement.FULL_TANK) {
            throw new IllegalArgumentException("fuelLevelEighths must be between 0 and 8, was " + fuelLevelEighths);
        }
        Objects.requireNonNull(litresToFill, "litresToFill");
        if (litresToFill.signum() < 0) {
            throw new IllegalArgumentException("litresToFill must be >= 0, was " + litresToFill);
        }
        damages = List.copyOf(Objects.requireNonNull(damages, "damages"));
    }
}
