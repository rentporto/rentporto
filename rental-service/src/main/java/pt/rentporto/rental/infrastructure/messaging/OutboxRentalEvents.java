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
import pt.rentporto.events.rental.Damage;
import pt.rentporto.events.rental.ExtraCharge;
import pt.rentporto.events.rental.RentalClosed;
import pt.rentporto.events.rental.RentalStarted;
import pt.rentporto.events.rental.VehicleReturned;
import pt.rentporto.messaging.outbox.OutboxWriter;
import pt.rentporto.rental.application.RentalEvents;
import pt.rentporto.rental.domain.RentalAgreement;

/**
 * Writes the events and commands of the rental context to the outbox, which sends them to RabbitMQ after the commit
 * (ADR-07). The correlation id is the reservation, which follows the whole flow from booking to settlement. A returned
 * vehicle goes back to the fleet with a full tank: it came back full or the agent filled it, and the level found stays
 * in the agreement, for the charge.
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
    public void vehicleReturned(RentalAgreement agreement) {
        VehicleReturned event = new VehicleReturned(
                agreement.id(),
                agreement.reservationId(),
                agreement.licensePlate(),
                agreement.returnStation(),
                agreement.returnOdometerKm(),
                RentalAgreement.FULL_TANK,
                agreement.returnDamages().stream()
                        .map(damage -> new Damage(damage.location(), damage.description()))
                        .toList());
        outbox.publish(RoutingKeys.VEHICLE_RETURNED, envelope(agreement, event));
    }

    @Override
    public void rentalClosed(RentalAgreement agreement) {
        RentalClosed event = new RentalClosed(
                agreement.id(),
                agreement.customerId(),
                agreement.extraCharges().stream()
                        .map(charge -> new ExtraCharge(
                                ExtraCharge.Type.valueOf(charge.type().name()),
                                charge.description(),
                                Money.ofCents(charge.amount().cents())))
                        .toList(),
                Money.ofCents(agreement.finalTotal().cents()),
                Money.ofCents(agreement.settlement().outstanding().cents()));
        outbox.publish(RoutingKeys.RENTAL_CLOSED, envelope(agreement, event));
    }

    @Override
    public void settleDeposit(RentalAgreement agreement, pt.rentporto.rental.domain.Money amountDue) {
        SettleDeposit command =
                new SettleDeposit(agreement.id(), agreement.depositId(), Money.ofCents(amountDue.cents()));
        outbox.send(CommandQueues.SETTLE_DEPOSIT, envelope(agreement, command));
    }

    private <T> EventEnvelope<T> envelope(RentalAgreement agreement, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), payload.getClass().getSimpleName(), 1, clock.instant(),
                agreement.reservationId().toString(), payload);
    }
}
