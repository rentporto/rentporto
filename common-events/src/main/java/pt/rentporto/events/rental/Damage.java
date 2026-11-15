package pt.rentporto.events.rental;

import java.util.Objects;

/**
 * Damage found at the return inspection that was not recorded at pick-up.
 *
 * @param area        part of the vehicle, e.g. {@code FRONT_BUMPER}
 * @param description what the agent recorded
 */
public record Damage(String area, String description) {

    public Damage {
        Objects.requireNonNull(area, "area");
        Objects.requireNonNull(description, "description");
    }
}
