package pt.rentporto.messaging.testapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.MountableFile;

@SpringBootApplication
public class MessagingTestApplication {

    public static final String QUEUE = "notification.reservation-expired";
    public static final String DEAD_LETTER_QUEUE = QUEUE + ".dlq";

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
                        "/etc/rabbitmq/definitions.json")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("../infra/rabbitmq/rentporto.conf"),
                        "/etc/rabbitmq/conf.d/20-rentporto.conf");
    }
}
