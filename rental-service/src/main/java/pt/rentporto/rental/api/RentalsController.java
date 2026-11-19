package pt.rentporto.rental.api;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.rental.application.PickUps;
import pt.rentporto.rental.domain.AdditionalDriver;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class RentalsController implements RentalsApi {

    private final PickUps pickUps;

    RentalsController(PickUps pickUps) {
        this.pickUps = pickUps;
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public List<RentalAgreementResponse> rentals(String station, RentalStatus status) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT', 'CUSTOMER')")
    public RentalAgreementResponse rental(UUID id) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public RentalAgreementResponse pickUp(UUID id, PickUpRequest request) {
        List<AdditionalDriver> drivers = request.additionalDrivers() == null ? List.of()
                : request.additionalDrivers().stream().map(RentalsController::driver).toList();
        return RentalAgreementResponse.from(pickUps.pickUp(id, drivers, request.notes()));
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public RentalAgreementResponse returnVehicle(UUID id, ReturnRequest request) {
        throw new EndpointNotImplementedException();
    }

    private static AdditionalDriver driver(Driver driver) {
        DriverLicense license = driver.driverLicense();
        return new AdditionalDriver(driver.fullName(), license.number(), license.category(),
                license.issuingCountry(), license.issueDate(), license.expiryDate());
    }
}
