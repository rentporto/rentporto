package pt.rentporto.notification.infrastructure.time;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The emails show the dates in Lisbon, where the stations are.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Lisbon"));
    }
}
