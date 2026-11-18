package pt.rentporto.fleet.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Physical vehicle of the fleet, identified by its licence plate, of one category, at one station and in one state.
 * Every change of state follows {@link VehicleStatus#canChangeTo(VehicleStatus)}.
 */
@Entity
@Table(name = "vehicles")
public class Vehicle {

    /** Fuel level of a full tank, in eighths. */
    public static final int FULL_TANK = 8;

    private static final Set<VehicleStatus> SET_BY_FLEET_MANAGER =
            EnumSet.of(VehicleStatus.AVAILABLE, VehicleStatus.IN_MAINTENANCE, VehicleStatus.RETIRED);

    @EmbeddedId
    private LicensePlate plate;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "category_code", nullable = false, updatable = false))
    private AcrissCode category;

    @Column(name = "station_code", nullable = false)
    private String station;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VehicleStatus status;

    @Column(name = "odometer_km", nullable = false)
    private long odometerKm;

    @Column(name = "fuel_level_eighths", nullable = false)
    private int fuelLevelEighths;

    @Column(name = "idempotency_key", updatable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Vehicle() {
    }

    /**
     * Registers a vehicle: it starts available at its station, with a full tank.
     *
     * @param plate          licence plate
     * @param category       ACRISS code of its category
     * @param station        code of the station where it is
     * @param odometerKm     odometer reading in kilometres
     * @param idempotencyKey key of the request that registers it, or {@code null}
     * @return the vehicle
     */
    public static Vehicle register(
            LicensePlate plate, AcrissCode category, String station, long odometerKm, String idempotencyKey) {
        if (odometerKm < 0) {
            throw new IllegalArgumentException("odometerKm must be >= 0, was " + odometerKm);
        }
        Vehicle vehicle = new Vehicle();
        vehicle.plate = Objects.requireNonNull(plate, "plate");
        vehicle.category = Objects.requireNonNull(category, "category");
        vehicle.station = Objects.requireNonNull(station, "station");
        vehicle.status = VehicleStatus.AVAILABLE;
        vehicle.odometerKm = odometerKm;
        vehicle.fuelLevelEighths = FULL_TANK;
        vehicle.idempotencyKey = idempotencyKey;
        vehicle.createdAt = Instant.now();
        vehicle.updatedAt = vehicle.createdAt;
        return vehicle;
    }

    /**
     * Changes the state as a fleet manager asks: to maintenance and back, or out of the fleet. The other states come
     * from rentals and transfers, so a rented vehicle, for instance, only becomes available when it is returned.
     *
     * @param target the state asked for
     * @return {@code true} if the state changed, {@code false} if the vehicle was already in it
     * @throws InvalidStatusTransitionException if a fleet manager cannot move the vehicle to that state
     */
    public boolean changeStatus(VehicleStatus target) {
        Objects.requireNonNull(target, "target");
        if (target == status) {
            return false;
        }
        if (!SET_BY_FLEET_MANAGER.contains(status) || !SET_BY_FLEET_MANAGER.contains(target)) {
            throw new InvalidStatusTransitionException(plate, status, target);
        }
        moveTo(target);
        return true;
    }

    /**
     * Tells whether the vehicle counts in the capacity of its station.
     *
     * @return {@code true} if its state is one of {@link VehicleStatus#countedInCapacity()}
     */
    public boolean countsInCapacity() {
        return VehicleStatus.countedInCapacity().contains(status);
    }

    /**
     * Tells whether a request repeats the one that registered the vehicle.
     *
     * @param key idempotency key of the request, or {@code null}
     * @return {@code true} if the vehicle was registered with this key
     */
    public boolean registeredWith(String key) {
        return key != null && key.equals(idempotencyKey);
    }

    private void moveTo(VehicleStatus target) {
        if (!status.canChangeTo(target)) {
            throw new InvalidStatusTransitionException(plate, status, target);
        }
        status = target;
        updatedAt = Instant.now();
    }

    public LicensePlate plate() {
        return plate;
    }

    public AcrissCode category() {
        return category;
    }

    public String station() {
        return station;
    }

    public VehicleStatus status() {
        return status;
    }

    public long odometerKm() {
        return odometerKm;
    }

    public int fuelLevelEighths() {
        return fuelLevelEighths;
    }
}
