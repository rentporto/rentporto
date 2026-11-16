package pt.rentporto.booking.pricing.api;

import pt.rentporto.events.Money;

/**
 * Line of a quote.
 *
 * @param type        what the line charges or discounts
 * @param description detail shown to the customer
 * @param amount      amount of the line, rounded once to the cent
 */
public record QuoteLine(QuoteLineType type, String description, Money amount) {
}
