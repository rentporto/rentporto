package pt.rentporto.fleet.application;

import java.util.ArrayList;
import java.util.List;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.StationCapacity;
import pt.rentporto.fleet.domain.VehicleCategory;

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
