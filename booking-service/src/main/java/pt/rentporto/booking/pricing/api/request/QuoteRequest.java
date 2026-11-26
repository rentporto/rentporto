package pt.rentporto.booking.pricing.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import pt.rentporto.booking.pricing.api.model.CoverageCode;
import pt.rentporto.booking.pricing.api.model.ExtraSelection;
import pt.rentporto.events.RentalPeriod;

/**
 * What a customer wants to rent.
 *
 * @param categoryCode    ACRISS code of the vehicle category, e.g. {@code ECMR}
 * @param pickupStation   code of the pick-up station: OPO, CMP, TRD or MAT
 * @param returnStation   code of the return station; differs from {@code pickupStation} on a one-way rental
 * @param rentalPeriod    pick-up and return date-times
 * @param driverBirthDate date of birth of the driver; decides the driver age band
 * @param extras          extras wanted, possibly none
 * @param coverage        coverage chosen
 */
public record QuoteRequest(
        @NotNull @Pattern(regexp = "[A-Z]{4}") String categoryCode,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String pickupStation,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String returnStation,
        @NotNull RentalPeriod rentalPeriod,
        @NotNull @Past LocalDate driverBirthDate,
        @NotNull List<@Valid ExtraSelection> extras,
        @NotNull CoverageCode coverage) {
}
