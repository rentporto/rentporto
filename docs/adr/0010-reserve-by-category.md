# ADR-10: Reserve by category and assign the vehicle at pick-up

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §1.5, §9.5

## Context

Rental companies sell a vehicle category (e.g. Group B – Economy), not a licence plate. The main current problem is overbooking at the airport in summer.

## Decision

A reservation takes up a `VehicleCategory` at a station for a rental period. Availability is a count per station, category and day: the capacity minus the active reservations that take up the station. The `fleet-service` assigns a specific vehicle, atomically, at pick-up.

## Alternatives considered

- **Reserving a specific licence plate** — makes availability depend on individual vehicles, and fails when a vehicle goes to maintenance.

## Consequences

### Positive

- Availability is a simple count, checked with a pessimistic lock.
- The fleet can be reorganised without touching the reservations.

### Negative

- Assigning the vehicle in Fleet must be atomic and idempotent.
- The capacity view can lag briefly; the one-way rule makes it err towards one vehicle too few, never one too many.
