# ADR-19: External configuration in the Config Server

- **Status:** Accepted
- **Date:** 2026-11-17
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-04, ADR-11, ADR-16, ADR-17; backlog RP-07, RP-08

## Context

The services need different addresses, databases and parameters in development and in k3d, and those values must change without rebuilding the images. In development, docker-compose only starts the infrastructure, and the services run in the IDE, on `localhost` with fixed ports (RP-07); in k3d, they call each other by the names of the Services (ADR-04). The repository is public (ADR-17).

## Decision

- The configuration lives in the `config/` folder of the monorepo (ADR-11): common files (`application.yml`, `application-dev.yml`, `application-k8s.yml`) and one `{service}.yml` per service. It changes through pull requests, like the code.
- The Config Server serves that folder with two backends, chosen by profile:
  - `dev`: the `native` backend, on the local folder, so that a change applies when the service restarts, without a push;
  - `k8s`: the `git` backend, on the `main` branch of the repository on GitHub.
- The gateway and the six services import their configuration from the Config Server with fail-fast and a few retries: without the Config Server, they do not start and, in k3d, the pod does not become ready.
- The addresses depend on the profile: `localhost` and the fixed ports in `dev`, the names of the Services in `k8s`. This replaces the sentence in ADR-04 that says docker-compose uses the names of the Services.
- The repository holds no secrets: the passwords of the `k8s` profile are placeholders filled in by Secrets.

## Alternatives considered

- **A Git repository only for the configuration** — separates the configuration from the code, but requires two repositories and two review flows.
- **The `git` backend in development too** — requires a push to test a configuration change.
- **Kubernetes ConfigMaps instead of the Config Server** — saves one component, but does not serve local development with the same configuration.

## Consequences

### Positive

- The same versioned configuration serves both environments; only the addresses change.
- Changing a parameter in k3d takes a commit on `main` and a restart of the service, with no new image.

### Negative

- In k3d, the Config Server needs internet access to read GitHub; without a network, the services do not start.
- In development, the Config Server must be running before the services.
