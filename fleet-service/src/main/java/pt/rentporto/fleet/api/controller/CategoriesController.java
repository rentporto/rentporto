package pt.rentporto.fleet.api.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.fleet.api.contract.CategoriesApi;
import pt.rentporto.fleet.api.request.CategoryRequest;
import pt.rentporto.fleet.api.request.CategoryUpdateRequest;
import pt.rentporto.fleet.api.response.CategoryResponse;
import pt.rentporto.fleet.application.service.FleetCatalogue;
import pt.rentporto.fleet.domain.model.AcrissCode;
import pt.rentporto.fleet.domain.model.DriverRequirements;

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
