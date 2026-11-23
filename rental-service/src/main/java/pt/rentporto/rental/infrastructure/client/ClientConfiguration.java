package pt.rentporto.rental.infrastructure.client;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import java.time.Duration;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.rentporto.rental.application.DepositDeclinedException;

/**
 * Clients of the other services. The circuit breaker of the payments opens when half of the last ten calls failed,
 * at least five of them, and lets a call through again after 30 s; a refused card is an answer, not a failure. The
 * time limit is above the timeout of the client, which is the one that applies. The circuit breaker is created when
 * the service starts, so that its state is in the metrics before the first pick-up (ADR-26).
 */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(clients = {FleetClient.class, PaymentClient.class})
class ClientConfiguration {

    static final String PAYMENT = "payment";

    private static final CircuitBreakerConfig PAYMENT_BREAKER = CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .ignoreExceptions(DepositDeclinedException.class)
            .build();

    @Bean
    Customizer<Resilience4JCircuitBreakerFactory> paymentCircuitBreaker() {
        return factory -> factory.configure(builder -> builder
                .circuitBreakerConfig(PAYMENT_BREAKER)
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofSeconds(10)).build()),
                PAYMENT);
    }

    @Bean
    ApplicationRunner paymentCircuitBreakerAtStartup(Resilience4JCircuitBreakerFactory factory) {
        return arguments -> factory.getCircuitBreakerRegistry().circuitBreaker(PAYMENT, PAYMENT_BREAKER);
    }
}
