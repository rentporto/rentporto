package pt.rentporto.gateway.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Browser origins allowed to call the API from another origin. None by default: there is no frontend (ADR-13), and
 * the Swagger UI of the gateway is on the same origin.
 *
 * @param allowedOrigins origins such as {@code http://localhost:3000}
 */
@ConfigurationProperties("rentporto.gateway.cors")
public record CorsProperties(@DefaultValue List<String> allowedOrigins) {
}
