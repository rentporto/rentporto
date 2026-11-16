package pt.rentporto.rental.api;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class RentalsController implements RentalsApi {

    @Override
    public List<RentalAgreementResponse> rentals(String station, RentalStatus status) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public RentalAgreementResponse rental(UUID id) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public RentalAgreementResponse pickUp(UUID id, PickUpRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public RentalAgreementResponse returnVehicle(UUID id, ReturnRequest request) {
        throw new EndpointNotImplementedException();
    }
}
