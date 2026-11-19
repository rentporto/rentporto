package pt.rentporto.messaging.testing;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

/**
 * Reads, through JPA, what a service wrote to its outbox and which messages it handled, for the tests of the
 * service. Import it with the test configuration of the service.
 */
public class MessagingRecords {

    private final EntityManager entityManager;

    /**
     * Creates the reader.
     *
     * @param entityManager shared entity manager of the service
     */
    public MessagingRecords(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * The envelopes written to the outbox with a routing key, in the order they were written.
     *
     * @param routingKey routing key, e.g. {@code fleet.station-capacity-changed}
     * @return each envelope as JSON
     */
    public List<String> outbox(String routingKey) {
        return entityManager.createQuery(
                        "SELECT m.payload FROM OutboxMessage m WHERE m.routingKey = :routingKey ORDER BY m.id",
                        String.class)
                .setParameter("routingKey", routingKey)
                .getResultList();
    }

    /**
     * Tells whether a consumer handled a message.
     *
     * @param consumer name of the consumer, its queue
     * @param eventId  identifier of the envelope
     * @return {@code true} if the message was recorded as handled
     */
    public boolean handled(String consumer, UUID eventId) {
        return entityManager.createQuery(
                        "SELECT count(p) FROM ProcessedEvent p"
                                + " WHERE p.key.consumer = :consumer AND p.key.eventId = :eventId",
                        Long.class)
                .setParameter("consumer", consumer)
                .setParameter("eventId", eventId)
                .getSingleResult() == 1;
    }
}
