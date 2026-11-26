package pt.rentporto.booking.pricing.api.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import pt.rentporto.booking.pricing.api.model.DriverAgeBand;
import pt.rentporto.booking.pricing.api.model.QuoteLine;
import pt.rentporto.events.Money;
import pt.rentporto.events.RentalPeriod;

/**
 * Detailed price of a rental; immutable and valid for 15 minutes.
 *
 * @param quoteId         quote identifier
 * @param expiresAt       instant after which the quote can no longer be booked, 15 minutes after it was issued
 * @param categoryCode    ACRISS code of the vehicle category
 * @param pickupStation   code of the pick-up station
 * @param returnStation   code of the return station
 * @param rentalPeriod    pick-up and return date-times
 * @param rentalDays      number of 24-hour days charged
 * @param driverAgeBand   age band of the driver
 * @param lines           price lines
 * @param vat             VAT on the sum of the lines
 * @param total           amount to pay in advance, VAT included
 * @param excess          maximum the customer pays for damages
 * @param securityDeposit amount held on the card at pick-up
 */
public record QuoteResponse(
        UUID quoteId,
        OffsetDateTime expiresAt,
        String categoryCode,
        String pickupStation,
        String returnStation,
        RentalPeriod rentalPeriod,
        int rentalDays,
        DriverAgeBand driverAgeBand,
        List<QuoteLine> lines,
        Money vat,
        Money total,
        Money excess,
        Money securityDeposit) {
}
