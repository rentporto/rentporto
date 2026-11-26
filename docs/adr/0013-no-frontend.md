# ADR-13: No frontend: interaction through the API

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §1.6, §8.2

## Context

The focus of the project is the microservices architecture. The available effort is about 28 person-days.

## Decision

There is no frontend. Every actor uses the API through the Swagger UI aggregated in the gateway and a Bruno collection with the complete flow of each actor. Payments are confirmed on the server with Stripe test payment methods (`pm_card_visa`, `pm_card_chargeDeclined`).

## Alternatives considered

- **A React frontend with TypeScript** — would take about 3.5 person-days away from the core services and the infrastructure.

## Consequences

### Positive

- More time for the core services, resilience and observability.

### Negative

- No 3-D Secure flow.
- The `paymentMethodId` in the request is a test shortcut: in production it would come from Stripe.js in the browser, and the card details would never pass through our API.
