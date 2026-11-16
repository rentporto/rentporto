package pt.rentporto.fleet.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Vehicle category a fleet manager creates or updates.
 *
 * @param code                ACRISS code of the category, e.g. {@code ECMR}
 * @param name                category name, e.g. {@code Group B - Economy}
 * @param minimumAge          minimum driver age
 * @param minimumLicenseYears minimum years since the driving licence was issued
 */
public record CategoryRequest(
        @NotNull @Pattern(regexp = "[A-Z]{4}") String code,
        @NotBlank String name,
        @PositiveOrZero int minimumAge,
        @PositiveOrZero int minimumLicenseYears) {
}
