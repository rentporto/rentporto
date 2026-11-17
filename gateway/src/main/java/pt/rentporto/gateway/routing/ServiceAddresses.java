package pt.rentporto.gateway.routing;

import java.net.URI;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Address of each service, by profile in the Config Server: {@code localhost} and the fixed port in dev, the name of
 * the Service in k8s.
 *
 * @param auth         auth-service
 * @param customer     customer-service
 * @param fleet        fleet-service
 * @param booking      booking-service
 * @param rental       rental-service
 * @param notification notification-service
 */
@ConfigurationProperties("rentporto.services")
public record ServiceAddresses(URI auth, URI customer, URI fleet, URI booking, URI rental, URI notification) {

    public ServiceAddresses {
        Objects.requireNonNull(auth, "rentporto.services.auth");
        Objects.requireNonNull(customer, "rentporto.services.customer");
        Objects.requireNonNull(fleet, "rentporto.services.fleet");
        Objects.requireNonNull(booking, "rentporto.services.booking");
        Objects.requireNonNull(rental, "rentporto.services.rental");
        Objects.requireNonNull(notification, "rentporto.services.notification");
    }
}
