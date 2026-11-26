package pt.rentporto.rental.domain.model;

import java.util.Objects;

/**
 * How the security deposit was settled after the return.
 *
 * @param captured    amount charged to the customer, from the deposit and any extra charge
 * @param released    part of the deposit released
 * @param outstanding amount still owed when the charge above the deposit failed; zero otherwise
 */
public record Settlement(Money captured, Money released, Money outstanding) {

    public Settlement {
        Objects.requireNonNull(captured, "captured");
        Objects.requireNonNull(released, "released");
        Objects.requireNonNull(outstanding, "outstanding");
    }
}
