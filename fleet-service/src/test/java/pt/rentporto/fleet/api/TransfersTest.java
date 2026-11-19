package pt.rentporto.fleet.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pt.rentporto.fleet.AcrissCodes;
import pt.rentporto.fleet.FleetRecords;
import pt.rentporto.fleet.FleetServiceTest;
import pt.rentporto.fleet.LicensePlates;
import pt.rentporto.fleet.application.FleetCatalogue;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.DriverRequirements;
import pt.rentporto.fleet.domain.StatusChange;
import pt.rentporto.fleet.domain.VehicleStatus;
import pt.rentporto.web.testing.TestJwt;

/**
 * A fleet manager moves a vehicle between stations: the origin loses it when it leaves and the destination gains it
 * when it arrives.
 */
@FleetServiceTest
class TransfersTest {

    private static final String FLEET_MANAGER = TestJwt.bearer("frota", "FLEET_MANAGER");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FleetRecords records;

    @Autowired
    private FleetCatalogue catalogue;

    @Test
    void theOriginLosesTheVehicleWhenItLeavesAndTheDestinationGainsItWhenItArrives() throws Exception {
        String category = newCategory();
        String plate = register(category, "OPO");

        String transfer = start(plate, "MAT", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value(plate))
                .andExpect(jsonPath("$.fromStation").value("OPO"))
                .andExpect(jsonPath("$.toStation").value("MAT"))
                .andExpect(jsonPath("$.status").value("IN_TRANSFER"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(transfer, "$.transferId");

        assertThat(capacities("OPO", category)).containsExactly(1, 0);
        assertThat(capacities("MAT", category)).isEmpty();
        assertThat(vehicle(plate)).containsEntry("station", "OPO").containsEntry("status", "IN_TRANSFER");

        complete(plate, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferId").value(id))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(capacities("OPO", category)).containsExactly(1, 0);
        assertThat(capacities("MAT", category)).containsExactly(1);
        assertThat(vehicle(plate)).containsEntry("station", "MAT").containsEntry("status", "AVAILABLE");
        assertThat(records.history(plate)).extracting(StatusChange::to)
                .containsExactly(VehicleStatus.AVAILABLE, VehicleStatus.IN_TRANSFER, VehicleStatus.AVAILABLE);
    }

    @Test
    void theSameIdempotencyKeyGetsTheSameTransferAndAnotherRequestFindsTheVehicleInTransfer() throws Exception {
        String category = newCategory();
        String plate = register(category, "OPO");
        String id = JsonPath.read(start(plate, "MAT", "move-" + plate)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.transferId");

        start(plate, "MAT", "move-" + plate)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferId").value(id));
        start(plate, "TRD", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        assertThat(capacities("OPO", category)).containsExactly(1, 0);
    }

    @Test
    void completingATransferAgainChangesNothing() throws Exception {
        String category = newCategory();
        String plate = register(category, "OPO");
        String id = JsonPath.read(start(plate, "MAT", null)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.transferId");
        complete(plate, id).andExpect(status().isOk());

        complete(plate, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(capacities("MAT", category)).containsExactly(1);
    }

    @Test
    void aVehicleInMaintenanceIsNotTransferred() throws Exception {
        String category = newCategory();
        String plate = register(category, "OPO");
        mockMvc.perform(patch("/vehicles/{plate}/status", plate)
                        .header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_MAINTENANCE\"}"))
                .andExpect(status().isOk());

        start(plate, "MAT", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void aDestinationThatDoesNotExistOrIsTheSameStationIsRejected() throws Exception {
        String category = newCategory();
        String plate = register(category, "OPO");

        start(plate, "BRG", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNKNOWN_STATION"));
        start(plate, "OPO", null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SAME_STATION"));
        assertThat(capacities("OPO", category)).containsExactly(1);
    }

    @Test
    void anUnknownVehicleOrATransferOfAnotherVehicleIsNotFound() throws Exception {
        String category = newCategory();
        String plate = register(category, "OPO");
        String other = register(category, "OPO");
        String id = JsonPath.read(start(plate, "MAT", null)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8), "$.transferId");

        start(LicensePlates.unique(), "MAT", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
        complete(other, id)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSFER_NOT_FOUND"));
        complete(plate, UUID.randomUUID().toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSFER_NOT_FOUND"));
    }

    private String newCategory() {
        String code = AcrissCodes.unique();
        catalogue.createCategory(new AcrissCode(code), "Group K - Van", new DriverRequirements(21, 2), null);
        return code;
    }

    private String register(String category, String station) throws Exception {
        String plate = LicensePlates.unique();
        mockMvc.perform(post("/vehicles")
                        .header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"licensePlate": "%s", "categoryCode": "%s", "station": "%s", "odometerKm": 0}
                                """.formatted(plate, category, station)))
                .andExpect(status().isCreated());
        return plate;
    }

    private ResultActions start(String plate, String destination, String key) throws Exception {
        MockHttpServletRequestBuilder request = post("/vehicles/{plate}/transfers", plate)
                .header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"toStation\": \"%s\"}".formatted(destination));
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mockMvc.perform(request);
    }

    private ResultActions complete(String plate, String id) throws Exception {
        return mockMvc.perform(post("/vehicles/{plate}/transfers/{id}/completion", plate, id)
                .header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER));
    }

    private Map<String, Object> vehicle(String plate) throws Exception {
        String json = mockMvc.perform(get("/vehicles").header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        List<Map<String, Object>> matching = JsonPath.read(json, "$[?(@.licensePlate == '%s')]".formatted(plate));
        assertThat(matching).hasSize(1);
        return matching.getFirst();
    }

    private List<Integer> capacities(String station, String category) {
        return records.capacities(station, category);
    }
}
