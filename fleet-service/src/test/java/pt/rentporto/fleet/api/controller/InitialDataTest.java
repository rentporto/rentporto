package pt.rentporto.fleet.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.fleet.testing.FleetRecords;
import pt.rentporto.fleet.testing.FleetServiceTest;

/**
 * A service started on an empty database has the four stations and the example categories, and the booking-service
 * hears of each category.
 */
@FleetServiceTest
class InitialDataTest {

    private static final List<String> EXAMPLES = List.of("CDMR", "ECMR", "IFAR", "MBMR", "PDAR");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FleetRecords records;

    @Test
    void theFourStationsExist() throws Exception {
        mockMvc.perform(get("/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[*].code").value(contains("CMP", "MAT", "OPO", "TRD")))
                .andExpect(jsonPath("$[*].name")
                        .value(contains("Campanhã", "Matosinhos", "Porto Airport", "Baixa-Trindade")));
    }

    @Test
    void theExampleCategoriesExistWithPremiumAsking25Years() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'MBMR')].name").value("Group A - Mini"))
                .andExpect(jsonPath("$[?(@.code == 'ECMR')].name").value("Group B - Economy"))
                .andExpect(jsonPath("$[?(@.code == 'CDMR')].name").value("Group C - Compact"))
                .andExpect(jsonPath("$[?(@.code == 'IFAR')].name").value("Group D - Intermediate SUV"))
                .andExpect(jsonPath("$[?(@.code == 'PDAR')].name").value("Group E - Premium"))
                .andExpect(jsonPath("$[?(@.code in ['MBMR', 'ECMR', 'CDMR', 'IFAR'])].minimumAge")
                        .value(contains(21, 21, 21, 21)))
                .andExpect(jsonPath("$[?(@.code == 'PDAR')].minimumAge").value(25))
                .andExpect(jsonPath("$[?(@.code in ['MBMR', 'ECMR', 'CDMR', 'IFAR', 'PDAR'])].minimumLicenseYears")
                        .value(contains(2, 2, 2, 2, 2)));
    }

    @Test
    void eachExampleCategoryIsPublishedOnce() {
        assertThat(EXAMPLES).allSatisfy(code -> assertThat(records.categoryEvents(code)).hasSize(1));
    }
}
