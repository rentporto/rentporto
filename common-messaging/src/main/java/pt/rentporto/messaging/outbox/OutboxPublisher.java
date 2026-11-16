package pt.rentporto.messaging.outbox;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.SmartLifecycle;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.messaging.lock.SchedulerLocks;

/**
 * Sends the pending outbox messages to RabbitMQ, in the order they were recorded and on one replica at a time.
 *
 * <p>A message is marked as published only after the broker confirms it; the first failure stops the run and the
 * next run starts again from that message, so a message may be sent more than once.
 */
public class OutboxPublisher implements SmartLifecycle {

    private static final Log log = LogFactory.getLog(OutboxPublisher.class);
    private static final String LOCK_NAME = "outbox-publisher";
    private static final Duration LOCK_AT_MOST_FOR = Duration.ofMinutes(1);

    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final SchedulerLocks schedulerLocks;
    private final OutboxProperties properties;
    private ScheduledExecutorService scheduler;

    /**
     * Creates a publisher.
     *
     * @param entityManager       shared entity manager of the service
     * @param transactionTemplate template on the transaction manager of the service
     * @param rabbitTemplate      template whose connection factory has publisher confirms enabled
     * @param schedulerLocks      locks that keep the publisher on a single replica
     * @param properties          polling and confirmation settings
     * @throws IllegalStateException if publisher confirms are disabled
     */
    public OutboxPublisher(
            EntityManager entityManager,
            TransactionTemplate transactionTemplate,
            RabbitTemplate rabbitTemplate,
            SchedulerLocks schedulerLocks,
            OutboxProperties properties) {
        if (!rabbitTemplate.getConnectionFactory().isPublisherConfirms()) {
            throw new IllegalStateException(
                    "The outbox needs publisher confirms: set spring.rabbitmq.publisher-confirm-type=correlated");
        }
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.schedulerLocks = schedulerLocks;
        this.properties = properties;
    }

    @Override
    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("outbox-publisher").factory());
        long delay = properties.pollInterval().toMillis();
        scheduler.scheduleWithFixedDelay(this::publishPendingOnOneReplica, delay, delay, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        scheduler.shutdownNow();
        scheduler = null;
    }

    @Override
    public boolean isRunning() {
        return scheduler != null;
    }

    private void publishPendingOnOneReplica() {
        try {
            schedulerLocks.runOnOneReplica(LOCK_NAME, LOCK_AT_MOST_FOR, this::publishPending);
        } catch (RuntimeException e) {
            log.warn("Outbox publisher run failed; retrying on the next run", e);
        }
    }

    private void publishPending() {
        List<OutboxMessage> pending = transactionTemplate.execute(status -> entityManager
                .createQuery(
                        "SELECT m FROM OutboxMessage m WHERE m.publishedAt IS NULL ORDER BY m.id", OutboxMessage.class)
                .setMaxResults(properties.batchSize())
                .getResultList());
        for (OutboxMessage message : pending) {
            if (!sendAndConfirm(message)) {
                return;
            }
            transactionTemplate.executeWithoutResult(status -> entityManager
                    .createQuery("UPDATE OutboxMessage m SET m.publishedAt = :now WHERE m.id = :id")
                    .setParameter("now", Instant.now())
                    .setParameter("id", message.id())
                    .executeUpdate());
        }
    }

    private boolean sendAndConfirm(OutboxMessage pending) {
        Message message = MessageBuilder.withBody(pending.payload().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setMessageId(pending.eventId().toString())
                .setType(pending.messageType())
                .build();
        CorrelationData correlation = new CorrelationData(pending.eventId().toString());
        try {
            rabbitTemplate.send(pending.exchange(), pending.routingKey(), message, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(properties.confirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.ack()) {
                log.warn("Broker rejected outbox message " + pending.eventId() + ": " + confirm.reason());
            }
            return confirm.ack();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("Could not publish outbox message " + pending.eventId() + "; retrying on the next run: " + e);
            return false;
        }
    }
}
