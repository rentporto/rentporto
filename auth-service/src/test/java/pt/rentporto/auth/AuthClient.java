package pt.rentporto.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Calls the API as a client would, for the steps a test needs before the one it checks.
 */
public final class AuthClient {

    public static final String ADMIN_EMAIL = "admin@rentporto.pt";
    public static final String ADMIN_PASSWORD = "admin-password";
    public static final String PASSWORD = "secret-password";

    private final MockMvc mockMvc;

    public AuthClient(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    /**
     * Email no other test uses, so the tests can share the database.
     *
     * @param name readable part of the email
     * @return a unique email
     */
    public static String uniqueEmail(String name) {
        return name + "-" + UUID.randomUUID() + "@rentporto.pt";
    }

    public ResultActions register(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password)));
    }

    public ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password)));
    }

    /**
     * Logs in and returns the access token.
     *
     * @param email    email of the account
     * @param password password of the account
     * @return the JWT
     */
    public String token(String email, String password) throws Exception {
        String body = login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    /**
     * Registers a customer and logs in.
     *
     * @return the JWT of a new customer
     */
    public String customerToken() throws Exception {
        String email = uniqueEmail("customer");
        register(email, PASSWORD).andExpect(status().isCreated());
        return token(email, PASSWORD);
    }

    public ResultActions createStaffUser(String token, String email, String... roles) throws Exception {
        var request = post("/auth/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s", "roles": ["%s"]}
                        """.formatted(email, PASSWORD, String.join("\", \"", roles)));
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private static String credentials(String email, String password) {
        return """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);
    }
}
