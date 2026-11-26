package pt.rentporto.booking.payment.api.controller;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.booking.payment.api.contract.ReceiptsApi;
import pt.rentporto.booking.payment.api.response.ReceiptResponse;
import pt.rentporto.web.error.EndpointNotImplementedException;

@RestController
class ReceiptsController implements ReceiptsApi {

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ReceiptResponse receipt(UUID id) {
        throw new EndpointNotImplementedException();
    }
}
