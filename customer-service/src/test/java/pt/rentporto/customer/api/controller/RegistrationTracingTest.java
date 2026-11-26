package pt.rentporto.customer.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.customer.testing.CustomerServiceTest;
import pt.rentporto.customer.testing.TaxNumbers;
import pt.rentporto.web.testing.TestJwt;

/**
 * The {@code CustomerRegistered} of a registration carries the trace of the request, through the outbox, to the
 * notification-service.
 */
@CustomerServiceTest
class RegistrationTracingTest {

    private static final String QUEUE = "notification.customer-registered";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitTemplate rabbit;

    @Test
    void theCustomerRegisteredOfARegistrationCarriesTheTraceIdOfTheRequest() throws Exception {
        UUID id = UUID.randomUUID();
        String traceId = HexFormat.of().formatHex(randomBytes(16));

        mockMvc.perform(put("/customers/me")
                        .header(HttpHeaders.AUTHORIZATION, TestJwt.customer(id.toString(), id + "@rentporto.pt"))
                        .header("traceparent", "00-" + traceId + "-" + HexFormat.of().formatHex(randomBytes(8)) + "-01")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Ana Ferreira",
                                  "taxNumber": "%s",
                                  "birthDate": "1990-05-10",
                                  "phone": "+351912345678",
                                  "driverLicense": {
                                    "number": "P-1234567 8",
                                    "category": "B",
                                    "issuingCountry": "PT",
                                    "issueDate": "2010-06-01",
                                    "expiryDate": "2035-06-01"
                                  }
                                }
                                """.formatted(TaxNumbers.unique())))
                .andExpect(status().isOk());

        assertThat(registeredMessage(id).getMessageProperties().<String>getHeader("traceparent"))
                .startsWith("00-" + traceId + "-");
    }

    private Message registeredMessage(UUID id) {
        AtomicReference<Message> registered = new AtomicReference<>();
        await().atMost(Duration.ofSeconds(15)).until(() -> {
            Message message = rabbit.receive(QUEUE, 500);
            if (message != null && id.toString().equals(
                    JsonPath.read(new String(message.getBody(), StandardCharsets.UTF_8), "$.payload.customerId"))) {
                registered.set(message);
            }
            return registered.get() != null;
        });
        return registered.get();
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }
}
