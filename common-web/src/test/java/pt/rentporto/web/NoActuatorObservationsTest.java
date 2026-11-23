package pt.rentporto.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.observation.Observation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class NoActuatorObservationsTest {

    private final NoActuatorObservations predicate = new NoActuatorObservations();

    @AfterEach
    void forgetTheRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void aRequestToTheActuatorIsNotObservedAndARequestToTheApiIs() {
        assertThat(predicate.test("http.server.requests", serverRequest("/actuator/health/readiness"))).isFalse();
        assertThat(predicate.test("http.server.requests", serverRequest("/actuator/prometheus"))).isFalse();
        assertThat(predicate.test("http.server.requests", serverRequest("/customers/me"))).isTrue();
    }

    @Test
    void whatHappensWhileServingTheActuatorIsNotObservedEither() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(get("/actuator/health")));
        assertThat(predicate.test("spring.security.filterchains", new Observation.Context())).isFalse();

        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(get("/rentals/42/pickup")));
        assertThat(predicate.test("http.client.requests", new Observation.Context())).isTrue();
    }

    @Test
    void anObservationOutsideARequestIsObserved() {
        assertThat(predicate.test("spring.rabbit.listener", new Observation.Context())).isTrue();
    }

    private static ServerRequestObservationContext serverRequest(String uri) {
        return new ServerRequestObservationContext(get(uri), new MockHttpServletResponse());
    }

    private static MockHttpServletRequest get(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }
}
