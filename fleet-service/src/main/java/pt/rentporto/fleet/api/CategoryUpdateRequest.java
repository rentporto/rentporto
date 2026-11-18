package pt.rentporto.fleet.api;

import jakarta.validation.constraints.NotBlank;

/**
 * New name and requirements of a vehicle category. The code is in the path and does not change.
 *
 * @param name                category name, e.g. {@code Group B - Economy}
 * @param minimumAge          minimum driver age, at least 21
 * @param minimumLicenseYears minimum years since the driving licence was issued, at least 2
 */
public record CategoryUpdateRequest(
        @NotBlank String name,
        int minimumAge,
        int minimumLicenseYears) {
}
