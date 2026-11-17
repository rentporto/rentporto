package pt.rentporto.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.InjectWireMock;
import pt.rentporto.web.testing.TestJwt;

/**
 * Each resource of {@code /api/v1} reaches the service that owns it, without the prefix; {@code /internal} is not
 * reachable, and a service that cannot be reached is answered 503.
 */
@GatewayTest
class RoutingTest {

    private static final String TOKEN = TestJwt.bearer("routing-user", "CUSTOMER");

    @Autowired
    private MockMvc mockMvc;

    @InjectWireMock("auth-service")
    private WireMockServer auth;

    @InjectWireMock("customer-service")
    private WireMockServer customer;

    @InjectWireMock("fleet-service")
    private WireMockServer fleet;

    @InjectWireMock("booking-service")
    private WireMockServer booking;

    @InjectWireMock("rental-service")
    private WireMockServer rental;

    @InjectWireMock("notification-service")
    private WireMockServer notification;

    private Map<String, WireMockServer> services;

    @BeforeEach
    void setUp() {
        Jwks.publish(auth);
        services = Map.of(
                "auth-service", auth,
                "customer-service", customer,
                "fleet-service", fleet,
                "booking-service", booking,
                "rental-service", rental,
                "notification-service", notification);
    }

    @ParameterizedTest
    @CsvSource({
            "/auth/users, auth-service",
            "/customers/me, customer-service",
            "/stations, fleet-service",
            "/categories/ECONOMY, fleet-service",
            "/vehicles/AA-00-AA/status, fleet-service",
            "/quotes, booking-service",
            "/rate-cards/ECONOMY, booking-service",
            "/seasons, booking-service",
            "/fees, booking-service",
            "/extras, booking-service",
            "/coverages, booking-service",
            "/availability, booking-service",
            "/reservations/me, booking-service",
            "/payments, booking-service",
            "/receipts/42, booking-service",
            "/rentals/42, rental-service",
            "/notifications, notification-service"})
    void sendsEachResourceToTheServiceThatOwnsItWithoutThePrefix(String path, String service) throws Exception {
        WireMockServer owner = services.get(service);
        owner.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo(path))
                .willReturn(ok("answered by " + service)));

        mockMvc.perform(get("/api/v1" + path).header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("answered by " + service));

        owner.verify(getRequestedFor(urlPathEqualTo(path)).withHeader(HttpHeaders.AUTHORIZATION, equalTo(TOKEN)));
        services.values().stream().filter(other -> other != owner)
                .forEach(other -> other.verify(0, anyRequestedFor(anyUrl())));
    }

    @Test
    void tellsTheServiceTheAddressTheClientUsed() throws Exception {
        customer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get("/customers/me").willReturn(ok()));

        mockMvc.perform(get("/api/v1/customers/me").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isOk());

        customer.verify(getRequestedFor(urlPathEqualTo("/customers/me"))
                .withHeader("X-Forwarded-Prefix", equalTo("/api/v1"))
                .withHeader("X-Forwarded-Host", equalTo("localhost"))
                .withHeader("X-Forwarded-Proto", equalTo("http")));
    }

    @Test
    void doesNotExposeTheInternalEndpoints() throws Exception {
        for (String token : new String[] {null, TOKEN}) {
            var request = get("/api/v1/internal/customers/42/driver-profile");
            if (token != null) {
                request.header(HttpHeaders.AUTHORIZATION, token);
            }
            mockMvc.perform(request)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.detail").value("There is no endpoint at this path"));
        }
        services.values().forEach(service -> service.verify(0, anyRequestedFor(anyUrl())));
    }

    @Test
    void answers503WhenTheServiceCannotBeReached() throws Exception {
        notification.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get("/notifications")
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        mockMvc.perform(get("/api/v1/notifications").header(HttpHeaders.AUTHORIZATION, TOKEN))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
    }
}
