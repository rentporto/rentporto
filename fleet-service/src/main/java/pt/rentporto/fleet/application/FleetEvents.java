package pt.rentporto.fleet.application;

import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.StationCapacity;
import pt.rentporto.fleet.domain.VehicleCategory;

/**
 * Events of the fleet context for the other services. Implemented in the infrastructure.
 */
public interface FleetEvents {

    /**
     * Publishes {@code VehicleCategoryChanged} with the current name and requirements of a category, in the
     * transaction of the change.
     *
     * @param category the category that was created or changed
     */
    void categoryChanged(VehicleCategory category);

    /**
     * Publishes {@code StationCapacityChanged} with the new capacity of a station for a category, in the transaction
     * of the change.
     *
     * @param capacity the new capacity
     * @param vehicle  licence plate of the vehicle whose change caused it
     */
    void capacityChanged(StationCapacity capacity, LicensePlate vehicle);
}
