package pt.rentporto.booking.payment.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Security deposits for other services; not exposed by the gateway.
 */
@Tag(name = "Internal")
public interface DepositsApi {

    /**
     * Pre-authorises the security deposit at pick-up, called by the rental-service while the customer waits at the
     * desk.
     *
     * @param idempotencyKey optional key that makes a repeated request return the same deposit
     * @param request        rental, reservation and amount
     * @return the deposit pre-authorised
     */
    @PostMapping("/internal/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    DepositResponse authorizeDeposit(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody DepositRequest request);
}
