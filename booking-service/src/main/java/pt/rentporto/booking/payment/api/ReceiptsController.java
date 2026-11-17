package pt.rentporto.booking.payment.api;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class ReceiptsController implements ReceiptsApi {

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ReceiptResponse receipt(UUID id) {
        throw new EndpointNotImplementedException();
    }
}
