package pt.rentporto.fleet.domain;

import pt.rentporto.web.BusinessException;

/**
 * The licence plate is not in one of the Portuguese formats (422).
 */
public class InvalidLicensePlateException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param plate the plate as given
     */
    public InvalidLicensePlateException(String plate) {
        super("INVALID_LICENSE_PLATE", Category.UNPROCESSABLE, plate + " is not a Portuguese licence plate");
    }
}
