package pt.rentporto.messaging.testapp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import pt.rentporto.messaging.testing.MessagingContainers;

@SpringBootApplication
@Import(MessagingContainers.class)
public class MessagingTestApplication {

    public static final String QUEUE = "notification.reservation-expired";
    public static final String DEAD_LETTER_QUEUE = QUEUE + ".dlq";
}
