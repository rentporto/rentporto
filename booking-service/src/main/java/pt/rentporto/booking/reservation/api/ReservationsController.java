package pt.rentporto.booking.reservation.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class ReservationsController implements ReservationsApi {

    @Override
    public ReservationResponse createReservation(String idempotencyKey, CreateReservationRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public ReservationResponse reservation(UUID id) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public List<ReservationResponse> myReservations() {
        throw new EndpointNotImplementedException();
    }

    @Override
    public ReservationResponse cancel(UUID id, CancellationRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public List<ReservationResponse> pickUpsOfTheDay(String station, LocalDate date) {
        throw new EndpointNotImplementedException();
    }
}
