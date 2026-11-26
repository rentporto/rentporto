# ADR-26: Grafana dashboard as code

- **Status:** Accepted
- **Date:** 2026-11-23
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-03, ADR-16, ADR-24, ADR-25; backlog RP-51

## Context

Prometheus already collects the metrics of every application (ADR-24), but there are no dashboards: to show the system's behaviour, someone would have to build them by hand in Grafana, in each cluster, and they would be lost when the pod restarted.

- **What to show:** the latency per endpoint, the state of the circuit breakers, the RabbitMQ queues and DLQs, and the rental agreements and reservations by status.
- **Sources:** the latency and the circuit breakers come from the applications; only RabbitMQ knows the queues; the business states are in the services' databases.
- **Reservations:** the booking-service does not have reservations yet, so the dashboard has to be ready before there is data.

## Decision

- **Dashboard as code:** a single dashboard, RentPorto, in `infra/grafana/dashboards/rentporto.json`, provisioned from a file in compose and on k3d, like the datasource (ADR-24). It is not edited in the UI; the file is changed through a pull request.
- **Latency:** `http.server.requests` records a histogram (`config/application.yml`), so Prometheus can compute the p50, p95 and p99 of each endpoint. The same metric gives the requests per second and the 5xx errors.
- **Circuit breakers:** the Micrometer `resilience4j_circuitbreaker_*` metrics. A service creates its circuit breakers at startup, so the dashboard shows them before the first call.
- **Queues and DLQs:** the `rabbitmq_prometheus` plugin, which already comes enabled in the RabbitMQ image. Prometheus reads the metrics of each queue at `/metrics/detailed` (port 15692), in compose and on k3d.
- **Business:**
  - each service exposes a gauge per status, which counts in the database when Prometheus reads it: `rentporto.rental.agreements` in the rental-service and, once there are reservations, `rentporto.booking.reservations` in the booking-service;
  - with several replicas, all of them count the same database, so the dashboard uses `max by (status)`.
- **Verification:** `infra/verify.sh` and `deploy.sh` confirm that the dashboard is provisioned and run each of the dashboard's queries against Prometheus, to catch a wrong metric name or query.

## Alternatives considered

- **Dashboards built in the UI and exported now and then** — faster at first, but what is in the cluster stops matching the repository and is lost in a clean deploy.
- **Community dashboards** (Spring Boot, RabbitMQ) — show dozens of generic metrics, but not the business states or the DLQs, and depend on names and labels that are not ours.
- **Counters updated by the events** — avoid reading the database, but start from zero when the service restarts and diverge between replicas. A `COUNT` per status every 15 seconds is cheap and always gives the right value.
- **Grafonnet or another JSON generator** — makes the dashboard more readable as code, but adds a tool and a generation step for a single dashboard.

## Consequences

### Positive

- A clean deploy already has the dashboard, the same in compose and on k3d.
- The dashboard shows the queues without a consumer and the messages in the DLQs, which otherwise could only be seen in the RabbitMQ console.
- A new circuit breaker or queue shows up on the dashboard without changing the JSON.
- The booking-service only has to expose the reservations gauge for the chart to have data.

### Negative

- The dashboard's JSON is long and changing it by hand takes care; the verification catches invalid queries, not ugly charts.
- The histograms multiply the `http.server.requests` series by each latency bucket.
- Each Prometheus read runs one database query per status, on each replica.
- Until the booking-service exposes the reservations, their chart has no data.
