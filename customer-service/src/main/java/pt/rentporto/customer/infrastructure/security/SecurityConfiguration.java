package pt.rentporto.customer.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;
import pt.rentporto.web.security.ResourceServerSecurity;

/**
 * Every endpoint needs a JWT issued by the auth-service, the internal one included: the booking-service sends the
 * token of the customer who is booking (ADR-22). The roles are checked with {@code @PreAuthorize}.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfiguration {

    static final RequestMatcher PUBLIC = ResourceServerSecurity.publicEndpoints();

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ResourceServerSecurity resourceServer) {
        return resourceServer.apply(http, PUBLIC).build();
    }
}
