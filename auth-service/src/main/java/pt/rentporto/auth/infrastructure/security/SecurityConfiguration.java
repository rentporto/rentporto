package pt.rentporto.auth.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Registration, login, the JWK Set, the API docs and the health probes are public; every other endpoint needs a
 * token issued by this service, and the roles are checked with {@code @PreAuthorize}. Stateless: no session and no
 * CSRF token, since the client sends the JWT in the {@code Authorization} header.
 */
@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
class SecurityConfiguration {

    private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();

    static final RequestMatcher PUBLIC = new OrRequestMatcher(
            PATHS.matcher(HttpMethod.POST, "/auth/register"),
            PATHS.matcher(HttpMethod.POST, "/auth/login"),
            PATHS.matcher(HttpMethod.GET, "/auth/jwks"),
            PATHS.matcher("/v3/api-docs/**"),
            PATHS.matcher("/v3/api-docs.yaml"),
            PATHS.matcher("/swagger-ui/**"),
            PATHS.matcher("/swagger-ui.html"),
            PATHS.matcher("/actuator/health/**"));

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, SecurityProblems problems, JwtAuthenticationConverter rolesAsAuthorities) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(server -> server
                        .bearerTokenResolver(ignoringPublicEndpoints())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(rolesAsAuthorities))
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .build();
    }

    /**
     * A client may still send an expired token when it logs in again; the public endpoints ignore it instead of
     * answering 401.
     */
    private static BearerTokenResolver ignoringPublicEndpoints() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        return request -> PUBLIC.matches(request) ? null : resolver.resolve(request);
    }
}
