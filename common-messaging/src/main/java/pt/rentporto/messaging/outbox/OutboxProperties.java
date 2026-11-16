package pt.rentporto.messaging.outbox;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the outbox publisher.
 *
 * @param pollInterval   delay between two runs of the publisher
 * @param batchSize      maximum number of rows published per run
 * @param confirmTimeout how long to wait for the broker to confirm a message before trying again later
 */
@ConfigurationProperties("rentporto.messaging.outbox")
public record OutboxProperties(
        @DefaultValue("1s") Duration pollInterval,
        @DefaultValue("100") int batchSize,
        @DefaultValue("5s") Duration confirmTimeout) {
}
