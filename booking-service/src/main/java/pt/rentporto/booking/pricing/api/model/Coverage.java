package pt.rentporto.booking.pricing.api.model;

import jakarta.validation.constraints.NotNull;
import pt.rentporto.events.Money;

/**
 * Insurance coverage a customer can choose.
 *
 * @param code        coverage code
 * @param pricePerDay price per 24-hour day
 * @param excess      maximum the customer pays for damages
 * @param deposit     security deposit held on the card at pick-up
 */
public record Coverage(
        @NotNull CoverageCode code, @NotNull Money pricePerDay, @NotNull Money excess, @NotNull Money deposit) {
}
