package pt.rentporto.auth.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class SigningKeyTest {

    @Test
    void theKeyFromTheSecretValidatesTokensSignedBeforeARestart() throws Exception {
        String pem = privateKeyPem(generateKeyPair());
        SigningKey beforeRestart = SigningKey.fromPem(pem);
        SigningKey afterRestart = SigningKey.fromPem(pem);

        String token = beforeRestart.encoder()
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(SignatureAlgorithm.RS256).build(),
                        JwtClaimsSet.builder().subject("user").build()))
                .getTokenValue();

        assertThat(afterRestart.keyId()).isEqualTo(beforeRestart.keyId());
        assertThat(NimbusJwtDecoder.withPublicKey(afterRestart.publicKey()).build().decode(token).getSubject())
                .isEqualTo("user");
    }

    @Test
    void aGeneratedKeyIsNewOnEveryStartup() {
        assertThat(SigningKey.generate().keyId()).isNotEqualTo(SigningKey.generate().keyId());
    }

    @Test
    void publishesOnlyThePublicPartOfTheKey() {
        Map<String, Object> jwks = SigningKey.generate().publicJwkSet();

        @SuppressWarnings("unchecked")
        Map<String, Object> key = ((List<Map<String, Object>>) jwks.get("keys")).getFirst();
        assertThat(key).containsOnlyKeys("kty", "use", "alg", "kid", "n", "e");
        assertThat(key).containsEntry("kty", "RSA").containsEntry("use", "sig").containsEntry("alg", "RS256");
    }

    @Test
    void rejectsAPemThatIsNotAPrivateKey() throws Exception {
        String publicKeyPem = pem("PUBLIC KEY", generateKeyPair().getPublic().getEncoded());

        assertThatThrownBy(() -> SigningKey.fromPem(publicKeyPem)).isInstanceOf(IllegalArgumentException.class);
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String privateKeyPem(KeyPair pair) {
        return pem("PRIVATE KEY", pair.getPrivate().getEncoded());
    }

    private static String pem(String type, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN " + type + "-----\n" + body + "\n-----END " + type + "-----\n";
    }
}
