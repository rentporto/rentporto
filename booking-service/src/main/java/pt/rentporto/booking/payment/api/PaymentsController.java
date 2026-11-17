package pt.rentporto.booking.payment.api;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class PaymentsController implements PaymentsApi {

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT', 'ADMIN')")
    public List<PaymentResponse> payments(UUID reservationId) {
        throw new EndpointNotImplementedException();
    }
}
