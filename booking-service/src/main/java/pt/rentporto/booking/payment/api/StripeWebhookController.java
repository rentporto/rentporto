package pt.rentporto.booking.payment.api;

import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class StripeWebhookController implements StripeWebhookApi {

    @Override
    public void receive(String payload, String signature) {
        throw new EndpointNotImplementedException();
    }
}
