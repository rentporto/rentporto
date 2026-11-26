package pt.rentporto.fleet.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Movement of a vehicle from one station to another. It starts when the vehicle leaves, see
 * {@link Vehicle#startTransfer(String, String)}, and is completed when the vehicle arrives.
 */
@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    private UUID id;

    @Embedded
    private LicensePlate plate;

    @Column(name = "from_station", nullable = false, updatable = false)
    private String fromStation;

    @Column(name = "to_station", nullable = false, updatable = false)
    private String toStation;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransferStatus status;

    @Column(name = "idempotency_key", updatable = false)
    private String idempotencyKey;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected Transfer() {
    }

    static Transfer start(LicensePlate plate, String fromStation, String toStation, String idempotencyKey) {
        Transfer transfer = new Transfer();
        transfer.id = UUID.randomUUID();
        transfer.plate = Objects.requireNonNull(plate, "plate");
        transfer.fromStation = Objects.requireNonNull(fromStation, "fromStation");
        transfer.toStation = Objects.requireNonNull(toStation, "toStation");
        transfer.status = TransferStatus.IN_TRANSFER;
        transfer.idempotencyKey = idempotencyKey;
        transfer.startedAt = Instant.now();
        return transfer;
    }

    /**
     * Completes the transfer when the vehicle arrives.
     *
     * @return {@code true} if it was completed now, {@code false} if it already was
     */
    public boolean complete() {
        if (status == TransferStatus.COMPLETED) {
            return false;
        }
        status = TransferStatus.COMPLETED;
        completedAt = Instant.now();
        return true;
    }

    /**
     * Tells whether a request repeats the one that started the transfer.
     *
     * @param key idempotency key of the request, or {@code null}
     * @return {@code true} if the transfer was started with this key
     */
    public boolean startedWith(String key) {
        return key != null && key.equals(idempotencyKey);
    }

    public UUID id() {
        return id;
    }

    public LicensePlate plate() {
        return plate;
    }

    public String fromStation() {
        return fromStation;
    }

    public String toStation() {
        return toStation;
    }

    public TransferStatus status() {
        return status;
    }
}
