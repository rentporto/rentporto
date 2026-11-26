# ADR-14: Amounts and rounding

- **Status:** Proposed
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §1.5, §10.1; backlog RP-24

## Context

Prices combine a daily rate, season multipliers, a duration discount, capped extras, coverage, fees and 23% VAT. Without a single rounding rule, the same quote can give totals that differ by one cent between the quote, the reservation and the receipt.

## Decision

- All amounts are stored and exchanged as `Money` in cents (`long amountCents`, currency `EUR`).
- Intermediate calculations use `BigDecimal`.
- Each price line (rental, each extra, coverage, each fee) is rounded once, to the cent, with `RoundingMode.HALF_UP`.
- VAT is computed on the sum of the rounded lines and rounded once with `HALF_UP`.
- The quote stores every line, so the reservation and the receipt never recalculate.

## Alternatives considered

- **Round only the total** — the lines shown to the customer would not add up to the total.
- **Banker's rounding (`HALF_EVEN`)** — less intuitive for customers and for hand-calculated test cases.

## Consequences

### Positive

- Reproducible totals that match the hand-calculated test cases to the cent.

### Negative

- Every line must be rounded in the same place, in the `pricing` module of the `booking-service`.
