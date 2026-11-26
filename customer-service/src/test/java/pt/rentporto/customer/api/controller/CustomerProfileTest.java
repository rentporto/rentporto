package pt.rentporto.customer.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.rentporto.customer.testing.CustomerServiceTest;
import pt.rentporto.customer.testing.TaxNumbers;
import pt.rentporto.messaging.testing.MessagingRecords;
import pt.rentporto.web.testing.TestJwt;

/**
 * A customer creates and updates their profile; agents and administrators read it.
 */
@CustomerServiceTest
class CustomerProfileTest {

    private static final String QUEUE = "notification.customer-registered";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private MessagingRecords messaging;

    @Test
    void theFirstSaveCreatesTheProfileOfTheTokenAndPublishesCustomerRegistered() throws Exception {
        UUID id = UUID.randomUUID();

        save(id, "Ana Ferreira", TaxNumbers.unique(), "2035-06-01", "B")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(id.toString()))
                .andExpect(jsonPath("$.email").value(email(id)))
                .andExpect(jsonPath("$.fullName").value("Ana Ferreira"))
                .andExpect(jsonPath("$.driverLicense.category").value("B"));

        String event = eventFor(id);
        assertThat(JsonPath.<String>read(event, "$.type")).isEqualTo("CustomerRegistered");
        assertThat(JsonPath.<String>read(event, "$.correlationId")).isEqualTo(id.toString());
        assertThat(JsonPath.<String>read(event, "$.payload.fullName")).isEqualTo("Ana Ferreira");
        assertThat(JsonPath.<String>read(event, "$.payload.email")).isEqualTo(email(id));
    }

    @Test
    void anUpdateChangesTheProfileWithoutPublishingAgain() throws Exception {
        UUID id = UUID.randomUUID();
        String taxNumber = TaxNumbers.unique();
        save(id, "Ana Ferreira", taxNumber, "2035-06-01", "B").andExpect(status().isOk());

        save(id, "Ana Sofia Ferreira", taxNumber, "2036-01-15", "BE").andExpect(status().isOk());

        mockMvc.perform(get("/customers/me").header(HttpHeaders.AUTHORIZATION, token(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ana Sofia Ferreira"))
                .andExpect(jsonPath("$.driverLicense.category").value("BE"))
                .andExpect(jsonPath("$.driverLicense.expiryDate").value("2036-01-15"));
        assertThat(eventsInTheOutbox(id)).isEqualTo(1);
    }

    @Test
    void anExpiredLicenceIsRejectedAndNothingIsStored() throws Exception {
        UUID id = UUID.randomUUID();

        save(id, "Ana Ferreira", TaxNumbers.unique(), "2020-01-01", "B")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("DRIVER_LICENSE_EXPIRED"));

        mockMvc.perform(get("/customers/me").header(HttpHeaders.AUTHORIZATION, token(id)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
        assertThat(eventsInTheOutbox(id)).isZero();
    }

    @Test
    void aLicenceThatCannotExistOrAWrongTaxNumberIsRejected() throws Exception {
        save(UUID.randomUUID(), "Ana Ferreira", TaxNumbers.unique(), "2035-06-01", "Z")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_DRIVER_LICENSE"));
        save(UUID.randomUUID(), "Ana Ferreira", "123456780", "2035-06-01", "B")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_TAX_NUMBER"));
    }

    @Test
    void aTaxNumberOfAnotherCustomerIsAConflict() throws Exception {
        String taxNumber = TaxNumbers.unique();
        save(UUID.randomUUID(), "Ana Ferreira", taxNumber, "2035-06-01", "B").andExpect(status().isOk());

        save(UUID.randomUUID(), "Rui Costa", taxNumber, "2035-06-01", "B")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TAX_NUMBER_ALREADY_REGISTERED"));
    }

    @Test
    void agentsAndAdministratorsReadAProfileThatExists() throws Exception {
        UUID id = UUID.randomUUID();
        save(id, "Ana Ferreira", TaxNumbers.unique(), "2035-06-01", "B").andExpect(status().isOk());

        for (String role : new String[] {"AGENT", "ADMIN"}) {
            mockMvc.perform(get("/customers/{id}", id).header(HttpHeaders.AUTHORIZATION, TestJwt.bearer("staff", role)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Ana Ferreira"));
        }
        mockMvc.perform(get("/customers/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, TestJwt.bearer("staff", "AGENT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"));
    }

    private ResultActions save(UUID id, String fullName, String taxNumber, String licenceExpiry, String category)
            throws Exception {
        return mockMvc.perform(put("/customers/me")
                .header(HttpHeaders.AUTHORIZATION, token(id))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "fullName": "%s",
                          "taxNumber": "%s",
                          "birthDate": "1990-05-10",
                          "phone": "+351912345678",
                          "driverLicense": {
                            "number": "P-1234567 8",
                            "category": "%s",
                            "issuingCountry": "PT",
                            "issueDate": "2010-06-01",
                            "expiryDate": "%s"
                          }
                        }
                        """.formatted(fullName, taxNumber, category, licenceExpiry)));
    }

    private String eventFor(UUID id) {
        AtomicReference<String> event = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(15)).until(() -> {
            Message message = rabbit.receive(QUEUE, 500);
            if (message != null) {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                if (id.toString().equals(JsonPath.read(body, "$.payload.customerId"))) {
                    event.set(body);
                }
            }
            return event.get() != null;
        });
        return event.get();
    }

    private long eventsInTheOutbox(UUID id) {
        return messaging.outbox("customer.registered").stream()
                .filter(event -> id.toString().equals(JsonPath.read(event, "$.payload.customerId")))
                .count();
    }

    private static String token(UUID id) {
        return TestJwt.customer(id.toString(), email(id));
    }

    private static String email(UUID id) {
        return "customer-" + id + "@rentporto.pt";
    }
}
