package pt.rentporto.rental.api.model;

import pt.rentporto.events.Money;

/**
 * Settlement of the security deposit after the return.
 *
 * @param captured    amount charged to the customer, from the deposit and any extra charge
 * @param released    part of the deposit released
 * @param outstanding amount still owed when the charge above the deposit failed; zero otherwise
 */
public record Settlement(Money captured, Money released, Money outstanding) {
}
