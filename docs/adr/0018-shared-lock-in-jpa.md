# ADR-18: Shared lock in JPA instead of ShedLock

- **Status:** Accepted
- **Date:** 2026-11-15
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-06, ADR-07; backlog RP-05, RP-60

## Context

With several replicas, the outbox publisher (ADR-07) and the reservation schedulers (ADR-06) must run on only one replica at a time. ADR-06 planned ShedLock on PostgreSQL, but the ShedLock provider for PostgreSQL uses JDBC, and the project's persistence is done in JPA.

## Decision

- The `common-messaging` library has a shared lock in JPA: the `SchedulerLock` entity, in the `scheduler_lock` table, stores the name of the lock, until when it is held and which replica holds it.
- A replica acquires the lock with a JPQL `UPDATE` that only changes the row if the lock is free or expired. If the row does not exist yet, the replica creates it, and the primary key prevents two replicas from creating it at the same time.
- At the end of the task, the replica releases the lock. If the replica goes down before that, the lock expires after the given maximum time.
- The outbox publisher and the booking-service schedulers use the same lock (`SchedulerLocks.runOnOneReplica`).
- This decision replaces the ShedLock mentioned in ADR-06. The rest of ADR-06 still stands.

## Alternatives considered

- **ShedLock on PostgreSQL** — a proven library, but its PostgreSQL provider uses JDBC, alongside the JPA persistence.
- **`SELECT … FOR UPDATE SKIP LOCKED` on the outbox rows** — removes the need for the global lock, but the replicas publish in different orders and the transaction stays open while waiting for the broker's confirmation.
- **A pessimistic lock on a row, held during the task** — the simplest option, but it ties up a database connection for the whole send.

## Consequences

### Positive

- All the project's persistence stays in JPA.
- Transactions are short, and the outbox messages go out in the order they were written.
- One dependency fewer.

### Negative

- The lock code is ours and needs its own tests.
- The lock deadline uses the application clock, so the replicas must have synchronised clocks; in k3d they run on the same machine.
