package pt.rentporto.auth.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import pt.rentporto.auth.domain.exception.EmailAlreadyRegisteredException;
import pt.rentporto.auth.domain.model.Account;
import pt.rentporto.auth.domain.model.Email;

class JpaAccountsTest {

    private final SpringDataAccounts jpa = mock(SpringDataAccounts.class);
    private final JpaAccounts accounts = new JpaAccounts(jpa);

    @Test
    void theSecondOfTwoSimultaneousRegistrationsIsAConflict() {
        when(jpa.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("accounts_email_key"));
        Account account = Account.customer(new Email("ana@rentporto.pt"), "hash", null);

        assertThatThrownBy(() -> accounts.add(account)).isInstanceOf(EmailAlreadyRegisteredException.class);
    }
}
