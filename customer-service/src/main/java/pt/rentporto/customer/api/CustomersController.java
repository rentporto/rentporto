package pt.rentporto.customer.api;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class CustomersController implements CustomersApi {

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse saveMyProfile(CustomerRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse myProfile() {
        throw new EndpointNotImplementedException();
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public CustomerResponse customer(UUID id) {
        throw new EndpointNotImplementedException();
    }
}
