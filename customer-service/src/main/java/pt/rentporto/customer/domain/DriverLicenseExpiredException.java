package pt.rentporto.customer.domain;

import java.time.LocalDate;
import pt.rentporto.web.BusinessException;

/**
 * The driving licence has expired (422).
 */
public class DriverLicenseExpiredException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param expiryDate date the licence expired
     */
    public DriverLicenseExpiredException(LocalDate expiryDate) {
        super("DRIVER_LICENSE_EXPIRED", Category.UNPROCESSABLE, "The driving licence expired on " + expiryDate);
    }
}
