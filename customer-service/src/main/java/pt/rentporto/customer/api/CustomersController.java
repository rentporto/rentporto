package pt.rentporto.customer.api;

import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.web.EndpointNotImplementedException;

@RestController
class CustomersController implements CustomersApi {

    @Override
    public CustomerResponse saveMyProfile(CustomerRequest request) {
        throw new EndpointNotImplementedException();
    }

    @Override
    public CustomerResponse myProfile() {
        throw new EndpointNotImplementedException();
    }

    @Override
    public CustomerResponse customer(UUID id) {
        throw new EndpointNotImplementedException();
    }
}
