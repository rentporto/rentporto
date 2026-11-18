package pt.rentporto.fleet.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import pt.rentporto.fleet.domain.LicensePlate;

/**
 * Vehicle a fleet manager registers.
 *
 * @param licensePlate licence plate in one of the formats AA-00-AA, 00-AA-00 or 00-00-AA, e.g. {@code AA-12-BB}
 * @param categoryCode ACRISS code of the vehicle category
 * @param station      code of the station where the vehicle is
 * @param odometerKm   odometer reading in kilometres
 */
public record VehicleRequest(
        @NotNull @Pattern(regexp = LicensePlate.FORMAT) String licensePlate,
        @NotNull @Pattern(regexp = "[A-Z]{4}") String categoryCode,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String station,
        @PositiveOrZero long odometerKm) {
}
