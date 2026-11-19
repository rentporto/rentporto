package pt.rentporto.rental;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import pt.rentporto.rental.domain.ConfirmedReservation;
import pt.rentporto.rental.domain.ContractExtra;
import pt.rentporto.rental.domain.ContractTerms;
import pt.rentporto.rental.domain.Money;
import pt.rentporto.rental.domain.RentalPeriod;

/**
 * A confirmed one-way reservation from the airport to Matosinhos, with a child seat.
 */
public final class Reservations {

    private Reservations() {
    }

    /**
     * A reservation with a new identifier.
     *
     * @return the reservation
     */
    public static ConfirmedReservation confirmed() {
        return returningAt(Instant.parse("2026-07-04T09:00:00Z"));
    }

    /**
     * A reservation with a new identifier, for three days up to a return instant.
     *
     * @param returnAt the return agreed
     * @return the reservation
     */
    public static ConfirmedReservation returningAt(Instant returnAt) {
        return new ConfirmedReservation(
                UUID.randomUUID(),
                UUID.fromString("3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b"),
                "OPO",
                "MAT",
                "ECMR",
                new RentalPeriod(returnAt.minus(Duration.ofDays(3)), returnAt),
                new ContractTerms(
                        Money.ofCents(4500),
                        List.of(new ContractExtra("CHILD_SEAT", Money.ofCents(500), Money.ofCents(2500))),
                        "MEDIUM",
                        Money.ofCents(100000),
                        Money.ofCents(60000),
                        Money.ofCents(215),
                        Money.ofCents(2500)),
                Money.ofCents(19680));
    }
}
