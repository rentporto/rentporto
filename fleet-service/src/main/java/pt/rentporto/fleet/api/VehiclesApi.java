package pt.rentporto.fleet.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import pt.rentporto.fleet.domain.LicensePlate;

/**
 * Vehicles of the fleet, their state and transfers between stations.
 */
@Tag(name = "Vehicles")
public interface VehiclesApi {

    /**
     * Registers a vehicle at a station: it starts available, with a full tank, and the new capacity of the station is
     * published. Roles: FLEET_MANAGER.
     *
     * @param idempotencyKey key that makes a repeated request return the same vehicle; optional
     * @param request        vehicle to register
     * @return the registered vehicle
     */
    @PostMapping("/vehicles")
    @ResponseStatus(HttpStatus.CREATED)
    VehicleResponse registerVehicle(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody VehicleRequest request);

    /**
     * Lists the vehicles by licence plate, optionally of one station and in one state. Roles: FLEET_MANAGER.
     *
     * @param station code of the station; optional
     * @param status  state of the vehicles; optional
     * @return the vehicles that match
     */
    @GetMapping("/vehicles")
    List<VehicleResponse> vehicles(
            @RequestParam(required = false) @Pattern(regexp = "[A-Z]{3}") String station,
            @RequestParam(required = false) VehicleStatus status);

    /**
     * Sends a vehicle to maintenance, retires it or brings it back to service; asking for its current state changes
     * nothing. Roles: FLEET_MANAGER.
     *
     * @param plate   licence plate of the vehicle
     * @param request new state
     * @return the vehicle in its new state
     */
    @PatchMapping("/vehicles/{plate}/status")
    VehicleResponse changeStatus(
            @PathVariable @Pattern(regexp = LicensePlate.FORMAT) String plate,
            @Valid @RequestBody VehicleStatusRequest request);

    /**
     * Starts the transfer of a vehicle to another station; the vehicle stays in transfer until the transfer is
     * completed. Roles: FLEET_MANAGER.
     *
     * @param plate          licence plate of the vehicle
     * @param idempotencyKey key that makes a repeated request return the same transfer; optional
     * @param request        destination station
     * @return the started transfer
     */
    @PostMapping("/vehicles/{plate}/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    TransferResponse startTransfer(
            @PathVariable @Pattern(regexp = LicensePlate.FORMAT) String plate,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request);

    /**
     * Completes a transfer: the vehicle is available at the destination station and the capacity of both stations is
     * published. Roles: FLEET_MANAGER.
     *
     * @param plate licence plate of the vehicle
     * @param id    transfer identifier
     * @return the completed transfer
     */
    @PostMapping("/vehicles/{plate}/transfers/{id}/completion")
    TransferResponse completeTransfer(
            @PathVariable @Pattern(regexp = LicensePlate.FORMAT) String plate, @PathVariable UUID id);
}
