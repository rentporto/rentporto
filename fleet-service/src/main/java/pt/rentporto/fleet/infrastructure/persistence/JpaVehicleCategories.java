package pt.rentporto.fleet.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import pt.rentporto.fleet.domain.exception.CategoryAlreadyExistsException;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.VehicleCategory;
import pt.rentporto.fleet.domain.repository.VehicleCategories;

/**
 * Vehicle categories in PostgreSQL. Each change is flushed at once, so that two requests creating the same code at
 * the same time fail here, as a domain exception, and so that the row is locked before the event joins the outbox.
 */
@Repository
class JpaVehicleCategories implements VehicleCategories {

    static final String PRIMARY_KEY = "vehicle_categories_pkey";

    private final SpringDataVehicleCategories jpa;
    private final EntityManager entityManager;

    JpaVehicleCategories(SpringDataVehicleCategories jpa, EntityManager entityManager) {
        this.jpa = jpa;
        this.entityManager = entityManager;
    }

    @Override
    public List<VehicleCategory> all() {
        return jpa.findAll(Sort.by("code.value"));
    }

    @Override
    public Optional<VehicleCategory> withCode(AcrissCode code) {
        return jpa.findById(code);
    }

    @Override
    public void add(VehicleCategory category) {
        try {
            entityManager.persist(category);
            entityManager.flush();
        } catch (PersistenceException e) {
            if (PRIMARY_KEY.equals(constraintName(e))) {
                throw new CategoryAlreadyExistsException(category.code());
            }
            throw e;
        }
    }

    @Override
    public void update(VehicleCategory category) {
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
