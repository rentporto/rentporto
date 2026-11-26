package pt.rentporto.rental.application.exception;

import java.util.UUID;
import pt.rentporto.web.error.BusinessException;

/**
 * The payments did not answer; the agent can try the pick-up again (503).
 */
public class PaymentUnavailableException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param rentalId the rental
     */
    public PaymentUnavailableException(UUID rentalId) {
        super("PAYMENT_UNAVAILABLE", Category.UNAVAILABLE, "The payments did not answer for the rental " + rentalId);
    }
}
