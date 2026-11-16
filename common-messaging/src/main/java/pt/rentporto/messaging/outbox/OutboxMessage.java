package pt.rentporto.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Message waiting in the {@code outbox} table to be sent to RabbitMQ. Internal to the library: public only so the
 * auto-configuration can register it in the service's persistence unit.
 */
@Entity
@Table(name = "outbox")
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "exchange", nullable = false, updatable = false)
    private String exchange;

    @Column(name = "routing_key", nullable = false, updatable = false)
    private String routingKey;

    @Column(name = "message_type", nullable = false, updatable = false)
    private String messageType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, updatable = false)
    private String payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "headers", nullable = false, updatable = false)
    private String headers;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxMessage() {
    }

    /**
     * Creates a pending message.
     *
     * @param eventId     identifier of the envelope
     * @param exchange    exchange to publish to; empty for the default exchange
     * @param routingKey  routing key, or queue name for a command
     * @param messageType envelope type, e.g. {@code ReservationConfirmed}
     * @param payload     envelope serialised as JSON
     */
    OutboxMessage(UUID eventId, String exchange, String routingKey, String messageType, String payload) {
        this.eventId = eventId;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.messageType = messageType;
        this.payload = payload;
        this.headers = "{}";
        this.createdAt = Instant.now();
    }

    Long id() {
        return id;
    }

    UUID eventId() {
        return eventId;
    }

    String exchange() {
        return exchange;
    }

    String routingKey() {
        return routingKey;
    }

    String messageType() {
        return messageType;
    }

    String payload() {
        return payload;
    }
}
