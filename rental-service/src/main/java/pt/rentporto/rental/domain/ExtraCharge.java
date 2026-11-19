package pt.rentporto.rental.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

/**
 * Amount charged at the return on top of the advance payment, with the detail shown to the customer.
 *
 * @param type        what the charge is for
 * @param description detail, e.g. {@code 18.5 litres to fill}
 * @param amount      amount charged
 */
@Embeddable
public record ExtraCharge(
        @Enumerated(EnumType.STRING) @Column(name = "type", nullable = false) Type type,
        @Column(name = "description", nullable = false) String description,
        @AttributeOverride(name = "cents", column = @Column(name = "amount_cents", nullable = false)) Money amount) {

    public ExtraCharge {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
    }

    /**
     * What a charge is for.
     */
    public enum Type {
        /** Days after the agreed return. */
        LATE_RETURN,
        /** Fuel missing from a full tank. */
        FUEL,
        /** New damage, up to the excess. */
        DAMAGE
    }
}
