package pt.rentporto.fleet.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import pt.rentporto.web.security.ResourceServerSecurity;

/**
 * The stations and the categories are public; every other endpoint needs a JWT issued by the auth-service, the
 * internal one included: the rental-service sends the token of the agent at the counter (ADR-22). The roles are
 * checked with {@code @PreAuthorize}.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfiguration {

    private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();

    static final RequestMatcher PUBLIC = ResourceServerSecurity.publicEndpoints(
            PATHS.matcher(HttpMethod.GET, "/stations"),
            PATHS.matcher(HttpMethod.GET, "/categories"));

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ResourceServerSecurity resourceServer) {
        return resourceServer.apply(http, PUBLIC).build();
    }
}
