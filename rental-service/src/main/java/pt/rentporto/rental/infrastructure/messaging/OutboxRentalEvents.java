package pt.rentporto.rental.infrastructure.messaging;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pt.rentporto.events.CommandQueues;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.Money;
import pt.rentporto.events.RoutingKeys;
import pt.rentporto.events.payment.SettleDeposit;
import pt.rentporto.events.rental.RentalStarted;
import pt.rentporto.messaging.outbox.OutboxWriter;
import pt.rentporto.rental.application.RentalEvents;
import pt.rentporto.rental.domain.RentalAgreement;

/**
 * Writes the events and commands of the rental context to the outbox, which sends them to RabbitMQ after the commit
 * (ADR-07). The correlation id is the reservation, which follows the whole flow from booking to settlement.
 */
@Component
class OutboxRentalEvents implements RentalEvents {

    private final OutboxWriter outbox;
    private final Clock clock;

    OutboxRentalEvents(OutboxWriter outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    public void rentalStarted(RentalAgreement agreement) {
        RentalStarted event = new RentalStarted(
                agreement.id(),
                agreement.reservationId(),
                agreement.customerId(),
                agreement.licensePlate(),
                agreement.pickedUpAt().atOffset(ZoneOffset.UTC));
        outbox.publish(RoutingKeys.RENTAL_STARTED, envelope(agreement, event));
    }

    @Override
    public void depositReleased(RentalAgreement agreement) {
        SettleDeposit command = new SettleDeposit(agreement.id(), agreement.depositId(), Money.ofCents(0));
        outbox.send(CommandQueues.SETTLE_DEPOSIT, envelope(agreement, command));
    }

    private <T> EventEnvelope<T> envelope(RentalAgreement agreement, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), payload.getClass().getSimpleName(), 1, clock.instant(),
                agreement.reservationId().toString(), payload);
    }
}
