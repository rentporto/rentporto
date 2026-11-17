package pt.rentporto.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.wiremock.spring.InjectWireMock;

/**
 * Only a valid token reaches a protected route; the public routes of the catalogue pass without one.
 */
@GatewayTest
class AuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @InjectWireMock("auth-service")
    private WireMockServer auth;

    @InjectWireMock("customer-service")
    private WireMockServer customer;

    @InjectWireMock("fleet-service")
    private WireMockServer fleet;

    @BeforeEach
    void setUp() {
        Tokens.publishKey(auth);
        customer.stubFor(WireMock.get("/customers/me").willReturn(ok()));
    }

    @Test
    void aProtectedRouteWithoutATokenIsA401() throws Exception {
        mockMvc.perform(get("/api/v1/customers/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        customer.verify(0, anyRequestedFor(anyUrl()));
    }

    @ParameterizedTest
    @MethodSource("invalidTokens")
    void anInvalidTokenIsA401AndNeverReachesTheService(String token) throws Exception {
        mockMvc.perform(get("/api/v1/customers/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        customer.verify(0, anyRequestedFor(anyUrl()));
    }

    @Test
    void aValidTokenReachesTheService() throws Exception {
        mockMvc.perform(get("/api/v1/customers/me").header(HttpHeaders.AUTHORIZATION, Tokens.bearer("ana", "CUSTOMER")))
                .andExpect(status().isOk());
    }

    @Test
    void thePublicRoutesPassWithoutATokenOrWithAnOldOne() throws Exception {
        fleet.stubFor(WireMock.get("/stations").willReturn(ok()));
        auth.stubFor(WireMock.post("/auth/login").willReturn(ok()));

        mockMvc.perform(get("/api/v1/stations")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/auth/login").header(HttpHeaders.AUTHORIZATION, Tokens.expired()))
                .andExpect(status().isOk());

        auth.verify(WireMock.postRequestedFor(urlPathEqualTo("/auth/login")));
    }

    static String[] invalidTokens() {
        return new String[] {
                Tokens.expired(), Tokens.fromAnotherIssuer(), Tokens.signedWithAnotherKey(), "Bearer not.a.token"};
    }
}
