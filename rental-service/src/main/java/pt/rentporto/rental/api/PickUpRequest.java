package pt.rentporto.rental.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Pick-up of the vehicle at the counter.
 *
 * @param additionalDrivers drivers besides the customer; possibly none
 * @param inspection        check-out inspection
 */
public record PickUpRequest(List<@Valid Driver> additionalDrivers, @NotNull @Valid Inspection inspection) {
}
