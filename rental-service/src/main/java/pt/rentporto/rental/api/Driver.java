package pt.rentporto.rental.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Additional driver added to the rental agreement at pick-up.
 *
 * @param fullName      full name as on the driving licence
 * @param driverLicense driving licence
 */
public record Driver(@NotBlank String fullName, @NotNull @Valid DriverLicense driverLicense) {
}
