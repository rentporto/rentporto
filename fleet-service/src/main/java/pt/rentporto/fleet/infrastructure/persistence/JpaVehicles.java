package pt.rentporto.fleet.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.LicensePlate;
import pt.rentporto.fleet.domain.StationCapacity;
import pt.rentporto.fleet.domain.Vehicle;
import pt.rentporto.fleet.domain.VehicleAlreadyRegisteredException;
import pt.rentporto.fleet.domain.VehicleStatus;
import pt.rentporto.fleet.domain.Vehicles;

/**
 * Vehicles in PostgreSQL. Each change is flushed at once, so that two requests registering the same plate fail here,
 * as a domain exception, and so that the capacity counted afterwards in the same transaction sees the change.
 */
@Repository
class JpaVehicles implements Vehicles {

    static final String PRIMARY_KEY = "vehicles_pkey";

    private static final String LOCK_TIMEOUT = "jakarta.persistence.lock.timeout";
    private static final int SKIP_LOCKED = -2;

    private final EntityManager entityManager;

    JpaVehicles(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Vehicle> withPlate(LicensePlate plate) {
        return Optional.ofNullable(entityManager.find(Vehicle.class, plate));
    }

    @Override
    public Optional<Vehicle> lockWithPlate(LicensePlate plate) {
        return Optional.ofNullable(entityManager.find(Vehicle.class, plate, LockModeType.PESSIMISTIC_WRITE));
    }

    @Override
    public Optional<Vehicle> lockFirstAvailable(String station, AcrissCode category) {
        return entityManager.createQuery("""
                        SELECT v FROM Vehicle v
                        WHERE v.station = :station AND v.category = :category AND v.status = :status
                        ORDER BY v.statusChangedAt, v.plate.value""", Vehicle.class)
                .setParameter("station", station)
                .setParameter("category", category)
                .setParameter("status", VehicleStatus.AVAILABLE)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint(LOCK_TIMEOUT, SKIP_LOCKED)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public List<Vehicle> matching(String station, VehicleStatus status) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Vehicle> query = builder.createQuery(Vehicle.class);
        Root<Vehicle> vehicle = query.from(Vehicle.class);
        List<Predicate> filters = new ArrayList<>();
        if (station != null) {
            filters.add(builder.equal(vehicle.get("station"), station));
        }
        if (status != null) {
            filters.add(builder.equal(vehicle.get("status"), status));
        }
        query.where(filters.toArray(Predicate[]::new)).orderBy(builder.asc(vehicle.get("plate").get("value")));
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public StationCapacity capacity(String station, AcrissCode category) {
        Long vehicles = entityManager.createQuery("""
                        SELECT count(v) FROM Vehicle v
                        WHERE v.station = :station AND v.category = :category AND v.status IN :statuses""", Long.class)
                .setParameter("station", station)
                .setParameter("category", category)
                .setParameter("statuses", VehicleStatus.countedInCapacity())
                .getSingleResult();
        return new StationCapacity(station, category, vehicles.intValue());
    }

    @Override
    public void add(Vehicle vehicle) {
        try {
            entityManager.persist(vehicle);
            entityManager.flush();
        } catch (PersistenceException e) {
            if (PRIMARY_KEY.equals(constraintName(e))) {
                throw new VehicleAlreadyRegisteredException(vehicle.plate());
            }
            throw e;
        }
    }

    @Override
    public void update(Vehicle vehicle) {
        entityManager.flush();
    }

    private static String constraintName(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
