package pt.rentporto.messaging.consumer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.reservation.ReservationExpired;
import pt.rentporto.events.topology.Exchanges;
import pt.rentporto.events.topology.RoutingKeys;
import pt.rentporto.messaging.testapp.MessagingTestApplication;
import pt.rentporto.messaging.testapp.RecordingListener;
import pt.rentporto.messaging.testapp.TestMessages;

@SpringBootTest(classes = MessagingTestApplication.class)
class IdempotentConsumerTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RecordingListener listener;

    @Test
    void handlesAMessageDeliveredTwiceOnce() {
        EventEnvelope<ReservationExpired> message = TestMessages.reservationExpired("PAYMENT_TIMEOUT");

        rabbitTemplate.convertAndSend(Exchanges.EVENTS, RoutingKeys.RESERVATION_EXPIRED, message);
        rabbitTemplate.convertAndSend(Exchanges.EVENTS, RoutingKeys.RESERVATION_EXPIRED, message);

        await().atMost(Duration.ofSeconds(10)).until(() -> listener.deliveries(message.eventId()) == 2);
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(2))
                .until(() -> listener.deliveries(message.eventId()) == 2);
        assertThat(listener.handled(message.eventId())).isEqualTo(1);
        assertThat(processedEvents(message.eventId())).isEqualTo(1);
    }

    @Test
    void sendsAMessageThatFailsThreeTimesToTheDeadLetterQueue() {
        EventEnvelope<ReservationExpired> message = TestMessages.reservationExpired(RecordingListener.FAILING_REASON);

        rabbitTemplate.convertAndSend(Exchanges.EVENTS, RoutingKeys.RESERVATION_EXPIRED, message);

        await().atMost(Duration.ofSeconds(15)).until(() -> isDeadLettered(message.eventId()));
        assertThat(listener.deliveries(message.eventId())).isEqualTo(3);
        assertThat(listener.handled(message.eventId())).isEqualTo(3);
        assertThat(processedEvents(message.eventId())).isZero();
    }

    private boolean isDeadLettered(UUID eventId) {
        Message deadLetter;
        while ((deadLetter = rabbitTemplate.receive(MessagingTestApplication.DEAD_LETTER_QUEUE)) != null) {
            if (new String(deadLetter.getBody(), UTF_8).contains(eventId.toString())) {
                return true;
            }
        }
        return false;
    }

    private long processedEvents(UUID eventId) {
        return entityManager
                .createQuery("SELECT COUNT(p) FROM ProcessedEvent p WHERE p.key.eventId = :eventId", Long.class)
                .setParameter("eventId", eventId)
                .getSingleResult();
    }
}
