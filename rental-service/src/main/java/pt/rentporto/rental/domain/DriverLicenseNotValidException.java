package pt.rentporto.rental.domain;

import java.time.LocalDate;
import pt.rentporto.web.BusinessException;

/**
 * The driving licence of an additional driver cannot be used on the day of the pick-up (422).
 */
public class DriverLicenseNotValidException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param driver full name of the driver
     * @param date   date of the pick-up
     */
    public DriverLicenseNotValidException(String driver, LocalDate date) {
        super("DRIVER_LICENSE_NOT_VALID", Category.UNPROCESSABLE,
                "The driving licence of " + driver + " is not valid on " + date);
    }
}
