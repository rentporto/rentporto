package pt.rentporto.booking.pricing.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.rentporto.events.Money;

/**
 * Extra a customer can add to a rental.
 *
 * @param code        extra code, e.g. {@code CHILD_SEAT}
 * @param name        name shown to the customer
 * @param pricePerDay price per 24-hour day
 * @param cap         maximum charged for the whole rental
 */
public record Extra(@NotBlank String code, @NotBlank String name, @NotNull Money pricePerDay, @NotNull Money cap) {
}
