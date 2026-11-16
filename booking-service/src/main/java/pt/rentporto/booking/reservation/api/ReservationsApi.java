package pt.rentporto.booking.reservation.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Reservations.
 */
@Tag(name = "Reservations")
public interface ReservationsApi {

    /**
     * Books a quote and starts the payment; the reservation stays in {@code PENDING_PAYMENT} until the payment is
     * confirmed. The response carries a {@code Location} header to follow its state. Business errors:
     * {@code DRIVER_NOT_ELIGIBLE}, {@code QUOTE_DRIVER_MISMATCH}, {@code QUOTE_EXPIRED}, {@code QUOTE_ALREADY_USED},
     * {@code NO_AVAILABILITY}. Roles: CUSTOMER.
     *
     * @param idempotencyKey key that makes a repeated request return the same reservation
     * @param request        quote to book and payment method
     * @return the reservation created
     */
    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ReservationResponse createReservation(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateReservationRequest request);

    /**
     * Reads a reservation. Roles: CUSTOMER (owner), AGENT.
     *
     * @param id reservation identifier
     * @return the reservation
     */
    @GetMapping("/reservations/{id}")
    ReservationResponse reservation(@PathVariable UUID id);

    /**
     * Lists the reservations of the authenticated customer. Roles: CUSTOMER.
     *
     * @return the reservations
     */
    @GetMapping("/reservations/me")
    List<ReservationResponse> myReservations();

    /**
     * Cancels a reservation; the penalty is zero up to 48 hours before pick-up and one day afterwards. The response
     * carries a {@code Location} header to follow the refund. Business error: {@code RESERVATION_NOT_CANCELLABLE}.
     * Roles: CUSTOMER (owner), AGENT.
     *
     * @param id      reservation identifier
     * @param request reason of the cancellation
     * @return the reservation being cancelled
     */
    @PostMapping("/reservations/{id}/cancellation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    ReservationResponse cancel(@PathVariable UUID id, @Valid @RequestBody CancellationRequest request);

    /**
     * Lists the pick-ups scheduled at a station on a day. Roles: AGENT.
     *
     * @param station code of the station
     * @param date    day of the pick-ups
     * @return the reservations to pick up
     */
    @GetMapping("/reservations")
    List<ReservationResponse> pickUpsOfTheDay(
            @RequestParam String station, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date);
}
