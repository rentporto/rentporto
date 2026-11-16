package pt.rentporto.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Fails when the generated OpenAPI document differs from the committed contract. To accept a change, run with
 * {@code -Dopenapi.update=true} and commit the file.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractTest {

    private static final Path CONTRACT = Path.of("../docs/openapi/booking-service.yaml");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void matchesTheCommittedContract() throws Exception {
        String generated = mockMvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        if (Boolean.getBoolean("openapi.update")) {
            Files.createDirectories(CONTRACT.getParent());
            Files.writeString(CONTRACT, generated);
        }

        assertThat(generated)
                .as("The OpenAPI contract changed: run with -Dopenapi.update=true and commit %s", CONTRACT)
                .isEqualTo(Files.readString(CONTRACT));
    }
}
