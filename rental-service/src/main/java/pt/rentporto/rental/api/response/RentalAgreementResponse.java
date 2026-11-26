package pt.rentporto.rental.api.response;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import pt.rentporto.events.Money;
import pt.rentporto.events.rental.ExtraCharge;
import pt.rentporto.events.reservation.ContractExtra;
import pt.rentporto.events.reservation.ContractTerms;
import pt.rentporto.rental.api.model.RentalStatus;
import pt.rentporto.rental.api.model.Settlement;
import pt.rentporto.rental.domain.model.RentalAgreement;

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

    public static RentalAgreementResponse from(RentalAgreement agreement) {
        pt.rentporto.rental.domain.model.ContractTerms terms = agreement.terms();
        return new RentalAgreementResponse(
                agreement.id(),
                agreement.reservationId(),
                RentalStatus.valueOf(agreement.status().name()),
                agreement.licensePlate(),
                agreement.pickupStation(),
                agreement.returnStation(),
                agreement.pickedUpAt() == null ? null : agreement.pickedUpAt().atOffset(ZoneOffset.UTC),
                agreement.returnedAt() == null ? null : agreement.returnedAt().atOffset(ZoneOffset.UTC),
                new ContractTerms(
                        money(terms.dailyRate()),
                        terms.extras().stream()
                                .map(extra -> new ContractExtra(
                                        extra.code(), money(extra.pricePerDay()), money(extra.cap())))
                                .toList(),
                        terms.coverage(),
                        money(terms.excess()),
                        money(terms.deposit()),
                        money(terms.fuelPricePerLitre()),
                        money(terms.refuellingFee())),
                agreement.extraCharges().stream()
                        .map(charge -> new ExtraCharge(
                                ExtraCharge.Type.valueOf(charge.type().name()),
                                charge.description(),
                                money(charge.amount())))
                        .toList(),
                settlement(agreement.settlement()));
    }

    private static Settlement settlement(pt.rentporto.rental.domain.model.Settlement settlement) {
        return settlement == null
                ? null
                : new Settlement(
                        money(settlement.captured()), money(settlement.released()), money(settlement.outstanding()));
    }

    private static Money money(pt.rentporto.rental.domain.model.Money money) {
        return Money.ofCents(money.cents());
    }
}
