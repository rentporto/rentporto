package pt.rentporto.auth.application.service;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.rentporto.auth.application.exception.PasswordTooLongException;
import pt.rentporto.auth.application.port.PasswordHasher;
import pt.rentporto.auth.domain.exception.EmailAlreadyRegisteredException;
import pt.rentporto.auth.domain.exception.RoleNotAssignableException;
import pt.rentporto.auth.domain.model.Account;
import pt.rentporto.auth.domain.model.Email;
import pt.rentporto.auth.domain.model.Role;
import pt.rentporto.auth.domain.repository.Accounts;

/**
 * Creates accounts: customers that register themselves, staff created by an administrator and the first
 * administrator. The email must be unique, and repeating a request with its idempotency key returns the account it
 * created.
 */
@Service
public class AccountService {

    private final Accounts accounts;
    private final PasswordHasher passwordHasher;

    AccountService(Accounts accounts, PasswordHasher passwordHasher) {
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
    }

    /**
     * Creates a customer account.
     *
     * @param email          email used to log in
     * @param password       password in clear text
     * @param idempotencyKey key of the request, or {@code null}
     * @return the account, with the CUSTOMER role
     * @throws EmailAlreadyRegisteredException if another request already created an account with this email
     * @throws PasswordTooLongException        if the password is too long to be hashed
     */
    @Transactional
    public Account register(String email, String password, String idempotencyKey) {
        Email address = new Email(email);
        return create(address, password, idempotencyKey, hash -> Account.customer(address, hash, idempotencyKey));
    }

    /**
     * Creates an agent or manager account.
     *
     * @param email          email used to log in
     * @param password       initial password in clear text
     * @param roles          AGENT, FLEET_MANAGER or PRICING_MANAGER
     * @param idempotencyKey key of the request, or {@code null}
     * @return the account
     * @throws RoleNotAssignableException      if a role is CUSTOMER or ADMIN
     * @throws EmailAlreadyRegisteredException if another request already created an account with this email
     * @throws PasswordTooLongException        if the password is too long to be hashed
     */
    @Transactional
    public Account createStaffUser(String email, String password, Set<Role> roles, String idempotencyKey) {
        Email address = new Email(email);
        return create(address, password, idempotencyKey,
                hash -> Account.staff(address, hash, roles, idempotencyKey));
    }

    /**
     * Creates an administrator account.
     *
     * @param email    email of the administrator
     * @param password password in clear text
     * @return the account
     * @throws EmailAlreadyRegisteredException if an account with this email exists; it is left as it is
     */
    @Transactional
    public Account createAdministrator(String email, String password) {
        Email address = new Email(email);
        return create(address, password, null, hash -> Account.administrator(address, hash));
    }

    private Account create(Email email, String password, String idempotencyKey, Function<String, Account> newAccount) {
        Optional<Account> existing = accounts.withEmail(email);
        if (existing.isPresent()) {
            if (existing.get().createdWith(idempotencyKey)) {
                return existing.get();
            }
            throw new EmailAlreadyRegisteredException(email);
        }
        return accounts.add(newAccount.apply(passwordHasher.hash(password)));
    }
}
