package pt.rentporto.fleet.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/**
 * Request for a vehicle of a category at pick-up.
 *
 * @param rentalId     rental agreement the vehicle is for
 * @param categoryCode ACRISS code of the category booked
 * @param station      code of the pick-up station
 */
public record VehicleAssignmentRequest(
        @NotNull UUID rentalId,
        @NotNull @Pattern(regexp = "[A-Z]{4}") String categoryCode,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String station) {
}
