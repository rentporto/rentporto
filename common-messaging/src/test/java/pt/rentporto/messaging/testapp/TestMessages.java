package pt.rentporto.messaging.testapp;

import java.time.Instant;
import java.util.UUID;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.reservation.ReservationExpired;

/**
 * Messages with fresh identifiers, so tests sharing the containers never see each other's messages.
 */
public final class TestMessages {

    private TestMessages() {
    }

    public static EventEnvelope<ReservationExpired> reservationExpired(String reason) {
        UUID reservationId = UUID.randomUUID();
        return new EventEnvelope<>(
                UUID.randomUUID(),
                "ReservationExpired",
                1,
                Instant.now(),
                reservationId.toString(),
                new ReservationExpired(reservationId, UUID.randomUUID(), reason));
    }
}
