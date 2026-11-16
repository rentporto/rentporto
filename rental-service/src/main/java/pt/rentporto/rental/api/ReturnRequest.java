package pt.rentporto.rental.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Return of the vehicle.
 *
 * @param station    code of the station where the vehicle was returned, e.g. {@code OPO}
 * @param inspection return inspection
 */
public record ReturnRequest(
        @NotNull @Pattern(regexp = "[A-Z]{3}") String station,
        @NotNull @Valid Inspection inspection) {
}
