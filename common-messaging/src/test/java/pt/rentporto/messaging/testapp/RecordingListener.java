package pt.rentporto.messaging.testapp;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.reservation.ReservationExpired;
import pt.rentporto.messaging.consumer.IdempotentConsumer;

/**
 * Consumer of the test queue that counts, per message, how many times it was delivered and how many times its handler
 * ran. The handler fails for messages whose reason is {@link #FAILING_REASON}.
 */
@Component
public class RecordingListener {

    public static final String FAILING_REASON = "FAIL";

    private final IdempotentConsumer consumer;
    private final Map<UUID, AtomicInteger> deliveries = new ConcurrentHashMap<>();
    private final Map<UUID, AtomicInteger> handled = new ConcurrentHashMap<>();

    RecordingListener(IdempotentConsumer consumer) {
        this.consumer = consumer;
    }

    @RabbitListener(queues = MessagingTestApplication.QUEUE)
    public void on(EventEnvelope<ReservationExpired> message) {
        increment(deliveries, message.eventId());
        consumer.process(MessagingTestApplication.QUEUE, message, expired -> {
            increment(handled, message.eventId());
            if (FAILING_REASON.equals(expired.reason())) {
                throw new IllegalStateException("Handler failed for " + message.eventId());
            }
        });
    }

    public int deliveries(UUID eventId) {
        return count(deliveries, eventId);
    }

    public int handled(UUID eventId) {
        return count(handled, eventId);
    }

    private static void increment(Map<UUID, AtomicInteger> counters, UUID eventId) {
        counters.computeIfAbsent(eventId, id -> new AtomicInteger()).incrementAndGet();
    }

    private static int count(Map<UUID, AtomicInteger> counters, UUID eventId) {
        AtomicInteger counter = counters.get(eventId);
        return counter == null ? 0 : counter.get();
    }
}
