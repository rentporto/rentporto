package pt.rentporto.customer.domain.exception;

import pt.rentporto.web.error.BusinessException;

/**
 * The driving licence cannot exist: an unknown category, an impossible number or inconsistent dates (422).
 */
public class InvalidDriverLicenseException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param reason what is wrong, e.g. {@code the category X does not exist}
     */
    public InvalidDriverLicenseException(String reason) {
        super("INVALID_DRIVER_LICENSE", Category.UNPROCESSABLE, "The driving licence is not valid: " + reason);
    }
}
