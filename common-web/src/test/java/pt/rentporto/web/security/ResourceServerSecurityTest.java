package pt.rentporto.web.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.web.testing.TestJwt;

@SpringBootTest(classes = SecurityTestApplication.class)
@AutoConfigureMockMvc
class ResourceServerSecurityTest {

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

    @ParameterizedTest
    @MethodSource("invalidTokens")
    void anInvalidTokenIsA401(String token) throws Exception {
        mockMvc.perform(get("/protected").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void theRolesClaimDecidesWhatPreAuthorizeAllows() throws Exception {
        mockMvc.perform(get("/admin").header(HttpHeaders.AUTHORIZATION, TestJwt.bearer("ana", "ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin").header(HttpHeaders.AUTHORIZATION, TestJwt.bearer("rui", "CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void thePublicEndpointsIgnoreAnInvalidToken() throws Exception {
        mockMvc.perform(get("/public").header(HttpHeaders.AUTHORIZATION, "Bearer expired.or.invalid"))
                .andExpect(status().isOk());
    }

    @Test
    void theOpenApiDocumentAndTheMetricsAreAlwaysPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
    }

    static String[] invalidTokens() {
        return new String[] {
                TestJwt.expired(), TestJwt.fromAnotherIssuer(), TestJwt.signedWithAnotherKey(), "Bearer not.a.token"};
    }
}
