package pt.rentporto.fleet.domain.exception;

import pt.rentporto.fleet.domain.model.DriverRequirements;
import pt.rentporto.web.error.BusinessException;

/**
 * The requirements of a category are below the minimum of the company (422).
 */
public class InvalidDriverRequirementsException extends BusinessException {

    /**
     * Creates the exception.
     *
     * @param minimumAge          minimum age as given
     * @param minimumLicenseYears minimum years of licence as given
     */
    public InvalidDriverRequirementsException(int minimumAge, int minimumLicenseYears) {
        super("INVALID_DRIVER_REQUIREMENTS", Category.UNPROCESSABLE,
                "A category needs at least %d years of age and %d years of licence, not %d and %d".formatted(
                        DriverRequirements.COMPANY_MINIMUM_AGE, DriverRequirements.COMPANY_MINIMUM_LICENSE_YEARS,
                        minimumAge, minimumLicenseYears));
    }
}
