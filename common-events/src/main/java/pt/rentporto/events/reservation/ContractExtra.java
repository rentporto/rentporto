package pt.rentporto.events.reservation;

import java.util.Objects;
import pt.rentporto.events.Money;

/**
 * Extra booked with the reservation, at the price agreed at booking time.
 *
 * @param code        extra code, e.g. {@code CHILD_SEAT}
 * @param pricePerDay price per 24-hour day
 * @param cap         maximum charged for the whole rental
 */
public record ContractExtra(String code, Money pricePerDay, Money cap) {

    public ContractExtra {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(pricePerDay, "pricePerDay");
        Objects.requireNonNull(cap, "cap");
    }
}
