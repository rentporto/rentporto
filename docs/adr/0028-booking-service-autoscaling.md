# ADR-28: Autoscaling of the booking-service

- **Status:** Accepted
- **Date:** 2026-11-23
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-15, ADR-16, ADR-18, ADR-21, ADR-23; backlog RP-54

## Context

We want to show that one service scales without the others. The booking-service receives the search, the quotes and the reservations, so it is the one that will have the most load; it already runs with 2 replicas, with the schedulers on a shared lock (ADR-18).

- **Metrics:** k3s, underneath k3d, already ships the metrics-server (ADR-16), so Kubernetes knows the CPU of each pod without installing anything.
- **JVM startup:** a Spring service uses close to one CPU during its first seconds, which is not load.
- **Budget:** the local cluster has about 10 GB of memory for everything (ADR-16); each booking-service replica requests 384 MiB and can reach 768 MiB.
- **Gateway:** it limits each client's requests (ADR-21), so it is no good for generating load.

## Decision

- **HorizontalPodAutoscaler on CPU:** between 2 and 3 booking-service replicas, to keep the average CPU below 70 % of what each pod requests (100m).
- **Stabilisation:**
  - it scales up one replica at a time, only if the CPU stays above the target for 60 seconds;
  - it scales down after 5 minutes without load.
  - Kubernetes ignores the CPU of pods that are not ready yet, and the 60-second window covers the rest of the JVM startup.
- **The manifest keeps 2 replicas**, the same as the HPA minimum, so that a deploy never works against the HPA at rest. `check-manifests.sh` confirms that each HPA scales a Deployment that requests CPU and that starts at the minimum.
- **Verification:**
  - `deploy.sh` confirms that the HPA measures the CPU and that, without load, it stays at the minimum;
  - `infra/k8s/load-booking.sh` generates load from a busybox pod inside the cluster, straight at the booking-service Service, and shows the HPA reaching 3 replicas.

## Alternatives considered

- **Scaling on memory** — the JVM does not give memory back when the load drops, so the service would never scale down again.
- **Custom metrics** (requests per second, through the Prometheus Adapter) — measure demand better, but add one more component to the cluster for a single HPA.
- **KEDA or the length of the RabbitMQ queues** — suit message consumers, not a service that answers HTTP requests.
- **Load through the gateway** — it is the clients' real path, but rate limiting would answer 429 before the booking-service warmed up.
- **More replicas at the maximum** — would show more scaling, but each replica takes up to 768 MiB of the cluster's budget.

## Consequences

### Positive

- The booking-service scales by itself with the load and goes back to 2 replicas when the load passes, without touching the other services.
- Deploys and restarts do not make the HPA scale.
- The demonstration runs with a single script, with no tools outside the cluster.

### Negative

- The 70 % target of 100m is low for a Java service: it scales quickly, which suits the demonstration, but with real traffic the CPU request would have to go up.
- While the booking-service endpoints answer "not implemented", the load is on `/v3/api-docs`; once there are quotes, the load should be on them.
- A `kubectl apply` with the HPA above the minimum resets the service to 2 replicas until the HPA decides again.
