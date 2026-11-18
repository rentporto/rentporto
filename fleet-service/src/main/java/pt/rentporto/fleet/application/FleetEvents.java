package pt.rentporto.fleet.application;

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
}
