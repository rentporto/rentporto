package pt.rentporto.customer.infrastructure.time;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Today is the day in Lisbon, where the stations are.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Lisbon"));
    }
}
