package pt.rentporto.fleet.api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.fleet.api.contract.VehicleAssignmentsApi;
import pt.rentporto.fleet.api.request.VehicleAssignmentRequest;
import pt.rentporto.fleet.api.response.VehicleAssignmentResponse;
import pt.rentporto.fleet.application.service.FleetAssignments;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.web.security.AuthenticatedUser;

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
                AuthenticatedUser.subject()));
    }
}
