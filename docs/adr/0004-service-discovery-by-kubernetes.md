# ADR-04: Discovery and load balancing by Kubernetes, without Eureka

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §8.3, §13; ADR-16

## Context

The target environment is a k3s cluster, created with k3d, which already provides Services with DNS names and load balancing between replicas through kube-proxy. Each extra JVM uses part of the memory budget. The same service names also resolve in docker-compose.

## Decision

The OpenFeign clients call the other services by the name of their Kubernetes Service (e.g. `http://booking-service:8084`). The docker-compose setup uses the same names. There is no Eureka server and no Spring Cloud LoadBalancer. The Config Server stays, for the external configuration.

## Alternatives considered

- **Eureka with Spring Cloud LoadBalancer** — duplicates what the cluster already does and adds one more JVM to run.

## Consequences

### Positive

- One less component to run and configure.
- The same service names in docker-compose and in k3d.

### Negative

- Load balancing happens on the server side (kube-proxy), not in the client.
- Running a service outside compose or the cluster requires setting the URLs explicitly.
