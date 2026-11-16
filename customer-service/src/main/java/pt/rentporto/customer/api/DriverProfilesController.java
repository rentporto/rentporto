package pt.rentporto.customer.api;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class DriverProfilesController implements DriverProfilesApi {

    @Override
    public DriverProfileResponse driverProfile(UUID id, LocalDate on) {
        throw new EndpointNotImplementedException();
    }
}
