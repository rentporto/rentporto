package pt.rentporto.rental.domain;

import java.util.UUID;
import pt.rentporto.web.BusinessException;

/**
 * The agreement is not in the state the operation needs (409).
 */
public class InvalidRentalStatusException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param rentalId identifier of the agreement
     * @param status   its current state
     * @param expected the state the operation needs
     */
    public InvalidRentalStatusException(UUID rentalId, RentalStatus status, RentalStatus expected) {
        super("INVALID_RENTAL_STATUS", Category.CONFLICT,
                "The rental " + rentalId + " is " + status + ", not " + expected);
    }
}
