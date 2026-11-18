package pt.rentporto.fleet.infrastructure.messaging;

import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pt.rentporto.events.EventEnvelope;
import pt.rentporto.events.RoutingKeys;
import pt.rentporto.events.fleet.StationCapacityChanged;
import pt.rentporto.events.fleet.VehicleCategoryChanged;
import pt.rentporto.fleet.application.FleetEvents;
import pt.rentporto.fleet.domain.DriverRequirements;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.StationCapacity;
import pt.rentporto.fleet.domain.VehicleCategory;
import pt.rentporto.messaging.outbox.OutboxWriter;

/**
 * Writes the events of the fleet context to the outbox, which sends them to RabbitMQ after the commit (ADR-07). The
 * correlation id of a category event is the code of the category, and that of a capacity event the licence plate of
 * the vehicle whose change caused it.
 */
@Component
class OutboxFleetEvents implements FleetEvents {

    private final OutboxWriter outbox;
    private final Clock clock;

    OutboxFleetEvents(OutboxWriter outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    public void categoryChanged(VehicleCategory category) {
        DriverRequirements requirements = category.requirements();
        VehicleCategoryChanged event = new VehicleCategoryChanged(
                category.code().value(),
                category.name(),
                requirements.minimumAge(),
                requirements.minimumLicenseYears());
        outbox.publish(RoutingKeys.VEHICLE_CATEGORY_CHANGED, new EventEnvelope<>(
                UUID.randomUUID(),
                VehicleCategoryChanged.class.getSimpleName(),
                1,
                clock.instant(),
                category.code().value(),
                event));
    }

    @Override
    public void capacityChanged(StationCapacity capacity, LicensePlate vehicle) {
        StationCapacityChanged event = new StationCapacityChanged(
                capacity.station(), capacity.category().value(), capacity.vehicles());
        outbox.publish(RoutingKeys.STATION_CAPACITY_CHANGED, new EventEnvelope<>(
                UUID.randomUUID(),
                StationCapacityChanged.class.getSimpleName(),
                1,
                clock.instant(),
                vehicle.value(),
                event));
    }
}
