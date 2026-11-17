package pt.rentporto.auth.infrastructure.token;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the tokens the service issues. The signing key is read apart, in {@link TokenConfiguration}.
 *
 * @param issuer value of the {@code iss} claim, which the services check
 * @param ttl    how long a token is valid
 */
@ConfigurationProperties("rentporto.auth.token")
public record TokenProperties(@DefaultValue("rentporto") String issuer, @DefaultValue("1h") Duration ttl) {
}
