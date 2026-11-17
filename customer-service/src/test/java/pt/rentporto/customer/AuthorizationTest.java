package pt.rentporto.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import pt.rentporto.customer.api.CustomersApi;
import pt.rentporto.customer.api.DriverProfilesApi;
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
    private CustomersApi customers;

    @Autowired
    private DriverProfilesApi driverProfiles;

    @Test
    void eachEndpointAllowsTheRolesOfTheCatalogue() {
        assertThat(AllowedRoles.of(() -> customers.saveMyProfile(null))).containsExactly("CUSTOMER");
        assertThat(AllowedRoles.of(() -> customers.myProfile())).containsExactly("CUSTOMER");
        assertThat(AllowedRoles.of(() -> customers.customer(null))).containsExactlyInAnyOrder("AGENT", "ADMIN");
        assertThat(AllowedRoles.of(() -> driverProfiles.driverProfile(null, null))).containsExactly("CUSTOMER");
    }

    @Test
    void aRoleWithoutPermissionGetsA403() throws Exception {
        mockMvc.perform(get("/customers/{id}", "8d3f6a2e-0b7c-4a51-9e43-2f1d5c6b7a80")
                        .header(HttpHeaders.AUTHORIZATION, TestJwt.bearer("ana", "CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @ParameterizedTest
    @MethodSource("pt.rentporto.web.testing.TestJwt#invalidTokens")
    void aMissingOrInvalidTokenGetsA401(String token) throws Exception {
        mockMvc.perform(get("/customers/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/customers/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void theInternalEndpointNeedsTheTokenOfTheCustomerWhoIsBooking() throws Exception {
        String driverProfile = "/internal/customers/8d3f6a2e-0b7c-4a51-9e43-2f1d5c6b7a80/driver-profile";

        mockMvc.perform(get(driverProfile).param("on", "2026-12-01"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(driverProfile).param("on", "2026-12-01")
                        .header(HttpHeaders.AUTHORIZATION, TestJwt.bearer("ana", "CUSTOMER")))
                .andExpect(status().isNotImplemented());
    }
}
