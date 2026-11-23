package pt.rentporto.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * The probes of Kubernetes and the scrapes of Prometheus stay out of the traces; the requests of the API do not.
 */
@GatewayTest
class ObservationsTest {

    @Autowired
    private ObservationRegistry observations;

    @Test
    void theActuatorIsLeftOutOfTheTracesAndTheApiIsNot() {
        assertThat(observed("/actuator/health/readiness")).isFalse();
        assertThat(observed("/actuator/prometheus")).isFalse();
        assertThat(observed("/api/v1/customers/me")).isTrue();
    }

    private boolean observed(String uri) {
        ServerRequestObservationContext request = new ServerRequestObservationContext(
                new MockHttpServletRequest("GET", uri), new MockHttpServletResponse());
        return !Observation.createNotStarted("http.server.requests", () -> request, observations).isNoop();
    }
}
