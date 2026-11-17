package pt.rentporto.customer.api;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class DriverProfilesController implements DriverProfilesApi {

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public DriverProfileResponse driverProfile(UUID id, LocalDate on) {
        throw new EndpointNotImplementedException();
    }
}
