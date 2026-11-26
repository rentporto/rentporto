package pt.rentporto.customer.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import pt.rentporto.customer.domain.exception.InvalidDriverLicenseException;

/**
 * What a customer fills in on their profile.
 *
 * @param fullName      full name as on the driving licence
 * @param taxNumber     tax number
 * @param birthDate     date of birth
 * @param phone         contact phone number
 * @param driverLicense driving licence
 */
public record CustomerDetails(
        String fullName, TaxNumber taxNumber, LocalDate birthDate, String phone, DriverLicense driverLicense) {

    public CustomerDetails {
        fullName = Objects.requireNonNull(fullName, "fullName").strip();
        Objects.requireNonNull(taxNumber, "taxNumber");
        Objects.requireNonNull(birthDate, "birthDate");
        phone = Objects.requireNonNull(phone, "phone").strip();
        Objects.requireNonNull(driverLicense, "driverLicense");
        if (driverLicense.issueDate().isBefore(birthDate)) {
            throw new InvalidDriverLicenseException("it was issued before the date of birth");
        }
    }
}
