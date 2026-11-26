package pt.rentporto.rental.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pt.rentporto.events.Money;

/**
 * Damage found at the return inspection that was not recorded at pick-up.
 *
 * @param location    part of the vehicle, e.g. {@code FRONT_BUMPER}
 * @param description what the agent recorded
 * @param amount      amount charged for the damage, up to the excess
 */
public record Damage(@NotBlank String location, @NotBlank String description, @NotNull Money amount) {
}
