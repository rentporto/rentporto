package pt.rentporto.booking.pricing.api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.booking.pricing.api.contract.QuotesApi;
import pt.rentporto.booking.pricing.api.request.QuoteRequest;
import pt.rentporto.booking.pricing.api.response.QuoteResponse;
import pt.rentporto.web.error.EndpointNotImplementedException;

@RestController
class QuotesController implements QuotesApi {

    @Override
    @PreAuthorize("permitAll()")
    public QuoteResponse createQuote(String idempotencyKey, QuoteRequest request) {
        throw new EndpointNotImplementedException();
    }
}
