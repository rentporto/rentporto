package pt.rentporto.fleet.api.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pt.rentporto.fleet.application.service.FleetCatalogue;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.DriverRequirements;
import pt.rentporto.fleet.domain.model.StatusChange;
import pt.rentporto.fleet.domain.model.VehicleAssignment;
import pt.rentporto.fleet.domain.model.VehicleStatus;
import pt.rentporto.fleet.testing.AcrissCodes;
import pt.rentporto.fleet.testing.FleetRecords;
import pt.rentporto.fleet.testing.FleetServiceTest;
import pt.rentporto.fleet.testing.LicensePlates;
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
    private FleetRecords records;

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

        assertThat(records.vehicle(second).status()).isEqualTo(VehicleStatus.RENTED);
        VehicleAssignment assignment = records.assignment(rental);
        assertThat(assignment.plate().value()).isEqualTo(second);
        assertThat(assignment.station()).isEqualTo("OPO");
        assertThat(assignment.category().value()).isEqualTo(category);
        assertThat(records.history(second).getLast())
                .extracting(StatusChange::from, StatusChange::to, StatusChange::reason, StatusChange::changedBy)
                .containsExactly(VehicleStatus.AVAILABLE, VehicleStatus.RENTED, "Rental " + rental, "agente");
        assertThat(records.capacities(category)).hasSize(4);
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
        assertThat(records.vehicles(category, VehicleStatus.RENTED)).isEqualTo(1);
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
}
