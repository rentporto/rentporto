# ADR-03: RabbitMQ as the message broker

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §9.6, §11

## Context

The system needs commands with a single consumer, events with several consumers, retries and dead-letter queues. Everything runs in a local k3d cluster, with a memory budget of about 10 GB. Replaying the event history is not a requirement.

## Decision

Use RabbitMQ with Spring AMQP:

- a topic exchange `rentporto.events`, with the routing key `{context}.{event}` (e.g. `reservation.confirmed`);
- one queue and one DLQ per consumer; in the `booking-service`, each module has its own queues;
- a dedicated queue per command;
- 3 attempts with backoff before the message goes to the DLQ.

## Alternatives considered

- **Apache Kafka** — offers replay and high throughput, but uses more memory, has no native per-message DLQ or command semantics, and adds operational work the project does not need.

## Consequences

### Positive

- Native command queues and DLQs.
- A management console that shows the queues and the DLQs while the system runs.
- Low memory use in k3d.

### Negative

- No replayable event history: rebuilding a read model requires the source service to republish its state.
