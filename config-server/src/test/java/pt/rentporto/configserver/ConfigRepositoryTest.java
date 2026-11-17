package pt.rentporto.configserver;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Serves the {@code config/} folder of the repository, as in development.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConfigRepositoryTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {
            "auth-service", "customer-service", "fleet-service",
            "booking-service", "rental-service", "notification-service"})
    void servesTheConfigurationOfEveryService(String service) throws Exception {
        mockMvc.perform(get("/{service}/dev", service))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertySources[*].name", hasItem(endsWith("/" + service + ".yml"))));
    }

    @ParameterizedTest
    @CsvSource({"dev, localhost", "k8s, postgres"})
    void servesTheAddressesOfTheRequestedProfile(String profile, String postgresHost) throws Exception {
        mockMvc.perform(get("/customer-service/{profile}", profile))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertySources[*].name", hasItem(endsWith("/application-" + profile + ".yml"))))
                .andExpect(jsonPath("$.propertySources[*].source['rentporto.postgres.host']", hasItem(postgresHost)));
    }
}
