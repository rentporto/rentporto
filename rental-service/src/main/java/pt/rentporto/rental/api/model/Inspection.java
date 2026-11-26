package pt.rentporto.rental.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * State of the vehicle recorded by the agent at pick-up or return.
 *
 * @param odometerKm       odometer reading in kilometres
 * @param fuelLevelEighths fuel level in eighths of a tank, from 0 to 8
 * @param damages          new damages, only recorded on return; possibly none
 * @param notes            free notes of the agent; optional
 */
public record Inspection(
        @PositiveOrZero long odometerKm,
        @Min(0) @Max(8) int fuelLevelEighths,
        List<@Valid Damage> damages,
        String notes) {
}
