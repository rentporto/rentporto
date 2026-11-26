package pt.rentporto.fleet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import pt.rentporto.fleet.domain.exception.InvalidDriverRequirementsException;

/**
 * What a driver needs on the pick-up date to rent a category. Every category asks for at least the minimum of the
 * company, 21 years of age and 2 years of licence, so what a customer sees is what a booking checks.
 *
 * @param minimumAge          minimum age of the driver
 * @param minimumLicenseYears minimum full years since the driving licence was issued
 */
@Embeddable
public record DriverRequirements(
        @Column(name = "minimum_age", nullable = false) int minimumAge,
        @Column(name = "minimum_license_years", nullable = false) int minimumLicenseYears) {

    public static final int COMPANY_MINIMUM_AGE = 21;
    public static final int COMPANY_MINIMUM_LICENSE_YEARS = 2;

    public DriverRequirements {
        if (minimumAge < COMPANY_MINIMUM_AGE || minimumLicenseYears < COMPANY_MINIMUM_LICENSE_YEARS) {
            throw new InvalidDriverRequirementsException(minimumAge, minimumLicenseYears);
        }
    }
}
