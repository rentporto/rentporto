package pt.rentporto.rental.application;

import java.util.UUID;
import pt.rentporto.web.BusinessException;

/**
 * The card refused the pre-authorisation of the security deposit (422).
 */
public class DepositDeclinedException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param rentalId the rental
     */
    public DepositDeclinedException(UUID rentalId) {
        super("DEPOSIT_DECLINED", Category.UNPROCESSABLE, "The card refused the deposit of the rental " + rentalId);
    }
}
