package pt.rentporto.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.rentporto.auth.AuthClient;
import pt.rentporto.auth.AuthServiceTest;
import pt.rentporto.auth.application.PasswordHasher;
import pt.rentporto.auth.domain.Account;
import pt.rentporto.auth.domain.Accounts;
import pt.rentporto.auth.domain.Email;

/**
 * A customer registers with an email not registered yet.
 */
@AuthServiceTest
class RegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Accounts accounts;

    @Autowired
    private PasswordHasher passwordHasher;

    private AuthClient client;

    @BeforeEach
    void setUp() {
        client = new AuthClient(mockMvc);
    }

    @Test
    void createsACustomerWithTheEmailInLowerCaseAndTheBcryptHashOfThePassword() throws Exception {
        String email = AuthClient.uniqueEmail("Ana.Silva");

        client.register(email, AuthClient.PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.roles").value("CUSTOMER"));

        Account account = accounts.withEmail(new Email(email)).orElseThrow();
        assertThat(account.passwordHash()).startsWith("$2a$").isNotEqualTo(AuthClient.PASSWORD);
        assertThat(passwordHasher.matches(AuthClient.PASSWORD, account.passwordHash())).isTrue();
    }

    @Test
    void rejectsAnEmailAlreadyRegisteredInAnyCase() throws Exception {
        String email = AuthClient.uniqueEmail("rui");
        client.register(email, AuthClient.PASSWORD).andExpect(status().isCreated());

        client.register(email.toUpperCase(), "another-password")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void returnsTheSameAccountWhenTheRequestIsRepeatedWithItsIdempotencyKey() throws Exception {
        String email = AuthClient.uniqueEmail("marta");
        String first = registerWithKey(email, "key-1")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        registerWithKey(email, "key-1")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value((String) JsonPath.read(first, "$.userId")));
        registerWithKey(email, "key-2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void rejectsAPasswordLongerThanWhatBcryptUses() throws Exception {
        String fortyCharactersOfTwoBytes = "ç".repeat(40);

        client.register(AuthClient.uniqueEmail("joao"), fortyCharactersOfTwoBytes)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PASSWORD_TOO_LONG"));
    }

    private ResultActions registerWithKey(String email, String idempotencyKey) throws Exception {
        return mockMvc.perform(post("/auth/register")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, AuthClient.PASSWORD)));
    }
}
