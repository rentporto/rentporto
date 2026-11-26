package pt.rentporto.auth.infrastructure.persistence;

import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import pt.rentporto.auth.domain.exception.EmailAlreadyRegisteredException;
import pt.rentporto.auth.domain.model.Account;
import pt.rentporto.auth.domain.model.Email;
import pt.rentporto.auth.domain.repository.Accounts;

/**
 * Accounts stored in PostgreSQL with Spring Data JPA.
 */
@Repository
class JpaAccounts implements Accounts {

    private final SpringDataAccounts jpa;

    JpaAccounts(SpringDataAccounts jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Account> withEmail(Email email) {
        return jpa.findByEmail(email);
    }

    /**
     * Stores the account at once, so that the unique email constraint fails here, as a domain exception, when two
     * requests register the same email at the same time.
     */
    @Override
    public Account add(Account account) {
        try {
            return jpa.saveAndFlush(account);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyRegisteredException(account.email());
        }
    }
}
