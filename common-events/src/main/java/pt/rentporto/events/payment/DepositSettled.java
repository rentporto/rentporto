package pt.rentporto.events.payment;

import java.util.Objects;
import java.util.UUID;
import pt.rentporto.events.Money;

/**
 * The security deposit of a rental was settled.
 *
 * <p>Published by the payment module of the booking-service on {@code payment.deposit-settled}; consumed by the
 * rental-service, which closes the agreement.
 *
 * @param rentalId    rental agreement identifier
 * @param depositId   deposit identifier
 * @param captured    amount charged to the customer, from the deposit and any extra charge
 * @param released    part of the deposit released
 * @param outstanding amount still owed when the charge above the deposit failed; zero otherwise
 */
public record DepositSettled(UUID rentalId, UUID depositId, Money captured, Money released, Money outstanding) {

    public DepositSettled {
        Objects.requireNonNull(rentalId, "rentalId");
        Objects.requireNonNull(depositId, "depositId");
        Objects.requireNonNull(captured, "captured");
        Objects.requireNonNull(released, "released");
        Objects.requireNonNull(outstanding, "outstanding");
    }
}
