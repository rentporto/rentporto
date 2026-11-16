package pt.rentporto.booking.payment.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Entry point of the Stripe webhooks.
 */
@Tag(name = "Stripe webhook")
public interface StripeWebhookApi {

    /**
     * Receives an event from Stripe, e.g. a payment that succeeded. Public, verified by the {@code Stripe-Signature}
     * header.
     *
     * @param payload   Stripe event, as sent
     * @param signature signature of the payload computed by Stripe
     */
    @SecurityRequirements
    @PostMapping("/payments/stripe-webhook")
    void receive(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature);
}
