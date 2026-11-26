package pt.rentporto.booking.reservation.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Booking of a quote, paid with a test card of the simulated payment provider (ADR-29): {@code card_ok} approves,
 * {@code card_declined} declines, {@code card_no_extra} declines only the charges above the deposit and
 * {@code card_slow} does not answer in time.
 *
 * @param quoteId         quote to book; must not have expired nor been used
 * @param paymentMethodId test card, e.g. {@code card_ok}
 */
public record CreateReservationRequest(@NotNull UUID quoteId, @NotBlank String paymentMethodId) {
}
