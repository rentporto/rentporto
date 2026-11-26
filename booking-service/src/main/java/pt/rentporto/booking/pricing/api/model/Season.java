package pt.rentporto.booking.pricing.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Date range whose rates are multiplied, e.g. the high season.
 *
 * @param name       season name, e.g. {@code High}
 * @param from       first day of the season
 * @param to         last day of the season
 * @param multiplier factor applied to the daily rate, e.g. {@code 1.4}
 */
public record Season(
        @NotBlank String name,
        @NotNull LocalDate from,
        @NotNull LocalDate to,
        @NotNull @Positive BigDecimal multiplier) {
}
