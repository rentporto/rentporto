package pt.rentporto.fleet.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import pt.rentporto.fleet.application.FleetAssignments;
import pt.rentporto.fleet.application.FleetCatalogue;
import pt.rentporto.fleet.application.FleetTransfers;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.DriverRequirements;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.StatusChange;
import pt.rentporto.fleet.domain.VehicleStatus;
import pt.rentporto.web.testing.TestJwt;

/**
 * A fleet manager registers vehicles, lists them and changes their state; every change to the capacity of a station
 * reaches the booking-service.
 */
@FleetServiceTest
class VehiclesTest {

    private static final String QUEUE = "booking.reservation.station-capacity-changed";
    private static final String FLEET_MANAGER = TestJwt.bearer("frota", "FLEET_MANAGER");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private FleetRecords records;

    @Autowired
    private FleetCatalogue catalogue;

    @Autowired
    private FleetAssignments assignments;

    @Autowired
    private FleetTransfers transfers;

    @Test
    void aRegisteredVehicleIsAvailableWithAFullTankAndRaisesTheCapacityOfItsStation() throws Exception {
        String category = newCategory();
        String first = LicensePlates.unique();
        String second = LicensePlates.unique();

        register(first, category, "OPO", 12000, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value(first))
                .andExpect(jsonPath("$.categoryCode").value(category))
                .andExpect(jsonPath("$.station").value("OPO"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.odometerKm").value(12000))
                .andExpect(jsonPath("$.fuelLevelEighths").value(8));
        register(second, category, "OPO", 500, null).andExpect(status().isCreated());

        assertThat(capacities("OPO", category)).containsExactly(1, 2);
        String event = eventFor(first);
        assertThat(JsonPath.<String>read(event, "$.type")).isEqualTo("StationCapacityChanged");
        assertThat(JsonPath.<String>read(event, "$.payload.stationCode")).isEqualTo("OPO");
        assertThat(JsonPath.<String>read(event, "$.payload.categoryCode")).isEqualTo(category);
        assertThat(JsonPath.<Integer>read(event, "$.payload.capacity")).isEqualTo(1);
    }

    @Test
    void theSameIdempotencyKeyGetsTheSameVehicleAndAnotherRequestIsAConflict() throws Exception {
        String category = newCategory();
        String plate = LicensePlates.unique();
        register(plate, category, "OPO", 12000, "register-" + plate).andExpect(status().isCreated());

        register(plate, category, "OPO", 12000, "register-" + plate)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.licensePlate").value(plate));
        register(plate, category, "CMP", 0, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_ALREADY_REGISTERED"));
        assertThat(capacities("OPO", category)).containsExactly(1);
        assertThat(capacities("CMP", category)).isEmpty();
    }

    @Test
    void aStationOrACategoryThatDoesNotExistIsRejectedAndNothingIsStored() throws Exception {
        String plate = LicensePlates.unique();

        register(plate, newCategory(), "BRG", 0, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNKNOWN_STATION"));
        register(plate, AcrissCodes.unique(), "OPO", 0, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNKNOWN_CATEGORY"));
        mockMvc.perform(get("/vehicles").header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.licensePlate == '%s')]".formatted(plate)).isEmpty());
    }

    @Test
    void thePlatesOfTheThreeFormatsSince1992AreAcceptedAndOthersAreAnInvalidRequest() throws Exception {
        String category = newCategory();

        register(LicensePlates.uniqueFrom2005(), category, "TRD", 98000, null).andExpect(status().isCreated());
        register(LicensePlates.uniqueFrom1992(), category, "TRD", 210000, null).andExpect(status().isCreated());
        register("AA-12-34", category, "TRD", 0, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void theVehiclesAreFilteredByStationAndStateAndSortedByPlate() throws Exception {
        String category = newCategory();
        String atPorto = LicensePlates.unique();
        String inMaintenance = LicensePlates.unique();
        String atCampanha = LicensePlates.unique();
        register(atCampanha, category, "CMP", 0, null).andExpect(status().isCreated());
        register(inMaintenance, category, "OPO", 0, null).andExpect(status().isCreated());
        register(atPorto, category, "OPO", 0, null).andExpect(status().isCreated());
        changeStatus(inMaintenance, "IN_MAINTENANCE", null).andExpect(status().isOk());

        assertThat(plates(list("?station=OPO"), atPorto, inMaintenance, atCampanha))
                .containsExactly(atPorto, inMaintenance);
        assertThat(plates(list("?status=IN_MAINTENANCE"), atPorto, inMaintenance, atCampanha))
                .containsExactly(inMaintenance);
        assertThat(plates(list("?station=OPO&status=AVAILABLE"), atPorto, inMaintenance, atCampanha))
                .containsExactly(atPorto);
        assertThat(plates(list(""), atPorto, inMaintenance, atCampanha))
                .containsExactly(atPorto, inMaintenance, atCampanha);
        assertThat(list("?station=BRG")).isEqualTo("[]");

        List<String> all = JsonPath.read(list(""), "$[*].licensePlate");
        assertThat(all).isSorted();
        List<String> stations = JsonPath.read(list("?station=OPO&status=AVAILABLE"), "$[*].station");
        List<String> states = JsonPath.read(list("?station=OPO&status=AVAILABLE"), "$[*].status");
        assertThat(stations).containsOnly("OPO");
        assertThat(states).containsOnly("AVAILABLE");
    }

    @Test
    void maintenanceTakesTheVehicleOutOfTheCapacityAndBackWithTheReasonInTheHistory() throws Exception {
        String category = newCategory();
        String plate = LicensePlates.unique();
        register(plate, category, "MAT", 0, null).andExpect(status().isCreated());

        changeStatus(plate, "IN_MAINTENANCE", "Brake service")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_MAINTENANCE"));
        changeStatus(plate, "AVAILABLE", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        assertThat(capacities("MAT", category)).containsExactly(1, 0, 1);
        assertThat(records.history(plate))
                .extracting(StatusChange::from, StatusChange::to, StatusChange::reason, StatusChange::changedBy)
                .containsExactly(
                        tuple(null, VehicleStatus.AVAILABLE, null, "frota"),
                        tuple(VehicleStatus.AVAILABLE, VehicleStatus.IN_MAINTENANCE, "Brake service", "frota"),
                        tuple(VehicleStatus.IN_MAINTENANCE, VehicleStatus.AVAILABLE, null, "frota"));
    }

    @Test
    void retiringAVehicleInMaintenanceDoesNotPublishAndARetiredVehicleNeverComesBack() throws Exception {
        String category = newCategory();
        String plate = LicensePlates.unique();
        register(plate, category, "MAT", 0, null).andExpect(status().isCreated());
        changeStatus(plate, "IN_MAINTENANCE", null).andExpect(status().isOk());

        changeStatus(plate, "RETIRED", "Total loss")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETIRED"));
        changeStatus(plate, "AVAILABLE", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        assertThat(capacities("MAT", category)).containsExactly(1, 0);
    }

    @Test
    void askingForTheCurrentStateChangesNothing() throws Exception {
        String category = newCategory();
        String plate = LicensePlates.unique();
        register(plate, category, "OPO", 0, null).andExpect(status().isCreated());

        changeStatus(plate, "AVAILABLE", "Again")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        assertThat(capacities("OPO", category)).containsExactly(1);
        assertThat(records.history(plate)).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RENTED", "IN_TRANSFER"})
    void aRentedVehicleOrOneInTransferOnlyBecomesAvailableThroughItsOwnFlow(String state) throws Exception {
        String category = newCategory();
        String plate = LicensePlates.unique();
        register(plate, category, "OPO", 0, null).andExpect(status().isCreated());
        if (state.equals("RENTED")) {
            assignments.assign(UUID.randomUUID(), new AcrissCode(category), "OPO", "assign-" + plate, "agente");
        } else {
            transfers.start(new LicensePlate(plate), "MAT", null, "frota");
        }
        assertThat(records.vehicle(plate).status()).hasToString(state);

        changeStatus(plate, "AVAILABLE", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
        changeStatus(plate, "IN_MAINTENANCE", null).andExpect(status().isConflict());
    }

    @Test
    void aVehicleThatDoesNotExistIsNotFoundAndAPlateThatIsNotAPlateIsAnInvalidRequest() throws Exception {
        changeStatus(LicensePlates.unique(), "IN_MAINTENANCE", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"));
        changeStatus("AA-12-34", "IN_MAINTENANCE", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private String newCategory() {
        String code = AcrissCodes.unique();
        catalogue.createCategory(new AcrissCode(code), "Group K - Van", new DriverRequirements(21, 2), null);
        return code;
    }

    private ResultActions register(String plate, String category, String station, long odometerKm, String key)
            throws Exception {
        MockHttpServletRequestBuilder request = post("/vehicles")
                .header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"licensePlate": "%s", "categoryCode": "%s", "station": "%s", "odometerKm": %d}
                        """.formatted(plate, category, station, odometerKm));
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mockMvc.perform(request);
    }

    private ResultActions changeStatus(String plate, String state, String reason) throws Exception {
        String body = reason == null
                ? "{\"status\": \"%s\"}".formatted(state)
                : "{\"status\": \"%s\", \"reason\": \"%s\"}".formatted(state, reason);
        return mockMvc.perform(patch("/vehicles/{plate}/status", plate)
                .header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String list(String query) throws Exception {
        return mockMvc.perform(get("/vehicles" + query).header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static List<String> plates(String json, String... ofThisTest) {
        List<String> plates = JsonPath.read(json, "$[*].licensePlate");
        return plates.stream().filter(List.of(ofThisTest)::contains).toList();
    }

    private List<Integer> capacities(String station, String category) {
        return records.capacities(station, category);
    }

    private String eventFor(String plate) {
        AtomicReference<String> event = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(15)).until(() -> {
            Message message = rabbit.receive(QUEUE, 500);
            if (message != null) {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                if (plate.equals(JsonPath.read(body, "$.correlationId"))) {
                    event.set(body);
                }
            }
            return event.get() != null;
        });
        return event.get();
    }
}
