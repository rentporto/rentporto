package pt.rentporto.messaging;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * RabbitMQ settings every service needs for reliable messaging, added with the lowest precedence so a service can
 * still override them.
 *
 * <ul>
 *   <li>publisher confirms, so the outbox marks a row as published only after the broker accepted it;</li>
 *   <li>3 delivery attempts (1 + 2 retries) with exponential backoff, then reject without requeue, so the message is
 *       dead-lettered to {@code {queue}.dlq}.</li>
 * </ul>
 */
public class MessagingDefaults implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "rentportoMessagingDefaults";

    private static final Map<String, Object> DEFAULTS = Map.of(
            "spring.rabbitmq.publisher-confirm-type", "correlated",
            "spring.rabbitmq.listener.simple.retry.enabled", "true",
            "spring.rabbitmq.listener.simple.retry.max-retries", "2",
            "spring.rabbitmq.listener.simple.retry.initial-interval", "1s",
            "spring.rabbitmq.listener.simple.retry.multiplier", "2",
            "spring.rabbitmq.listener.simple.default-requeue-rejected", "false");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        environment.getPropertySources().addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, DEFAULTS));
    }
}
