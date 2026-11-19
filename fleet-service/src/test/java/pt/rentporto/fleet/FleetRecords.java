package pt.rentporto.fleet;

import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import pt.rentporto.events.RoutingKeys;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.StatusChange;
import pt.rentporto.fleet.domain.Vehicle;
import pt.rentporto.fleet.domain.VehicleAssignment;
import pt.rentporto.fleet.domain.VehicleStatus;
import pt.rentporto.messaging.testing.MessagingRecords;

/**
 * Reads, through JPA, what the tests of the fleet check: the vehicles, their history, the assignments, the transfers
 * and the events written to the outbox.
 */
public class FleetRecords {

    private final EntityManager entityManager;
    private final MessagingRecords messaging;

    /**
     * Creates the reader.
     *
     * @param entityManager shared entity manager of the service
     * @param messaging     reader of the outbox
     */
    public FleetRecords(EntityManager entityManager, MessagingRecords messaging) {
        this.entityManager = entityManager;
        this.messaging = messaging;
    }

    /**
     * The capacities published for a category at a station.
     *
     * @param station  code of the station
     * @param category ACRISS code of the category
     * @return the capacities, in the order they were published
     */
    public List<Integer> capacities(String station, String category) {
        return capacityEvents(category).stream()
                .filter(event -> station.equals(JsonPath.read(event, "$.payload.stationCode")))
                .map(event -> JsonPath.<Integer>read(event, "$.payload.capacity"))
                .toList();
    }

    /**
     * The capacities published for a category at every station.
     *
     * @param category ACRISS code of the category
     * @return each capacity as {@code STATION=capacity}, in the order they were published
     */
    public List<String> capacities(String category) {
        return capacityEvents(category).stream()
                .map(event -> JsonPath.read(event, "$.payload.stationCode") + "="
                        + JsonPath.read(event, "$.payload.capacity"))
                .toList();
    }

    /**
     * The {@code VehicleCategoryChanged} events of a category.
     *
     * @param code ACRISS code of the category
     * @return each envelope as JSON, in the order they were published
     */
    public List<String> categoryEvents(String code) {
        return messaging.outbox(RoutingKeys.VEHICLE_CATEGORY_CHANGED).stream()
                .filter(event -> code.equals(JsonPath.read(event, "$.correlationId")))
                .toList();
    }

    /**
     * Finds a vehicle.
     *
     * @param plate licence plate
     * @return the vehicle
     */
    public Vehicle vehicle(String plate) {
        return entityManager.find(Vehicle.class, new LicensePlate(plate));
    }

    /**
     * The history of a vehicle.
     *
     * @param plate licence plate
     * @return its entries, in order
     */
    public List<StatusChange> history(String plate) {
        return entityManager.createQuery(
                        "SELECT c FROM StatusChange c WHERE c.plate = :plate ORDER BY c.id", StatusChange.class)
                .setParameter("plate", new LicensePlate(plate))
                .getResultList();
    }

    /**
     * Counts the vehicles of a category in a state.
     *
     * @param category ACRISS code of the category
     * @param status   the state
     * @return how many there are
     */
    public long vehicles(String category, VehicleStatus status) {
        return entityManager.createQuery(
                        "SELECT count(v) FROM Vehicle v WHERE v.category = :category AND v.status = :status",
                        Long.class)
                .setParameter("category", new AcrissCode(category))
                .setParameter("status", status)
                .getSingleResult();
    }

    /**
     * Counts the transfers of a vehicle.
     *
     * @param plate licence plate
     * @return how many there are
     */
    public long transfers(String plate) {
        return entityManager.createQuery("SELECT count(t) FROM Transfer t WHERE t.plate = :plate", Long.class)
                .setParameter("plate", new LicensePlate(plate))
                .getSingleResult();
    }

    /**
     * Finds the assignment of a rental.
     *
     * @param rentalId the rental
     * @return the assignment
     */
    public VehicleAssignment assignment(UUID rentalId) {
        return entityManager.find(VehicleAssignment.class, rentalId);
    }

    private List<String> capacityEvents(String category) {
        return messaging.outbox(RoutingKeys.STATION_CAPACITY_CHANGED).stream()
                .filter(event -> category.equals(JsonPath.read(event, "$.payload.categoryCode")))
                .toList();
    }
}
