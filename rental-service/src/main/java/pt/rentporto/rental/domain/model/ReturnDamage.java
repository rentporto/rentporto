package pt.rentporto.rental.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

/**
 * Damage found at the return that was not recorded at pick-up, with the amount the agent set for it.
 *
 * @param location    part of the vehicle, e.g. {@code FRONT_BUMPER}
 * @param description what the agent recorded
 * @param amount      amount the agent set; the charges of all damages stop at the excess
 */
@Embeddable
public record ReturnDamage(
        @Column(name = "location", nullable = false) String location,
        @Column(name = "description", nullable = false) String description,
        @AttributeOverride(name = "cents", column = @Column(name = "amount_cents", nullable = false)) Money amount) {

    public ReturnDamage {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
    }
}
