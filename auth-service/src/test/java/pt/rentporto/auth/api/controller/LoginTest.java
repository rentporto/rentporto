package pt.rentporto.auth.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.auth.testing.AuthClient;
import pt.rentporto.auth.testing.AuthServiceTest;

/**
 * Logging in issues a JWT that the key published at {@code /auth/jwks} validates; wrong credentials get
 * a 401 that does not say which part was wrong.
 */
@AuthServiceTest
class LoginTest {

    @Autowired
    private MockMvc mockMvc;

    private AuthClient client;

    @BeforeEach
    void setUp() {
        client = new AuthClient(mockMvc);
    }

    @Test
    void issuesAnRs256TokenThatThePublishedKeyValidates() throws Exception {
        String email = AuthClient.uniqueEmail("ines");
        String registered = client.register(email, AuthClient.PASSWORD).andReturn().getResponse().getContentAsString();

        String body = client.login(email.toUpperCase(), AuthClient.PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn().getResponse().getContentAsString();

        RSAKey publishedKey = publishedKey();
        Jwt token = NimbusJwtDecoder.withPublicKey(publishedKey.toRSAPublicKey()).build()
                .decode(JsonPath.read(body, "$.accessToken"));
        assertThat(token.getHeaders())
                .containsEntry("alg", "RS256")
                .containsEntry("kid", publishedKey.getKeyID());
        assertThat(token.getSubject()).isEqualTo(JsonPath.read(registered, "$.userId"));
        assertThat(token.getClaimAsString("email")).isEqualTo(email.toLowerCase());
        assertThat(token.getClaimAsStringList("roles")).containsExactly("CUSTOMER");
        assertThat(token.getClaimAsString("iss")).isEqualTo("rentporto");
        assertThat(Duration.between(token.getIssuedAt(), token.getExpiresAt())).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void answersAWrongPasswordAndAnUnknownEmailWithTheSame401() throws Exception {
        String email = AuthClient.uniqueEmail("pedro");
        client.register(email, AuthClient.PASSWORD);

        String wrongPassword = client.login(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = client.login(AuthClient.uniqueEmail("nobody"), AuthClient.PASSWORD)
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String tooLongPassword = client.login(email, "ç".repeat(40))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(unknownEmail).isEqualTo(wrongPassword);
        assertThat(tooLongPassword).isEqualTo(wrongPassword);
    }

    @Test
    void ignoresAnOldTokenSentToThePublicEndpoints() throws Exception {
        String email = AuthClient.uniqueEmail("sofia");
        client.register(email, AuthClient.PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired.or.invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, AuthClient.PASSWORD)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/auth/jwks").header(HttpHeaders.AUTHORIZATION, "Bearer expired.or.invalid"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    private RSAKey publishedKey() throws Exception {
        String jwks = mockMvc.perform(get("/auth/jwks"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JWKSet set = JWKSet.parse(jwks);
        assertThat(set.getKeys()).hasSize(1);
        return set.getKeys().getFirst().toRSAKey();
    }
}
