package pt.rentporto.fleet.api;

import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class VehicleAssignmentsController implements VehicleAssignmentsApi {

    @Override
    public VehicleAssignmentResponse assignVehicle(String idempotencyKey, VehicleAssignmentRequest request) {
        throw new EndpointNotImplementedException();
    }
}
