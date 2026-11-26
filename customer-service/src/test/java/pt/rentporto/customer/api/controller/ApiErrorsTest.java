package pt.rentporto.customer.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.customer.testing.CustomerServiceTest;
import pt.rentporto.web.testing.TestJwt;

@CustomerServiceTest
class ApiErrorsTest {

    private static final String CUSTOMER = TestJwt.bearer("ana", "CUSTOMER");

    @Autowired
    private MockMvc mockMvc;

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
