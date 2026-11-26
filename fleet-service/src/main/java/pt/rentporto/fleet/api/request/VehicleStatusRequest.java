package pt.rentporto.fleet.api.request;

import jakarta.validation.constraints.NotNull;
import pt.rentporto.fleet.api.model.VehicleStatusChange;

/**
 * State change a fleet manager requests for a vehicle.
 *
 * @param status new state
 * @param reason why the state changes, e.g. {@code Brake service}; optional
 */
public record VehicleStatusRequest(@NotNull VehicleStatusChange status, String reason) {
}
