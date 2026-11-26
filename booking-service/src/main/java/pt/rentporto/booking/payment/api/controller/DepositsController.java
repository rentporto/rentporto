package pt.rentporto.booking.payment.api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.booking.payment.api.contract.DepositsApi;
import pt.rentporto.booking.payment.api.request.DepositRequest;
import pt.rentporto.booking.payment.api.response.DepositResponse;
import pt.rentporto.web.error.EndpointNotImplementedException;

@RestController
class DepositsController implements DepositsApi {

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public DepositResponse authorizeDeposit(String idempotencyKey, DepositRequest request) {
        throw new EndpointNotImplementedException();
    }
}
