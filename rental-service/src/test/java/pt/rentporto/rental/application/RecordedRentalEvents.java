package pt.rentporto.rental.application;

import java.util.ArrayList;
import java.util.List;
import pt.rentporto.rental.domain.Money;
import pt.rentporto.rental.domain.RentalAgreement;

/**
 * Keeps the events and commands the application writes, in order.
 */
final class RecordedRentalEvents implements RentalEvents {

    final List<RentalAgreement> started = new ArrayList<>();
    final List<RentalAgreement> returned = new ArrayList<>();
    final List<RentalAgreement> closed = new ArrayList<>();
    final List<String> settled = new ArrayList<>();

    @Override
    public void rentalStarted(RentalAgreement agreement) {
        started.add(agreement);
    }

    @Override
    public void vehicleReturned(RentalAgreement agreement) {
        returned.add(agreement);
    }

    @Override
    public void rentalClosed(RentalAgreement agreement) {
        closed.add(agreement);
    }

    @Override
    public void settleDeposit(RentalAgreement agreement, Money amountDue) {
        settled.add(agreement.id() + "=" + amountDue.cents());
    }
}
