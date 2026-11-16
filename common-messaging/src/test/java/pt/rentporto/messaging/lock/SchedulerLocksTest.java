package pt.rentporto.messaging.lock;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.messaging.testapp.MessagingTestApplication;

@SpringBootTest(classes = MessagingTestApplication.class)
class SchedulerLocksTest {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    @Autowired
    private SchedulerLocks replica;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void runsTheTaskWhenTheLockIsFree() {
        AtomicInteger runs = new AtomicInteger();

        boolean ran = replica.runOnOneReplica(lockName(), ONE_MINUTE, runs::incrementAndGet);

        assertThat(ran).isTrue();
        assertThat(runs).hasValue(1);
    }

    @Test
    void skipsTheTaskWhileAnotherReplicaHoldsTheLock() {
        String name = lockName();
        AtomicBoolean otherRan = new AtomicBoolean();

        replica.runOnOneReplica(name, ONE_MINUTE,
                () -> otherReplica().runOnOneReplica(name, ONE_MINUTE, () -> otherRan.set(true)));

        assertThat(otherRan).isFalse();
    }

    @Test
    void letsAnotherReplicaRunOnceTheLockIsReleased() {
        String name = lockName();
        replica.runOnOneReplica(name, ONE_MINUTE, () -> { });

        assertThat(otherReplica().runOnOneReplica(name, ONE_MINUTE, () -> { })).isTrue();
    }

    @Test
    void takesOverTheLockOfAReplicaThatDied() {
        String name = lockName();
        Instant now = Instant.now();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> entityManager.persist(
                new SchedulerLock(name, now.minusSeconds(1), now.minusSeconds(61), "replica-that-died")));

        assertThat(replica.runOnOneReplica(name, ONE_MINUTE, () -> { })).isTrue();
    }

    @Test
    void runsOnOneOfTwoReplicasStartingTogether() throws Exception {
        String name = lockName();
        AtomicInteger runs = new AtomicInteger();
        CyclicBarrier start = new CyclicBarrier(2);
        Callable<Boolean> first = attempt(replica, name, start, runs);
        Callable<Boolean> second = attempt(otherReplica(), name, start, runs);

        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = threads.invokeAll(List.of(first, second));
            assertThat(results.get(0).get() ^ results.get(1).get()).isTrue();
        } finally {
            threads.shutdownNow();
        }
        assertThat(runs).hasValue(1);
    }

    private Callable<Boolean> attempt(SchedulerLocks locks, String name, CyclicBarrier start, AtomicInteger runs) {
        return () -> {
            start.await();
            return locks.runOnOneReplica(name, ONE_MINUTE, () -> {
                runs.incrementAndGet();
                sleep(Duration.ofMillis(500));
            });
        };
    }

    private SchedulerLocks otherReplica() {
        return new SchedulerLocks(entityManager, new TransactionTemplate(transactionManager));
    }

    private static String lockName() {
        return "test-" + UUID.randomUUID();
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
