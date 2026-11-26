package pt.rentporto.customer.domain.exception;

import pt.rentporto.customer.domain.model.TaxNumber;
import pt.rentporto.web.error.BusinessException;

/**
 * Another customer already has this tax number (409).
 */
public class TaxNumberAlreadyRegisteredException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param taxNumber the tax number
     */
    public TaxNumberAlreadyRegisteredException(TaxNumber taxNumber) {
        super("TAX_NUMBER_ALREADY_REGISTERED", Category.CONFLICT,
                "The tax number " + taxNumber + " belongs to another customer");
    }
}
