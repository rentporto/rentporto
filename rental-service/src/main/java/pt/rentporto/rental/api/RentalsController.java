package pt.rentporto.rental.api;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.rentporto.rental.application.PickUps;
import pt.rentporto.rental.application.Returns;
import pt.rentporto.rental.domain.AdditionalDriver;
import pt.rentporto.rental.domain.Money;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.ReturnDamage;
import pt.rentporto.rental.domain.ReturnInspection;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class RentalsController implements RentalsApi {

    private final PickUps pickUps;
    private final Returns returns;

    RentalsController(PickUps pickUps, Returns returns) {
        this.pickUps = pickUps;
        this.returns = returns;
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
    public ResponseEntity<RentalAgreementResponse> returnVehicle(UUID id, ReturnRequest request) {
        Inspection inspection = request.inspection();
        List<ReturnDamage> damages = inspection.damages() == null ? List.of()
                : inspection.damages().stream()
                        .map(damage -> new ReturnDamage(damage.location(), damage.description(),
                                Money.ofCents(damage.amount().amountCents())))
                        .toList();
        RentalAgreement agreement = returns.returnVehicle(id, request.station(), new ReturnInspection(
                inspection.odometerKm(),
                inspection.fuelLevelEighths(),
                request.litresToFill() == null ? BigDecimal.ZERO : request.litresToFill(),
                damages,
                inspection.notes()));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/rentals/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.accepted().location(location).body(RentalAgreementResponse.from(agreement));
    }

    private static AdditionalDriver driver(Driver driver) {
        DriverLicense license = driver.driverLicense();
        return new AdditionalDriver(driver.fullName(), license.number(), license.category(),
                license.issuingCountry(), license.issueDate(), license.expiryDate());
    }
}
