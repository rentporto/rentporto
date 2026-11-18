package pt.rentporto.fleet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Entry of the history of a vehicle: the state it left, the state it entered, why and who changed it. The registration
 * is the first entry, without a previous state.
 */
@Entity
@Table(name = "vehicle_status_changes")
public class StatusChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private LicensePlate plate;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private VehicleStatus from;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private VehicleStatus to;

    @Column(name = "reason")
    private String reason;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected StatusChange() {
    }

    /**
     * Creates an entry.
     *
     * @param plate     licence plate of the vehicle
     * @param from      state it left, or {@code null} when it was registered
     * @param to        state it entered
     * @param reason    why, as the user wrote it, or {@code null}
     * @param changedBy id of the user who changed it
     * @param changedAt when
     * @return the entry
     */
    public static StatusChange of(
            LicensePlate plate,
            VehicleStatus from,
            VehicleStatus to,
            String reason,
            String changedBy,
            Instant changedAt) {
        StatusChange change = new StatusChange();
        change.plate = Objects.requireNonNull(plate, "plate");
        change.from = from;
        change.to = Objects.requireNonNull(to, "to");
        change.reason = reason;
        change.changedBy = Objects.requireNonNull(changedBy, "changedBy");
        change.changedAt = Objects.requireNonNull(changedAt, "changedAt");
        return change;
    }

    public LicensePlate plate() {
        return plate;
    }

    public VehicleStatus from() {
        return from;
    }

    public VehicleStatus to() {
        return to;
    }

    public String reason() {
        return reason;
    }

    public String changedBy() {
        return changedBy;
    }
}
