package pt.rentporto.fleet.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Vehicle a fleet manager registers.
 *
 * @param licensePlate licence plate, e.g. {@code AA-12-BB}
 * @param categoryCode ACRISS code of the vehicle category
 * @param station      code of the station where the vehicle is
 * @param odometerKm   odometer reading in kilometres
 */
public record VehicleRequest(
        @NotNull @Pattern(regexp = "[A-Z]{2}-\\d{2}-[A-Z]{2}") String licensePlate,
        @NotNull @Pattern(regexp = "[A-Z]{4}") String categoryCode,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String station,
        @PositiveOrZero long odometerKm) {
}
