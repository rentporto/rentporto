package pt.rentporto.gateway.docs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.wiremock.spring.InjectWireMock;
import pt.rentporto.gateway.GatewayTest;

/**
 * The Swagger UI of the gateway, at {@code /api/docs} and without a token, lists the six services with an API and
 * shows each one without its internal endpoints, with the requests going through the gateway.
 */
@GatewayTest
class ApiDocsTest {

    private static final String FLEET_DOCS = """
            {"openapi": "3.1.0", "servers": [{"url": "http://fleet-service:8083"}],
             "paths": {"/stations": {"get": {}}, "/internal/vehicles/assignments": {"post": {}}}}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServiceDocs serviceDocs;

    @InjectWireMock("fleet-service")
    private WireMockServer fleet;

    @InjectWireMock("rental-service")
    private WireMockServer rental;

    @BeforeEach
    void resetTheServices() {
        fleet.resetAll();
        rental.resetAll();
    }

    @Test
    void theSwaggerUiOpensWithoutATokenAndListsTheSixServices() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/api/swagger-ui/index.html")));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/swagger-ui/index.html"))
                .andExpect(status().isOk());

        String config = mockMvc.perform(MockMvcRequestBuilders.get("/api/docs/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, String>> urls = JsonPath.read(config, "$.urls");
        assertThat(urls).extracting(url -> url.get("name")).containsExactlyInAnyOrderElementsOf(serviceDocs.names());
        assertThat(urls).allSatisfy(url -> assertThat(url.get("url")).isEqualTo("/api/docs/services/" + url.get("name")));
    }

    @Test
    void aServiceIsShownWithoutItsInternalEndpointsAndWithTheGatewayAsItsServer() throws Exception {
        fleet.stubFor(get("/v3/api-docs").willReturn(okJson(FLEET_DOCS)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/docs/services/fleet-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/stations']").exists())
                .andExpect(jsonPath("$.paths['/internal/vehicles/assignments']").doesNotExist())
                .andExpect(jsonPath("$.servers.length()").value(1))
                .andExpect(jsonPath("$.servers[0].url").value("/api/v1"));
    }

    @Test
    void aServiceThatDoesNotAnswerIsA503AndTheOthersAreStillShown() throws Exception {
        rental.stubFor(get("/v3/api-docs").willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        fleet.stubFor(get("/v3/api-docs").willReturn(okJson(FLEET_DOCS)));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/docs/services/rental-service"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("API_DOCS_UNAVAILABLE"));
        rental.stubFor(get("/v3/api-docs").willReturn(aResponse().withStatus(500)));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/docs/services/rental-service"))
                .andExpect(status().isServiceUnavailable());
        mockMvc.perform(MockMvcRequestBuilders.get("/api/docs/services/fleet-service"))
                .andExpect(status().isOk());
    }

    @Test
    void aServiceWithoutAnApiIsNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/docs/services/config-server"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SERVICE_NOT_FOUND"));
    }
}
