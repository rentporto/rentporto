package pt.rentporto.rental.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import pt.rentporto.events.rental.ExtraCharge;
import pt.rentporto.events.reservation.ContractTerms;

/**
 * Rental agreement with its charges and status.
 *
 * @param rentalId       rental agreement identifier
 * @param reservationId  reservation fulfilled by this rental
 * @param status         status of the agreement
 * @param licensePlate   licence plate of the vehicle assigned; set after pick-up
 * @param pickupStation  code of the pick-up station
 * @param returnStation  code of the return station
 * @param pickedUpAt     pick-up date-time; set after pick-up
 * @param returnedAt     return date-time; set after return
 * @param terms          conditions frozen at booking time
 * @param extraCharges   charges on top of the advance payment, possibly none
 * @param settlement     settlement of the deposit; set after the deposit is settled
 */
public record RentalAgreementResponse(
        UUID rentalId,
        UUID reservationId,
        RentalStatus status,
        String licensePlate,
        String pickupStation,
        String returnStation,
        OffsetDateTime pickedUpAt,
        OffsetDateTime returnedAt,
        ContractTerms terms,
        List<ExtraCharge> extraCharges,
        Settlement settlement) {
}
