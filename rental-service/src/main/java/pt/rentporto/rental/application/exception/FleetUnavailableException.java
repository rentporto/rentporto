package pt.rentporto.rental.application.exception;

import java.util.UUID;
import pt.rentporto.web.error.BusinessException;

/**
 * The fleet did not answer; the agent can try the pick-up again (503).
 */
public class FleetUnavailableException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param rentalId the rental
     */
    public FleetUnavailableException(UUID rentalId) {
        super("FLEET_UNAVAILABLE", Category.UNAVAILABLE, "The fleet did not answer for the rental " + rentalId);
    }
}
