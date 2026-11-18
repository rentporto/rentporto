package pt.rentporto.fleet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Group of equivalent vehicles that a customer books, identified by its ACRISS code, with the requirements a driver
 * must meet. The code never changes: the bookings and the rates refer to it.
 */
@Entity
@Table(name = "vehicle_categories")
public class VehicleCategory {

    @EmbeddedId
    private AcrissCode code;

    @Column(name = "name", nullable = false)
    private String name;

    @Embedded
    private DriverRequirements requirements;

    @Column(name = "idempotency_key", updatable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VehicleCategory() {
    }

    /**
     * Creates a category.
     *
     * @param code           ACRISS code
     * @param name           name shown to customers, e.g. {@code Group B - Economy}
     * @param requirements   what a driver needs
     * @param idempotencyKey key of the request that creates it, or {@code null}
     * @return the category
     */
    public static VehicleCategory create(
            AcrissCode code, String name, DriverRequirements requirements, String idempotencyKey) {
        VehicleCategory category = new VehicleCategory();
        category.code = Objects.requireNonNull(code, "code");
        category.name = Objects.requireNonNull(name, "name").strip();
        category.requirements = Objects.requireNonNull(requirements, "requirements");
        category.idempotencyKey = idempotencyKey;
        category.createdAt = Instant.now();
        category.updatedAt = category.createdAt;
        return category;
    }

    /**
     * Replaces the name and the requirements.
     *
     * @param name         new name
     * @param requirements new requirements
     * @return {@code true} if something changed
     */
    public boolean change(String name, DriverRequirements requirements) {
        String newName = Objects.requireNonNull(name, "name").strip();
        Objects.requireNonNull(requirements, "requirements");
        if (this.name.equals(newName) && this.requirements.equals(requirements)) {
            return false;
        }
        this.name = newName;
        this.requirements = requirements;
        this.updatedAt = Instant.now();
        return true;
    }

    /**
     * Tells whether a request repeats the one that created the category.
     *
     * @param key idempotency key of the request, or {@code null}
     * @return {@code true} if the category was created with this key
     */
    public boolean createdWith(String key) {
        return key != null && key.equals(idempotencyKey);
    }

    public AcrissCode code() {
        return code;
    }

    public String name() {
        return name;
    }

    public DriverRequirements requirements() {
        return requirements;
    }
}
