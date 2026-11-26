package pt.rentporto.fleet.domain.repository;

import java.util.List;
import java.util.Optional;
import pt.rentporto.fleet.domain.exception.CategoryAlreadyExistsException;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.VehicleCategory;

/**
 * Stored vehicle categories. Implemented in the infrastructure.
 */
public interface VehicleCategories {

    /**
     * Lists the categories.
     *
     * @return every category, by code
     */
    List<VehicleCategory> all();

    /**
     * Finds a category.
     *
     * @param code ACRISS code
     * @return the category, if there is one
     */
    Optional<VehicleCategory> withCode(AcrissCode code);

    /**
     * Stores a new category.
     *
     * @param category the category
     * @throws CategoryAlreadyExistsException if a category with the same code was stored first
     */
    void add(VehicleCategory category);

    /**
     * Stores the changes to a category.
     *
     * @param category the category
     */
    void update(VehicleCategory category);
}
