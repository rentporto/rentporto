package pt.rentporto.rental.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.reservation.ReservationCancelled;
import pt.rentporto.events.reservation.ReservationConfirmed;
import pt.rentporto.events.reservation.ReservationNoShow;
import pt.rentporto.messaging.consumer.IdempotentConsumer;
import pt.rentporto.rental.application.PendingAgreements;
import pt.rentporto.rental.domain.ConfirmedReservation;
import pt.rentporto.rental.domain.ContractExtra;
import pt.rentporto.rental.domain.ContractTerms;
import pt.rentporto.rental.domain.DiscardReason;
import pt.rentporto.rental.domain.Money;
import pt.rentporto.rental.domain.RentalPeriod;

/**
 * Consumes the events of the reservation module of the booking-service and translates them into the rental domain.
 * Each event is applied once, in the transaction that records it as handled (ADR-07); one that cannot be applied
 * fails its three attempts and goes to the dead letter queue of its queue.
 */
@Component
class ReservationEventsListener {

    static final String CONFIRMED = "rental.reservation-confirmed";
    static final String CANCELLED = "rental.reservation-cancelled";
    static final String NO_SHOW = "rental.reservation-no-show";

    private final IdempotentConsumer consumer;
    private final PendingAgreements agreements;

    ReservationEventsListener(IdempotentConsumer consumer, PendingAgreements agreements) {
        this.consumer = consumer;
        this.agreements = agreements;
    }

    @RabbitListener(queues = CONFIRMED)
    void onConfirmed(EventEnvelope<ReservationConfirmed> message) {
        consumer.process(CONFIRMED, message, confirmed -> agreements.reservationConfirmed(reservation(confirmed)));
    }

    @RabbitListener(queues = CANCELLED)
    void onCancelled(EventEnvelope<ReservationCancelled> message) {
        consumer.process(CANCELLED, message, cancelled ->
                agreements.reservationDiscarded(cancelled.reservationId(), DiscardReason.CANCELLED));
    }

    @RabbitListener(queues = NO_SHOW)
    void onNoShow(EventEnvelope<ReservationNoShow> message) {
        consumer.process(NO_SHOW, message, noShow ->
                agreements.reservationDiscarded(noShow.reservationId(), DiscardReason.NO_SHOW));
    }

    private static ConfirmedReservation reservation(ReservationConfirmed confirmed) {
        pt.rentporto.events.reservation.ContractTerms terms = confirmed.contractTerms();
        return new ConfirmedReservation(
                confirmed.reservationId(),
                confirmed.customerId(),
                confirmed.pickupStation(),
                confirmed.returnStation(),
                confirmed.categoryCode(),
                new RentalPeriod(
                        confirmed.rentalPeriod().pickupAt().toInstant(),
                        confirmed.rentalPeriod().returnAt().toInstant()),
                new ContractTerms(
                        money(terms.dailyRate()),
                        terms.extras().stream()
                                .map(extra -> new ContractExtra(
                                        extra.code(), money(extra.pricePerDay()), money(extra.cap())))
                                .toList(),
                        terms.coverage(),
                        money(terms.excess()),
                        money(terms.deposit()),
                        money(terms.fuelPricePerLitre()),
                        money(terms.refuellingFee())),
                money(confirmed.total()));
    }

    private static Money money(pt.rentporto.events.Money money) {
        return Money.ofCents(money.amountCents());
    }
}
