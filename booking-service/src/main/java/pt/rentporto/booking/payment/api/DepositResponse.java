package pt.rentporto.booking.payment.api;

import java.time.OffsetDateTime;
import java.util.UUID;
import pt.rentporto.events.Money;

/**
 * Security deposit pre-authorised on the card.
 *
 * @param depositId deposit identifier, used later to settle it
 * @param status    current state
 * @param amount    amount pre-authorised
 * @param expiresAt instant the pre-authorisation lapses unless renewed
 */
public record DepositResponse(UUID depositId, DepositStatus status, Money amount, OffsetDateTime expiresAt) {
}
