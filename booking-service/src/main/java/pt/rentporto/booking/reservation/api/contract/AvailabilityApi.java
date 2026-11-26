package pt.rentporto.booking.reservation.api.contract;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pt.rentporto.booking.reservation.api.response.AvailabilityResponse;

/**
 * Availability of vehicle categories.
 */
@Tag(name = "Availability")
public interface AvailabilityApi {

    /**
     * Lists the categories that can still be booked between two stations for a period. Public.
     *
     * @param pickupStation code of the pick-up station
     * @param returnStation code of the return station
     * @param from          pick-up date-time
     * @param to            return date-time
     * @return the available categories
     */
    @SecurityRequirements
    @GetMapping("/availability")
    List<AvailabilityResponse> availability(
            @RequestParam String pickupStation,
            @RequestParam String returnStation,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to);
}
