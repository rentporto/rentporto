package pt.rentporto.notification.infrastructure.scheduling;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the pick-up reminders.
 *
 * @param before       how long before the pick-up the reminder is sent
 * @param pollInterval delay between two searches for the reminders due
 */
@ConfigurationProperties("rentporto.notification.pickup-reminder")
record PickupReminderProperties(
        @DefaultValue("24h") Duration before,
        @DefaultValue("1m") Duration pollInterval) {
}
