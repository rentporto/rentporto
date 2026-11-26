package pt.rentporto.customer.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import pt.rentporto.customer.api.model.DriverLicense;

/**
 * Profile a customer creates or updates.
 *
 * @param fullName      full name as on the driving licence
 * @param taxNumber     Portuguese tax number (NIF), 9 digits
 * @param birthDate     date of birth
 * @param phone         contact phone number
 * @param driverLicense driving licence
 */
public record CustomerRequest(
        @NotBlank String fullName,
        @NotNull @Pattern(regexp = "\\d{9}") String taxNumber,
        @NotNull @Past LocalDate birthDate,
        @NotBlank String phone,
        @NotNull @Valid DriverLicense driverLicense) {
}
