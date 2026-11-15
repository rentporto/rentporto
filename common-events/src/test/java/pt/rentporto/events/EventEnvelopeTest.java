package pt.rentporto.events;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventEnvelopeTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final Instant OCCURRED_AT = Instant.parse("2026-11-15T10:00:00Z");

    @Test
    void rejectsVersionBelowOne() {
        assertThatThrownBy(() -> new EventEnvelope<>(EVENT_ID, "ReservationConfirmed", 0, OCCURRED_AT, "c-1", "payload"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version");
    }

    @Test
    void rejectsMissingEventId() {
        assertThatThrownBy(() -> new EventEnvelope<>(null, "ReservationConfirmed", 1, OCCURRED_AT, "c-1", "payload"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("eventId");
    }
}
