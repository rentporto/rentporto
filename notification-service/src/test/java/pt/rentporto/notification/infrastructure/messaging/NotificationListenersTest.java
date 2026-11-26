package pt.rentporto.notification.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.Money;
import pt.rentporto.events.RentalPeriod;
import pt.rentporto.events.customer.CustomerRegistered;
import pt.rentporto.events.payment.PaymentFailed;
import pt.rentporto.events.payment.PaymentRefunded;
import pt.rentporto.events.rental.RentalClosed;
import pt.rentporto.events.rental.RentalStarted;
import pt.rentporto.events.reservation.ContractTerms;
import pt.rentporto.events.reservation.ReservationCancelled;
import pt.rentporto.events.reservation.ReservationConfirmed;
import pt.rentporto.events.reservation.ReservationExpired;
import pt.rentporto.events.reservation.ReservationNoShow;
import pt.rentporto.events.topology.Exchanges;
import pt.rentporto.events.topology.RoutingKeys;
import pt.rentporto.notification.testing.Mailbox;
import pt.rentporto.notification.testing.NotificationServiceTest;
import pt.rentporto.web.testing.TestJwt;

/**
 * The nine events reach the customer by email, once each, through RabbitMQ and Mailpit.
 */
@NotificationServiceTest
class NotificationListenersTest {

    private static final String ADMIN = TestJwt.bearer("admin", "ADMIN");

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private Mailbox mailbox;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void eachOfTheNineEventsEmailsTheCustomerAndTheAdministratorSeesTheHistory() throws Exception {
        UUID customer = UUID.randomUUID();
        String address = customer + "@test.rentporto.pt";
        UUID reservation = UUID.randomUUID();
        UUID welcome = publish(RoutingKeys.CUSTOMER_REGISTERED,
                new CustomerRegistered(customer, "Ana Ferreira", address));
        awaitEmails(address, 1);

        publish(RoutingKeys.RESERVATION_CONFIRMED, new ReservationConfirmed(reservation, customer, "OPO", "MAT",
                "ECMR", new RentalPeriod(OffsetDateTime.parse("2026-12-01T10:00:00Z"),
                        OffsetDateTime.parse("2026-12-04T10:00:00Z")),
                new ContractTerms(Money.ofCents(4500), List.of(), "MEDIUM", Money.ofCents(100000),
                        Money.ofCents(60000), Money.ofCents(215), Money.ofCents(2500)), Money.ofCents(19680)));
        publish(RoutingKeys.RESERVATION_CANCELLED,
                new ReservationCancelled(reservation, customer, Money.ofCents(0), Money.ofCents(19680)));
        publish(RoutingKeys.RESERVATION_EXPIRED, new ReservationExpired(reservation, customer, "PAYMENT_TIMEOUT"));
        publish(RoutingKeys.RESERVATION_NO_SHOW, new ReservationNoShow(reservation, customer, Money.ofCents(4500)));
        publish(RoutingKeys.PAYMENT_FAILED, new PaymentFailed(UUID.randomUUID(), reservation, customer, "DECLINED"));
        publish(RoutingKeys.PAYMENT_REFUNDED,
                new PaymentRefunded(UUID.randomUUID(), reservation, customer, Money.ofCents(15180)));
        publish(RoutingKeys.RENTAL_STARTED, new RentalStarted(UUID.randomUUID(), reservation, customer, "AA-12-BB",
                OffsetDateTime.parse("2026-12-01T10:05:00Z")));
        publish(RoutingKeys.RENTAL_CLOSED, new RentalClosed(UUID.randomUUID(), customer, List.of(),
                Money.ofCents(19680), Money.ofCents(0)));

        List<Mailbox.Email> emails = awaitEmails(address, 9);
        assertThat(emails).extracting(Mailbox.Email::subject).containsExactlyInAnyOrder(
                "Welcome to RentPorto", "Your reservation is confirmed", "Your reservation was cancelled",
                "Your reservation expired", "You did not pick up your vehicle", "Your payment failed",
                "Your refund is on its way", "Enjoy your trip", "Your receipt");
        assertThat(emails).filteredOn(email -> email.subject().equals("Welcome to RentPorto")).singleElement()
                .satisfies(email -> {
                    assertThat(email.messageId()).isEqualTo(welcome + "@rentporto.pt");
                    assertThat(email.text()).contains("Hello Ana Ferreira");
                });
        mockMvc.perform(get("/notifications").param("customerId", customer.toString())
                        .header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[*].status", everyItem(is("SENT"))))
                .andExpect(jsonPath("$[8].type").value("CustomerRegistered"))
                .andExpect(jsonPath("$[8].subject").value("Welcome to RentPorto"));
    }

    @Test
    void anEventDeliveredTwiceSendsOneEmail() {
        UUID customer = UUID.randomUUID();
        String address = customer + "@test.rentporto.pt";
        publish(RoutingKeys.CUSTOMER_REGISTERED, new CustomerRegistered(customer, "Rui Costa", address));
        EventEnvelope<RentalStarted> started = envelope(new RentalStarted(UUID.randomUUID(), UUID.randomUUID(),
                customer, "CC-34-DD", OffsetDateTime.parse("2026-12-01T10:05:00Z")));

        rabbit.convertAndSend(Exchanges.EVENTS, RoutingKeys.RENTAL_STARTED, started);
        rabbit.convertAndSend(Exchanges.EVENTS, RoutingKeys.RENTAL_STARTED, started);
        publish(RoutingKeys.RENTAL_CLOSED, new RentalClosed(UUID.randomUUID(), customer, List.of(),
                Money.ofCents(19680), Money.ofCents(0)));

        List<Mailbox.Email> emails = awaitEmails(address, 3);
        assertThat(emails).extracting(Mailbox.Email::messageId)
                .filteredOn((started.eventId() + "@rentporto.pt")::equals)
                .hasSize(1);
    }

    @Test
    void anEventOfACustomerTheServiceDoesNotKnowGoesToTheDeadLetterQueue() {
        UUID stranger = UUID.randomUUID();

        UUID event = publish(RoutingKeys.PAYMENT_FAILED,
                new PaymentFailed(UUID.randomUUID(), UUID.randomUUID(), stranger, "DECLINED"));

        AtomicBoolean found = new AtomicBoolean();
        await().atMost(Duration.ofSeconds(20)).until(() -> {
            Message message = rabbit.receive(NotificationListeners.PAYMENT_FAILED + ".dlq", 500);
            if (message != null) {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                found.compareAndSet(false, event.toString().equals(JsonPath.read(body, "$.eventId")));
            }
            return found.get();
        });
    }

    private <T> UUID publish(String routingKey, T payload) {
        EventEnvelope<T> envelope = envelope(payload);
        rabbit.convertAndSend(Exchanges.EVENTS, routingKey, envelope);
        return envelope.eventId();
    }

    private static <T> EventEnvelope<T> envelope(T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), payload.getClass().getSimpleName(), 1, Instant.now(),
                UUID.randomUUID().toString(), payload);
    }

    private List<Mailbox.Email> awaitEmails(String address, int count) {
        await().atMost(Duration.ofSeconds(20)).until(() -> mailbox.to(address).size() >= count);
        return mailbox.to(address);
    }
}
