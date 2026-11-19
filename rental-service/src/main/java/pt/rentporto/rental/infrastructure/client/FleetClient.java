package pt.rentporto.rental.infrastructure.client;

import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Internal endpoint of the fleet-service that assigns vehicles.
 */
@FeignClient(name = "fleet", url = "${rentporto.services.fleet}", configuration = FleetClientConfiguration.class)
interface FleetClient {

    @PostMapping("/internal/vehicles/assignments")
    Assignment assign(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody AssignmentRequest request);

    record AssignmentRequest(UUID rentalId, String categoryCode, String station) {
    }

    record Assignment(String licensePlate, long odometerKm) {
    }
}
