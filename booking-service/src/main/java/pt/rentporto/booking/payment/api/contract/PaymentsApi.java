package pt.rentporto.booking.payment.api.contract;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pt.rentporto.booking.payment.api.response.PaymentResponse;

/**
 * Payments of reservations.
 */
@Tag(name = "Payments")
public interface PaymentsApi {

    /**
     * Lists the payments of a reservation. Roles: CUSTOMER (owner), AGENT, ADMIN.
     *
     * @param reservationId reservation identifier
     * @return the payments
     */
    @GetMapping("/payments")
    List<PaymentResponse> payments(@RequestParam UUID reservationId);
}
