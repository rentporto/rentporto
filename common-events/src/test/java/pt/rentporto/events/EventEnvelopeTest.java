package pt.rentporto.events;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EventEnvelopeTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final String TYPE = "ReservationConfirmed";
    private static final Instant OCCURRED_AT = Instant.parse("2026-11-15T10:00:00Z");
    private static final String CORRELATION_ID = "c-1";
    private static final String PAYLOAD = "payload";

    @Test
    void rejectsVersionBelowOne() {
        assertThatThrownBy(() -> new EventEnvelope<>(EVENT_ID, TYPE, 0, OCCURRED_AT, CORRELATION_ID, PAYLOAD))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version");
    }

    static Stream<Arguments> envelopesMissingAField() {
        return Stream.of(
                arguments("eventId", (ThrowingCallable) () ->
                        new EventEnvelope<>(null, TYPE, 1, OCCURRED_AT, CORRELATION_ID, PAYLOAD)),
                arguments("type", (ThrowingCallable) () ->
                        new EventEnvelope<>(EVENT_ID, null, 1, OCCURRED_AT, CORRELATION_ID, PAYLOAD)),
                arguments("occurredAt", (ThrowingCallable) () ->
                        new EventEnvelope<>(EVENT_ID, TYPE, 1, null, CORRELATION_ID, PAYLOAD)),
                arguments("correlationId", (ThrowingCallable) () ->
                        new EventEnvelope<>(EVENT_ID, TYPE, 1, OCCURRED_AT, null, PAYLOAD)),
                arguments("payload", (ThrowingCallable) () ->
                        new EventEnvelope<>(EVENT_ID, TYPE, 1, OCCURRED_AT, CORRELATION_ID, null)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("envelopesMissingAField")
    void rejectsMissingField(String field, ThrowingCallable createEnvelope) {
        assertThatThrownBy(createEnvelope)
                .isInstanceOf(NullPointerException.class)
                .hasMessage(field);
    }
}
