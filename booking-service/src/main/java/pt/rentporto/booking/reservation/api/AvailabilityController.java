package pt.rentporto.booking.reservation.api;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class AvailabilityController implements AvailabilityApi {

    @Override
    public List<AvailabilityResponse> availability(
            String pickupStation, String returnStation, OffsetDateTime from, OffsetDateTime to) {
        throw new EndpointNotImplementedException();
    }
}
