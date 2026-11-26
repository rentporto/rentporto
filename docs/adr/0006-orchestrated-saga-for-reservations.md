# ADR-06: Orchestrated saga for the reservation, choreography for the reactions

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §9.2, §9.3

## Context

Creating a reservation involves the capacity, the quote and a payment with an external provider. The process needs compensations: a declined payment, an unavailable provider, a payment not completed within 15 minutes, or a payment that arrives after the reservation has expired. The call to Stripe cannot sit inside the transaction that locks the capacity, or the rows stay locked while waiting for the provider. Other flows, such as sending notifications or updating the fleet after a return, are simple reactions, with no decisions to make.

## Decision

The `reservation` module of the `booking-service` orchestrates the reservation and cancellation sagas. The state of the saga is the state of the `Reservation` (`PENDING_PAYMENT`, `CONFIRMED`, `PAYMENT_FAILED`, `EXPIRED`, `CANCELLED`…).

Creation flow:

1. In a single transaction: it locks the capacity, validates the quote and marks it as used, creates the reservation as `PENDING_PAYMENT` and records the internal event `ReservationPaymentRequested`.
2. After the commit, the `payment` module handles the event and creates the payment in Stripe, in its own thread pool (ADR-12).
3. The Stripe webhook produces the internal event `PaymentCompleted` or `PaymentFailed`.
4. The reservation becomes `CONFIRMED` and the outbox publishes `ReservationConfirmed` to RabbitMQ, or it becomes `PAYMENT_FAILED` and releases the capacity.

A scheduler handles expiry, no-shows and lost messages. With several replicas, the schedulers use a shared lock (ShedLock on PostgreSQL), so they run on one replica only. The simple reactions (notifications, updating the fleet, creating the rental agreement) are choreographed by events.

## Alternatives considered

- **Choreography only** — spreads the state of the saga and makes the compensations harder to follow and to test.
- **Calling Stripe inside the reservation request** — holds the capacity locks while waiting for the provider and ties the latency of the reservation to that of Stripe.
- **Two-phase commit (2PC)** — neither the broker nor Stripe supports it, and it locks resources while waiting.

## Consequences

### Positive

- The state of the saga is in one place, with explicit, testable compensations.
- Because the saga is internal to the `booking-service`, the compensations do not depend on the broker.
- The reactions stay decoupled.

### Negative

- The `reservation` module knows the steps of the saga.
- Requires schedulers with a shared lock, and care with events that arrive late.
