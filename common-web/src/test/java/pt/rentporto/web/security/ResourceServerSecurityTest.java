package pt.rentporto.web.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = SecurityTestApplication.class)
@AutoConfigureMockMvc
class ResourceServerSecurityTest {

    private static final JwtEncoder ENCODER = NimbusJwtEncoder.withKeyPair(
            (RSAPublicKey) SecurityTestApplication.KEYS.getPublic(),
            (RSAPrivateKey) SecurityTestApplication.KEYS.getPrivate()).build();

    @Autowired
    private MockMvc mockMvc;

    @Test
    void withoutATokenTheAnswerIsA401ProblemDetail() throws Exception {
        mockMvc.perform(get("/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void anInvalidTokenIsA401() throws Exception {
        mockMvc.perform(get("/protected").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void theRolesClaimDecidesWhatPreAuthorizeAllows() throws Exception {
        mockMvc.perform(get("/admin").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin").header(HttpHeaders.AUTHORIZATION, bearer("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void aPublicEndpointIgnoresAnInvalidToken() throws Exception {
        mockMvc.perform(get("/public").header(HttpHeaders.AUTHORIZATION, "Bearer expired.or.invalid"))
                .andExpect(status().isOk());
    }

    private static String bearer(String role) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .claim("roles", List.of(role))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return "Bearer " + ENCODER.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
