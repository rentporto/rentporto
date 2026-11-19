package pt.rentporto.notification;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;

/**
 * Mailpit, the mail server of development and k3d, for the tests; its HTTP API shows the emails received.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MailpitContainer {

    static final int SMTP = 1025;
    static final int API = 8025;

    @Bean
    GenericContainer<?> mailpit() {
        return new GenericContainer<>("axllent/mailpit:v1.33").withExposedPorts(SMTP, API);
    }

    @Bean
    DynamicPropertyRegistrar mailpitProperties(GenericContainer<?> mailpit) {
        return registry -> {
            registry.add("spring.mail.host", mailpit::getHost);
            registry.add("spring.mail.port", () -> mailpit.getMappedPort(SMTP));
        };
    }

    @Bean
    Mailbox mailbox(GenericContainer<?> mailpit) {
        return new Mailbox("http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(API));
    }
}
