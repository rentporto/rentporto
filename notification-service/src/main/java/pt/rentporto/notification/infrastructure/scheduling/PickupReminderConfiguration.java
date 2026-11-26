package pt.rentporto.notification.infrastructure.scheduling;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import pt.rentporto.notification.application.port.ReminderTemplate;
import pt.rentporto.notification.application.service.CustomerEmails;
import pt.rentporto.notification.application.service.UpcomingPickups;
import pt.rentporto.notification.domain.repository.PickupReminders;

/**
 * The pick-up reminders, with the time before the pick-up from {@link PickupReminderProperties}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PickupReminderProperties.class)
class PickupReminderConfiguration {

    @Bean
    UpcomingPickups upcomingPickups(
            PickupReminders reminders,
            CustomerEmails emails,
            ReminderTemplate template,
            Clock clock,
            PickupReminderProperties properties,
            PlatformTransactionManager transactionManager) {
        return new UpcomingPickups(reminders, emails, template, clock, properties.before(), transactionManager);
    }
}
