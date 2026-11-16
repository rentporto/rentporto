package pt.rentporto.fleet.api;

import jakarta.validation.constraints.NotNull;

/**
 * State change a fleet manager requests for a vehicle.
 *
 * @param status new state
 * @param reason why the state changes, e.g. {@code Brake service}; optional
 */
public record VehicleStatusRequest(@NotNull VehicleStatusChange status, String reason) {
}
