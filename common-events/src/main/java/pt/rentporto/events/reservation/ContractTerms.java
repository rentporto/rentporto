package pt.rentporto.events.reservation;

import java.util.List;
import java.util.Objects;
import pt.rentporto.events.Money;

/**
 * Contract conditions frozen at booking time; the rental-service settles the agreement with them.
 *
 * @param dailyRate         price per 24-hour day
 * @param extras            extras booked, possibly none
 * @param coverage          code of the coverage chosen, e.g. {@code FULL}
 * @param excess            maximum the customer pays for damages
 * @param deposit           security deposit pre-authorised at pick-up
 * @param fuelPricePerLitre price charged per missing litre of fuel
 * @param refuellingFee     service fee charged when the vehicle is returned with less fuel
 */
public record ContractTerms(
        Money dailyRate,
        List<ContractExtra> extras,
        String coverage,
        Money excess,
        Money deposit,
        Money fuelPricePerLitre,
        Money refuellingFee) {

    public ContractTerms {
        Objects.requireNonNull(dailyRate, "dailyRate");
        extras = List.copyOf(Objects.requireNonNull(extras, "extras"));
        Objects.requireNonNull(coverage, "coverage");
        Objects.requireNonNull(excess, "excess");
        Objects.requireNonNull(deposit, "deposit");
        Objects.requireNonNull(fuelPricePerLitre, "fuelPricePerLitre");
        Objects.requireNonNull(refuellingFee, "refuellingFee");
    }
}
