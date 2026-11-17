package pt.rentporto.customer.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.rentporto.customer.CustomerServiceTest;
import pt.rentporto.customer.TaxNumbers;
import pt.rentporto.web.testing.TestJwt;

/**
 * The booking-service reads the driver profile of the customer who is booking, with that customer's JWT.
 */
@CustomerServiceTest
class DriverProfileTest {

    @Autowired
    private MockMvc mockMvc;

    private UUID id;

    @BeforeEach
    void registerACustomer() throws Exception {
        id = UUID.randomUUID();
        mockMvc.perform(put("/customers/me")
                        .header(HttpHeaders.AUTHORIZATION, token(id))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Ana Ferreira",
                                  "taxNumber": "%s",
                                  "birthDate": "2000-05-10",
                                  "phone": "+351912345678",
                                  "driverLicense": {
                                    "number": "P-1234567 8",
                                    "category": "B",
                                    "issuingCountry": "PT",
                                    "issueDate": "2024-11-18",
                                    "expiryDate": "2034-11-18"
                                  }
                                }
                                """.formatted(TaxNumbers.unique())))
                .andExpect(status().isOk());
    }

    @Test
    void returnsTheAgeAndTheLicenceSeniorityOnTheDate() throws Exception {
        driverProfile(id, "2026-11-18", token(id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(id.toString()))
                .andExpect(jsonPath("$.on").value("2026-11-18"))
                .andExpect(jsonPath("$.age").value(26))
                .andExpect(jsonPath("$.licenseYears").value(2))
                .andExpect(jsonPath("$.licenseValid").value(true));
    }

    @Test
    void onlyTheCustomerReadsTheirOwnProfile() throws Exception {
        driverProfile(id, "2026-11-18", null).andExpect(status().isUnauthorized());
        driverProfile(id, "2026-11-18", token(UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        driverProfile(id, "2026-11-18", TestJwt.bearer(id.toString(), "AGENT")).andExpect(status().isForbidden());
    }

    @Test
    void aCustomerWithoutProfileOrADateBeforeTheBirthIsAnError() throws Exception {
        UUID withoutProfile = UUID.randomUUID();
        driverProfile(withoutProfile, "2026-11-18", token(withoutProfile))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
        driverProfile(id, "1999-01-01", token(id))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DATE_BEFORE_BIRTH"));
    }

    private ResultActions driverProfile(UUID customer, String on, String token) throws Exception {
        var request = get("/internal/customers/{id}/driver-profile", customer).param("on", on);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, token);
        }
        return mockMvc.perform(request);
    }

    private static String token(UUID customer) {
        return TestJwt.customer(customer.toString(), "customer-" + customer + "@rentporto.pt");
    }
}
