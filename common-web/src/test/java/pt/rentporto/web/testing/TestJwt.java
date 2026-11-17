package pt.rentporto.web.testing;

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
 * Tokens signed as the auth-service signs them, with a key that only exists in the tests. {@link TestJwtDecoder}
 * validates them as the services validate the real ones.
 */
public final class TestJwt {

    /** Value of the {@code iss} claim the services accept. */
    public static final String ISSUER = "rentporto";

    /** Every role of the RentPorto accounts. */
    public static final List<String> ROLES = List.of("CUSTOMER", "AGENT", "FLEET_MANAGER", "PRICING_MANAGER", "ADMIN");

    private static final String KEY_ID = "test-key";
    private static final KeyPair KEYS = generateKeys();
    private static final JwtEncoder ENCODER = encoder(KEYS);

    private TestJwt() {
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

    /**
     * {@code Authorization} header with a token that expired an hour ago.
     *
     * @return {@code Bearer <token>}
     */
    public static String expired() {
        Instant now = Instant.now();
        return bearer(ENCODER, ISSUER, "user", now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1)), "ADMIN");
    }

    /**
     * {@code Authorization} header with a token issued by someone else.
     *
     * @return {@code Bearer <token>}
     */
    public static String fromAnotherIssuer() {
        Instant now = Instant.now();
        return bearer(ENCODER, "someone-else", "user", now, now.plus(Duration.ofHours(1)), "ADMIN");
    }

    /**
     * {@code Authorization} header with a token signed with a key the services do not know.
     *
     * @return {@code Bearer <token>}
     */
    public static String signedWithAnotherKey() {
        Instant now = Instant.now();
        return bearer(encoder(generateKeys()), ISSUER, "user", now, now.plus(Duration.ofHours(1)), "ADMIN");
    }

    /**
     * {@code Authorization} headers that every service answers with 401: expired, from another issuer, signed with
     * another key and not a JWT. For {@code @MethodSource("pt.rentporto.web.testing.TestJwt#invalidTokens")}.
     *
     * @return the headers
     */
    public static List<String> invalidTokens() {
        return List.of(expired(), fromAnotherIssuer(), signedWithAnotherKey(), "Bearer not.a.token");
    }

    /**
     * Public key that validates the tokens of this class.
     *
     * @return the key
     */
    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEYS.getPublic();
    }

    /**
     * JWK Set with the public key, as the auth-service publishes it at {@code /auth/jwks}.
     *
     * @return the JWK Set as JSON
     */
    public static String jwkSet() {
        return new JWKSet(new RSAKey.Builder(publicKey()).keyID(KEY_ID).build()).toString();
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
