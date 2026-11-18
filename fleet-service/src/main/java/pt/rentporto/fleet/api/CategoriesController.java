package pt.rentporto.fleet.api;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.fleet.application.FleetCatalogue;
import pt.rentporto.fleet.domain.AcrissCode;
import pt.rentporto.fleet.domain.DriverRequirements;

@RestController
class CategoriesController implements CategoriesApi {

    private final FleetCatalogue catalogue;

    CategoriesController(FleetCatalogue catalogue) {
        this.catalogue = catalogue;
    }

    @Override
    @PreAuthorize("permitAll()")
    public List<CategoryResponse> categories() {
        return catalogue.categories().stream().map(CategoryResponse::from).toList();
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public CategoryResponse createCategory(String idempotencyKey, CategoryRequest request) {
        return CategoryResponse.from(catalogue.createCategory(
                new AcrissCode(request.code()),
                request.name(),
                new DriverRequirements(request.minimumAge(), request.minimumLicenseYears()),
                idempotencyKey));
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public CategoryResponse updateCategory(String code, CategoryUpdateRequest request) {
        return CategoryResponse.from(catalogue.changeCategory(
                new AcrissCode(code),
                request.name(),
                new DriverRequirements(request.minimumAge(), request.minimumLicenseYears())));
    }
}
