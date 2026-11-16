package pt.rentporto.messaging.testapp;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import pt.rentporto.events.Exchanges;
import pt.rentporto.events.RoutingKeys;

@SpringBootApplication
public class MessagingTestApplication {

    public static final String QUEUE = "test.reservation-expired";
    public static final String DEAD_LETTER_QUEUE = QUEUE + ".dlq";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbit() {
        return new RabbitMQContainer("rabbitmq:4.1-management-alpine");
    }

    /**
     * Declares a consumer queue the same way as the broker definitions file: dead-letter through the default exchange
     * to {@code {queue}.dlq}.
     *
     * @return the exchange, the queue, its dead-letter queue and the binding
     */
    @Bean
    Declarables topology() {
        TopicExchange events = new TopicExchange(Exchanges.EVENTS);
        Queue queue = QueueBuilder.durable(QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
        return new Declarables(
                events,
                queue,
                QueueBuilder.durable(DEAD_LETTER_QUEUE).build(),
                BindingBuilder.bind(queue).to(events).with(RoutingKeys.RESERVATION_EXPIRED));
    }
}
