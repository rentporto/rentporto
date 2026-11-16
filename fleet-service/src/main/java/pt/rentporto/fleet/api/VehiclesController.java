package pt.rentporto.fleet.api;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class VehiclesController implements VehiclesApi {

    @Override
    public VehicleResponse registerVehicle(String idempotencyKey, VehicleRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public List<VehicleResponse> vehicles(String station, VehicleStatus status) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public VehicleResponse changeStatus(String plate, VehicleStatusRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public TransferResponse startTransfer(String plate, String idempotencyKey, TransferRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public TransferResponse completeTransfer(String plate, UUID id) {
        throw new EndpointNotImplementedException();
    }
}
