package pt.rentporto.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.rentporto.auth.domain.Account;
import pt.rentporto.auth.domain.Email;

interface SpringDataAccounts extends JpaRepository<Account, UUID> {

    Optional<Account> findByEmail(Email email);
}
