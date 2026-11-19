package pt.rentporto.rental.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Charges of a return, from the terms frozen at booking (ADR-09) and rounded as in ADR-14: each line once, to the
 * cent, half up, and the VAT once, on the sum of the lines.
 *
 * <ul>
 *   <li>A day is 24 hours with 59 minutes of tolerance: a return up to 59 minutes late costs nothing, and each day
 *       after that costs the daily rate and the extras per day, up to the cap of each extra counting the days already
 *       paid, plus VAT.</li>
 *   <li>The vehicle leaves full: the litres the agent put in to fill it cost the price per litre, plus the refuelling
 *       fee once.</li>
 *   <li>The damages cost what the agent set for each one, all of them together up to the excess.</li>
 * </ul>
 */
public final class ReturnCharges {

    static final BigDecimal VAT = new BigDecimal("0.23");

    private static final long MINUTES_PER_DAY = 24 * 60;
    private static final long TOLERANCE_MINUTES = 59;

    private ReturnCharges() {
    }

    /**
     * Works out the charges of a return.
     *
     * @param terms      terms frozen at booking
     * @param period     pick-up and return agreed
     * @param returnedAt when the vehicle came back
     * @param inspection state of the vehicle at the return
     * @return the charges, late return first, then fuel, then each damage; none if there is nothing to charge
     * @throws FuelMismatchException if the litres to fill do not match the fuel level
     */
    public static List<ExtraCharge> of(
            ContractTerms terms, RentalPeriod period, Instant returnedAt, ReturnInspection inspection) {
        List<ExtraCharge> charges = new ArrayList<>();
        lateReturn(terms, period, returnedAt).ifPresent(charges::add);
        fuel(terms, inspection).ifPresent(charges::add);
        charges.addAll(damages(terms, inspection));
        return List.copyOf(charges);
    }

    /**
     * Days a duration is charged as, with the tolerance of 59 minutes for each day.
     *
     * @param duration the duration
     * @return the days, zero for a duration under one hour
     */
    static long days(Duration duration) {
        long minutes = duration.toMinutes();
        if (minutes <= TOLERANCE_MINUTES) {
            return 0;
        }
        return Math.ceilDiv(minutes - TOLERANCE_MINUTES, MINUTES_PER_DAY);
    }

    private static Optional<ExtraCharge> lateReturn(ContractTerms terms, RentalPeriod period, Instant returnedAt) {
        long extraDays = returnedAt.isAfter(period.returnAt())
                ? days(Duration.between(period.returnAt(), returnedAt))
                : 0;
        if (extraDays == 0) {
            return Optional.empty();
        }
        long bookedDays = Math.max(1, days(Duration.between(period.pickupAt(), period.returnAt())));
        long lines = terms.dailyRate().cents() * extraDays;
        for (ContractExtra extra : terms.extras()) {
            long alreadyCharged = Math.min(extra.cap().cents(), extra.pricePerDay().cents() * bookedDays);
            lines += Math.min(extra.pricePerDay().cents() * extraDays, extra.cap().cents() - alreadyCharged);
        }
        long vat = BigDecimal.valueOf(lines).multiply(VAT).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String description = extraDays == 1 ? "1 extra day" : extraDays + " extra days";
        return Optional.of(new ExtraCharge(ExtraCharge.Type.LATE_RETURN, description, Money.ofCents(lines + vat)));
    }

    private static Optional<ExtraCharge> fuel(ContractTerms terms, ReturnInspection inspection) {
        boolean missing = inspection.fuelLevelEighths() < RentalAgreement.FULL_TANK;
        boolean filled = inspection.litresToFill().signum() > 0;
        if (missing != filled) {
            throw new FuelMismatchException(inspection.fuelLevelEighths(), inspection.litresToFill());
        }
        if (!filled) {
            return Optional.empty();
        }
        long litres = inspection.litresToFill()
                .multiply(BigDecimal.valueOf(terms.fuelPricePerLitre().cents()))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        String description = inspection.litresToFill().stripTrailingZeros().toPlainString() + " litres to fill";
        return Optional.of(new ExtraCharge(
                ExtraCharge.Type.FUEL, description, Money.ofCents(litres + terms.refuellingFee().cents())));
    }

    private static List<ExtraCharge> damages(ContractTerms terms, ReturnInspection inspection) {
        List<ExtraCharge> charges = new ArrayList<>();
        long left = terms.excess().cents();
        for (ReturnDamage damage : inspection.damages()) {
            long charged = Math.min(damage.amount().cents(), left);
            if (charged > 0) {
                charges.add(new ExtraCharge(ExtraCharge.Type.DAMAGE,
                        damage.location() + ": " + damage.description(), Money.ofCents(charged)));
                left -= charged;
            }
        }
        return charges;
    }
}
