package pt.rentporto.booking.payment.api.model;

import pt.rentporto.events.Money;

/**
 * Line of a receipt.
 *
 * @param description what was charged
 * @param amount      amount charged
 */
public record ReceiptLine(String description, Money amount) {
}
