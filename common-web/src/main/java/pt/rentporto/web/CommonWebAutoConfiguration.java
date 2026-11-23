package pt.rentporto.web;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Error format, OpenAPI setup and the actuator out of the observations, for a servlet web service.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CommonWebAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ProblemDetailsAdvice problemDetailsAdvice() {
        return new ProblemDetailsAdvice();
    }

    @Bean
    OpenApiContract openApiContract(Environment environment) {
        return new OpenApiContract(environment.getProperty("spring.application.name", "rentporto"));
    }

    @Bean
    ObservationPredicate noActuatorObservations() {
        return new NoActuatorObservations();
    }
}
