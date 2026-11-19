package pt.rentporto.rental.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Return of the vehicle.
 *
 * @param station      code of the station where the vehicle was returned, the one agreed at booking, e.g. {@code MAT}
 * @param inspection   return inspection
 * @param litresToFill litres the agent put in to fill the tank, which left full; absent or zero when it came back full
 */
public record ReturnRequest(
        @NotNull @Pattern(regexp = "[A-Z]{3}") String station,
        @NotNull @Valid Inspection inspection,
        @PositiveOrZero @Digits(integer = 3, fraction = 2) BigDecimal litresToFill) {
}
