package pt.rentporto.notification;

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
import pt.rentporto.notification.api.contract.NotificationsApi;
import pt.rentporto.notification.testing.NotificationServiceTest;
import pt.rentporto.web.testing.AllowedRoles;
import pt.rentporto.web.testing.TestJwt;

/**
 * The delivery history is only for administrators, also when called directly, without the gateway.
 */
@NotificationServiceTest
class AuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationsApi notifications;

    @Test
    void eachEndpointAllowsTheRolesOfTheCatalogue() {
        assertThat(AllowedRoles.of(() -> notifications.notifications(null))).containsExactly("ADMIN");
    }

    @Test
    void aRoleWithoutPermissionGetsA403() throws Exception {
        mockMvc.perform(get("/notifications")
                        .param("customerId", "8d3f6a2e-0b7c-4a51-9e43-2f1d5c6b7a80")
                        .header(HttpHeaders.AUTHORIZATION, TestJwt.bearer("rui", "AGENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @ParameterizedTest
    @MethodSource("pt.rentporto.web.testing.TestJwt#invalidTokens")
    void aMissingOrInvalidTokenGetsA401(String token) throws Exception {
        mockMvc.perform(get("/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/notifications").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
