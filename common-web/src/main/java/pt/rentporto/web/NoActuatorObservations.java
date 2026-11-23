package pt.rentporto.web;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationPredicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Leaves the requests to the actuator, the probes of Kubernetes and the scrapes of Prometheus, out of the traces and
 * of the request metrics, so that Zipkin keeps the requests of the users (ADR-25). What is observed while serving such
 * a request, such as the filters of Spring Security, is left out too.
 */
class NoActuatorObservations implements ObservationPredicate {

    private static final String ACTUATOR = "/actuator";

    @Override
    public boolean test(String name, Observation.Context context) {
        HttpServletRequest request = context instanceof ServerRequestObservationContext server
                ? server.getCarrier()
                : currentRequest();
        return request == null || !request.getRequestURI().startsWith(ACTUATOR);
    }

    private static HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest()
                : null;
    }
}
