# ADR-15: Hexagonal architecture in the services and in the modules of the booking-service

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-01, ADR-06, ADR-07, ADR-12; backlog RP-09, RP-22

## Context

Each service is one bounded context, except the `booking-service`, which combines three (ADR-01). Without a common structure, each service would end up organised in its own way, with the business rules mixed with Spring, the database and the calls to other services. In the `booking-service`, the three contexts would end up reading each other's tables and sharing entities, which is exactly the coupling of the monolith that the migration aims to remove. It must also be possible to split the `booking-service` later, if the scale or the team justify it.

## Decision

### Layers and dependency rule

Each service follows a hexagonal architecture with four layers:

```
pt/rentporto/{service}/
├── api/             # inbound adapter: XxxApi contract, controllers, request and response records (RP-06)
├── application/     # transactional use cases and outbound ports (interfaces)
├── domain/          # aggregates, value objects, repository ports and business exceptions
└── infrastructure/  # outbound adapters, one subpackage per technology: persistence, security, messaging, client
```

- **Domain:** Java without Spring. The `jakarta.persistence` annotations are allowed, as mapping metadata; there is no separate persistence model.
- **Application:** orchestrates the domain and talks to the outside only through ports it defines itself (for example `PasswordHasher`, `DriverProfiles`, `CustomerEvents`). It may use `@Service` and `@Transactional`, but not Spring Data, Spring Security, Spring Web, AMQP or Feign.
- **API and infrastructure:** implement the adapters. No layer depends on them.
- **Dependencies:** point inwards: `api` and `infrastructure` → `application` → `domain`.
- **Test:** an ArchUnit test in each service checks these rules.

### Domain conventions

- **Value objects** for the concepts that have rules (`Email`, `TaxNumber`, `Plate`, `Money`, `RentalPeriod`): they validate and normalise in the constructor and are `@Embeddable` in JPA.
- **Aggregates with behaviour:**
  - no setters;
  - they are created by factories that prevent an invalid state;
  - they change state through methods that validate the transition (for example `vehicle.sendToMaintenance()`);
  - they refer to other aggregates only by id.
- **Other services behind a port:** the application defines the interface, and an adapter in the infrastructure (Feign with Resilience4j) translates the other service's DTO into its own model, like the Anti-Corruption Layer of ADR-12.
- **Integration events only in the adapters:** the domain does not use `common-events`. An adapter translates the state of the aggregate into the contract and writes it to the outbox, in the same transaction (ADR-07).

### Modules of the booking-service

- The code is split into three top-level packages, one per context, each with the same four layers:

```
booking-service/src/main/java/pt/rentporto/booking/
├── reservation/
├── pricing/
├── payment/
└── shared/        # outbox, messaging config
```

- A module uses another only through the public interface of its `application` layer (`PricingApi`, `PaymentApi`). It never accesses the repositories, entities or controllers of another module.
- Each module has its own schema in the `booking` database, with its own Flyway migrations, and no foreign keys or joins between schemas. An operation can span two modules in the same transaction only through those interfaces (e.g. `PricingApi.markQuoteUsed` inside the creation of the reservation).
- The modules react to each other through internal events (`ReservationPaymentRequested`, `PaymentCompleted`, `PaymentFailed`), handled after the commit with `@ApplicationModuleListener` and persisted by the Spring Modulith event publication registry.
- A test with `ApplicationModules.of(BookingApplication.class).verify()` fails if any rule between modules is broken; the ArchUnit test checks the layers inside each module.
- Each module has its own queues in RabbitMQ.

The Spring Modulith version is 2.1.x, the line compatible with Spring Boot 4.1. It was checked with Java 25: Spring Modulith 2.1.2 uses ArchUnit 1.4.2, which reads Java 25 classes, and `ApplicationModules.verify()` detects violations between modules compiled with `--release 25`.

## Alternatives considered

- **Folders by type (`controller`, `service`, `entity`, `repository`, `dto`)** — the best-known structure, but it tends to put the business rules in the services, leave the entities without behaviour and mix the domain with Spring.
- **Packages by feature** (e.g. `account`, `token`, `security`) — groups what changes together and allows package-private classes, but does not separate the domain from the infrastructure.
- **Pure hexagonal architecture, with a separate persistence model** — the domain is also free of JPA, but it requires a JPA entity and mappers in both directions for each aggregate, with little gain while the two models are the same.
- **Packages without checked rules** — the boundaries erode under the first deadline pressure.
- **One Maven submodule per context inside the booking-service** — gives stronger compile-time isolation, but complicates the build and the pipeline for little gain with two people.

## Consequences

### Positive

- The business rules are tested without Spring, without a database and without the other services; in the application tests, a port is replaced by a fake implementation.
- Replacing a technology (Feign, Stripe, the password algorithm) only touches one adapter.
- One structure across the whole repository, checked in the CI and not only in review.
- Splitting a module of the booking-service into its own service means replacing the `application` interface with a remote client and the internal events with messages, without rewriting the domain.
- The event publication registry ensures that an internal event is not lost if the service goes down.

### Negative

- More interfaces and adapters: one port for each external dependency.
- The classes used across layers must be public.
- The domain still has the JPA annotations, so a mapping change can touch the domain classes.
- One more dependency (Spring Modulith) and an event publication registry table in the booking-service.
- It takes discipline in pull request reviews, on top of the verification tests.
