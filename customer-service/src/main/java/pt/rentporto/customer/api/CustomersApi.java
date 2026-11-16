package pt.rentporto.customer.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Customer profiles.
 */
@Tag(name = "Customers")
public interface CustomersApi {

    /**
     * Creates or updates the profile of the authenticated customer; the first call creates it and publishes
     * {@code CustomerRegistered}. Roles: CUSTOMER.
     *
     * @param request profile and driving licence
     * @return the stored profile
     */
    @PutMapping("/customers/me")
    CustomerResponse saveMyProfile(@Valid @RequestBody CustomerRequest request);

    /**
     * Reads the profile of the authenticated customer. Roles: CUSTOMER.
     *
     * @return the profile
     */
    @GetMapping("/customers/me")
    CustomerResponse myProfile();

    /**
     * Reads the profile of a customer. Roles: AGENT, ADMIN.
     *
     * @param id customer identifier
     * @return the profile
     */
    @GetMapping("/customers/{id}")
    CustomerResponse customer(@PathVariable UUID id);
}
