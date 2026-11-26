# ADR-12: Stripe in test mode behind an Anti-Corruption Layer, with a WireMock adapter

- **Status:** Superseded by ADR-29
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §5, §8.3, §9.2

## Context

The business wants online prepayment and a deposit held on the card at pick-up. The system must handle realistic payments and reproduce failures on demand (a slow or unavailable provider). Since Payments live in the same service as search and quotes (ADR-01), a slow Stripe must not consume the resources that serve those requests.

## Decision

The `payment` module of the `booking-service` defines a `PaymentProvider` port with two adapters, chosen by configuration:

- **Stripe** in test mode, with the Java SDK; the webhooks reach the cluster through the Stripe CLI;
- **WireMock**, with configurable latency and failures.

The Stripe types (`PaymentIntent`, `PaymentMethod`) never leave the adapter.

Calls to the provider run on their own bounded thread pool (bulkhead), with a timeout, 3 retries with backoff and an idempotency key, and a circuit breaker. They never use the threads that serve `/availability` and `/quotes`.

## Alternatives considered

- **Only a simulated gateway** — less realistic, and it exercises neither the webhooks nor real pre-authorisations.
- **Payments in their own service** — isolates failures at the process level, but forces the saga to cross the broker (see ADR-01).

## Consequences

### Positive

- Realistic online payments, visible in the Stripe dashboard.
- Switching adapters demonstrates the Anti-Corruption Layer and the circuit breaker scenarios.
- With the bulkhead, a slow provider affects neither search nor quotes.

### Negative

- The Stripe CLI is needed in the cluster, because k3d is not reachable from the internet.
- The isolation is at the thread level, not the process level: a severe JVM failure affects all three modules.
- Card pre-authorisations last about 7 days, so long rentals need a renewal.
- No 3-D Secure (see ADR-13).
