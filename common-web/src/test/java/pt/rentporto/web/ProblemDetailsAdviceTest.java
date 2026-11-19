package pt.rentporto.web;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class ProblemDetailsAdviceTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new ProblemDetailsAdvice())
            .build();

    @ParameterizedTest
    @CsvSource({"NOT_FOUND, 404", "CONFLICT, 409", "UNPROCESSABLE, 422", "UNAVAILABLE, 503"})
    void answersABusinessErrorWithItsCode(BusinessException.Category category, int expectedStatus) throws Exception {
        mockMvc.perform(get("/business/{category}", category))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.code").value("RULE_BROKEN"))
                .andExpect(jsonPath("$.detail").value("Rule broken"));
    }

    @Test
    void answersAnInvalidBodyWithTheInvalidFields() throws Exception {
        mockMvc.perform(post("/named").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void answersAnInvalidParameterWithTheInvalidParameter() throws Exception {
        mockMvc.perform(get("/page").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void answersAnEndpointNotImplementedWith501() throws Exception {
        mockMvc.perform(get("/not-implemented"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code").value("NOT_IMPLEMENTED"));
    }

    @Test
    void answersAnUnexpectedErrorWithoutItsDetails() throws Exception {
        mockMvc.perform(get("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(not(containsString("secret"))));
    }

    @Test
    void addsTheStatusAsCodeToOtherErrors() throws Exception {
        mockMvc.perform(post("/named").contentType(MediaType.TEXT_PLAIN).content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @RestController
    static class TestController {

        @GetMapping("/business/{category}")
        void business(@PathVariable BusinessException.Category category) {
            throw new RuleBroken(category);
        }

        @PostMapping("/named")
        void named(@Valid @RequestBody Named named) {
        }

        @GetMapping("/page")
        void page(@RequestParam @Min(1) int size) {
        }

        @GetMapping("/not-implemented")
        void notImplemented() {
            throw new EndpointNotImplementedException();
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("the database password is secret");
        }
    }

    record Named(@NotBlank String name) {
    }

    static class RuleBroken extends BusinessException {

        RuleBroken(Category category) {
            super("RULE_BROKEN", category, "Rule broken");
        }
    }
}
