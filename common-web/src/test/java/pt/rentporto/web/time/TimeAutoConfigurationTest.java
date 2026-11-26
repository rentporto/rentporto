package pt.rentporto.web.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TimeAutoConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TimeAutoConfiguration.class));

    @Test
    void theClockOfAServiceIsInLisbon() {
        context.run(started -> assertThat(started.getBean(Clock.class).getZone())
                .isEqualTo(ZoneId.of("Europe/Lisbon")));
    }

    @Test
    void aServiceCanDeclareItsOwnClock() {
        Clock fixed = Clock.fixed(Instant.parse("2026-11-26T10:00:00Z"), ZoneOffset.UTC);

        context.withBean(Clock.class, () -> fixed)
                .run(started -> assertThat(started.getBean(Clock.class)).isSameAs(fixed));
    }
}
