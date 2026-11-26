package pt.rentporto.rental.api.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.rentporto.rental.api.contract.RentalsApi;
import pt.rentporto.rental.api.model.Driver;
import pt.rentporto.rental.api.model.DriverLicense;
import pt.rentporto.rental.api.model.Inspection;
import pt.rentporto.rental.api.model.RentalStatus;
import pt.rentporto.rental.api.request.PickUpRequest;
import pt.rentporto.rental.api.request.ReturnRequest;
import pt.rentporto.rental.api.response.RentalAgreementResponse;
import pt.rentporto.rental.application.service.PickUps;
import pt.rentporto.rental.application.service.RentalQueries;
import pt.rentporto.rental.application.service.Returns;
import pt.rentporto.rental.domain.model.AdditionalDriver;
import pt.rentporto.rental.domain.model.Money;
import pt.rentporto.rental.domain.model.RentalAgreement;
import pt.rentporto.rental.domain.model.ReturnDamage;
import pt.rentporto.rental.domain.model.ReturnInspection;
import pt.rentporto.web.security.AuthenticatedUser;

@RestController
class RentalsController implements RentalsApi {

    private final PickUps pickUps;
    private final Returns returns;
    private final RentalQueries queries;

    RentalsController(PickUps pickUps, Returns returns, RentalQueries queries) {
        this.pickUps = pickUps;
        this.returns = returns;
        this.queries = queries;
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public List<RentalAgreementResponse> rentals(String station, RentalStatus status) {
        pt.rentporto.rental.domain.model.RentalStatus state =
                status == null ? null : pt.rentporto.rental.domain.model.RentalStatus.valueOf(status.name());
        return queries.agreements(station, state).stream().map(RentalAgreementResponse::from).toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT', 'CUSTOMER')")
    public RentalAgreementResponse rental(UUID id) {
        RentalAgreement agreement = AuthenticatedUser.hasRole("AGENT")
                ? queries.agreement(id)
                : queries.agreementOf(id, AuthenticatedUser.id());
        return RentalAgreementResponse.from(agreement);
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
