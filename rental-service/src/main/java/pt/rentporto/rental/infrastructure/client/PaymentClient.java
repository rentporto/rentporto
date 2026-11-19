package pt.rentporto.rental.infrastructure.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import pt.rentporto.events.Money;

/**
 * Internal endpoint of the payment module of the booking-service that pre-authorises deposits.
 */
@FeignClient(name = "payment", url = "${rentporto.services.booking}", configuration = PaymentClientConfiguration.class)
interface PaymentClient {

    @PostMapping("/internal/deposits")
    Deposit authorize(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody DepositRequest request);

    record DepositRequest(UUID rentalId, UUID reservationId, Money amount) {
    }

    record Deposit(UUID depositId) {
    }
}
