# ADR-01: Decomposition into microservices by bounded context

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §3, §5, §7

## Context

The current system is a modular monolith with eight modules on a single PostgreSQL database. The modules read and write each other's tables, so they cannot be scaled or deployed separately. A simplified Event Storming and an analysis of the ubiquitous language found eight bounded contexts. The strongest boundary signal was polysemy: "vehicle" and "customer" mean different things in Fleet, Reservations, Rental, Pricing and Payments.

Reservations, Pricing and Payments take part in the same flow: quote, reservation and prepayment. With one service per context, each reservation would cross three processes, with synchronous calls, commands and compensations between them. The argument for deploying Pricing on its own (rates change every week) does not require a service of its own either: rates are data changed through the API, not code, so they change without a new deployment. The team has two people, and the local cluster has a memory budget of about 10 GB.

## Decision

The eight bounded contexts go into six microservices. Each service owns its data:

| Service | Bounded contexts |
|---|---|
| `auth-service` | Identity and Access |
| `customer-service` | Customers |
| `fleet-service` | Fleet |
| `booking-service` | Reservations, Pricing, Payments |
| `rental-service` | Rental |
| `notification-service` | Notifications |

Inside the `booking-service`, each context is a module (`reservation`, `pricing`, `payment`) with boundaries checked by tests and its own database schema (ADR-15). The inspection and the damages stay inside the `rental-service`, and the receipts inside the `payment` module, because they share the life cycle of their aggregate. Invoicing certified by the Portuguese Tax Authority (AT) stays in the remaining monolith and is out of scope.

## Alternatives considered

- **One service per bounded context (8 services)** — isolates Payments and lets Pricing be deployed alone, but each reservation crosses three processes, with remote calls, commands and compensations, and there are two more JVMs in k3d.
- **One service per entity (Vehicle, Inspection, Receipt…)** — creates nano-services that call each other constantly and need distributed transactions for a single life cycle.
- **Split by technical layer (API, business, data)** — keeps the coupling through the shared database, which is the original problem.

## Consequences

### Positive

- The boundaries follow the language of the domain, so they are stable.
- The quote, reservation and payment flow runs in a single process: validating the quote and marking it as used happen in the same transaction as the reservation.
- Fewer remote calls, fewer messages and fewer parts to deploy and observe.
- The contexts stay separate as modules, so the `booking-service` can be split later without rewriting the domain.

### Negative

- Payments, a generic subdomain with an external dependency, live next to the core; only the Anti-Corruption Layer and the bulkhead protect them (ADR-12).
- Reservations and Pricing scale and are deployed together.
- The boundaries between modules depend on discipline and on architecture tests (ADR-15).
- The two team members share the same deployable: Nuno on the `reservation` and `payment` modules, Vitor on the `pricing` module.
