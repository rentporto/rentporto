package pt.rentporto.customer.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * Driving licence of a customer.
 *
 * @param number         licence number
 * @param category       licence category, e.g. {@code B}
 * @param issuingCountry country that issued the licence, ISO 3166-1 alpha-2, e.g. {@code PT}
 * @param issueDate      date the licence was issued
 * @param expiryDate     date the licence expires
 */
public record DriverLicense(
        @NotBlank String number,
        @NotBlank String category,
        @NotNull @Pattern(regexp = "[A-Z]{2}") String issuingCountry,
        @NotNull @PastOrPresent LocalDate issueDate,
        @NotNull LocalDate expiryDate) {
}
