package pt.rentporto.messaging.outbox;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.Exchanges;
import tools.jackson.databind.json.JsonMapper;

/**
 * Records outgoing messages in the same transaction as the state change that produced them. The
 * {@link OutboxPublisher} sends them after the commit, so a message is never lost and never sent for a change that was
 * rolled back. Each message keeps the trace of the request that wrote it.
 */
public class OutboxWriter {

    private static final String DEFAULT_EXCHANGE = "";

    private final EntityManager entityManager;
    private final JsonMapper jsonMapper;
    private final OutboxTracing tracing;

    /**
     * Creates a writer.
     *
     * @param entityManager shared entity manager of the service, bound to the current transaction
     * @param jsonMapper    mapper used to serialise the envelopes
     * @param tracing       keeps the trace of the request with the message
     */
    public OutboxWriter(EntityManager entityManager, JsonMapper jsonMapper, OutboxTracing tracing) {
        this.entityManager = entityManager;
        this.jsonMapper = jsonMapper;
        this.tracing = tracing;
    }

    /**
     * Records an event for the {@code rentporto.events} exchange.
     *
     * @param routingKey routing key from {@code RoutingKeys}
     * @param event      event wrapped in its envelope
     * @throws IllegalStateException if there is no active transaction
     */
    public void publish(String routingKey, EventEnvelope<?> event) {
        write(Exchanges.EVENTS, routingKey, event);
    }

    /**
     * Records a command for its dedicated queue, sent through the default exchange.
     *
     * @param queue   queue from {@code CommandQueues}
     * @param command command wrapped in its envelope
     * @throws IllegalStateException if there is no active transaction
     */
    public void send(String queue, EventEnvelope<?> command) {
        write(DEFAULT_EXCHANGE, queue, command);
    }

    private void write(String exchange, String routingKey, EventEnvelope<?> envelope) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("The outbox must be written in the transaction of the state change");
        }
        entityManager.persist(new OutboxMessage(envelope.eventId(), exchange, routingKey, envelope.type(),
                jsonMapper.writeValueAsString(envelope), tracing.currentHeaders()));
    }
}
