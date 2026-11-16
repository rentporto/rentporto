package pt.rentporto.booking.reservation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Booking of a quote.
 *
 * @param quoteId         quote to book; must not have expired nor been used
 * @param paymentMethodId Stripe test payment method, e.g. {@code pm_card_visa}
 */
public record CreateReservationRequest(@NotNull UUID quoteId, @NotBlank String paymentMethodId) {
}
