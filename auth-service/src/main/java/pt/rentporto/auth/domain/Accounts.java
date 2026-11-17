package pt.rentporto.auth.domain;

import java.util.Optional;

/**
 * Stored accounts. Implemented in the infrastructure.
 */
public interface Accounts {

    /**
     * Finds an account by its email.
     *
     * @param email email of the account
     * @return the account, if there is one
     */
    Optional<Account> withEmail(Email email);

    /**
     * Stores a new account.
     *
     * @param account the account
     * @return the stored account, with its id
     * @throws EmailAlreadyRegisteredException if another account with the same email was stored first
     */
    Account add(Account account);
}
