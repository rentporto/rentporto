package pt.rentporto.messaging;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.messaging.consumer.IdempotentConsumer;
import pt.rentporto.messaging.consumer.ProcessedEvent;
import pt.rentporto.messaging.lock.SchedulerLock;
import pt.rentporto.messaging.lock.SchedulerLocks;
import pt.rentporto.messaging.outbox.OutboxMessage;
import pt.rentporto.messaging.outbox.OutboxProperties;
import pt.rentporto.messaging.outbox.OutboxPublisher;
import pt.rentporto.messaging.outbox.OutboxWriter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Outbox, idempotent consumer, locks shared by the replicas and JSON message conversion.
 *
 * <p>Inactive until the service has JPA and a RabbitMQ template, so depending on this library changes nothing in a
 * service that does not use them yet. The library's entities are registered directly in the service's persistence
 * unit, so they also work with an {@code @EntityScan}. Their tables come from the migration in
 * {@code classpath:db/messaging}, which the service adds to {@code spring.flyway.locations}.
 */
@AutoConfiguration(afterName = {
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
        "org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration"})
@ConditionalOnClass(EntityManagerFactoryBuilderCustomizer.class)
@ConditionalOnBean({EntityManagerFactory.class, RabbitTemplate.class})
@EnableConfigurationProperties(OutboxProperties.class)
public class MessagingAutoConfiguration {

    @Bean
    EntityManagerFactoryBuilderCustomizer messagingEntities() {
        return builder -> builder.addPersistenceUnitPostProcessors(unit -> {
            unit.addManagedClassName(OutboxMessage.class.getName());
            unit.addManagedClassName(ProcessedEvent.class.getName());
            unit.addManagedClassName(SchedulerLock.class.getName());
        });
    }

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    JacksonJsonMessageConverter messagingMessageConverter(ObjectProvider<JsonMapper> jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper(jsonMapper));
    }

    @Bean
    @ConditionalOnMissingBean
    SchedulerLocks schedulerLocks(
            EntityManagerFactory entityManagerFactory, PlatformTransactionManager transactionManager) {
        return new SchedulerLocks(sharedEntityManager(entityManagerFactory), new TransactionTemplate(transactionManager));
    }

    @Bean
    OutboxWriter outboxWriter(EntityManagerFactory entityManagerFactory, ObjectProvider<JsonMapper> jsonMapper) {
        return new OutboxWriter(sharedEntityManager(entityManagerFactory), jsonMapper(jsonMapper));
    }

    @Bean
    OutboxPublisher outboxPublisher(
            EntityManagerFactory entityManagerFactory,
            PlatformTransactionManager transactionManager,
            RabbitTemplate rabbitTemplate,
            SchedulerLocks schedulerLocks,
            OutboxProperties properties) {
        return new OutboxPublisher(
                sharedEntityManager(entityManagerFactory),
                new TransactionTemplate(transactionManager),
                rabbitTemplate,
                schedulerLocks,
                properties);
    }

    @Bean
    IdempotentConsumer idempotentConsumer(
            EntityManagerFactory entityManagerFactory, PlatformTransactionManager transactionManager) {
        return new IdempotentConsumer(
                sharedEntityManager(entityManagerFactory), new TransactionTemplate(transactionManager));
    }

    private static EntityManager sharedEntityManager(EntityManagerFactory entityManagerFactory) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }

    private static JsonMapper jsonMapper(ObjectProvider<JsonMapper> jsonMapper) {
        return jsonMapper.getIfAvailable(() -> JsonMapper.builder().build());
    }
}
