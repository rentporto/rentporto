package pt.rentporto.booking.pricing.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class QuotesController implements QuotesApi {

    @Override
    @PreAuthorize("permitAll()")
    public QuoteResponse createQuote(String idempotencyKey, QuoteRequest request) {
        throw new EndpointNotImplementedException();
    }
}
