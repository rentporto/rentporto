package pt.rentporto.rental.infrastructure.client;

import feign.FeignException;
import feign.RetryableException;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pt.rentporto.rental.application.Fleet;
import pt.rentporto.rental.application.FleetUnavailableException;
import pt.rentporto.rental.application.NoVehicleAvailableException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Gets vehicles from the fleet-service with the token of the agent. The key of a rental is always the same, so a
 * pick-up tried again gets the vehicle already assigned.
 */
@Component
class FeignFleet implements Fleet {

    private static final int CONFLICT = 409;

    private final FleetClient client;
    private final JsonMapper jsonMapper;

    FeignFleet(FleetClient client, JsonMapper jsonMapper) {
        this.client = client;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public AssignedVehicle assign(UUID rentalId, String categoryCode, String station) {
        try {
            FleetClient.Assignment assignment = client.assign(
                    CurrentAuthorization.header(),
                    "assignment-" + rentalId,
                    new FleetClient.AssignmentRequest(rentalId, categoryCode, station));
            return new AssignedVehicle(assignment.licensePlate(), assignment.odometerKm());
        } catch (RetryableException e) {
            throw new FleetUnavailableException(rentalId);
        } catch (FeignException e) {
            if (e.status() == CONFLICT && "NO_VEHICLE_AVAILABLE".equals(code(e))) {
                throw new NoVehicleAvailableException(station, categoryCode);
            }
            if (e.status() >= 500) {
                throw new FleetUnavailableException(rentalId);
            }
            throw e;
        }
    }

    private String code(FeignException e) {
        return jsonMapper.readTree(e.contentUTF8()).path("code").asString();
    }
}
