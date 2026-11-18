package pt.rentporto.fleet.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Vehicle assignment for other services; not exposed by the gateway.
 */
@Tag(name = "Internal")
public interface VehicleAssignmentsApi {

    /**
     * Assigns the vehicle of the category that has been available the longest at the station, called by the
     * rental-service at pick-up. The vehicle becomes rented. A rental has one vehicle: a request with the same key gets
     * the same assignment, and one with another key is a conflict. Roles: AGENT.
     *
     * @param idempotencyKey key that makes a repeated request return the same assignment
     * @param request        rental, category and station
     * @return the assigned vehicle
     */
    @PostMapping("/internal/vehicles/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    VehicleAssignmentResponse assignVehicle(
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody VehicleAssignmentRequest request);
}
