package pt.rentporto.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Locale;
import java.util.Objects;

/**
 * Email of an account, always without surrounding spaces and in lower case, so that the case never creates a second
 * account or misses an existing one.
 *
 * @param value the normalised email
 */
@Embeddable
public record Email(@Column(name = "email", nullable = false, updatable = false) String value) {

    public Email {
        Objects.requireNonNull(value, "email");
        value = value.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return value;
    }
}
