package pt.rentporto.customer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import pt.rentporto.messaging.testing.MessagingContainers;
import pt.rentporto.web.testing.TestJwtDecoder;

/**
 * Starts the service against PostgreSQL and RabbitMQ, with the schema checked against the entities and the tables of
 * the outbox. Every test class uses the same settings, so they share one context and one database.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.locations=classpath:db/migration,classpath:db/messaging"})
@AutoConfigureMockMvc
@Import({TestJwtDecoder.class, MessagingContainers.class})
public @interface CustomerServiceTest {
}
