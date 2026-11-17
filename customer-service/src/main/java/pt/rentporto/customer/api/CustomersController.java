package pt.rentporto.customer.api;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import pt.rentporto.customer.application.CustomerProfiles;
import pt.rentporto.customer.domain.Customer;
import pt.rentporto.customer.domain.CustomerDetails;
import pt.rentporto.customer.domain.TaxNumber;

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
                new pt.rentporto.customer.domain.DriverLicense(
                        license.number(),
                        license.category(),
                        license.issuingCountry(),
                        license.issueDate(),
                        license.expiryDate()));
    }
}
