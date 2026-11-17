package pt.rentporto.customer.domain;

import pt.rentporto.web.BusinessException;

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
