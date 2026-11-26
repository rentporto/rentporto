# ADR-05: One PostgreSQL instance with one logical database and one user per service

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §8.3, §13.1

## Context

A database per service is mandatory, so that no service reads another service's data. Running six PostgreSQL instances in k3d would use a large part of the memory budget.

## Decision

Run a single PostgreSQL StatefulSet. A startup script creates six databases and six users, one per service, and each user only has privileges on its own database. Each service manages its schema with Flyway.

The `booking` database has one schema per module (`reservation`, `pricing`, `payment`), with no foreign keys or joins between schemas (ADR-15).

## Alternatives considered

- **One PostgreSQL instance per service** — gives physical isolation, but costs too much memory in k3d.
- **Shared schema** — brings back the coupling of the monolith.

## Consequences

### Positive

- Data isolation enforced by the database privileges.
- Low memory use.
- The per-module schemas let the `booking-service` be split later by changing only the connection.

### Negative

- The isolation is logical, not physical; the instance is a single point of failure.
- In production, each service would have its own instance.
