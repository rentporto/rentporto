package pt.rentporto.rental.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Extra booked with the rental, at the price agreed at booking.
 *
 * @param code        extra code, e.g. {@code CHILD_SEAT}
 * @param pricePerDay price per 24-hour day
 * @param cap         maximum charged for the whole rental
 */
@Embeddable
public record ContractExtra(
        @Column(name = "code", nullable = false) String code,
        @AttributeOverride(name = "cents", column = @Column(name = "price_per_day_cents", nullable = false))
        Money pricePerDay,
        @AttributeOverride(name = "cents", column = @Column(name = "cap_cents", nullable = false))
        Money cap) {

    public ContractExtra {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(pricePerDay, "pricePerDay");
        Objects.requireNonNull(cap, "cap");
    }
}
