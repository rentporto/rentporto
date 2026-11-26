package pt.rentporto.fleet.application.service;

import java.util.ArrayList;
import java.util.List;
import pt.rentporto.fleet.application.port.FleetEvents;
import pt.rentporto.fleet.domain.model.LicensePlate;
import pt.rentporto.fleet.domain.model.StationCapacity;
import pt.rentporto.fleet.domain.model.VehicleCategory;

/**
 * Keeps the events the application publishes, in order.
 */
final class RecordedEvents implements FleetEvents {

    final List<VehicleCategory> categories = new ArrayList<>();
    final List<StationCapacity> capacities = new ArrayList<>();

    @Override
    public void categoryChanged(VehicleCategory category) {
        categories.add(category);
    }

    @Override
    public void capacityChanged(StationCapacity capacity, LicensePlate vehicle) {
        capacities.add(capacity);
    }
}
