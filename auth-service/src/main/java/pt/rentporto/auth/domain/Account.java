package pt.rentporto.auth.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * User account: the email used to log in, the hash of the password and the roles. Its identifier is the {@code sub}
 * of the tokens and, for a customer, the customer identifier. It is created by one of the factories, which only allow
 * the roles each kind of account may have.
 */
@Entity
@Table(name = "accounts")
public class Account {

    private static final Set<Role> STAFF_ROLES = EnumSet.of(Role.AGENT, Role.FLEET_MANAGER, Role.PRICING_MANAGER);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    private Email email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "account_roles", joinColumns = @JoinColumn(name = "account_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<Role> roles;

    @Column(name = "idempotency_key", updatable = false)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Account() {
    }

    private Account(Email email, String passwordHash, Set<Role> roles, String idempotencyKey) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = EnumSet.copyOf(roles);
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now();
    }

    /**
     * Account of a customer who registered.
     *
     * @param email          email used to log in
     * @param passwordHash   hash of the password
     * @param idempotencyKey key of the request that created it, or {@code null}
     * @return the account, with the CUSTOMER role
     */
    public static Account customer(Email email, String passwordHash, String idempotencyKey) {
        return new Account(email, passwordHash, EnumSet.of(Role.CUSTOMER), idempotencyKey);
    }

    /**
     * Agent or manager account, created by an administrator.
     *
     * @param email          email used to log in
     * @param passwordHash   hash of the initial password
     * @param roles          AGENT, FLEET_MANAGER or PRICING_MANAGER
     * @param idempotencyKey key of the request that created it, or {@code null}
     * @return the account
     * @throws RoleNotAssignableException if a role is CUSTOMER or ADMIN
     */
    public static Account staff(Email email, String passwordHash, Set<Role> roles, String idempotencyKey) {
        Set<Role> notAssignable = EnumSet.noneOf(Role.class);
        notAssignable.addAll(roles);
        notAssignable.removeAll(STAFF_ROLES);
        if (!notAssignable.isEmpty()) {
            throw new RoleNotAssignableException(notAssignable);
        }
        return new Account(email, passwordHash, roles, idempotencyKey);
    }

    /**
     * Administrator account, created at startup.
     *
     * @param email        email used to log in
     * @param passwordHash hash of the password
     * @return the account, with the ADMIN role
     */
    public static Account administrator(Email email, String passwordHash) {
        return new Account(email, passwordHash, EnumSet.of(Role.ADMIN), null);
    }

    public UUID id() {
        return id;
    }

    public Email email() {
        return email;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Set<Role> roles() {
        return Set.copyOf(roles);
    }

    /**
     * Whether a request with this idempotency key created the account, so that repeating it returns this account.
     *
     * @param key idempotency key of the request, or {@code null}
     * @return {@code true} if the account was created with this key
     */
    public boolean createdWith(String key) {
        return key != null && key.equals(idempotencyKey);
    }
}
