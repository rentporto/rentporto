# ADR-07: Transactional outbox and idempotent consumers

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §9.6

## Context

A service that updates its database and then publishes to the broker can fail between the two writes: it loses the event, or it publishes an event for a change that was rolled back. RabbitMQ delivers at least once, so a consumer can receive the same message more than once. Stripe webhooks can also arrive more than once.

## Decision

- Each service writes the events and commands it sends to RabbitMQ to an `outbox` table, in the same transaction as the state change; a publisher sends them to the broker.
- The Spring Modulith event publication registry persists the internal events between the modules of the `booking-service`, and reprocesses them if the service goes down before handling them (ADR-15).
- Each consumer records the `eventId` in `processed_events` and ignores duplicates. Stripe webhooks are deduplicated by the Stripe event id.
- Every external message uses the common envelope: `eventId`, `type`, `version`, `occurredAt`, `correlationId`, `payload`. The Micrometer `traceId` travels in the headers.

## Alternatives considered

- **Publishing directly after the commit** — loses the event if the service goes down between the commit and the publication.
- **Publishing before the commit** — can publish events for changes that never happened.

## Consequences

### Positive

- No lost or invented messages, between services or between modules.
- Safe retries across the whole system.

### Negative

- One table and one publisher per service.
- A small delay between the commit and the publication.
- The `outbox` and `processed_events` tables and the event publication registry need periodic cleanup.
