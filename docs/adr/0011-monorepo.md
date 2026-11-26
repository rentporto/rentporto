# ADR-11: Monorepo with one module, one image and one pipeline per service

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §12.1, §14.3; backlog RP-01

## Context

The team has two people, and anyone who clones the repository needs a simple way to build and run the system. Even so, the services must be built and deployed independently. Since every module inherits from the same parent POM, the Java and Spring versions are the same across the whole system, and Spring Cloud is only compatible with some Spring Boot lines.

## Decision

A private GitHub repository with a multi-module Maven build: a parent POM, one module per service, the gateway, the Config Server and `common-events`.

- Each service builds its own image and is deployed independently.
- A single GitHub Actions workflow detects the changed modules and builds only those, with `mvn -pl <service> -am verify`.
- The only shared code is `common-events` (message contracts and constants). No domain code is shared.
- The parent POM pins Java 25, Spring Boot 4.1.x and Spring Cloud 2025.1.x, a combination from the official matrix (`start.spring.io`). Java 25 is used both to compile (`--release 25`) and to run: the images use a JRE 25.
- The build only accepts JDK 25: the `maven-enforcer-plugin` requires `[25,26)`. The `.sdkmanrc` at the root pins the local JDK, and the CI uses `actions/setup-java` with version 25.
- The Maven Wrapper (`mvnw`) pins the Maven version, so anyone who clones the repository only needs JDK 25.

## Alternatives considered

- **One repository per service** — adds work for a team of two and for anyone who builds the whole system, and makes contract changes between services harder.
- **Java 21** — the previous LTS, and also supported, but it would require installing another JDK and would lose the improvements of Java 25.
- **Accept any JDK from 25 onwards (`[25,)`)** — a JDK 26 would enter the build without an explicit decision, and the code could behave differently on each machine.
- **Spring Boot 4.0.x** — also compatible with Spring Cloud 2025.1, but it is the older line and loses support sooner.

## Consequences

### Positive

- One place for the code, the ADRs, the API specifications and the Kubernetes manifests.
- Contract changes are atomic, in a single pull request.
- The same JDK in development, in the CI and in the images. Java 25 brings (optional) compact object headers and AOT startup improvements, which help with the k3d memory budget (ADR-16).

### Negative

- It takes discipline not to share domain code.
- A change to `common-events` or to the parent POM rebuilds every service.
- Anyone without JDK 25 has to install it before building.
- Changing the JDK requires a new ADR and an update of the enforcer, the `.sdkmanrc`, the CI and the images.
