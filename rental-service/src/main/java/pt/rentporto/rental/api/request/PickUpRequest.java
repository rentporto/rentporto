package pt.rentporto.rental.api.request;

import jakarta.validation.Valid;
import java.util.List;
import pt.rentporto.rental.api.model.Driver;

/**
 * Pick-up of the vehicle at the counter. The agent does not know the vehicle before the request: its odometer comes
 * from the fleet and it leaves with a full tank.
 *
 * @param additionalDrivers drivers besides the customer, with a licence valid today; possibly none
 * @param notes             notes of the check-out inspection; optional
 */
public record PickUpRequest(List<@Valid Driver> additionalDrivers, String notes) {
}
