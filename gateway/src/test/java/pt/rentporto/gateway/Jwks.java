package pt.rentporto.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;

import com.github.tomakehurst.wiremock.WireMockServer;
import pt.rentporto.web.testing.TestJwt;

/**
 * Makes the fake auth-service publish the key of {@link TestJwt}, which the gateway reads from {@code /auth/jwks}.
 */
final class Jwks {

    private Jwks() {
    }

    static void publish(WireMockServer authService) {
        authService.stubFor(get("/auth/jwks").willReturn(okJson(TestJwt.jwkSet())));
    }
}
