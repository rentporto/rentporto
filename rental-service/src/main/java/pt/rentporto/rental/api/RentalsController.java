package pt.rentporto.rental.api;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class RentalsController implements RentalsApi {

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
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public RentalAgreementResponse returnVehicle(UUID id, ReturnRequest request) {
        throw new EndpointNotImplementedException();
    }
}
