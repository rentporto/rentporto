package pt.rentporto.fleet.api;

/**
 * Vehicle assigned to a rental.
 *
 * @param licensePlate licence plate of the vehicle, e.g. {@code AA-12-BB}
 * @param odometerKm   odometer reading in kilometres at assignment
 */
public record VehicleAssignmentResponse(String licensePlate, long odometerKm) {
}
