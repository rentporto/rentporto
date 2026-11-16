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
     * Creates a vehicle category and publishes {@code VehicleCategoryChanged}. Roles: FLEET_MANAGER.
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
     * Updates a vehicle category and publishes {@code VehicleCategoryChanged}. Roles: FLEET_MANAGER.
     *
     * @param code    ACRISS code of the category
     * @param request category and its requirements
     * @return the updated category
     */
    @PutMapping("/categories/{code}")
    CategoryResponse updateCategory(
            @PathVariable @Pattern(regexp = "[A-Z]{4}") String code, @Valid @RequestBody CategoryRequest request);
}
