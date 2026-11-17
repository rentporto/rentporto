package pt.rentporto.booking.payment.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class DepositsController implements DepositsApi {

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public DepositResponse authorizeDeposit(String idempotencyKey, DepositRequest request) {
        throw new EndpointNotImplementedException();
    }
}
