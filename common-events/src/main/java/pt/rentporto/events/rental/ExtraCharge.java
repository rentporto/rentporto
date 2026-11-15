package pt.rentporto.events.rental;

import java.util.Objects;
import pt.rentporto.events.Money;

/**
 * Amount charged at settlement on top of the advance payment.
 *
 * @param type        what the charge is for
 * @param description detail shown to the customer, e.g. {@code 12 litres missing}
 * @param amount      amount charged
 */
public record ExtraCharge(Type type, String description, Money amount) {

    public ExtraCharge {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(amount, "amount");
    }

    /**
     * What an extra charge is for. Adding a value breaks consumers still on the old version, so it needs a new
     * message version.
     */
    public enum Type {
        FUEL,
        LATE_RETURN,
        DAMAGE
    }
}
