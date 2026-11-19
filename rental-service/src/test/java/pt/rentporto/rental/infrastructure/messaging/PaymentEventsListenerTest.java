package pt.rentporto.rental.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.Exchanges;
import pt.rentporto.events.Money;
import pt.rentporto.events.RoutingKeys;
import pt.rentporto.events.payment.DepositSettled;
import pt.rentporto.messaging.testing.MessagingRecords;
import pt.rentporto.rental.RentalServiceTest;
import pt.rentporto.rental.TestAgreements;
import pt.rentporto.rental.application.PendingAgreements;
import pt.rentporto.rental.domain.DiscardReason;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalAgreements;
import pt.rentporto.rental.domain.RentalStatus;
import pt.rentporto.web.testing.TestJwt;

/**
 * The payments settle the deposit and the rental-service closes the agreement, once, publishing it for the receipt.
 */
@RentalServiceTest
class PaymentEventsListenerTest {

    private static final String AGENT = TestJwt.bearer("agente", "AGENT");

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAgreements testAgreements;

    @Autowired
    private RentalAgreements agreements;

    @Autowired
    private PendingAgreements pending;

    @Autowired
    private MessagingRecords messaging;

    @Test
    void aSettledReturnClosesTheAgreementWithItsSettlementAndPublishesItForTheReceipt() throws Exception {
        RentalAgreement agreement = testAgreements.returned();

        awaitHandled(publish(agreement.id(), agreement.depositId(), 0, 60000, 0));

        RentalAgreement closed = agreements.withId(agreement.id()).orElseThrow();
        assertThat(closed.status()).isEqualTo(RentalStatus.CLOSED);
        assertThat(closed.settlement().released().cents()).isEqualTo(60000);
        List<String> published = rentalsClosed(agreement);
        assertThat(published).hasSize(1);
        assertThat(JsonPath.<Integer>read(published.getFirst(), "$.payload.finalTotal.amountCents")).isEqualTo(19680);
        assertThat(JsonPath.<Integer>read(published.getFirst(), "$.payload.outstanding.amountCents")).isZero();
        assertThat(JsonPath.<String>read(published.getFirst(), "$.payload.customerId"))
                .isEqualTo(agreement.customerId().toString());
    }

    @Test
    void anAmountStillOwedIsShownOnTheAgreementAndInTheEvent() throws Exception {
        RentalAgreement agreement = testAgreements.returned();

        awaitHandled(publish(agreement.id(), agreement.depositId(), 60000, 0, 2500));

        mockMvc.perform(get("/rentals/{id}", agreement.id()).header(HttpHeaders.AUTHORIZATION, AGENT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.settlement.captured.amountCents").value(60000))
                .andExpect(jsonPath("$.settlement.released.amountCents").value(0))
                .andExpect(jsonPath("$.settlement.outstanding.amountCents").value(2500));
        assertThat(JsonPath.<Integer>read(rentalsClosed(agreement).getFirst(), "$.payload.outstanding.amountCents"))
                .isEqualTo(2500);
    }

    @Test
    void aSecondSettlementOrTheReleaseOfADiscardedAgreementChangesNothing() {
        RentalAgreement closed = testAgreements.returned();
        awaitHandled(publish(closed.id(), closed.depositId(), 0, 60000, 0));
        RentalAgreement waiting = testAgreements.awaitingPickup();
        pending.reservationDiscarded(waiting.reservationId(), DiscardReason.NO_SHOW);

        awaitHandled(publish(closed.id(), closed.depositId(), 1, 1, 1));
        awaitHandled(publish(waiting.id(), UUID.randomUUID(), 0, 60000, 0));

        assertThat(rentalsClosed(closed)).hasSize(1);
        assertThat(agreements.withId(closed.id()).orElseThrow().settlement().released().cents()).isEqualTo(60000);
        assertThat(agreements.withId(waiting.id()).orElseThrow().status()).isEqualTo(RentalStatus.DISCARDED);
        assertThat(rentalsClosed(waiting)).isEmpty();
    }

    @Test
    void aSettlementOfAnotherDepositOrOfARentalStillOutGoesToTheDeadLetterQueue() {
        RentalAgreement returned = testAgreements.returned();
        RentalAgreement active = testAgreements.active();

        UUID otherDeposit = publish(returned.id(), UUID.randomUUID(), 0, 60000, 0);
        UUID stillOut = publish(active.id(), active.depositId(), 0, 60000, 0);

        awaitDeadLetters(otherDeposit, stillOut);
        assertThat(agreements.withId(returned.id()).orElseThrow().status()).isEqualTo(RentalStatus.RETURNED);
        assertThat(agreements.withId(active.id()).orElseThrow().status()).isEqualTo(RentalStatus.ACTIVE);
    }

    private UUID publish(UUID rentalId, UUID depositId, long captured, long released, long outstanding) {
        DepositSettled settled = new DepositSettled(rentalId, depositId, Money.ofCents(captured),
                Money.ofCents(released), Money.ofCents(outstanding));
        EventEnvelope<DepositSettled> envelope = new EventEnvelope<>(UUID.randomUUID(),
                DepositSettled.class.getSimpleName(), 1, Instant.now(), rentalId.toString(), settled);
        rabbit.convertAndSend(Exchanges.EVENTS, RoutingKeys.DEPOSIT_SETTLED, envelope);
        return envelope.eventId();
    }

    private void awaitHandled(UUID event) {
        await().atMost(Duration.ofSeconds(15))
                .until(() -> messaging.handled(PaymentEventsListener.DEPOSIT_SETTLED, event));
    }

    private void awaitDeadLetters(UUID... events) {
        List<String> expected = Arrays.stream(events).map(UUID::toString).toList();
        Set<String> found = new HashSet<>();
        await().atMost(Duration.ofSeconds(25)).until(() -> {
            Message message = rabbit.receive(PaymentEventsListener.DEPOSIT_SETTLED + ".dlq", 500);
            if (message != null) {
                found.add(JsonPath.read(new String(message.getBody(), StandardCharsets.UTF_8), "$.eventId"));
            }
            return found.containsAll(expected);
        });
    }

    private List<String> rentalsClosed(RentalAgreement agreement) {
        return messaging.outbox(RoutingKeys.RENTAL_CLOSED).stream()
                .filter(event -> agreement.id().toString().equals(JsonPath.read(event, "$.payload.rentalId")))
                .toList();
    }
}
