package pt.rentporto.customer.api.contract;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import pt.rentporto.customer.api.response.DriverProfileResponse;

/**
 * Driver profiles for other services; not exposed by the gateway.
 */
@Tag(name = "Internal")
public interface DriverProfilesApi {

    /**
     * Age and licence seniority of a customer on a date, used by the booking-service to check eligibility.
     *
     * @param id customer identifier
     * @param on date to compute the profile for, usually the pick-up date
     * @return the driver profile
     */
    @GetMapping("/internal/customers/{id}/driver-profile")
    DriverProfileResponse driverProfile(
            @PathVariable UUID id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on);
}
