package pt.rentporto.fleet.api;

/**
 * Vehicle of the fleet.
 *
 * @param licensePlate     licence plate, e.g. {@code AA-12-BB}
 * @param categoryCode     ACRISS code of the vehicle category
 * @param station          code of the station where the vehicle is
 * @param status           current state
 * @param odometerKm       odometer reading in kilometres
 * @param fuelLevelEighths fuel level in eighths of a tank, from 0 to 8
 */
public record VehicleResponse(
        String licensePlate,
        String categoryCode,
        String station,
        VehicleStatus status,
        long odometerKm,
        int fuelLevelEighths) {
}
