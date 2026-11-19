package pt.rentporto.rental.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.Exchanges;
import pt.rentporto.events.RoutingKeys;
import pt.rentporto.events.reservation.ReservationCancelled;
import pt.rentporto.events.reservation.ReservationConfirmed;
import pt.rentporto.events.reservation.ReservationNoShow;
import pt.rentporto.messaging.testing.MessagingRecords;
import pt.rentporto.rental.RentalServiceTest;
import pt.rentporto.rental.Reservations;
import pt.rentporto.rental.domain.ConfirmedReservation;
import pt.rentporto.rental.domain.ContractTerms;
import pt.rentporto.rental.domain.Money;
import pt.rentporto.rental.domain.RentalAgreement;
import pt.rentporto.rental.domain.RentalAgreements;
import pt.rentporto.rental.domain.RentalStatus;

/**
 * The reservation module publishes the life of a reservation and the rental-service keeps its agreement in step.
 */
@RentalServiceTest
class ReservationEventsListenerTest {

    @Autowired
    private RabbitTemplate rabbit;

    @Autowired
    private MessagingRecords messaging;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RentalAgreements agreements;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void aConfirmedReservationGetsAnAgreementWithEverythingThePickUpAndTheSettlementNeed() {
        ConfirmedReservation reservation = Reservations.confirmed();

        awaitHandled(ReservationEventsListener.CONFIRMED, publishConfirmed(reservation));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            RentalAgreement agreement = agreements.forReservation(reservation.reservationId()).orElseThrow();
            assertThat(agreement.status()).isEqualTo(RentalStatus.AWAITING_PICKUP);
            assertThat(agreement.customerId()).isEqualTo(reservation.customerId());
            assertThat(agreement.pickupStation()).isEqualTo("OPO");
            assertThat(agreement.returnStation()).isEqualTo("MAT");
            assertThat(agreement.categoryCode()).isEqualTo("ECMR");
            assertThat(agreement.period()).isEqualTo(reservation.period());
            assertThat(agreement.terms()).isEqualTo(reservation.terms());
            assertThat(agreement.total()).isEqualTo(reservation.total());
        });
    }

    @Test
    void aSecondConfirmationOfTheReservationDoesNotCreateAnotherAgreement() {
        ConfirmedReservation reservation = Reservations.confirmed();
        awaitHandled(ReservationEventsListener.CONFIRMED, publishConfirmed(reservation));

        awaitHandled(ReservationEventsListener.CONFIRMED, publishConfirmed(reservation));

        assertThat(entityManager.createQuery(
                        "SELECT count(a) FROM RentalAgreement a WHERE a.reservationId = :reservationId", Long.class)
                .setParameter("reservationId", reservation.reservationId())
                .getSingleResult()).isEqualTo(1);
    }

    @Test
    void aCancellationOrANoShowDiscardsTheAgreementOnceWithItsReason() {
        ConfirmedReservation cancelled = Reservations.confirmed();
        ConfirmedReservation notPickedUp = Reservations.confirmed();
        awaitHandled(ReservationEventsListener.CONFIRMED, publishConfirmed(cancelled));
        awaitHandled(ReservationEventsListener.CONFIRMED, publishConfirmed(notPickedUp));

        awaitHandled(ReservationEventsListener.CANCELLED, publishCancelled(cancelled));
        awaitHandled(ReservationEventsListener.NO_SHOW, publishNoShow(notPickedUp));
        awaitHandled(ReservationEventsListener.NO_SHOW, publishNoShow(cancelled));

        assertThat(statusAndReason(cancelled)).isEqualTo("DISCARDED/CANCELLED");
        assertThat(statusAndReason(notPickedUp)).isEqualTo("DISCARDED/NO_SHOW");
    }

    @Test
    void aCancellationAfterThePickUpGoesToTheDeadLetterQueueAndChangesNothing() {
        ConfirmedReservation reservation = Reservations.confirmed();
        awaitHandled(ReservationEventsListener.CONFIRMED, publishConfirmed(reservation));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> entityManager.createQuery(
                        "UPDATE RentalAgreement a SET a.status = :status WHERE a.reservationId = :reservationId")
                .setParameter("status", RentalStatus.ACTIVE)
                .setParameter("reservationId", reservation.reservationId())
                .executeUpdate());

        UUID event = publishCancelled(reservation);

        awaitDeadLetter(ReservationEventsListener.CANCELLED + ".dlq", event);
        assertThat(statusAndReason(reservation)).isEqualTo("ACTIVE/null");
    }

    @Test
    void aCancellationThatArrivesBeforeTheConfirmationIsAppliedWhenItIsRetried() {
        ConfirmedReservation reservation = Reservations.confirmed();

        UUID cancellation = publishCancelled(reservation);
        publishConfirmed(reservation);

        awaitHandled(ReservationEventsListener.CANCELLED, cancellation);
        assertThat(statusAndReason(reservation)).isEqualTo("DISCARDED/CANCELLED");
    }

    private UUID publishConfirmed(ConfirmedReservation reservation) {
        ContractTerms terms = reservation.terms();
        ReservationConfirmed confirmed = new ReservationConfirmed(
                reservation.reservationId(),
                reservation.customerId(),
                reservation.pickupStation(),
                reservation.returnStation(),
                reservation.categoryCode(),
                new pt.rentporto.events.RentalPeriod(
                        reservation.period().pickupAt().atOffset(ZoneOffset.ofHours(1)),
                        reservation.period().returnAt().atOffset(ZoneOffset.ofHours(1))),
                new pt.rentporto.events.reservation.ContractTerms(
                        money(terms.dailyRate()),
                        terms.extras().stream()
                                .map(extra -> new pt.rentporto.events.reservation.ContractExtra(
                                        extra.code(), money(extra.pricePerDay()), money(extra.cap())))
                                .toList(),
                        terms.coverage(),
                        money(terms.excess()),
                        money(terms.deposit()),
                        money(terms.fuelPricePerLitre()),
                        money(terms.refuellingFee())),
                money(reservation.total()));
        return publish(RoutingKeys.RESERVATION_CONFIRMED, reservation, confirmed);
    }

    private UUID publishCancelled(ConfirmedReservation reservation) {
        return publish(RoutingKeys.RESERVATION_CANCELLED, reservation, new ReservationCancelled(
                reservation.reservationId(), reservation.customerId(),
                pt.rentporto.events.Money.ofCents(0), money(reservation.total())));
    }

    private UUID publishNoShow(ConfirmedReservation reservation) {
        return publish(RoutingKeys.RESERVATION_NO_SHOW, reservation, new ReservationNoShow(
                reservation.reservationId(), reservation.customerId(), pt.rentporto.events.Money.ofCents(4500)));
    }

    private <T> UUID publish(String routingKey, ConfirmedReservation reservation, T payload) {
        EventEnvelope<T> envelope = new EventEnvelope<>(UUID.randomUUID(), payload.getClass().getSimpleName(), 1,
                Instant.now(), reservation.reservationId().toString(), payload);
        rabbit.convertAndSend(Exchanges.EVENTS, routingKey, envelope);
        return envelope.eventId();
    }

    private static pt.rentporto.events.Money money(Money money) {
        return pt.rentporto.events.Money.ofCents(money.cents());
    }

    private void awaitHandled(String queue, UUID event) {
        await().atMost(Duration.ofSeconds(15)).until(() -> messaging.handled(queue, event));
    }

    private void awaitDeadLetter(String queue, UUID event) {
        AtomicBoolean found = new AtomicBoolean();
        await().atMost(Duration.ofSeconds(20)).until(() -> {
            Message message = rabbit.receive(queue, 500);
            if (message != null) {
                String body = new String(message.getBody(), StandardCharsets.UTF_8);
                found.compareAndSet(false, event.toString().equals(JsonPath.read(body, "$.eventId")));
            }
            return found.get();
        });
    }

    private String statusAndReason(ConfirmedReservation reservation) {
        RentalAgreement agreement = agreements.forReservation(reservation.reservationId()).orElseThrow();
        return agreement.status() + "/" + agreement.discardReason();
    }
}
