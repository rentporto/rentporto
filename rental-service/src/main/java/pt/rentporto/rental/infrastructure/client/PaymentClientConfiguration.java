package pt.rentporto.rental.infrastructure.client;

import feign.Request;
import feign.Retryer;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;

/**
 * Settings of the payment client: 5 s to connect and to answer, while the customer waits at the counter, and no
 * repetition, which is left to the agent and guarded by the circuit breaker. Not a {@code @Configuration}, so that it
 * only applies to this client.
 */
class PaymentClientConfiguration {

    @Bean
    Request.Options paymentOptions() {
        return new Request.Options(5, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }

    @Bean
    Retryer paymentRetryer() {
        return Retryer.NEVER_RETRY;
    }
}
