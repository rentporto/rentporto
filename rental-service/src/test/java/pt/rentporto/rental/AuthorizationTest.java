package pt.rentporto.rental;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.rental.api.contract.RentalsApi;
import pt.rentporto.rental.testing.RentalServiceTest;
import pt.rentporto.web.testing.AllowedRoles;
import pt.rentporto.web.testing.TestJwt;

/**
 * Each endpoint lets through the roles of the catalogue and nothing else, also when called directly, without the
 * gateway.
 */
@RentalServiceTest
class AuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RentalsApi rentals;

    @Test
    void eachEndpointAllowsTheRolesOfTheCatalogue() {
        assertThat(AllowedRoles.of(() -> rentals.rentals(null, null))).containsExactly("AGENT");
        assertThat(AllowedRoles.of(() -> rentals.rental(null))).containsExactlyInAnyOrder("AGENT", "CUSTOMER");
        assertThat(AllowedRoles.of(() -> rentals.pickUp(null, null))).containsExactly("AGENT");
        assertThat(AllowedRoles.of(() -> rentals.returnVehicle(null, null))).containsExactly("AGENT");
    }

    @Test
    void aRoleWithoutPermissionGetsA403() throws Exception {
        mockMvc.perform(get("/rentals").header(HttpHeaders.AUTHORIZATION, TestJwt.bearer("ana", "CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @ParameterizedTest
    @MethodSource("pt.rentporto.web.testing.TestJwt#invalidTokens")
    void aMissingOrInvalidTokenGetsA401(String token) throws Exception {
        mockMvc.perform(get("/rentals")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/rentals").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
