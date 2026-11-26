package pt.rentporto.booking.pricing.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * Extra chosen for a quote.
 *
 * @param code     extra code, e.g. {@code CHILD_SEAT}
 * @param quantity how many units
 */
public record ExtraSelection(@NotBlank String code, @Positive int quantity) {
}
