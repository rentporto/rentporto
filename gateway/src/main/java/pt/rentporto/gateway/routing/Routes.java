package pt.rentporto.gateway.routing;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

import java.net.URI;
import java.util.Arrays;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import pt.rentporto.gateway.ratelimit.RateLimits;

/**
 * Routes each resource of the public API to the service that owns it, without the {@code /api/v1} prefix. There is
 * no route for {@code /internal/**}, so those endpoints are answered 404 from outside the cluster.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServiceAddresses.class)
public class Routes {

    /** Prefix of the public API, removed before the request reaches the service. */
    public static final String API_PREFIX = "/api/v1";

    private static final int API_PREFIX_SEGMENTS = 2;

    @Bean
    RouterFunction<ServerResponse> serviceRoutes(ServiceAddresses services, RateLimits rateLimits) {
        return route("auth-service", services.auth(), rateLimits, "/auth")
                .and(route("customer-service", services.customer(), rateLimits, "/customers"))
                .and(route("fleet-service", services.fleet(), rateLimits, "/stations", "/categories", "/vehicles"))
                .and(route("booking-service", services.booking(), rateLimits,
                        "/quotes", "/rate-cards", "/seasons", "/fees", "/extras", "/coverages", "/availability",
                        "/reservations", "/payments", "/receipts"))
                .and(route("rental-service", services.rental(), rateLimits, "/rentals"))
                .and(route("notification-service", services.notification(), rateLimits, "/notifications"));
    }

    private static RouterFunction<ServerResponse> route(
            String id, URI service, RateLimits rateLimits, String... resources) {
        String[] paths = Arrays.stream(resources).map(resource -> API_PREFIX + resource + "/**").toArray(String[]::new);
        return GatewayRouterFunctions.route(id)
                .route(path(paths), http())
                .filter(rateLimits)
                .before(uri(service))
                .before(stripPrefix(API_PREFIX_SEGMENTS))
                .build();
    }
}
