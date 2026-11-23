package pt.rentporto.messaging.outbox;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Carries the trace of the request that writes a message to the publisher, which sends it later on another thread
 * (ADR-25). The writer keeps the propagation headers of the current span in the {@code headers} of the outbox row,
 * and the publisher sends the message inside a span that continues that trace, so the message and its consumers carry
 * the trace id of the request.
 */
public class OutboxTracing {

    static final String PUBLISH_SPAN = "outbox publish";
    private static final TypeReference<Map<String, String>> HEADERS = new TypeReference<>() {
    };

    private final Tracer tracer;
    private final Propagator propagator;
    private final JsonMapper jsonMapper;

    /**
     * Creates the tracing of the outbox.
     *
     * @param tracer     tracer of the service, or {@link Tracer#NOOP} if the service does not trace
     * @param propagator propagator of the service, or {@link Propagator#NOOP} if the service does not trace
     * @param jsonMapper mapper of the headers stored with the messages
     */
    public OutboxTracing(Tracer tracer, Propagator propagator, JsonMapper jsonMapper) {
        this.tracer = tracer;
        this.propagator = propagator;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Writes the propagation headers of the current span, to store with a message.
     *
     * @return the headers as a JSON object, empty if there is no current span
     */
    String currentHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        Span span = tracer.currentSpan();
        if (span != null) {
            propagator.inject(span.context(), headers, Map::put);
        }
        return jsonMapper.writeValueAsString(headers);
    }

    /**
     * Publishes a message in the trace its headers carry, or in a new trace if they carry none.
     *
     * @param headers    headers stored with the message, as a JSON object
     * @param routingKey routing key of the message, recorded on the span
     * @param publishing sends the message and tells whether the broker took it
     * @return what {@code publishing} returned
     */
    boolean publishInTrace(String headers, String routingKey, BooleanSupplier publishing) {
        Map<String, String> context = jsonMapper.readValue(headers, HEADERS);
        if (context.isEmpty()) {
            return publishing.getAsBoolean();
        }
        Span span = propagator.extract(context, Map::get)
                .name(PUBLISH_SPAN)
                .tag("messaging.destination.name", routingKey)
                .start();
        try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
            return publishing.getAsBoolean();
        } finally {
            span.end();
        }
    }
}
