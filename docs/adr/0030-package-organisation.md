# ADR-30: Package organisation by kind of file

- **Status:** Accepted
- **Date:** 2026-11-26
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-08, ADR-15; backlog RP-06

## Context

ADR-15 splits each service into the `api`, `application`, `domain` and `infrastructure` layers. Until now, inside each layer, everything was in a single folder:

- **Size:**
  - the fleet-service `domain` had 33 files, 15 of them exceptions;
  - the rental-service `domain` had 22 and the customer-service one 13;
  - the fleet-service `api` had 23 and the one of the booking-service `pricing` module 18.
- **Mixing:**
  - `domain` held the aggregates, the value objects, the repository ports and the exceptions;
  - `application` held the use cases, the ports, the exceptions and the startup tasks;
  - `api` held the API contract, its implementation and the data that goes in and out.
- **Shared libraries:**
  - the root of `common-web` gathered the error format, the OpenAPI configuration and the observation configuration;
  - the root of `common-events` gathered the envelope, the shared value objects and the names of the RabbitMQ topology.
- **Tests:** the containers, the integration test annotations and the test data were at the root, mixed with the tests.
- **Repetition:**
  - the class that reads the request's account from the JWT (`AuthenticatedUser`) was copied, with small differences, in three services;
  - the configuration of the Lisbon clock (`ClockConfiguration`) was identical in four.

## Decision

### Services

- **One folder per kind of file in each layer**, the same in every service and in the modules of the booking-service:

  ```
  pt/rentporto/{service or module}/
  ├── api/
  │   ├── contract/      # XxxApi: interfaces with the paths, the roles (@PreAuthorize) and the OpenAPI
  │   ├── controller/    # XxxController and XxxAdvice, which implement the contracts
  │   ├── request/       # XxxRequest: request bodies
  │   ├── response/      # XxxResponse: response bodies
  │   └── model/         # enums and records used inside the requests and the responses
  ├── application/
  │   ├── service/       # use cases and startup tasks
  │   ├── port/          # interfaces that the application uses and the infrastructure implements
  │   ├── exception/     # application exceptions
  │   └── model/         # records that pass through the use cases and the ports
  ├── domain/
  │   ├── model/         # aggregates, entities, value objects and enums
  │   ├── repository/    # repository ports
  │   ├── service/       # stateless rules that do not belong to an aggregate
  │   └── exception/     # domain exceptions
  └── infrastructure/    # one folder per technology: persistence, messaging, security, client, mail, ...
  ```

- **What goes in each folder:**
  - **`api`:** the suffix gives the folder. `Api` goes to `contract`, `Controller` and `Advice` to `controller`, `Request` to `request` and `Response` to `response`. The rest goes to `model`.
  - **`application`:** exceptions go to `exception`, interfaces to `port` and the records that pass through the ports to `model`. The rest goes to `service`.
  - **`domain`:** exceptions go to `exception`, repository interfaces to `repository` and stateless rules, such as the calculation of the return charges (`ReturnCharges`), to `service`. The rest goes to `model`.
  - **Folders and nested types:**
    - there are no classes at the root of a layer;
    - a folder only exists when it has files;
    - nested types stay with their class, for example `Mailer.Email` stays with the `Mailer` port.
- **Visibility:**
  - the controllers stay package-private;
  - the `from(...)` methods of the responses become public, because the controller is in another package. ADR-15 already prevents the other layers from using `api`;
  - nothing else changes visibility. The use case constructors and the startup tasks (`AdminBootstrap`, `ExampleCategories`) stay package-private in the `service` package, next to the tests that use them.

### Shared libraries

- **`common-web` by area.** The root keeps only `CommonWebAutoConfiguration`:
  - `error`: `BusinessException`, `EndpointNotImplementedException` and `ProblemDetailsAdvice`;
  - `openapi`: `OpenApiContract` and `WebDefaults`;
  - `observation`: `NoActuatorObservations`;
  - `security`: the resource server configuration and `AuthenticatedUser`;
  - `time`: `TimeAutoConfiguration`.
- **`AuthenticatedUser`** in `pt.rentporto.web.security`: a single class, with `subject()`, `id()`, `email()` and `hasRole()`, read from the JWT (ADR-08). It replaces the three copies.
- **`TimeAutoConfiguration`:** gives every service a `Clock` with Lisbon time and replaces the four `ClockConfiguration` classes. A service or a test can declare its own `Clock`.
- **`common-events`:**
  - the names of the RabbitMQ topology (`Exchanges`, `RoutingKeys`, `CommandQueues`) move to `topology`;
  - `EventEnvelope`, `Money` and `RentalPeriod` stay at the root;
  - the events stay in one folder per context;
  - the messages and the topology do not change: the packages of the constants change, not their values.
- **No layers:** `common-messaging` (`consumer`, `lock`, `outbox`) and the gateway (`docs`, `ratelimit`, `routing`, `security`) stay organised by feature.

### Tests

- **Each test stays in the package of the class it tests**, so it can use what is package-private:
  - endpoint tests go to `api/controller`;
  - use case tests and their fakes of the ports go to `application/service`;
  - domain tests go to `domain/model` or `domain/service`;
  - infrastructure tests stay in the folder of their technology.
- **`OpenApiContractTest` stays at the root of `api`**, because it covers the whole API of the service. In the booking-service it stays at the root of the service, because it covers the three modules.
- **The helpers shared by the tests go to `{service}/testing`**, as `common-web` and `common-messaging` already did. They are the containers, the `@XxxServiceTest` annotations and the test data.
- **Only the whole-service tests stay at the root:** `ArchitectureTest`, `AuthorizationTest` and `XxxApplicationTests`.

### Automatic verification

The `ArchitectureRules` of `common-web`, which every service already runs, check that:

- in `api`, `application` and `domain` there are no classes outside the folders above, and there are no classes at the root of `infrastructure`;
- in `api`, each class is in the folder of its suffix, and only controllers depend on controllers;
- exceptions are only in the `exception` folders, and those folders hold only exceptions;
- the `port` and `repository` folders hold only interfaces, and `application/service` holds none.

The rest of ADR-15 still applies.

## Alternatives considered

- **One folder per resource or aggregate in each layer** (for example `vehicle`, `transfer` and `category`) — groups the files that change together and leaves more classes package-private. But each folder mixes kinds of file again, and the types shared by several resources need one more folder. The team preferred to see at a glance what is a contract, a port, a use case, an entity or an exception.
- **Keeping one folder per layer** — takes no work, but the largest folders keep growing with each new feature.
- **Separating only the exceptions** — it is the folder that grows the most in the fleet-service, but the ports and the repositories would stay mixed with the use cases and the entities.
- **Separate mappers** (for example MapStruct) instead of the `from(...)` methods in the responses — one more dependency and one more folder for one-line conversions.

## Consequences

### Positive

- Each folder holds a single kind of file and has the same name in every service. The ports, the repositories, the exceptions or the contract of any service are found in the same place.
- The API contract is separate from its implementation, and the application's ports are separate from the use cases.
- The organisation is checked in each service's tests, so it does not erode over time.
- `AuthenticatedUser` and the Lisbon clock have a single version, tested once.
- The test helpers are kept apart from the tests.

### Negative

- A feature touches several folders: for example, a new rule can touch the aggregate, the exception and the use case.
- There are more imports between folders of the same layer.
- The responses' `from(...)` methods are public.
- A test moves folder when the class it tests moves, to stay in the same package.
