package pt.rentporto.gateway;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Tokens signed as the auth-service signs them, with a key whose public part the fake auth-service publishes.
 */
public final class Tokens {

    public static final String ISSUER = "rentporto";

    private static final String KEY_ID = "test-key";
    private static final KeyPair KEYS = generateKeys();
    private static final JwtEncoder ENCODER = encoder(KEYS);

    private Tokens() {
    }

    /**
     * Makes the fake auth-service publish the public key at {@code /auth/jwks}.
     *
     * @param authService the WireMock server in the place of the auth-service
     */
    public static void publishKey(WireMockServer authService) {
        RSAKey publicKey = new RSAKey.Builder((RSAPublicKey) KEYS.getPublic()).keyID(KEY_ID).build();
        authService.stubFor(get("/auth/jwks").willReturn(okJson(new JWKSet(publicKey).toString())));
    }

    /**
     * {@code Authorization} header with a valid token.
     *
     * @param subject id of the account
     * @param roles   roles of the account
     * @return {@code Bearer <token>}
     */
    public static String bearer(String subject, String... roles) {
        Instant now = Instant.now();
        return bearer(ENCODER, ISSUER, subject, now, now.plus(Duration.ofHours(1)), roles);
    }

    public static String expired() {
        Instant now = Instant.now();
        return bearer(ENCODER, ISSUER, "user", now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1)));
    }

    public static String fromAnotherIssuer() {
        Instant now = Instant.now();
        return bearer(ENCODER, "someone-else", "user", now, now.plus(Duration.ofHours(1)));
    }

    public static String signedWithAnotherKey() {
        Instant now = Instant.now();
        return bearer(encoder(generateKeys()), ISSUER, "user", now, now.plus(Duration.ofHours(1)));
    }

    private static String bearer(
            JwtEncoder encoder, String issuer, String subject, Instant issuedAt, Instant expiresAt, String... roles) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of(roles))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(KEY_ID).build();
        return "Bearer " + encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static JwtEncoder encoder(KeyPair keys) {
        return NimbusJwtEncoder.withKeyPair((RSAPublicKey) keys.getPublic(), (RSAPrivateKey) keys.getPrivate())
                .jwkPostProcessor(key -> key.keyID(KEY_ID))
                .build();
    }

    private static KeyPair generateKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
