package pt.rentporto.gateway.security;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pt.rentporto.web.security.ResourceServerSecurity;

/**
 * Authentication at the entry point: the public endpoints of the catalogue pass without a token, every other request
 * needs a valid JWT. The roles are checked by each service.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CorsProperties.class)
class GatewaySecurity {

    private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();

    /**
     * Endpoints open without a token, besides the API docs and the health probes. {@code /api/v1/internal/**} is here
     * so that it is answered 404, with or without a token, since no route leads to it.
     */
    static final RequestMatcher PUBLIC = ResourceServerSecurity.publicEndpoints(
            PATHS.matcher(HttpMethod.POST, "/api/v1/auth/register"),
            PATHS.matcher(HttpMethod.POST, "/api/v1/auth/login"),
            PATHS.matcher(HttpMethod.GET, "/api/v1/auth/jwks"),
            PATHS.matcher(HttpMethod.GET, "/api/v1/stations"),
            PATHS.matcher(HttpMethod.GET, "/api/v1/categories"),
            PATHS.matcher(HttpMethod.GET, "/api/v1/extras"),
            PATHS.matcher(HttpMethod.GET, "/api/v1/coverages"),
            PATHS.matcher(HttpMethod.GET, "/api/v1/availability"),
            PATHS.matcher(HttpMethod.POST, "/api/v1/quotes"),
            PATHS.matcher(HttpMethod.POST, "/api/v1/payments/stripe-webhook"),
            PATHS.matcher("/api/v1/internal/**"));

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, ResourceServerSecurity resourceServer, CorsProperties cors) {
        return resourceServer.apply(http, PUBLIC)
                .cors(configurer -> configurer.configurationSource(corsConfiguration(cors)))
                .build();
    }

    private static CorsConfigurationSource corsConfiguration(CorsProperties cors) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(cors.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        configuration.setAllowedHeaders(
                List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "Idempotency-Key"));
        configuration.setExposedHeaders(List.of(HttpHeaders.LOCATION, HttpHeaders.RETRY_AFTER));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
