package pt.rentporto.rental.application;

import java.util.ArrayList;
import java.util.List;
import pt.rentporto.rental.domain.RentalAgreement;

/**
 * Keeps the events and commands the application writes, in order.
 */
final class RecordedRentalEvents implements RentalEvents {

    final List<RentalAgreement> started = new ArrayList<>();
    final List<RentalAgreement> released = new ArrayList<>();

    @Override
    public void rentalStarted(RentalAgreement agreement) {
        started.add(agreement);
    }

    @Override
    public void depositReleased(RentalAgreement agreement) {
        released.add(agreement);
    }
}
