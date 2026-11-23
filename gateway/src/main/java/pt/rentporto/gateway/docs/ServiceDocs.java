package pt.rentporto.gateway.docs;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pt.rentporto.gateway.routing.Routes;
import pt.rentporto.gateway.routing.ServiceAddresses;
import tools.jackson.databind.node.ObjectNode;

/**
 * The public OpenAPI documents of the services with an API, read from each service when they are asked for, so that
 * they are always those of the running version.
 */
@Component
class ServiceDocs {

    private final Map<String, URI> services = new LinkedHashMap<>();
    private final RestClient restClient;

    ServiceDocs(ServiceAddresses addresses, RestClient.Builder restClients) {
        services.put("auth-service", addresses.auth());
        services.put("customer-service", addresses.customer());
        services.put("fleet-service", addresses.fleet());
        services.put("booking-service", addresses.booking());
        services.put("rental-service", addresses.rental());
        services.put("notification-service", addresses.notification());
        this.restClient = restClients.build();
    }

    /**
     * Names the services with an API.
     *
     * @return the names, e.g. {@code fleet-service}
     */
    Set<String> names() {
        return services.keySet();
    }

    /**
     * Reads the public document of a service.
     *
     * @param service name of the service, e.g. {@code fleet-service}
     * @return its OpenAPI document, as the clients reach it through the gateway
     * @throws UnknownServiceException  if there is no service with an API by that name
     * @throws DocsUnavailableException if the service does not answer with its document
     */
    ObjectNode publicDocs(String service) {
        URI address = services.get(service);
        if (address == null) {
            throw new UnknownServiceException(service);
        }
        ObjectNode docs;
        try {
            docs = restClient.get().uri(address.resolve("/v3/api-docs")).retrieve().body(ObjectNode.class);
        } catch (RestClientException e) {
            throw new DocsUnavailableException(service);
        }
        if (docs == null) {
            throw new DocsUnavailableException(service);
        }
        return PublicDocs.of(docs, Routes.API_PREFIX);
    }
}
