package pt.rentporto.messaging.testing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.MountableFile;

/**
 * PostgreSQL and RabbitMQ for the tests of a service, with the topology and the user that the compose loads from
 * {@code infra/rabbitmq}. The paths are relative to the module, which sits at the root of the repository.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MessagingContainers {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }

    @Bean
    @ServiceConnection
    RabbitMQContainer rabbit() {
        return new RabbitMQContainer("rabbitmq:4.1-management-alpine")
                .withAdminUser("rentporto")
                .withAdminPassword("rentporto")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("../infra/rabbitmq/definitions.json"),
                        "/etc/rabbitmq/definitions/10-topology.json")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("../infra/rabbitmq/users.json"),
                        "/etc/rabbitmq/definitions/20-users.json")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("../infra/rabbitmq/rentporto.conf"),
                        "/etc/rabbitmq/conf.d/20-rentporto.conf");
    }
}
