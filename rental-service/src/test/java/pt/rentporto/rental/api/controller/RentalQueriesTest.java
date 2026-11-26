package pt.rentporto.rental.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.rental.domain.model.RentalAgreement;
import pt.rentporto.rental.testing.RentalServiceTest;
import pt.rentporto.rental.testing.TestAgreements;
import pt.rentporto.web.testing.TestJwt;

/**
 * Agents list the agreements of a station and read any of them; a customer reads only their own.
 */
@RentalServiceTest
class RentalQueriesTest {

    private static final String AGENT = TestJwt.bearer("agente", "AGENT");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAgreements testAgreements;

    @Test
    void theAgentsListTheAgreementsPickedUpOrReturnedAtAStationByPickUpTime() throws Exception {
        RentalAgreement waiting = testAgreements.awaitingPickup();
        RentalAgreement active = testAgreements.active();
        RentalAgreement returned = testAgreements.returned();
        String[] ours = {waiting.id().toString(), active.id().toString(), returned.id().toString()};

        assertThat(rentals("?station=OPO", ours)).containsExactly(ours);
        assertThat(rentals("?station=MAT", ours)).containsExactly(ours);
        assertThat(rentals("?station=MAT&status=ACTIVE", ours)).containsExactly(active.id().toString());
        assertThat(rentals("?status=RETURNED", ours)).containsExactly(returned.id().toString());
        assertThat(rentals("?station=TRD", ours)).isEmpty();
        assertThat(rentals("", ours)).containsExactly(ours);
    }

    @Test
    void theListComesByPickUpTimeWhateverTheOrderTheAgreementsWereMadeIn() throws Exception {
        RentalAgreement later = testAgreements.awaitingPickup(Instant.now().plus(Duration.ofDays(10)));
        RentalAgreement sooner = testAgreements.awaitingPickup(Instant.now().plus(Duration.ofDays(5)));

        assertThat(rentals("?station=OPO", later.id().toString(), sooner.id().toString()))
                .containsExactly(sooner.id().toString(), later.id().toString());
    }

    @Test
    void anAgreementShowsItsVehicleItsChargesAndItsStatus() throws Exception {
        RentalAgreement returned = testAgreements.returned();

        mockMvc.perform(get("/rentals/{id}", returned.id()).header(HttpHeaders.AUTHORIZATION, AGENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.licensePlate").value("AA-12-BB"))
                .andExpect(jsonPath("$.pickedUpAt").isNotEmpty())
                .andExpect(jsonPath("$.returnedAt").isNotEmpty())
                .andExpect(jsonPath("$.extraCharges").isArray())
                .andExpect(jsonPath("$.terms.dailyRate.amountCents").value(4500));
    }

    @Test
    void aCustomerReadsTheirOwnAgreementAndTheOneOfAnotherCustomerIsNotFound() throws Exception {
        RentalAgreement agreement = testAgreements.active();
        String owner = agreement.customerId().toString();
        String someoneElse = UUID.randomUUID().toString();

        mockMvc.perform(get("/rentals/{id}", agreement.id())
                        .header(HttpHeaders.AUTHORIZATION, TestJwt.customer(owner, "ana@rentporto.pt")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rentalId").value(agreement.id().toString()));
        mockMvc.perform(get("/rentals/{id}", agreement.id())
                        .header(HttpHeaders.AUTHORIZATION, TestJwt.customer(someoneElse, "rui@rentporto.pt")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RENTAL_NOT_FOUND"));
        mockMvc.perform(get("/rentals/{id}", UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, AGENT))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RENTAL_NOT_FOUND"));
    }

    private List<String> rentals(String query, String... ofThisTest) throws Exception {
        String json = mockMvc.perform(get("/rentals" + query).header(HttpHeaders.AUTHORIZATION, AGENT))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<String> ids = JsonPath.read(json, "$[*].rentalId");
        return ids.stream().filter(List.of(ofThisTest)::contains).toList();
    }
}
