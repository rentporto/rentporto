# ADR-02: Hybrid communication: REST for immediate decisions, messages for state changes

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §9.1, §9.4

## Context

Some interactions need an answer before the caller can continue: the customer waits for the reservation to be accepted, or the agent waits at the counter for the licence plate. Others only propagate a state change that may matter to other contexts. With online payments, an external provider enters the flow, and a slow provider must not block search or quotes.

## Decision

Between services:

- **Synchronous REST (OpenFeign)** only when the caller needs the answer to decide now: the `reservation` module asks the `customer-service` for the driver profile; at pick-up, the `rental-service` asks the `fleet-service` to assign a vehicle and the `booking-service` for the deposit pre-authorisation.
- **Commands** on a dedicated queue with a single consumer when one service asks another to do something: `SettleDeposit` (from the `rental-service` to the `booking-service`).
- **Events** on the topic exchange for everything that has happened: `ReservationConfirmed`, `VehicleReturned`, etc.

Inside the `booking-service`, the modules communicate through calls to the `application` interfaces and through internal events (ADR-15), not through RabbitMQ.

Every synchronous call between services has a timeout and a circuit breaker (Resilience4j).

## Alternatives considered

- **Everything over REST** — creates temporal coupling and cascading failures; a slow payment provider would hold threads and stop search.
- **Everything asynchronous** — forces the user to check the status of decisions that must be made immediately, such as driver eligibility, and adds complexity without benefit.

## Consequences

### Positive

- Low temporal coupling, without making asynchronous what the user expects to see immediately.
- Failures of the payment provider stay contained in the `payment` module.

### Negative

- Eventual consistency between services.
- Requires reliable messaging (ADR-07) and idempotent consumers.
