package pt.rentporto.booking.payment.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class StripeWebhookController implements StripeWebhookApi {

    @Override
    @PreAuthorize("permitAll()")
    public void receive(String payload, String signature) {
        throw new EndpointNotImplementedException();
    }
}
