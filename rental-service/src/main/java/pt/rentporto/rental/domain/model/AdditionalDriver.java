package pt.rentporto.rental.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Objects;
import pt.rentporto.rental.domain.exception.DriverLicenseNotValidException;

/**
 * Driver added to the agreement at pick-up, besides the customer, with the driving licence they showed.
 *
 * @param fullName          full name as on the driving licence
 * @param licenseNumber     licence number
 * @param licenseCategory   licence category, e.g. {@code B}
 * @param licenseCountry    ISO 3166-1 alpha-2 code of the country that issued the licence
 * @param licenseIssueDate  date the licence was issued
 * @param licenseExpiryDate date the licence expires
 */
@Embeddable
public record AdditionalDriver(
        @Column(name = "full_name", nullable = false) String fullName,
        @Column(name = "license_number", nullable = false) String licenseNumber,
        @Column(name = "license_category", nullable = false) String licenseCategory,
        @Column(name = "license_country", nullable = false) String licenseCountry,
        @Column(name = "license_issue_date", nullable = false) LocalDate licenseIssueDate,
        @Column(name = "license_expiry_date", nullable = false) LocalDate licenseExpiryDate) {

    public AdditionalDriver {
        Objects.requireNonNull(fullName, "fullName");
        Objects.requireNonNull(licenseNumber, "licenseNumber");
        Objects.requireNonNull(licenseCategory, "licenseCategory");
        Objects.requireNonNull(licenseCountry, "licenseCountry");
        Objects.requireNonNull(licenseIssueDate, "licenseIssueDate");
        Objects.requireNonNull(licenseExpiryDate, "licenseExpiryDate");
    }

    /**
     * Checks that the licence can be used on a date: already issued and not expired, the expiry date included.
     *
     * @param date the date of the pick-up
     * @throws DriverLicenseNotValidException if it cannot
     */
    public void requireValidLicenseOn(LocalDate date) {
        if (licenseIssueDate.isAfter(date) || licenseExpiryDate.isBefore(date)) {
            throw new DriverLicenseNotValidException(fullName, date);
        }
    }
}
