package pt.rentporto.messaging.lock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Lock shared by the replicas of a service. Internal to the library: public only so the auto-configuration can
 * register it in the service's persistence unit.
 */
@Entity
@Table(name = "scheduler_lock")
public class SchedulerLock {

    @Id
    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "locked_until", nullable = false)
    private Instant lockedUntil;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;

    @Column(name = "locked_by", nullable = false)
    private String lockedBy;

    protected SchedulerLock() {
    }

    /**
     * Creates a lock held by a replica.
     *
     * @param name        lock name
     * @param lockedUntil instant after which the lock is free even if not released
     * @param lockedAt    instant the lock was taken
     * @param lockedBy    replica holding the lock
     */
    SchedulerLock(String name, Instant lockedUntil, Instant lockedAt, String lockedBy) {
        this.name = name;
        this.lockedUntil = lockedUntil;
        this.lockedAt = lockedAt;
        this.lockedBy = lockedBy;
    }
}
