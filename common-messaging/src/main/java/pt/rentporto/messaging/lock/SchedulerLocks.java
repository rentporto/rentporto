package pt.rentporto.messaging.lock;

import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Runs a task on one replica at a time, using a {@link SchedulerLock} as a lease.
 *
 * <p>A replica takes the lock only if it is free or expired, and releases it when the task ends. If the replica dies
 * first, the lock expires after the given time. The lease uses the application clock, so the replicas need their
 * clocks in sync.
 */
public class SchedulerLocks {

    private static final Log log = LogFactory.getLog(SchedulerLocks.class);

    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final String replica;

    /**
     * Creates the locks of this replica.
     *
     * @param entityManager       shared entity manager of the service
     * @param transactionTemplate template on the transaction manager of the service
     */
    public SchedulerLocks(EntityManager entityManager, TransactionTemplate transactionTemplate) {
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
        this.replica = System.getenv().getOrDefault("HOSTNAME", "replica") + "-" + UUID.randomUUID();
    }

    /**
     * Runs the task unless another replica holds the lock.
     *
     * @param name          lock name, e.g. {@code outbox-publisher}
     * @param lockAtMostFor how long the lock lasts if this replica dies before releasing it; longer than the task
     * @param task          work to run on one replica
     * @return {@code true} if the task ran, {@code false} if another replica holds the lock
     */
    public boolean runOnOneReplica(String name, Duration lockAtMostFor, Runnable task) {
        if (!acquire(name, lockAtMostFor)) {
            return false;
        }
        try {
            task.run();
        } finally {
            release(name);
        }
        return true;
    }

    private boolean acquire(String name, Duration lockAtMostFor) {
        Instant now = Instant.now();
        Instant until = now.plus(lockAtMostFor);
        try {
            return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
                int taken = entityManager.createQuery("""
                                UPDATE SchedulerLock l
                                SET l.lockedUntil = :until, l.lockedAt = :now, l.lockedBy = :replica
                                WHERE l.name = :name AND l.lockedUntil <= :now
                                """)
                        .setParameter("until", until)
                        .setParameter("now", now)
                        .setParameter("replica", replica)
                        .setParameter("name", name)
                        .executeUpdate();
                if (taken == 1) {
                    return true;
                }
                if (entityManager.find(SchedulerLock.class, name) != null) {
                    return false;
                }
                entityManager.persist(new SchedulerLock(name, until, now, replica));
                entityManager.flush();
                return true;
            }));
        } catch (ConstraintViolationException e) {
            return false;
        }
    }

    private void release(String name) {
        try {
            transactionTemplate.executeWithoutResult(status -> entityManager.createQuery("""
                            UPDATE SchedulerLock l
                            SET l.lockedUntil = :now
                            WHERE l.name = :name AND l.lockedBy = :replica
                            """)
                    .setParameter("now", Instant.now())
                    .setParameter("name", name)
                    .setParameter("replica", replica)
                    .executeUpdate());
        } catch (RuntimeException e) {
            log.warn("Could not release lock " + name + "; it expires on its own", e);
        }
    }
}
