package pt.rentporto.booking.payment.api;

import java.util.UUID;
import pt.rentporto.events.Money;

/**
 * Security deposit held on the card of the reservation.
 *
 * @param depositId deposit identifier, used later to settle it
 * @param status    current state
 * @param amount    amount held
 */
public record DepositResponse(UUID depositId, DepositStatus status, Money amount) {
}
