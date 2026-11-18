package pt.rentporto.fleet.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.fleet.application.FleetAssignments;
import pt.rentporto.fleet.domain.AcrissCode;

@RestController
class VehicleAssignmentsController implements VehicleAssignmentsApi {

    private final FleetAssignments assignments;

    VehicleAssignmentsController(FleetAssignments assignments) {
        this.assignments = assignments;
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public VehicleAssignmentResponse assignVehicle(String idempotencyKey, VehicleAssignmentRequest request) {
        return VehicleAssignmentResponse.from(assignments.assign(
                request.rentalId(),
                new AcrissCode(request.categoryCode()),
                request.station(),
                idempotencyKey,
                AuthenticatedUser.id()));
    }
}
