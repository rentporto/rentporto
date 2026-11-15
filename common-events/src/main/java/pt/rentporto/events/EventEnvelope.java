package pt.rentporto.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Common envelope for every message exchanged through RabbitMQ.
 *
 * <p>The {@code traceId} is not part of the envelope: it travels in the message headers.
 *
 * @param eventId       unique identifier
 * @param type          event or command name (e.g. {@code ReservationConfirmed})
 * @param version       payload schema version, starting at 1
 * @param occurredAt    instant at which the fact happened in the source service
 * @param correlationId identifier linking the messages of the same flow
 * @param payload       event- or command-specific content
 * @param <T>           payload type
 */
public record EventEnvelope<T>(
        UUID eventId,
        String type,
        int version,
        Instant occurredAt,
        String correlationId,
        T payload) {

    public EventEnvelope {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(correlationId, "correlationId");
        Objects.requireNonNull(payload, "payload");
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1, was " + version);
        }
    }
}
