package pt.rentporto.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Customer the emails go to. Only {@code CustomerRegistered} carries the email, so the service keeps the name and the
 * email of each customer from it; the other events only carry the id.
 */
@Entity
@Table(name = "recipients")
public class Recipient {

    @Id
    private UUID id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Recipient() {
    }

    /**
     * Creates a recipient.
     *
     * @param id       id of the customer
     * @param fullName full name
     * @param email    email address
     * @return the recipient
     */
    public static Recipient of(UUID id, String fullName, String email) {
        Recipient recipient = new Recipient();
        recipient.id = Objects.requireNonNull(id, "id");
        recipient.change(fullName, email);
        return recipient;
    }

    /**
     * Replaces the name and the email.
     *
     * @param fullName full name
     * @param email    email address
     */
    public void change(String fullName, String email) {
        this.fullName = Objects.requireNonNull(fullName, "fullName");
        this.email = Objects.requireNonNull(email, "email");
        this.updatedAt = Instant.now();
    }

    public UUID id() {
        return id;
    }

    public String fullName() {
        return fullName;
    }

    public String email() {
        return email;
    }
}
