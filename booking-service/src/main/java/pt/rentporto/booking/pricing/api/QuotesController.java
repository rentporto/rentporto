package pt.rentporto.booking.pricing.api;

import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class QuotesController implements QuotesApi {

    @Override
    public QuoteResponse createQuote(String idempotencyKey, QuoteRequest request) {
        throw new EndpointNotImplementedException();
    }
}
