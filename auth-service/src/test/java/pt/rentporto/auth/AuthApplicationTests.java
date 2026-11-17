package pt.rentporto.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@AuthServiceTest
class AuthApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void startsWithTheSchemaOfTheMigrations() {
    }

    @Test
    void healthIsPublicForTheProbes() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
