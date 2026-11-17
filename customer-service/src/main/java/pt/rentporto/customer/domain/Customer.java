package pt.rentporto.customer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Profile of a customer. Its id is the id of the account in the auth-service, the {@code sub} of the JWT, and the
 * email comes from the account. A profile is only stored with a driving licence that is valid on the day it is saved.
 */
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Embedded
    private TaxNumber taxNumber;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Embedded
    private DriverLicense driverLicense;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Customer() {
    }

    /**
     * Creates the profile of a customer.
     *
     * @param id      id of the account
     * @param email   email of the account
     * @param details what the customer filled in
     * @param today   today, in Lisbon
     * @return the profile
     * @throws DriverLicenseExpiredException if the licence has expired
     */
    public static Customer register(UUID id, String email, CustomerDetails details, LocalDate today) {
        Customer customer = new Customer();
        customer.id = Objects.requireNonNull(id, "id");
        customer.createdAt = Instant.now();
        customer.change(email, details, today);
        return customer;
    }

    /**
     * Replaces the profile with what the customer filled in.
     *
     * @param email   email of the account
     * @param details what the customer filled in
     * @param today   today, in Lisbon
     * @throws DriverLicenseExpiredException if the licence has expired
     */
    public void update(String email, CustomerDetails details, LocalDate today) {
        change(email, details, today);
    }

    private void change(String email, CustomerDetails details, LocalDate today) {
        if (!details.driverLicense().validOn(today)) {
            throw new DriverLicenseExpiredException(details.driverLicense().expiryDate());
        }
        this.email = Objects.requireNonNull(email, "email");
        this.fullName = details.fullName();
        this.taxNumber = details.taxNumber();
        this.birthDate = details.birthDate();
        this.phone = details.phone();
        this.driverLicense = details.driverLicense();
        this.updatedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String fullName() {
        return fullName;
    }

    public TaxNumber taxNumber() {
        return taxNumber;
    }

    public LocalDate birthDate() {
        return birthDate;
    }

    public String phone() {
        return phone;
    }

    public DriverLicense driverLicense() {
        return driverLicense;
    }
}
