package pt.rentporto.fleet.api;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Vehicle categories and their driver requirements.
 */
@Tag(name = "Categories")
public interface CategoriesApi {

    /**
     * Lists the vehicle categories with their requirements. Public.
     *
     * @return the categories
     */
    @GetMapping("/categories")
    @SecurityRequirements
    List<CategoryResponse> categories();

    /**
     * Creates a vehicle category and publishes {@code VehicleCategoryChanged}. The code must be an ACRISS code and the
     * requirements at least 21 years of age and 2 years of licence. Roles: FLEET_MANAGER.
     *
     * @param idempotencyKey key that makes a repeated request return the same category; optional
     * @param request        category and its requirements
     * @return the created category
     */
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    CategoryResponse createCategory(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CategoryRequest request);

    /**
     * Changes the name and the requirements of a vehicle category; publishes {@code VehicleCategoryChanged} when
     * something changes. Roles: FLEET_MANAGER.
     *
     * @param code    ACRISS code of the category
     * @param request new name and requirements
     * @return the updated category
     */
    @PutMapping("/categories/{code}")
    CategoryResponse updateCategory(
            @PathVariable @Pattern(regexp = "[A-Z]{4}") String code, @Valid @RequestBody CategoryUpdateRequest request);
}
