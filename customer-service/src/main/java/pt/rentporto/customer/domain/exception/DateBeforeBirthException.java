package pt.rentporto.customer.domain.exception;

import java.time.LocalDate;
import pt.rentporto.web.error.BusinessException;

/**
 * A driver profile was asked for a date before the customer was born (422).
 */
public class DateBeforeBirthException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param date the date asked for
     */
    public DateBeforeBirthException(LocalDate date) {
        super("DATE_BEFORE_BIRTH", Category.UNPROCESSABLE, "The customer was not born yet on " + date);
    }
}
