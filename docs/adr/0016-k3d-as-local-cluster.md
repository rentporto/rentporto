# ADR-16: k3d as the local Kubernetes cluster

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §13; backlog RP-12, RP-55, RP-59

## Context

The system must run on Kubernetes on a local machine, and anyone who clones the repository must be able to rebuild it easily. The cluster must handle 6 Spring services, the gateway, the Config Server, PostgreSQL, RabbitMQ, Prometheus, Grafana, Zipkin, Mailpit, WireMock and the Stripe CLI within a budget of about 10 GB of memory. The proposal planned Minikube, but it is not mandatory.

## Decision

Use k3d, which runs k3s (a lightweight Kubernetes distribution) in Docker containers:

- `deploy.sh` creates the cluster with `k3d cluster create rentporto --agents 1 -p "80:80@loadbalancer"`;
- the locally built images enter the cluster with `k3d image import <image> -c rentporto`;
- the Ingress uses the Traefik bundled with k3s, without controller-specific annotations, and `rentporto.local` points to `127.0.0.1`;
- the PersistentVolumes use the `local-path` provisioner and the HPA uses the metrics-server, both bundled with k3s;
- the Kustomize manifests are the same ones another cluster would use; docker-compose is still used for day-to-day development.

## Alternatives considered

- **Minikube** — has more addons and documentation, but it is heavier (especially with a VM driver) and requires enabling the Ingress and the metrics-server.
- **kind** — equally lightweight, but without extra configuration it has no Ingress controller, no metrics-server and no port mapping to a load balancer.
- **Only docker-compose** — does not meet the requirement of deploying the system on Kubernetes.

## Consequences

### Positive

- Faster startup and less memory used by the cluster itself.
- Ingress, persistent storage and metrics-server available without configuration.
- The manifests remain portable to any Kubernetes cluster.

### Negative

- The memory limit is Docker's, not a cluster parameter; in Docker Desktop it must be set in the settings.
- The Ingress controller is Traefik, not NGINX: examples on the internet with NGINX annotations do not apply.
- k3s differs slightly from upstream Kubernetes in the bundled components, which does not affect this project.
- The proposal (section 13) and the README must reflect the change.
