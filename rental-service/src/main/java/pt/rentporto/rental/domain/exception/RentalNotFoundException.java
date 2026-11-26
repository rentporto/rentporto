package pt.rentporto.rental.domain.exception;

import java.util.UUID;
import pt.rentporto.web.error.BusinessException;

/**
 * There is no rental agreement with the identifier (404).
 */
public class RentalNotFoundException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param rentalId identifier of the agreement
     */
    public RentalNotFoundException(UUID rentalId) {
        super("RENTAL_NOT_FOUND", Category.NOT_FOUND, "There is no rental " + rentalId);
    }
}
