package pt.rentporto.fleet.application;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.CategoryAlreadyExistsException;
import pt.rentporto.fleet.domain.VehicleCategories;
import pt.rentporto.fleet.domain.VehicleCategory;

/**
 * Categories in memory. {@code createdMeanwhile} plays another request, or another replica, that stores a category
 * with the same code just before this one.
 */
final class InMemoryVehicleCategories implements VehicleCategories {

    private final Map<String, VehicleCategory> byCode = new TreeMap<>();
    VehicleCategory createdMeanwhile;

    @Override
    public List<VehicleCategory> all() {
        return byCode.values().stream().sorted(Comparator.comparing(category -> category.code().value())).toList();
    }

    @Override
    public Optional<VehicleCategory> withCode(AcrissCode code) {
        return Optional.ofNullable(byCode.get(code.value()));
    }

    @Override
    public void add(VehicleCategory category) {
        if (createdMeanwhile != null && createdMeanwhile.code().equals(category.code())) {
            byCode.put(createdMeanwhile.code().value(), createdMeanwhile);
            createdMeanwhile = null;
            throw new CategoryAlreadyExistsException(category.code());
        }
        byCode.put(category.code().value(), category);
    }

    @Override
    public void update(VehicleCategory category) {
        byCode.put(category.code().value(), category);
    }
}
