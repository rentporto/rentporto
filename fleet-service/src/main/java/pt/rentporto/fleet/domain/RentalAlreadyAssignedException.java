package pt.rentporto.fleet.domain;

import java.util.UUID;
import pt.rentporto.web.BusinessException;

/**
 * The rental already has a vehicle, assigned by another request (409).
 */
public class RentalAlreadyAssignedException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param rentalId the rental
     */
    public RentalAlreadyAssignedException(UUID rentalId) {
        super("RENTAL_ALREADY_ASSIGNED", Category.CONFLICT, "The rental " + rentalId + " already has a vehicle");
    }
}
