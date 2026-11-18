package pt.rentporto.fleet.api;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.fleet.application.FleetVehicles;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class VehiclesController implements VehiclesApi {

    private final FleetVehicles fleet;

    VehiclesController(FleetVehicles fleet) {
        this.fleet = fleet;
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public VehicleResponse registerVehicle(String idempotencyKey, VehicleRequest request) {
        return VehicleResponse.from(fleet.register(
                new LicensePlate(request.licensePlate()),
                new AcrissCode(request.categoryCode()),
                request.station(),
                request.odometerKm(),
                idempotencyKey,
                AuthenticatedUser.id()));
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public List<VehicleResponse> vehicles(String station, VehicleStatus status) {
        pt.rentporto.fleet.domain.VehicleStatus state =
                status == null ? null : pt.rentporto.fleet.domain.VehicleStatus.valueOf(status.name());
        return fleet.vehicles(station, state).stream().map(VehicleResponse::from).toList();
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public VehicleResponse changeStatus(String plate, VehicleStatusRequest request) {
        return VehicleResponse.from(fleet.changeStatus(
                new LicensePlate(plate),
                pt.rentporto.fleet.domain.VehicleStatus.valueOf(request.status().name()),
                request.reason(),
                AuthenticatedUser.id()));
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public TransferResponse startTransfer(String plate, String idempotencyKey, TransferRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public TransferResponse completeTransfer(String plate, UUID id) {
        throw new EndpointNotImplementedException();
    }
}
