package pt.rentporto.fleet.api.response;

import pt.rentporto.fleet.api.model.VehicleStatus;
import pt.rentporto.fleet.domain.model.Vehicle;

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

    public static VehicleResponse from(Vehicle vehicle) {
        return new VehicleResponse(
                vehicle.plate().value(),
                vehicle.category().value(),
                vehicle.station(),
                VehicleStatus.valueOf(vehicle.status().name()),
                vehicle.odometerKm(),
                vehicle.fuelLevelEighths());
    }
}
