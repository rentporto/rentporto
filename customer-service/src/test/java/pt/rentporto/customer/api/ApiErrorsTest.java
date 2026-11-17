package pt.rentporto.customer.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.customer.CustomerServiceTest;
import pt.rentporto.web.testing.TestJwt;

@CustomerServiceTest
class ApiErrorsTest {

    private static final String CUSTOMER = TestJwt.bearer("ana", "CUSTOMER");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void answersAnEndpointNotImplementedYetWith501() throws Exception {
        mockMvc.perform(get("/internal/customers/{id}/driver-profile", "8d3f6a2e-0b7c-4a51-9e43-2f1d5c6b7a80")
                        .param("on", "2026-12-01")
                        .header(HttpHeaders.AUTHORIZATION, CUSTOMER))
                .andExpect(status().isNotImplemented())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_IMPLEMENTED"));
    }

    @Test
    void answersAnInvalidProfileWithTheInvalidFields() throws Exception {
        mockMvc.perform(put("/customers/me")
                        .header(HttpHeaders.AUTHORIZATION, CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taxNumber\": \"12\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[?(@.field == 'taxNumber')]").exists());
    }
}
