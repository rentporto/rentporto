package pt.rentporto.messaging.consumer;

import jakarta.persistence.EntityManager;
import java.util.function.Consumer;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.events.EventEnvelope;

/**
 * Runs the handler of a message at most once per consumer, however many times RabbitMQ delivers it.
 *
 * <p>The handler runs in the same transaction that records the message as handled: if the handler fails, nothing is
 * recorded and the message can be retried. Only effects in the service's database are covered; calls to other systems
 * need their own idempotency.
 */
public class IdempotentConsumer {

    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    /**
     * Creates a consumer.
     *
     * @param entityManager       shared entity manager of the service
     * @param transactionTemplate template on the transaction manager of the service
     */
    public IdempotentConsumer(EntityManager entityManager, TransactionTemplate transactionTemplate) {
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Handles a message unless this consumer already handled it.
     *
     * @param consumer name of the consumer, by convention its queue (e.g. {@code rental.reservation-confirmed})
     * @param message  message received
     * @param handler  effect of the message, given its payload
     * @param <T>      payload type
     * @return {@code true} if the handler ran, {@code false} if the message was a duplicate
     */
    public <T> boolean process(String consumer, EventEnvelope<T> message, Consumer<T> handler) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            ProcessedEvent.Key key = new ProcessedEvent.Key(consumer, message.eventId());
            if (entityManager.find(ProcessedEvent.class, key) != null) {
                return false;
            }
            entityManager.persist(new ProcessedEvent(key));
            entityManager.flush();
            handler.accept(message.payload());
            return true;
        }));
    }
}
