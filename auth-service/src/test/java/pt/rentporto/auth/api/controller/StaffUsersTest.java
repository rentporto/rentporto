package pt.rentporto.auth.api.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.auth.testing.AuthClient;
import pt.rentporto.auth.testing.AuthServiceTest;

/**
 * Only an administrator creates agent and manager accounts.
 */
@AuthServiceTest
class StaffUsersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder encoder;

    private AuthClient client;

    @BeforeEach
    void setUp() {
        client = new AuthClient(mockMvc);
    }

    @Test
    void anAdministratorCreatesAManagerWhoCanLogIn() throws Exception {
        String email = AuthClient.uniqueEmail("manager");

        client.createStaffUser(adminToken(), email, "FLEET_MANAGER", "PRICING_MANAGER")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles", Matchers.containsInAnyOrder("FLEET_MANAGER", "PRICING_MANAGER")));
        client.login(email, AuthClient.PASSWORD).andExpect(status().isOk());
    }

    @Test
    void otherRolesAreForbidden() throws Exception {
        String agentEmail = AuthClient.uniqueEmail("agent");
        client.createStaffUser(adminToken(), agentEmail, "AGENT").andExpect(status().isCreated());

        for (String token : List.of(client.customerToken(), client.token(agentEmail, AuthClient.PASSWORD))) {
            client.createStaffUser(token, AuthClient.uniqueEmail("staff"), "AGENT")
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Test
    void withoutATokenTheAnswerIs401() throws Exception {
        client.createStaffUser(null, AuthClient.uniqueEmail("staff"), "AGENT")
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsATokenThatIsExpiredFromAnotherIssuerOrSignedWithAnotherKey() throws Exception {
        Instant now = Instant.now();
        List<String> invalidTokens = List.of(
                adminTokenFrom(encoder, "rentporto", now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1))),
                adminTokenFrom(encoder, "someone-else", now, now.plus(Duration.ofHours(1))),
                adminTokenFrom(encoderWithAnotherKey(), "rentporto", now, now.plus(Duration.ofHours(1))));

        for (String token : invalidTokens) {
            client.createStaffUser(token, AuthClient.uniqueEmail("staff"), "AGENT")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }

    @Test
    void anAdministratorCannotGiveTheAdminOrCustomerRole() throws Exception {
        for (String role : List.of("ADMIN", "CUSTOMER")) {
            client.createStaffUser(adminToken(), AuthClient.uniqueEmail("staff"), "AGENT", role)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("ROLE_NOT_ASSIGNABLE"));
        }
    }

    private String adminToken() throws Exception {
        return client.token(AuthClient.ADMIN_EMAIL, AuthClient.ADMIN_PASSWORD);
    }

    private static String adminTokenFrom(JwtEncoder encoder, String issuer, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("00000000-0000-0000-0000-000000000001")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of("ADMIN"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                .getTokenValue();
    }

    private static JwtEncoder encoderWithAnotherKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        return NimbusJwtEncoder.withKeyPair((RSAPublicKey) pair.getPublic(), (RSAPrivateKey) pair.getPrivate()).build();
    }
}
