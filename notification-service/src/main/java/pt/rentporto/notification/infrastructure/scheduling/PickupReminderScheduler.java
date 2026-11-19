package pt.rentporto.notification.infrastructure.scheduling;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import pt.rentporto.messaging.lock.SchedulerLocks;
import pt.rentporto.notification.application.UpcomingPickups;

/**
 * Sends the reminders due at every poll interval, on one replica at a time (ADR-18).
 */
@Component
class PickupReminderScheduler implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(PickupReminderScheduler.class);
    private static final String LOCK_NAME = "pickup-reminders";
    private static final Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(10);

    private final UpcomingPickups pickups;
    private final SchedulerLocks locks;
    private final Duration pollInterval;
    private ScheduledExecutorService scheduler;

    PickupReminderScheduler(UpcomingPickups pickups, SchedulerLocks locks, PickupReminderProperties properties) {
        this.pickups = pickups;
        this.locks = locks;
        this.pollInterval = properties.pollInterval();
    }

    @Override
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name(LOCK_NAME).factory());
        long delay = pollInterval.toMillis();
        scheduler.scheduleWithFixedDelay(this::remindOnOneReplica, delay, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        scheduler.shutdownNow();
        scheduler = null;
    }

    @Override
    public boolean isRunning() {
        return scheduler != null;
    }

    private void remindOnOneReplica() {
        try {
            locks.runOnOneReplica(LOCK_NAME, LOCK_AT_MOST_FOR, pickups::remindDue);
        } catch (RuntimeException e) {
            log.warn("Pick-up reminder run failed; trying again on the next run", e);
        }
    }
}
