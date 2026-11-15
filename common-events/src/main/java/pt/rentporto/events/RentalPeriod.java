package pt.rentporto.events;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Pick-up and return date-times of a rental, normalised to UTC.
 *
 * <p>Normalising keeps the JSON the same whatever offset the producer used, so a message always deserialises to an
 * equal object. Showing the local time is up to the consumer.
 *
 * @param pickupAt pick-up date-time
 * @param returnAt return date-time; after {@code pickupAt}
 */
public record RentalPeriod(OffsetDateTime pickupAt, OffsetDateTime returnAt) {

    public RentalPeriod {
        Objects.requireNonNull(pickupAt, "pickupAt");
        Objects.requireNonNull(returnAt, "returnAt");
        if (!returnAt.isAfter(pickupAt)) {
            throw new IllegalArgumentException("returnAt must be after pickupAt");
        }
        pickupAt = pickupAt.withOffsetSameInstant(ZoneOffset.UTC);
        returnAt = returnAt.withOffsetSameInstant(ZoneOffset.UTC);
    }
}
