package pt.rentporto.fleet.api;

import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class CategoriesController implements CategoriesApi {

    @Override
    public List<CategoryResponse> categories() {
        throw new EndpointNotImplementedException();
    }

    @Override
    public CategoryResponse createCategory(String idempotencyKey, CategoryRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public CategoryResponse updateCategory(String code, CategoryRequest request) {
        throw new EndpointNotImplementedException();
    }
}
