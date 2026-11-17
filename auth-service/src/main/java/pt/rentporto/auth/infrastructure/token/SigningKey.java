package pt.rentporto.auth.infrastructure.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Map;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * RSA key pair that signs the tokens with RS256. Its key id is the RFC 7638 thumbprint of the public key, so the same
 * key has the same id after a restart.
 */
public final class SigningKey {

    private static final int KEY_SIZE = 2048;

    private final RSAKey key;

    private SigningKey(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        try {
            this.key = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyIDFromThumbprint()
                    .build();
        } catch (JOSEException e) {
            throw new IllegalStateException("Cannot compute the key id", e);
        }
    }

    /**
     * Reads a key from PEM, as {@code openssl genpkey -algorithm RSA} writes it.
     *
     * @param pem RSA private key in PKCS#8 PEM ({@code -----BEGIN PRIVATE KEY-----})
     * @return the key pair
     * @throws IllegalArgumentException if the PEM is not an RSA private key in PKCS#8
     */
    public static SigningKey fromPem(String pem) {
        RSAPrivateKey privateKey = RsaKeyConverters.pkcs8()
                .convert(new ByteArrayInputStream(pem.strip().getBytes(StandardCharsets.US_ASCII)));
        if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
            throw new IllegalArgumentException("The signing key must include the public exponent");
        }
        try {
            RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent()));
            return new SigningKey(publicKey, privateKey);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid RSA signing key", e);
        }
    }

    /**
     * Generates a new 2048-bit key, which only lives in memory.
     *
     * @return the key pair
     */
    public static SigningKey generate() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(KEY_SIZE);
            KeyPair pair = generator.generateKeyPair();
            return new SigningKey((RSAPublicKey) pair.getPublic(), (RSAPrivateKey) pair.getPrivate());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot generate an RSA key", e);
        }
    }

    /**
     * Identifier of the key, sent in the {@code kid} header of the tokens.
     *
     * @return the RFC 7638 thumbprint of the public key
     */
    public String keyId() {
        return key.getKeyID();
    }

    /**
     * Public key that validates the tokens.
     *
     * @return the public key
     */
    public RSAPublicKey publicKey() {
        try {
            return key.toRSAPublicKey();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * JWK Set published at {@code /auth/jwks}, without the private part.
     *
     * @return the JWK Set as a JSON object
     */
    public Map<String, Object> publicJwkSet() {
        return new JWKSet(key.toPublicJWK()).toJSONObject();
    }

    /**
     * Encoder that signs with this key and puts its id in the {@code kid} header. The private key never leaves this
     * class.
     *
     * @return the encoder
     */
    public JwtEncoder encoder() {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
}
