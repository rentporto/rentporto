package pt.rentporto.fleet.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Vehicle category a fleet manager creates.
 *
 * @param code                ACRISS code of the category, e.g. {@code ECMR}
 * @param name                category name, e.g. {@code Group B - Economy}
 * @param minimumAge          minimum driver age, at least 21
 * @param minimumLicenseYears minimum years since the driving licence was issued, at least 2
 */
public record CategoryRequest(
        @NotNull @Pattern(regexp = "[A-Z]{4}") String code,
        @NotBlank String name,
        int minimumAge,
        int minimumLicenseYears) {
}
