package pt.rentporto.fleet.api.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.fleet.api.contract.VehiclesApi;
import pt.rentporto.fleet.api.model.VehicleStatus;
import pt.rentporto.fleet.api.request.TransferRequest;
import pt.rentporto.fleet.api.request.VehicleRequest;
import pt.rentporto.fleet.api.request.VehicleStatusRequest;
import pt.rentporto.fleet.api.response.TransferResponse;
import pt.rentporto.fleet.api.response.VehicleResponse;
import pt.rentporto.fleet.application.service.FleetTransfers;
import pt.rentporto.fleet.application.service.FleetVehicles;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.web.security.AuthenticatedUser;

@RestController
class VehiclesController implements VehiclesApi {

    private final FleetVehicles fleet;
    private final FleetTransfers transfers;

    VehiclesController(FleetVehicles fleet, FleetTransfers transfers) {
        this.fleet = fleet;
        this.transfers = transfers;
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
                AuthenticatedUser.subject()));
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public List<VehicleResponse> vehicles(String station, VehicleStatus status) {
        pt.rentporto.fleet.domain.model.VehicleStatus state =
                status == null ? null : pt.rentporto.fleet.domain.model.VehicleStatus.valueOf(status.name());
        return fleet.vehicles(station, state).stream().map(VehicleResponse::from).toList();
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public VehicleResponse changeStatus(String plate, VehicleStatusRequest request) {
        return VehicleResponse.from(fleet.changeStatus(
                new LicensePlate(plate),
                pt.rentporto.fleet.domain.model.VehicleStatus.valueOf(request.status().name()),
                request.reason(),
                AuthenticatedUser.subject()));
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public TransferResponse startTransfer(String plate, String idempotencyKey, TransferRequest request) {
        return TransferResponse.from(transfers.start(
                new LicensePlate(plate), request.toStation(), idempotencyKey, AuthenticatedUser.subject()));
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public TransferResponse completeTransfer(String plate, UUID id) {
        return TransferResponse.from(transfers.complete(new LicensePlate(plate), id, AuthenticatedUser.subject()));
    }
}
