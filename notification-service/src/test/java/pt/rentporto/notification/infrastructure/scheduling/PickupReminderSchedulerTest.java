package pt.rentporto.notification.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.Money;
import pt.rentporto.events.RentalPeriod;
import pt.rentporto.events.customer.CustomerRegistered;
import pt.rentporto.events.rental.RentalStarted;
import pt.rentporto.events.reservation.ContractTerms;
import pt.rentporto.events.reservation.ReservationCancelled;
import pt.rentporto.events.reservation.ReservationConfirmed;
import pt.rentporto.events.topology.Exchanges;
import pt.rentporto.events.topology.RoutingKeys;
import pt.rentporto.notification.domain.model.PickupReminder;
import pt.rentporto.notification.domain.model.PickupReminderStatus;
import pt.rentporto.notification.domain.repository.PickupReminders;
import pt.rentporto.notification.testing.Mailbox;
import pt.rentporto.notification.testing.NotificationServiceTest;
import pt.rentporto.web.testing.TestJwt;

/**
 * The reminder reaches the customer the configured time (24 hours) before the pick-up, through RabbitMQ, the
 * scheduler and Mailpit, unless the reservation was cancelled or picked up first.
 */
@NotificationServiceTest
class PickupReminderSchedulerTest {

    private static final String ADMIN = TestJwt.bearer("admin", "ADMIN");
    private static final String REMINDER = "Your pick-up is coming up";

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private Mailbox mailbox;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PickupReminders reminders;

    private final UUID customer = UUID.randomUUID();
    private final String address = customer + "@test.rentporto.pt";

    @Test
    void aReservationConfirmedTheDayBeforeIsRemindedOnceThePickUpIsLessThanADayAway() throws Exception {
        register();
        UUID reservation = UUID.randomUUID();

        publish(RoutingKeys.RESERVATION_CONFIRMED, confirmed(reservation, inHours(23)), inHours(-2));

        Mailbox.Email email = awaitEmail(REMINDER);
        PickupReminder reminder = reminders.forReservation(reservation).orElseThrow();
        assertThat(email.messageId()).isEqualTo(reminder.noticeId() + "@rentporto.pt");
        assertThat(email.text()).contains("Pick-up: OPO");
        awaitStatus(reservation, PickupReminderStatus.SENT);
        assertThat(mailbox.to(address)).filteredOn(sent -> sent.subject().equals(REMINDER)).hasSize(1);
        mockMvc.perform(get("/notifications").param("customerId", customer.toString())
                        .header(HttpHeaders.AUTHORIZATION, ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'PickupReminder')].status").value("SENT"));
    }

    @Test
    void aReservationCancelledOrPickedUpBeforeItsReminderIsNotReminded() {
        register();
        UUID cancelled = UUID.randomUUID();
        UUID pickedUp = UUID.randomUUID();
        publish(RoutingKeys.RESERVATION_CONFIRMED, confirmed(cancelled, inHours(30)), Instant.now());
        publish(RoutingKeys.RESERVATION_CONFIRMED, confirmed(pickedUp, inHours(30)), Instant.now());
        awaitStatus(cancelled, PickupReminderStatus.PENDING);
        awaitStatus(pickedUp, PickupReminderStatus.PENDING);

        publish(RoutingKeys.RESERVATION_CANCELLED,
                new ReservationCancelled(cancelled, customer, Money.ofCents(0), Money.ofCents(19680)), Instant.now());
        publish(RoutingKeys.RENTAL_STARTED, new RentalStarted(UUID.randomUUID(), pickedUp, customer, "AA-12-BB",
                OffsetDateTime.now(ZoneOffset.UTC)), Instant.now());

        awaitStatus(cancelled, PickupReminderStatus.CANCELLED);
        awaitStatus(pickedUp, PickupReminderStatus.CANCELLED);
    }

    @Test
    void aCancellationHeardBeforeTheConfirmationCallsOffTheReminder() {
        register();
        UUID reservation = UUID.randomUUID();
        publish(RoutingKeys.RESERVATION_CANCELLED,
                new ReservationCancelled(reservation, customer, Money.ofCents(0), Money.ofCents(19680)), Instant.now());
        awaitStatus(reservation, PickupReminderStatus.CANCELLED);

        publish(RoutingKeys.RESERVATION_CONFIRMED, confirmed(reservation, inHours(23)), inHours(-2));

        awaitEmail("Your reservation is confirmed");
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(5)).until(() ->
                mailbox.to(address).stream().noneMatch(email -> email.subject().equals(REMINDER)));
        assertThat(reminders.forReservation(reservation)).get()
                .extracting(PickupReminder::status).isEqualTo(PickupReminderStatus.CANCELLED);
    }

    @Test
    void aReservationConfirmedWhenLessThanADayIsLeftHasNoReminder() {
        register();
        UUID reservation = UUID.randomUUID();

        publish(RoutingKeys.RESERVATION_CONFIRMED, confirmed(reservation, inHours(23)), Instant.now());

        awaitEmail("Your reservation is confirmed");
        assertThat(reminders.forReservation(reservation)).isEmpty();
    }

    private static Instant inHours(int hours) {
        return Instant.now().plus(Duration.ofHours(hours));
    }

    private void register() {
        publish(RoutingKeys.CUSTOMER_REGISTERED, new CustomerRegistered(customer, "Ana Ferreira", address),
                Instant.now());
        awaitEmail("Welcome to RentPorto");
    }

    private ReservationConfirmed confirmed(UUID reservation, Instant pickupAt) {
        OffsetDateTime pickup = pickupAt.atOffset(ZoneOffset.UTC);
        return new ReservationConfirmed(reservation, customer, "OPO", "OPO", "ECMR",
                new RentalPeriod(pickup, pickup.plusDays(3)),
                new ContractTerms(Money.ofCents(4500), List.of(), "MEDIUM", Money.ofCents(100000),
                        Money.ofCents(60000), Money.ofCents(215), Money.ofCents(2500)), Money.ofCents(19680));
    }

    private <T> void publish(String routingKey, T payload, Instant occurredAt) {
        rabbit.convertAndSend(Exchanges.EVENTS, routingKey, new EventEnvelope<>(UUID.randomUUID(),
                payload.getClass().getSimpleName(), 1, occurredAt, UUID.randomUUID().toString(), payload));
    }

    private Mailbox.Email awaitEmail(String subject) {
        await().atMost(Duration.ofSeconds(20)).until(() ->
                mailbox.to(address).stream().anyMatch(email -> email.subject().equals(subject)));
        return mailbox.to(address).stream().filter(email -> email.subject().equals(subject)).findFirst().orElseThrow();
    }

    private void awaitStatus(UUID reservation, PickupReminderStatus expected) {
        await().atMost(Duration.ofSeconds(15)).until(() -> reminders.forReservation(reservation)
                .map(PickupReminder::status).filter(expected::equals).isPresent());
    }
}
