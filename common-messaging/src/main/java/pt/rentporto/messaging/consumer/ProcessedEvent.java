package pt.rentporto.messaging.consumer;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Record that a consumer already handled a message. Internal to the library: public only so the auto-configuration
 * can register it in the service's persistence unit.
 */
@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @EmbeddedId
    private Key key;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedEvent() {
    }

    /**
     * Records a message as handled now.
     *
     * @param key consumer and message
     */
    ProcessedEvent(Key key) {
        this.key = key;
        this.processedAt = Instant.now();
    }

    /**
     * Identifies a message for a given consumer: two consumers in the same database may handle the same message.
     *
     * @param consumer name of the consumer, by convention its queue
     * @param eventId  identifier of the envelope
     */
    @Embeddable
    record Key(
            @Column(name = "consumer", nullable = false) String consumer,
            @Column(name = "event_id", nullable = false) UUID eventId) implements Serializable {
    }
}
