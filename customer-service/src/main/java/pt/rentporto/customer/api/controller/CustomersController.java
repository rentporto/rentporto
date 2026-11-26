package pt.rentporto.customer.api.controller;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.customer.api.contract.CustomersApi;
import pt.rentporto.customer.api.model.DriverLicense;
import pt.rentporto.customer.api.request.CustomerRequest;
import pt.rentporto.customer.api.response.CustomerResponse;
import pt.rentporto.customer.application.service.CustomerProfiles;
import pt.rentporto.customer.domain.model.Customer;
import pt.rentporto.customer.domain.model.CustomerDetails;
import pt.rentporto.customer.domain.model.TaxNumber;
import pt.rentporto.web.security.AuthenticatedUser;

@RestController
class CustomersController implements CustomersApi {

    private final CustomerProfiles profiles;

    CustomersController(CustomerProfiles profiles) {
        this.profiles = profiles;
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse saveMyProfile(CustomerRequest request) {
        Customer customer = profiles.save(AuthenticatedUser.id(), AuthenticatedUser.email(), details(request));
        return CustomerResponse.from(customer);
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerResponse myProfile() {
        return CustomerResponse.from(profiles.profile(AuthenticatedUser.id()));
    }

    @Override
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public CustomerResponse customer(UUID id) {
        return CustomerResponse.from(profiles.profile(id));
    }

    private static CustomerDetails details(CustomerRequest request) {
        DriverLicense license = request.driverLicense();
        return new CustomerDetails(
                request.fullName(),
                new TaxNumber(request.taxNumber()),
                request.birthDate(),
                request.phone(),
                new pt.rentporto.customer.domain.model.DriverLicense(
                        license.number(),
                        license.category(),
                        license.issuingCountry(),
                        license.issueDate(),
                        license.expiryDate()));
    }
}
