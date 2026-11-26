package pt.rentporto.web.time;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * The clock of every service: today is the day in Lisbon, where the stations are. A service or a test can declare its
 * own {@link Clock} instead.
 */
@AutoConfiguration
public class TimeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.system(ZoneId.of("Europe/Lisbon"));
    }
}
