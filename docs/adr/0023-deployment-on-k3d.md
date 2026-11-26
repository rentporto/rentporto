# ADR-23: Deployment on k3d

- **Status:** Accepted
- **Date:** 2026-11-17
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-04, ADR-16, ADR-17, ADR-19, ADR-20; backlog RP-07, RP-12

## Context

The system has to start on k3d with a single command (ADR-16), from a machine with Docker and k3d. For that, four things have to be decided:

- **Secrets:** the services need passwords, a signing key (ADR-20) and an administrator, and the repository is public (ADR-17).
- **Images:** the CI images live in GHCR, but on k3d we also want to run code that has not been published yet.
- **Startup order:** without the Config Server, the services do not start (ADR-19), and Kubernetes restarts them with ever longer waits.
- **RabbitMQ:** the definitions file from RP-07 carried the user and the hash of its password.

## Decision

- **Secrets generated at deploy time.**
  - `deploy.sh` creates the `auth-service`, `postgres` and `rabbitmq` Secrets only if they do not exist yet, with random passwords and a key generated with `openssl`.
  - No secret goes into the repository.
  - Running `deploy.sh` again does not change them, so they keep matching the data stored in the volumes.
- **Images by overlay.**
  - The manifests live in `infra/k8s`, with Kustomize.
  - The base points to the GHCR images. The `k3d` overlay replaces them with the locally built images, which `deploy.sh` imports with `k3d image import`.
- **One source for the RP-07 configuration:** the RabbitMQ topology and the PostgreSQL `init.sh` enter the cluster through ConfigMaps generated from the compose files.
- **RabbitMQ users outside the definitions.**
  - RabbitMQ loads a folder with two files: the topology, which is shared, and the users.
  - In compose, the users come from the repository's `users.json`; on k3d, they come from the `rabbitmq` Secret.
- **Explicit startup order.**
  - The services have an initContainer that waits for the Config Server, PostgreSQL and RabbitMQ.
  - A startupProbe gives the JVM time to start.
  - The readiness and liveness probes use the Actuator, and every container has CPU and memory limits.
- **Ports:** each Service exposes the fixed development port, so only the names change between profiles (ADR-19).

## Alternatives considered

- **Secrets in the repository, encrypted with SOPS or Sealed Secrets** — gives stable secrets across machines, but requires managing encryption keys for a cluster that is created and destroyed locally.
- **Helm** — gives templates and values per environment, but adds a tool and a templating language for manifests that Kustomize already covers.
- **Images always from GHCR** — avoids the local build, but only tests the code that is already on `main`.
- **Letting Kubernetes restart the services until the Config Server starts** — it works, but the backoff delays startup by minutes and fills the events with false errors.

## Consequences

### Positive

- `./deploy.sh` creates everything from scratch and ends with a login through the Ingress.
- The repository holds no password or key of the cluster.
- Compose and k3d share the RabbitMQ topology and the creation of the databases.
- The CI validates the manifests and checks the probes and the limits.

### Negative

- The secrets live only in the cluster: deleting the cluster deletes them, and the data with them.
- The k3d Config Server reads `main` from GitHub; to test the configuration of a branch, you have to push it and use `CONFIG_LABEL`.
- A change to the topology ConfigMap only reaches RabbitMQ when the pod restarts.
