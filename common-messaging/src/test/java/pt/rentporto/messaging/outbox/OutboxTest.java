package pt.rentporto.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.RoutingKeys;
import pt.rentporto.events.reservation.ReservationExpired;
import pt.rentporto.messaging.testapp.MessagingTestApplication;
import pt.rentporto.messaging.testapp.RecordingListener;
import pt.rentporto.messaging.testapp.TestMessages;

@SpringBootTest(classes = MessagingTestApplication.class)
class OutboxTest {

    @Autowired
    private OutboxWriter outbox;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RabbitMQContainer rabbit;

    @Autowired
    private RecordingListener listener;

    @Test
    void publishesTheMessageOnceTheBrokerIsBack() {
        EventEnvelope<ReservationExpired> warmUp = TestMessages.reservationExpired("PAYMENT_TIMEOUT");
        EventEnvelope<ReservationExpired> message = TestMessages.reservationExpired("PAYMENT_TIMEOUT");
        inTransaction(() -> outbox.publish(RoutingKeys.RESERVATION_EXPIRED, warmUp));
        await().atMost(Duration.ofSeconds(10)).until(() -> pendingMessages(warmUp.eventId()) == 0);

        pauseBroker();
        try {
            inTransaction(() -> outbox.publish(RoutingKeys.RESERVATION_EXPIRED, message));
            await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(3))
                    .until(() -> pendingMessages(message.eventId()) == 1);
        } finally {
            resumeBroker();
        }

        await().atMost(Duration.ofSeconds(30)).until(() -> pendingMessages(message.eventId()) == 0);
        await().atMost(Duration.ofSeconds(10)).until(() -> listener.handled(message.eventId()) == 1);
    }

    @Test
    void publishesNothingWhenTheTransactionRollsBack() {
        EventEnvelope<ReservationExpired> message = TestMessages.reservationExpired("PAYMENT_TIMEOUT");

        assertThatThrownBy(() -> inTransaction(() -> {
            outbox.publish(RoutingKeys.RESERVATION_EXPIRED, message);
            throw new IllegalStateException("state change failed");
        })).hasMessage("state change failed");

        assertThat(outboxMessages(message.eventId())).isZero();
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(2))
                .until(() -> listener.deliveries(message.eventId()) == 0);
    }

    @Test
    void refusesToWriteOutsideATransaction() {
        EventEnvelope<ReservationExpired> message = TestMessages.reservationExpired("PAYMENT_TIMEOUT");

        assertThatThrownBy(() -> outbox.publish(RoutingKeys.RESERVATION_EXPIRED, message))
                .isInstanceOf(IllegalStateException.class);
    }

    private void inTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private long outboxMessages(UUID eventId) {
        return entityManager
                .createQuery("SELECT COUNT(m) FROM OutboxMessage m WHERE m.eventId = :eventId", Long.class)
                .setParameter("eventId", eventId)
                .getSingleResult();
    }

    private long pendingMessages(UUID eventId) {
        return entityManager
                .createQuery(
                        "SELECT COUNT(m) FROM OutboxMessage m WHERE m.eventId = :eventId AND m.publishedAt IS NULL",
                        Long.class)
                .setParameter("eventId", eventId)
                .getSingleResult();
    }

    private void pauseBroker() {
        rabbit.getDockerClient().pauseContainerCmd(rabbit.getContainerId()).exec();
    }

    private void resumeBroker() {
        rabbit.getDockerClient().unpauseContainerCmd(rabbit.getContainerId()).exec();
    }
}
