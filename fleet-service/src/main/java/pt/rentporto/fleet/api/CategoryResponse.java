package pt.rentporto.fleet.api;

/**
 * Vehicle category and its driver requirements.
 *
 * @param code                ACRISS code of the category, e.g. {@code ECMR}
 * @param name                category name, e.g. {@code Group B - Economy}
 * @param minimumAge          minimum driver age
 * @param minimumLicenseYears minimum years since the driving licence was issued
 */
public record CategoryResponse(String code, String name, int minimumAge, int minimumLicenseYears) {
}
