package pt.rentporto.gateway.ratelimit;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Request limits per client.
 *
 * @param client limit for each client: the {@code sub} of the JWT, or the IP without a token
 * @param login  lower limit for logins, per IP, against guessing passwords
 */
@ConfigurationProperties("rentporto.gateway.rate-limit")
public record RateLimitProperties(Limit client, Limit login) {

    public RateLimitProperties {
        Objects.requireNonNull(client, "rentporto.gateway.rate-limit.client");
        Objects.requireNonNull(login, "rentporto.gateway.rate-limit.login");
    }

    /**
     * Number of requests allowed in a period; the allowance refills gradually.
     *
     * @param capacity requests allowed in the period
     * @param period   length of the period
     */
    public record Limit(long capacity, Duration period) {

        public Limit {
            if (capacity < 1) {
                throw new IllegalArgumentException("The capacity of a rate limit must be at least 1");
            }
            Objects.requireNonNull(period, "period");
        }
    }
}
