package pt.rentporto.fleet.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Transfer of a vehicle to another station.
 *
 * @param toStation code of the destination station
 */
public record TransferRequest(@NotNull @Pattern(regexp = "[A-Z]{3}") String toStation) {
}
