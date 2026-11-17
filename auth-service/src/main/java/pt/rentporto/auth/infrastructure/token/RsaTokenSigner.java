package pt.rentporto.auth.infrastructure.token;

import java.time.Instant;
import java.util.Map;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import pt.rentporto.auth.application.TokenSigner;
import pt.rentporto.auth.domain.Account;
import pt.rentporto.auth.domain.Role;

/**
 * JWT signed with RS256: {@code sub} is the account id, and the {@code email} and {@code roles} claims carry what the
 * other services need.
 */
@Component
class RsaTokenSigner implements TokenSigner {

    static final String EMAIL = "email";
    static final String ROLES = "roles";

    private final JwtEncoder encoder;
    private final SigningKey signingKey;
    private final TokenProperties properties;

    RsaTokenSigner(JwtEncoder encoder, SigningKey signingKey, TokenProperties properties) {
        this.encoder = encoder;
        this.signingKey = signingKey;
        this.properties = properties;
    }

    @Override
    public IssuedToken sign(Account account) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(account.id().toString())
                .issuedAt(now)
                .expiresAt(now.plus(properties.ttl()))
                .claim(EMAIL, account.email().value())
                .claim(ROLES, account.roles().stream().map(Role::name).sorted().toList())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, properties.ttl());
    }

    @Override
    public Map<String, Object> publicKeys() {
        return signingKey.publicJwkSet();
    }
}
