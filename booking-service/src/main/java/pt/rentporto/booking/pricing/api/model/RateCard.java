package pt.rentporto.booking.pricing.api.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import pt.rentporto.events.Money;

/**
 * Base daily rate of a vehicle category.
 *
 * @param categoryCode ACRISS code of the category; matches the path
 * @param dailyRate    price per 24-hour day, before seasons and discounts
 */
public record RateCard(@NotNull @Pattern(regexp = "[A-Z]{4}") String categoryCode, @NotNull Money dailyRate) {
}
