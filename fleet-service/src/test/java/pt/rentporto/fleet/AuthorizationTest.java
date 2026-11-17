package pt.rentporto.fleet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.fleet.api.CategoriesApi;
import pt.rentporto.fleet.api.StationsApi;
import pt.rentporto.fleet.api.VehicleAssignmentsApi;
import pt.rentporto.fleet.api.VehiclesApi;
import pt.rentporto.web.testing.AllowedRoles;
import pt.rentporto.web.testing.TestJwt;
import pt.rentporto.web.testing.TestJwtDecoder;

/**
 * Each endpoint lets through the roles of the catalogue and nothing else, also when called directly, without the
 * gateway.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestJwtDecoder.class)
class AuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StationsApi stations;

    @Autowired
    private CategoriesApi categories;

    @Autowired
    private VehiclesApi vehicles;

    @Autowired
    private VehicleAssignmentsApi assignments;

    @Test
    void eachEndpointAllowsTheRolesOfTheCatalogue() {
        assertThat(AllowedRoles.of(() -> stations.stations())).hasSameElementsAs(TestJwt.ROLES);
        assertThat(AllowedRoles.of(() -> categories.categories())).hasSameElementsAs(TestJwt.ROLES);
        assertThat(AllowedRoles.of(() -> categories.createCategory(null, null))).containsExactly("FLEET_MANAGER");
        assertThat(AllowedRoles.of(() -> categories.updateCategory(null, null))).containsExactly("FLEET_MANAGER");
        assertThat(AllowedRoles.of(() -> vehicles.registerVehicle(null, null))).containsExactly("FLEET_MANAGER");
        assertThat(AllowedRoles.of(() -> vehicles.vehicles(null, null))).containsExactly("FLEET_MANAGER");
        assertThat(AllowedRoles.of(() -> vehicles.changeStatus(null, null))).containsExactly("FLEET_MANAGER");
        assertThat(AllowedRoles.of(() -> vehicles.startTransfer(null, null, null))).containsExactly("FLEET_MANAGER");
        assertThat(AllowedRoles.of(() -> vehicles.completeTransfer(null, null))).containsExactly("FLEET_MANAGER");
        assertThat(AllowedRoles.of(() -> assignments.assignVehicle(null, null))).containsExactly("AGENT");
    }

    @Test
    void aRoleWithoutPermissionGetsA403() throws Exception {
        mockMvc.perform(get("/vehicles").header(HttpHeaders.AUTHORIZATION, TestJwt.bearer("rui", "AGENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @ParameterizedTest
    @MethodSource("pt.rentporto.web.testing.TestJwt#invalidTokens")
    void aMissingOrInvalidTokenGetsA401(String token) throws Exception {
        mockMvc.perform(get("/vehicles")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/vehicles").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void theStationsAndTheCategoriesArePublic() throws Exception {
        mockMvc.perform(get("/stations")).andExpect(status().isNotImplemented());
        mockMvc.perform(get("/categories")).andExpect(status().isNotImplemented());
    }

    @Test
    void theInternalEndpointNeedsTheTokenOfTheAgentAtTheCounter() throws Exception {
        mockMvc.perform(post("/internal/vehicles/assignments")).andExpect(status().isUnauthorized());
    }
}
