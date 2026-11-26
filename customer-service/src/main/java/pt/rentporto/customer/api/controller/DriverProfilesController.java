package pt.rentporto.customer.api.controller;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.customer.api.contract.DriverProfilesApi;
import pt.rentporto.customer.api.response.DriverProfileResponse;
import pt.rentporto.customer.application.service.CustomerProfiles;
import pt.rentporto.customer.domain.model.DriverProfile;

/**
 * Called by the booking-service with the JWT of the customer who is booking (ADR-22), so the customer can only read
 * their own profile.
 */
@RestController
class DriverProfilesController implements DriverProfilesApi {

    private final CustomerProfiles profiles;

    DriverProfilesController(CustomerProfiles profiles) {
        this.profiles = profiles;
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER') and #id.toString() == authentication.name")
    public DriverProfileResponse driverProfile(UUID id, LocalDate on) {
        DriverProfile profile = profiles.driverProfile(id, on);
        return new DriverProfileResponse(id, on, profile.age(), profile.licenseYears(), profile.licenseValid());
    }
}
