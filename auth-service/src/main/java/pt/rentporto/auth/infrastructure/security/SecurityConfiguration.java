package pt.rentporto.auth.infrastructure.security;

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
 * Registration, login, the JWK Set, the API docs and the health probes are public; every other endpoint needs a
 * token issued by this service, and the roles are checked with {@code @PreAuthorize}.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfiguration {

    private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();

    static final RequestMatcher PUBLIC = ResourceServerSecurity.publicEndpoints(
            PATHS.matcher(HttpMethod.POST, "/auth/register"),
            PATHS.matcher(HttpMethod.POST, "/auth/login"),
            PATHS.matcher(HttpMethod.GET, "/auth/jwks"));

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ResourceServerSecurity resourceServer) {
        return resourceServer.apply(http, PUBLIC).build();
    }
}
