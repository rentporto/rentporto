# ADR-09: Contract terms frozen in ReservationConfirmed

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §7, §11.2

## Context

Rates change every week. At return, the rental agreement must be settled with the terms agreed at booking: daily rate, extras, coverage, excess, deposit, price per litre and service fee.

## Decision

The `ContractTerms` are frozen at booking time, from the quote, and travel in the `ReservationConfirmed` event. The `rental-service` stores them in the rental agreement and does the settlement without calling the `booking-service` or the `customer-service`.

## Alternatives considered

- **Rental looks up Pricing at return** — applies today's rates to an agreement made in the past, and couples the `rental-service` to the `booking-service`.

## Consequences

### Positive

- Follows the real business rule: a price change never affects an existing reservation.
- Less coupling between Rental and Pricing.

### Negative

- Larger events and data duplicated on purpose.
- Changes to the event schema require versioning.
