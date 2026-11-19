package pt.rentporto.notification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import pt.rentporto.messaging.testing.MessagingContainers;
import pt.rentporto.messaging.testing.MessagingRecords;
import pt.rentporto.web.testing.TestJwtDecoder;

/**
 * Starts the service against PostgreSQL, RabbitMQ and Mailpit, without a session open for the whole request, as in
 * {@code config/application.yml}, and with the schema checked against the entities and the tables of the outbox. The
 * mail host is set here so that the mail sender is configured; {@link MailpitContainer} then points it to the
 * container. Every test class uses the same settings, so they share one context, one database and one mailbox.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.locations=classpath:db/migration,classpath:db/messaging",
        "spring.mail.host=localhost"})
@AutoConfigureMockMvc
@Import({TestJwtDecoder.class, MessagingContainers.class, MessagingRecords.class, MailpitContainer.class})
public @interface NotificationServiceTest {
}
