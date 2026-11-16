package pt.rentporto.booking.pricing.api;

import jakarta.validation.constraints.NotNull;
import pt.rentporto.events.Money;

/**
 * Fee added to a quote when it applies, e.g. at the airport or on a one-way rental.
 *
 * @param code   fee code
 * @param amount amount charged
 * @param per    whether the amount is charged per day or once per rental
 */
public record Fee(@NotNull FeeCode code, @NotNull Money amount, @NotNull FeeBasis per) {
}
