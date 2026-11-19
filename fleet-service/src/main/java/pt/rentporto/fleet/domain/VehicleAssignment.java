package pt.rentporto.fleet.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Vehicle handed over to a rental at pick-up, with its odometer reading at that moment. A rental has one assignment,
 * made by {@link Vehicle#assignTo(UUID, String)} and closed when the vehicle is returned.
 */
@Entity
@Table(name = "vehicle_assignments")
public class VehicleAssignment {

    @Id
    @Column(name = "rental_id")
    private UUID rentalId;

    @Embedded
    private LicensePlate plate;

    @Column(name = "station_code", nullable = false, updatable = false)
    private String station;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "category_code", nullable = false, updatable = false))
    private AcrissCode category;

    @Column(name = "odometer_km", nullable = false, updatable = false)
    private long odometerKm;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    protected VehicleAssignment() {
    }

    static VehicleAssignment of(UUID rentalId, Vehicle vehicle, String idempotencyKey) {
        VehicleAssignment assignment = new VehicleAssignment();
        assignment.rentalId = Objects.requireNonNull(rentalId, "rentalId");
        assignment.plate = vehicle.plate();
        assignment.station = vehicle.station();
        assignment.category = vehicle.category();
        assignment.odometerKm = vehicle.odometerKm();
        assignment.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
        assignment.assignedAt = Instant.now();
        return assignment;
    }

    /**
     * Closes the assignment when the vehicle is returned.
     *
     * @return {@code true} if it was closed now, {@code false} if the vehicle had already been returned
     */
    public boolean close() {
        if (returnedAt != null) {
            return false;
        }
        returnedAt = Instant.now();
        return true;
    }

    /**
     * Tells whether the vehicle was returned.
     *
     * @return {@code true} if the assignment is closed
     */
    public boolean returned() {
        return returnedAt != null;
    }

    /**
     * Tells whether a request repeats the one that made the assignment.
     *
     * @param key idempotency key of the request
     * @return {@code true} if the assignment was made with this key
     */
    public boolean createdWith(String key) {
        return idempotencyKey.equals(key);
    }

    public UUID rentalId() {
        return rentalId;
    }

    public LicensePlate plate() {
        return plate;
    }

    public String station() {
        return station;
    }

    public AcrissCode category() {
        return category;
    }

    public long odometerKm() {
        return odometerKm;
    }
}
