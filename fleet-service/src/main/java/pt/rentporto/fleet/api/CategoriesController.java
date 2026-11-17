package pt.rentporto.fleet.api;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class CategoriesController implements CategoriesApi {

    @Override
    @PreAuthorize("permitAll()")
    public List<CategoryResponse> categories() {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public CategoryResponse createCategory(String idempotencyKey, CategoryRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('FLEET_MANAGER')")
    public CategoryResponse updateCategory(String code, CategoryRequest request) {
        throw new EndpointNotImplementedException();
    }
}
