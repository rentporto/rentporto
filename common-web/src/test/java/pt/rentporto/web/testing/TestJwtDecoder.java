package pt.rentporto.web.testing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Validates the tokens of {@link TestJwt} as a service validates the real ones: signature, expiry and issuer. It
 * replaces the decoder that reads {@code /auth/jwks}, which the tests do not have.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestJwtDecoder {

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(TestJwt.publicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(TestJwt.ISSUER));
        return decoder;
    }
}
