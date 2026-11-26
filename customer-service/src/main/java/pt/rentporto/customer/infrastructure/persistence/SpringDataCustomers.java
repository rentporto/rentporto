package pt.rentporto.customer.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.rentporto.customer.domain.model.Customer;
import pt.rentporto.customer.domain.model.TaxNumber;

interface SpringDataCustomers extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByTaxNumber(TaxNumber taxNumber);
}
