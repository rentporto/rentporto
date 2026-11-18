package pt.rentporto.fleet.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.rentporto.fleet.AcrissCodes;
import pt.rentporto.fleet.FleetServiceTest;
import pt.rentporto.web.testing.TestJwt;

/**
 * A fleet manager creates and changes categories, each change reaches the booking-service, and anyone reads them.
 */
@FleetServiceTest
class CategoriesTest {

    private static final String QUEUE = "booking.pricing.vehicle-category-changed";
    private static final String FLEET_MANAGER = TestJwt.bearer("frota", "FLEET_MANAGER");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void anyoneReadsTheCategoriesWithTheirRequirementsWithoutLoggingIn() throws Exception {
        String code = AcrissCodes.unique();
        create(code, "Group K - Van", 23, 3, null).andExpect(status().isCreated());

        String category = "$[?(@.code == '" + code + "')]";
        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(category + ".name").value("Group K - Van"))
                .andExpect(jsonPath(category + ".minimumAge").value(23))
                .andExpect(jsonPath(category + ".minimumLicenseYears").value(3));
    }

    @Test
    void aNewCategoryReachesTheBookingServiceWithItsRequirements() throws Exception {
        String code = AcrissCodes.unique();

        create(code, "Group K - Van", 23, 3, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.name").value("Group K - Van"))
                .andExpect(jsonPath("$.minimumAge").value(23))
                .andExpect(jsonPath("$.minimumLicenseYears").value(3));

        String event = eventFor(code);
        assertThat(JsonPath.<String>read(event, "$.type")).isEqualTo("VehicleCategoryChanged");
        assertThat(JsonPath.<String>read(event, "$.payload.categoryCode")).isEqualTo(code);
        assertThat(JsonPath.<String>read(event, "$.payload.name")).isEqualTo("Group K - Van");
        assertThat(JsonPath.<Integer>read(event, "$.payload.minimumAge")).isEqualTo(23);
        assertThat(JsonPath.<Integer>read(event, "$.payload.minimumLicenseYears")).isEqualTo(3);
    }

    @Test
    void theSameIdempotencyKeyGetsTheSameCategoryAndAnotherRequestIsAConflict() throws Exception {
        String code = AcrissCodes.unique();
        String key = "create-" + code;
        create(code, "Group K - Van", 23, 3, key).andExpect(status().isCreated());

        create(code, "Group K - Van", 23, 3, key)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code));
        create(code, "Another van", 21, 2, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_ALREADY_EXISTS"));
        assertThat(eventsInTheOutbox(code)).isEqualTo(1);
    }

    @Test
    void aChangePublishesTheCurrentRequirementsAndAChangeWithoutDifferencesPublishesNothing() throws Exception {
        String code = AcrissCodes.unique();
        create(code, "Group K - Van", 21, 2, null).andExpect(status().isCreated());

        change(code, "Group K - Large van", 25, 4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.name").value("Group K - Large van"))
                .andExpect(jsonPath("$.minimumAge").value(25))
                .andExpect(jsonPath("$.minimumLicenseYears").value(4));
        change(code, "Group K - Large van", 25, 4).andExpect(status().isOk());

        assertThat(eventsInTheOutbox(code)).isEqualTo(2);
        String last = jdbc.queryForObject(
                "SELECT payload::text FROM outbox WHERE routing_key = 'fleet.vehicle-category-changed'"
                        + " AND payload ->> 'correlationId' = ? ORDER BY id DESC LIMIT 1",
                String.class, code);
        assertThat(JsonPath.<String>read(last, "$.payload.name")).isEqualTo("Group K - Large van");
        assertThat(JsonPath.<Integer>read(last, "$.payload.minimumAge")).isEqualTo(25);
        assertThat(JsonPath.<Integer>read(last, "$.payload.minimumLicenseYears")).isEqualTo(4);
    }

    @Test
    void changingACategoryThatDoesNotExistIsNotFound() throws Exception {
        String code = AcrissCodes.unique();

        change(code, "Group K - Van", 21, 2)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));
        assertThat(eventsInTheOutbox(code)).isZero();
    }

    @Test
    void aCodeOutsideTheAcrissTableOrRequirementsBelowTheMinimumOfTheCompanyAreRejected() throws Exception {
        create("EAMR", "Group B - Economy", 21, 2, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_CATEGORY_CODE"));

        String code = AcrissCodes.unique();
        create(code, "Group K - Van", 18, 2, null)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_DRIVER_REQUIREMENTS"));
        assertThat(eventsInTheOutbox(code)).isZero();

        create(code, "Group K - Van", 21, 2, null).andExpect(status().isCreated());
        change(code, "Group K - Van", 21, 1)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_DRIVER_REQUIREMENTS"));
        assertThat(eventsInTheOutbox(code)).isEqualTo(1);
    }

    @Test
    void aCodeThatIsNotFourCapitalLettersIsAnInvalidRequest() throws Exception {
        create("ecmr", "Group B - Economy", 21, 2, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        change("ECM", "Group B - Economy", 21, 2)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    private ResultActions create(String code, String name, int minimumAge, int minimumLicenseYears, String key)
            throws Exception {
        var request = post("/categories")
                .header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code": "%s", "name": "%s", "minimumAge": %d, "minimumLicenseYears": %d}
                        """.formatted(code, name, minimumAge, minimumLicenseYears));
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mockMvc.perform(request);
    }

    private ResultActions change(String code, String name, int minimumAge, int minimumLicenseYears)
            throws Exception {
        return mockMvc.perform(put("/categories/{code}", code)
                .header(HttpHeaders.AUTHORIZATION, FLEET_MANAGER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "%s", "minimumAge": %d, "minimumLicenseYears": %d}
                        """.formatted(name, minimumAge, minimumLicenseYears)));
    }

    private String eventFor(String code) {
        AtomicReference<String> event = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(15)).until(() -> {
            Message message = rabbit.receive(QUEUE, 500);
            if (message != null) {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                if (code.equals(JsonPath.read(body, "$.correlationId"))) {
                    event.set(body);
                }
            }
            return event.get() != null;
        });
        return event.get();
    }

    private int eventsInTheOutbox(String code) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM outbox WHERE routing_key = 'fleet.vehicle-category-changed'"
                        + " AND payload ->> 'correlationId' = ?",
                Integer.class, code);
    }
}
