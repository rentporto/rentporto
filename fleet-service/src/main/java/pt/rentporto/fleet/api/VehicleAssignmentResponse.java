package pt.rentporto.fleet.api;

import pt.rentporto.fleet.domain.VehicleAssignment;

/**
 * Vehicle assigned to a rental.
 *
 * @param licensePlate licence plate of the vehicle, e.g. {@code AA-12-BB}
 * @param odometerKm   odometer reading in kilometres at assignment
 */
public record VehicleAssignmentResponse(String licensePlate, long odometerKm) {

    static VehicleAssignmentResponse from(VehicleAssignment assignment) {
        return new VehicleAssignmentResponse(assignment.plate().value(), assignment.odometerKm());
    }
}
