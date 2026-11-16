package pt.rentporto.rental.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Rental agreements: pick-up, return and settlement.
 */
@Tag(name = "Rentals")
public interface RentalsApi {

    /**
     * Lists the rental agreements of a station, optionally by status. Roles: AGENT.
     *
     * @param station code of the station, e.g. {@code OPO}; all stations if absent
     * @param status  status of the agreements; all statuses if absent
     * @return the matching agreements
     */
    @GetMapping("/rentals")
    List<RentalAgreementResponse> rentals(
            @RequestParam(required = false) String station, @RequestParam(required = false) RentalStatus status);

    /**
     * Reads a rental agreement with its charges and status. Roles: AGENT, CUSTOMER (owner).
     *
     * @param id rental agreement identifier
     * @return the agreement
     */
    @GetMapping("/rentals/{id}")
    RentalAgreementResponse rental(@PathVariable UUID id);

    /**
     * Registers the pick-up: assigns a vehicle of the category, pre-authorises the security deposit and records the
     * check-out inspection. Roles: AGENT.
     *
     * @param id      rental agreement identifier
     * @param request additional drivers and check-out inspection
     * @return the active agreement
     */
    @PostMapping("/rentals/{id}/pickup")
    RentalAgreementResponse pickUp(@PathVariable UUID id, @Valid @RequestBody PickUpRequest request);

    /**
     * Registers the return and starts the settlement of the deposit, which finishes asynchronously; the response has
     * a {@code Location} header to follow the agreement. Roles: AGENT.
     *
     * @param id      rental agreement identifier
     * @param request return station and inspection
     * @return the returned agreement
     */
    @PostMapping("/rentals/{id}/return")
    @ResponseStatus(HttpStatus.ACCEPTED)
    RentalAgreementResponse returnVehicle(@PathVariable UUID id, @Valid @RequestBody ReturnRequest request);
}
