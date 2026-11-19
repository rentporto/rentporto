package pt.rentporto.fleet.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import pt.rentporto.fleet.domain.VehicleAssignment;
import pt.rentporto.fleet.domain.VehicleAssignments;

/**
 * Assignments in memory; remembers the rentals that were locked before their assignment was looked for.
 */
final class InMemoryAssignments implements VehicleAssignments {

    private final Map<UUID, VehicleAssignment> byRental = new HashMap<>();
    final List<UUID> lockedBeforeLookingFor = new ArrayList<>();
    private UUID locked;

    @Override
    public void lock(UUID rentalId) {
        locked = rentalId;
    }

    @Override
    public Optional<VehicleAssignment> forRental(UUID rentalId) {
        if (rentalId.equals(locked)) {
            lockedBeforeLookingFor.add(rentalId);
            locked = null;
        }
        return Optional.ofNullable(byRental.get(rentalId));
    }

    @Override
    public void add(VehicleAssignment assignment) {
        byRental.put(assignment.rentalId(), assignment);
    }

    @Override
    public void update(VehicleAssignment assignment) {
        byRental.put(assignment.rentalId(), assignment);
    }
}
