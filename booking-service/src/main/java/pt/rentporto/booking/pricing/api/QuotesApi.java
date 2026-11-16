package pt.rentporto.booking.pricing.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Quotes.
 */
@Tag(name = "Quotes")
public interface QuotesApi {

    /**
     * Issues a quote for a rental, valid for 15 minutes. Public.
     *
     * @param idempotencyKey optional key that makes a repeated request return the same quote
     * @param request        what the customer wants to rent
     * @return the quote with its price lines
     */
    @SecurityRequirements
    @PostMapping("/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    QuoteResponse createQuote(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody QuoteRequest request);
}
