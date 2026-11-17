package pt.rentporto.booking.reservation.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class ReservationsController implements ReservationsApi {

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ReservationResponse createReservation(String idempotencyKey, CreateReservationRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT')")
    public ReservationResponse reservation(UUID id) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<ReservationResponse> myReservations() {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT')")
    public ReservationResponse cancel(UUID id, CancellationRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('AGENT')")
    public List<ReservationResponse> pickUpsOfTheDay(String station, LocalDate date) {
        throw new EndpointNotImplementedException();
    }
}
