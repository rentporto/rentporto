package pt.rentporto.fleet.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.rental.VehicleReturned;
import pt.rentporto.fleet.application.service.FleetReturns;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.messaging.consumer.IdempotentConsumer;

/**
 * Consumes {@code VehicleReturned} from the rental-service. Each event is applied once, in the transaction that records
 * it as handled (ADR-07); one that cannot be applied fails its three attempts and goes to the dead letter queue.
 */
@Component
class VehicleReturnedListener {

    static final String QUEUE = "fleet.vehicle-returned";

    private final IdempotentConsumer consumer;
    private final FleetReturns returns;

    VehicleReturnedListener(IdempotentConsumer consumer, FleetReturns returns) {
        this.consumer = consumer;
        this.returns = returns;
    }

    @RabbitListener(queues = QUEUE)
    void on(EventEnvelope<VehicleReturned> message) {
        consumer.process(QUEUE, message, returned -> returns.vehicleReturned(
                returned.rentalId(),
                new LicensePlate(returned.licensePlate()),
                returned.returnStation(),
                returned.odometerKm(),
                returned.fuelLevelEighths()));
    }
}
