package pt.rentporto.gateway.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.security.Principal;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import pt.rentporto.gateway.ratelimit.RateLimitProperties.Limit;

/**
 * Limits the requests of each client with a token bucket per client. The buckets live in the memory of the gateway,
 * so with more than one replica each replica applies the limit on its own.
 */
@Component
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimits implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    private static final RequestPredicate LOGIN = RequestPredicates.POST("/api/v1/auth/login");

    private final RateLimitProperties limits;
    private final Cache<String, Bucket> buckets;

    RateLimits(RateLimitProperties limits) {
        this.limits = limits;
        Duration longestPeriod = limits.client().period().compareTo(limits.login().period()) >= 0
                ? limits.client().period()
                : limits.login().period();
        this.buckets = Caffeine.newBuilder().expireAfterAccess(longestPeriod).build();
    }

    @Override
    public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
        boolean login = LOGIN.test(request);
        Limit limit = login ? limits.login() : limits.client();
        String key = (login ? "login/" : "client/") + client(request);
        ConsumptionProbe probe = buckets.get(key, ignored -> bucket(limit)).tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            throw new TooManyRequestsException(Duration.ofNanos(probe.getNanosToWaitForRefill()));
        }
        return next.handle(request);
    }

    /**
     * The client is the account of the token, or the IP without a token; behind the Ingress, the IP comes from
     * {@code X-Forwarded-For}.
     */
    private static String client(ServerRequest request) {
        return request.principal()
                .map(Principal::getName)
                .map(subject -> "user/" + subject)
                .orElseGet(() -> "ip/" + request.servletRequest().getRemoteAddr());
    }

    private static Bucket bucket(Limit limit) {
        return Bucket.builder()
                .addLimit(bandwidth -> bandwidth.capacity(limit.capacity())
                        .refillGreedy(limit.capacity(), limit.period()))
                .build();
    }
}
