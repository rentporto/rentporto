package pt.rentporto.events.payment;

import java.util.Objects;
import java.util.UUID;
import pt.rentporto.events.Money;

/**
 * Command to settle the security deposit of a returned rental.
 *
 * <p>Sent by the rental-service to the {@code payment.settle-deposit} queue; consumed by the payment module of the
 * booking-service, which captures what is due, releases the rest and charges any amount above the deposit.
 *
 * @param rentalId  rental agreement identifier
 * @param depositId identifier of the deposit pre-authorised at pick-up
 * @param amountDue extra charges owed by the customer; may exceed the deposit
 */
public record SettleDeposit(UUID rentalId, UUID depositId, Money amountDue) {

    public SettleDeposit {
        Objects.requireNonNull(rentalId, "rentalId");
        Objects.requireNonNull(depositId, "depositId");
        Objects.requireNonNull(amountDue, "amountDue");
    }
}
