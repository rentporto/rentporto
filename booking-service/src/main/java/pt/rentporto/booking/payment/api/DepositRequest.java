package pt.rentporto.booking.payment.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import pt.rentporto.events.Money;

/**
 * Pre-authorisation of the security deposit at pick-up, on the card used for the advance payment.
 *
 * @param rentalId      rental agreement identifier
 * @param reservationId reservation paid in advance; its card is used
 * @param amount        amount to pre-authorise
 */
public record DepositRequest(@NotNull UUID rentalId, @NotNull UUID reservationId, @NotNull Money amount) {
}
