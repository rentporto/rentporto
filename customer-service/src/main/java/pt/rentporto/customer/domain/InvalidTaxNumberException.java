package pt.rentporto.customer.domain;

import pt.rentporto.web.BusinessException;

/**
 * The tax number does not have nine digits or its check digit is wrong (422).
 */
public class InvalidTaxNumberException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param taxNumber the tax number as given
     */
    public InvalidTaxNumberException(String taxNumber) {
        super("INVALID_TAX_NUMBER", Category.UNPROCESSABLE, taxNumber + " is not a valid Portuguese tax number");
    }
}
