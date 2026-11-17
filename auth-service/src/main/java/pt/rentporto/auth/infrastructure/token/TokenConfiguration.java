package pt.rentporto.auth.infrastructure.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

/**
 * Signing key, the encoder that issues the tokens, and the decoder that validates them on this service's own
 * endpoints with the local key, without calling its own JWK Set.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(TokenProperties.class)
class TokenConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TokenConfiguration.class);

    /**
     * Key that signs the tokens. Read with {@code @Value} rather than in {@link TokenProperties}, so that a
     * placeholder without a value, such as the Secret of the k8s profile, stops the startup instead of becoming the
     * key.
     *
     * @param pem RSA private key in PKCS#8 PEM, or empty to generate a key that only lasts until the service restarts
     * @return the signing key
     */
    @Bean
    SigningKey signingKey(@Value("${rentporto.auth.token.signing-key:}") String pem) {
        if (StringUtils.hasText(pem)) {
            return SigningKey.fromPem(pem);
        }
        log.warn("No rentporto.auth.token.signing-key: signing with a key generated for this run, "
                + "so the tokens stop being valid when the service restarts");
        return SigningKey.generate();
    }

    @Bean
    JwtEncoder jwtEncoder(SigningKey signingKey) {
        return signingKey.encoder();
    }

    @Bean
    JwtDecoder jwtDecoder(SigningKey signingKey, TokenProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(signingKey.publicKey()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }
}
