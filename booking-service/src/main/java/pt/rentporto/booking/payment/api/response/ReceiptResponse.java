package pt.rentporto.booking.payment.api.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import pt.rentporto.booking.payment.api.model.ReceiptLine;
import pt.rentporto.events.Money;

/**
 * Receipt of what a customer paid for a reservation.
 *
 * @param receiptId     receipt identifier
 * @param reservationId reservation the receipt refers to
 * @param lines         amounts charged
 * @param vat           VAT included in the total
 * @param total         total charged
 * @param issuedAt      instant the receipt was issued
 */
public record ReceiptResponse(
        UUID receiptId, UUID reservationId, List<ReceiptLine> lines, Money vat, Money total, OffsetDateTime issuedAt) {
}
