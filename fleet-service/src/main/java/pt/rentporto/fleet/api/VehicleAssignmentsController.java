package pt.rentporto.fleet.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class VehicleAssignmentsController implements VehicleAssignmentsApi {

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public VehicleAssignmentResponse assignVehicle(String idempotencyKey, VehicleAssignmentRequest request) {
        throw new EndpointNotImplementedException();
    }
}
