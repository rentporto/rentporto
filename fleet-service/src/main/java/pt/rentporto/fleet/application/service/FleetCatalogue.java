package pt.rentporto.fleet.application.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.rentporto.fleet.application.port.FleetEvents;
import pt.rentporto.fleet.domain.exception.CategoryAlreadyExistsException;
import pt.rentporto.fleet.domain.exception.CategoryNotFoundException;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.DriverRequirements;
import pt.rentporto.fleet.domain.model.Station;
import pt.rentporto.fleet.domain.model.VehicleCategory;
import pt.rentporto.fleet.domain.repository.Stations;
import pt.rentporto.fleet.domain.repository.VehicleCategories;

/**
 * Stations and vehicle categories. Creating a category, or changing its name or requirements, publishes
 * {@code VehicleCategoryChanged} in the same transaction, so the bookings and the rates always use the definition of
 * the fleet.
 */
@Service
public class FleetCatalogue {

    private final Stations stations;
    private final VehicleCategories categories;
    private final FleetEvents events;
    private final TransactionTemplate transaction;

    FleetCatalogue(
            Stations stations,
            VehicleCategories categories,
            FleetEvents events,
            PlatformTransactionManager transactionManager) {
        this.stations = stations;
        this.categories = categories;
        this.events = events;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Lists the stations.
     *
     * @return every station, by code
     */
    public List<Station> stations() {
        return stations.all();
    }

    /**
     * Lists the vehicle categories.
     *
     * @return every category, by code
     */
    public List<VehicleCategory> categories() {
        return categories.all();
    }

    /**
     * Creates a category and publishes {@code VehicleCategoryChanged}. A request that repeats the idempotency key of
     * the one that created the category gets that category, without publishing again.
     *
     * @param code           ACRISS code
     * @param name           name shown to customers
     * @param requirements   what a driver needs
     * @param idempotencyKey key of the request, or {@code null}
     * @return the category
     * @throws CategoryAlreadyExistsException if another request created a category with the code
     */
    public VehicleCategory createCategory(
            AcrissCode code, String name, DriverRequirements requirements, String idempotencyKey) {
        try {
            return transaction.execute(status -> createOnce(code, name, requirements, idempotencyKey));
        } catch (CategoryAlreadyExistsException e) {
            return transaction.execute(status -> createOnce(code, name, requirements, idempotencyKey));
        }
    }

    /**
     * Changes the name and the requirements of a category. {@code VehicleCategoryChanged} is only published when
     * something changes.
     *
     * @param code         ACRISS code
     * @param name         new name
     * @param requirements new requirements
     * @return the category
     * @throws CategoryNotFoundException if there is no category with the code
     */
    public VehicleCategory changeCategory(AcrissCode code, String name, DriverRequirements requirements) {
        return transaction.execute(status -> {
            VehicleCategory category = categories.withCode(code).orElseThrow(() -> new CategoryNotFoundException(code));
            if (category.change(name, requirements)) {
                categories.update(category);
                events.categoryChanged(category);
            }
            return category;
        });
    }

    private VehicleCategory createOnce(
            AcrissCode code, String name, DriverRequirements requirements, String idempotencyKey) {
        Optional<VehicleCategory> existing = categories.withCode(code);
        if (existing.isPresent()) {
            if (existing.get().createdWith(idempotencyKey)) {
                return existing.get();
            }
            throw new CategoryAlreadyExistsException(code);
        }
        VehicleCategory category = VehicleCategory.create(code, name, requirements, idempotencyKey);
        categories.add(category);
        events.categoryChanged(category);
        return category;
    }
}
