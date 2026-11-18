package pt.rentporto.fleet.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pt.rentporto.fleet.AcrissCodes;
import pt.rentporto.fleet.FleetServiceTest;
import pt.rentporto.fleet.LicensePlates;
import pt.rentporto.fleet.application.FleetCatalogue;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.DriverRequirements;
import pt.rentporto.web.testing.TestJwt;

/**
 * The rental-service, with the token of the agent at the counter, gets a vehicle of the booked category for a rental.
 */
@FleetServiceTest
class AssignmentsTest {

    private static final String FLEET_MANAGER = TestJwt.bearer("frota", "FLEET_MANAGER");
    private static final String AGENT = TestJwt.bearer("agente", "AGENT");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private FleetCatalogue catalogue;

    @Test
    void theRentalGetsTheVehicleAvailableTheLongestWhichBecomesRentedWithoutChangingTheCapacity() throws Exception {
        String category = newCategory();
        String first = register(category, "OPO", 12000);
        String second = register(category, "OPO", 500);
        changeStatus(first, "IN_MAINTENANCE");
        changeStatus(first, "AVAILABLE");
        UUID rental = UUID.randomUUID();

        assign(rental, category, "OPO", "assign-" + rental)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value(second))
                .andExpect(jsonPath("$.odometerKm").value(500));

        assertThat(jdbc.queryForObject(
                "SELECT status FROM vehicles WHERE license_plate = ?", String.class, second)).isEqualTo("RENTED");
        assertThat(jdbc.queryForMap(
                "SELECT license_plate, station_code, category_code FROM vehicle_assignments WHERE rental_id = ?",
                rental))
                .containsEntry("license_plate", second)
                .containsEntry("station_code", "OPO")
                .containsEntry("category_code", category);
        assertThat(jdbc.queryForMap(
                "SELECT from_status, to_status, reason, changed_by FROM vehicle_status_changes"
                        + " WHERE license_plate = ? ORDER BY id DESC LIMIT 1", second))
                .containsEntry("from_status", "AVAILABLE")
                .containsEntry("to_status", "RENTED")
                .containsEntry("reason", "Rental " + rental)
                .containsEntry("changed_by", "agente");
        assertThat(capacityEvents(category)).isEqualTo(4);
    }

    @Test
    void theSameIdempotencyKeyGetsTheSameAssignmentAndAnotherKeyIsAConflict() throws Exception {
        String category = newCategory();
        String plate = register(category, "CMP", 0);
        register(category, "CMP", 0);
        UUID rental = UUID.randomUUID();
        assign(rental, category, "CMP", "assign-" + rental).andExpect(status().isCreated());

        assign(rental, category, "CMP", "assign-" + rental)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value(plate));
        assign(rental, category, "CMP", "another-" + rental)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RENTAL_ALREADY_ASSIGNED"));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM vehicles WHERE category_code = ? AND status = 'RENTED'", Integer.class, category))
                .isEqualTo(1);
    }

    @Test
    void aStationWithoutAnAvailableVehicleOfTheCategoryIsAConflict() throws Exception {
        String category = newCategory();
        String plate = register(category, "TRD", 0);
        changeStatus(plate, "IN_MAINTENANCE");

        assign(UUID.randomUUID(), category, "TRD", "assign-" + plate)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_VEHICLE_AVAILABLE"));
        assign(UUID.randomUUID(), category, "MAT", "assign-mat-" + plate)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_VEHICLE_AVAILABLE"));
    }

    @Test
    void aStationOrACategoryThatDoesNotExistIsRejected() throws Exception {
        UUID rental = UUID.randomUUID();

        assign(rental, newCategory(), "BRG", "assign-" + rental)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNKNOWN_STATION"));
        assign(rental, AcrissCodes.unique(), "OPO", "assign-" + rental)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNKNOWN_CATEGORY"));
    }

    @Test
    void theIdempotencyKeyIsRequired() throws Exception {
        assign(UUID.randomUUID(), newCategory(), "OPO", null)
                .andExpect(status().isBadRequest());
    }

    private String newCategory() {
        String code = AcrissCodes.unique();
        catalogue.createCategory(new AcrissCode(code), "Group K - Van", new DriverRequirements(21, 2), null);
        return code;
    }

    private String register(String category, String station, long odometerKm) throws Exception {
        String plate = LicensePlates.unique();
        mockMvc.perform(post("/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"licensePlate": "%s", "categoryCode": "%s", "station": "%s", "odometerKm": %d}
                                """.formatted(plate, category, station, odometerKm)))
                .andExpect(status().isCreated());
        return plate;
    }

    private void changeStatus(String plate, String state) throws Exception {
        mockMvc.perform(patch("/vehicles/{plate}/status", plate)
                        .header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"%s\"}".formatted(state)))
                .andExpect(status().isOk());
    }

    private ResultActions assign(UUID rental, String category, String station, String key) throws Exception {
        MockHttpServletRequestBuilder request = post("/internal/vehicles/assignments")
                .header(HttpHeaders.AUTHORIZATION, AGENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"rentalId": "%s", "categoryCode": "%s", "station": "%s"}
                        """.formatted(rental, category, station));
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mockMvc.perform(request);
    }

    private int capacityEvents(String category) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE routing_key = 'fleet.station-capacity-changed'"
                        + " AND payload -> 'payload' ->> 'categoryCode' = ?",
                Integer.class, category);
    }
}
