package pt.rentporto.rental.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.Objects;

/**
 * Pick-up and return instants agreed at booking.
 *
 * @param pickupAt when the customer picks up the vehicle
 * @param returnAt when the customer returns it, after the pick-up
 */
@Embeddable
public record RentalPeriod(
        @Column(name = "pickup_at", nullable = false) Instant pickupAt,
        @Column(name = "return_at", nullable = false) Instant returnAt) {

    public RentalPeriod {
        Objects.requireNonNull(pickupAt, "pickupAt");
        Objects.requireNonNull(returnAt, "returnAt");
        if (!returnAt.isAfter(pickupAt)) {
            throw new IllegalArgumentException("returnAt must be after pickupAt");
        }
    }
}
