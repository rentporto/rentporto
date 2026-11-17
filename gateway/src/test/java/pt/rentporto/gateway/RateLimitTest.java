package pt.rentporto.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.wiremock.spring.InjectWireMock;

/**
 * Each client has its own limit, and logins have a lower one per IP.
 */
@GatewayTest
@TestPropertySource(properties = {
        "rentporto.gateway.rate-limit.client.capacity=3",
        "rentporto.gateway.rate-limit.login.capacity=2"})
class RateLimitTest {

    @Autowired
    private MockMvc mockMvc;

    @InjectWireMock("auth-service")
    private WireMockServer auth;

    @InjectWireMock("customer-service")
    private WireMockServer customer;

    @BeforeEach
    void setUp() {
        Tokens.publishKey(auth);
        auth.stubFor(WireMock.post("/auth/login").willReturn(ok()));
        customer.stubFor(WireMock.get("/customers/me").willReturn(ok()));
    }

    @Test
    void aClientOverItsLimitGetsA429UntilTheLimitRefills() throws Exception {
        String ana = Tokens.bearer("ana", "CUSTOMER");
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/customers/me").header(HttpHeaders.AUTHORIZATION, ana))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/v1/customers/me").header(HttpHeaders.AUTHORIZATION, ana))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("\\d+")))
                .andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
        mockMvc.perform(get("/api/v1/customers/me").header(HttpHeaders.AUTHORIZATION, Tokens.bearer("rui", "CUSTOMER")))
                .andExpect(status().isOk());
    }

    @Test
    void loginsHaveALowerLimitPerIp() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(login("192.168.1.10")).andExpect(status().isOk());
        }

        mockMvc.perform(login("192.168.1.10")).andExpect(status().isTooManyRequests());
        mockMvc.perform(login("192.168.1.11")).andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder login(String ip) {
        return post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
    }
}
