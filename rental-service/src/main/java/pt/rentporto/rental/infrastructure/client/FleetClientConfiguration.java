package pt.rentporto.rental.infrastructure.client;

import feign.Request;
import feign.Retryer;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;

/**
 * Settings of the fleet client: 3 s to connect and to answer, and up to three attempts when the fleet does not
 * answer. Repeating is safe, because every attempt carries the same idempotency key. Not a {@code @Configuration},
 * so that it only applies to this client.
 */
class FleetClientConfiguration {

    @Bean
    Request.Options fleetOptions() {
        return new Request.Options(3, TimeUnit.SECONDS, 3, TimeUnit.SECONDS, true);
    }

    @Bean
    Retryer fleetRetryer() {
        return new Retryer.Default(200, 1000, 3);
    }
}
