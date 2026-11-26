# ADR-24: Metrics with Prometheus and Grafana

- **Status:** Accepted
- **Date:** 2026-11-17
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-16, ADR-21, ADR-23; backlog RP-14, RP-50, RP-51

## Context

We want metrics from every application from the start, on k3d and in compose, to observe them as the system grows and to feed the dashboards of RP-51.

- **Replicas:** the booking-service runs with 2 replicas, so the metrics have to be collected per pod and not through the Service, which would mix them.
- **Exposure:** the metrics reveal the inner workings of the applications, so they must not leave the cluster, and today the Ingress routes everything to the gateway.
- **Resources:** the cluster has to fit in Docker's roughly 8 GB (ADR-16).

## Decision

- **Metrics endpoint:** each application exposes `/actuator/prometheus` with Micrometer. The metrics carry the `application` label with the application's name.
- **Pull with pod discovery.**
  - On k3d, Prometheus discovers the pods of the `rentporto` namespace with `kubernetes_sd_configs` and scrapes those with the `prometheus.io/scrape`, `prometheus.io/port` and `prometheus.io/path` annotations. Each replica is a target.
  - A Role that can only read pods, in the namespace, gives it the access it needs.
- **Metrics inside the cluster.**
  - `/actuator/prometheus` does not require a token, like health.
  - The Ingress only routes `/api`, so it does not reach the gateway's actuator.
- **Grafana provisioned from files:** the Prometheus datasource comes from a provisioning file, shared by k3d and compose. On k3d, the admin password comes from a Secret that `deploy.sh` generates (ADR-23), and access is through `kubectl port-forward`.
- **In compose:** there are also Prometheus and Grafana, and Prometheus scrapes the applications running on the host.
- **No Prometheus Operator or kube-prometheus-stack:** one Prometheus Deployment and one Grafana Deployment are enough.

## Alternatives considered

- **kube-prometheus-stack (Helm)** — brings the operator, Alertmanager, the exporters and dozens of dashboards, but uses several GB and adds Helm and CRDs to a local cluster on a tight budget.
- **A fixed list of Services in Prometheus** — it is simpler, but the Service sends each scrape to a different replica and the metrics of the two booking replicas get mixed.
- **A separate management port** (`management.server.port`) — isolates the actuator, but requires changing the probes, the security and the Services of every application; with the Ingress limited to `/api`, the result is the same.

## Consequences

### Positive

- Each pod, including each replica, is a target with its own status.
- A new application only needs the three annotations to be scraped.
- `deploy.sh` confirms that every application is UP and that the Grafana datasource responds.

### Negative

- The metrics are kept for 3 days in a 2 GB PVC; deleting the cluster deletes them.
- A new route outside `/api`, such as the aggregated Swagger of RP-52, requires adding the path to the Ingress.
- On k3d, the endpoint's exposure comes from `config/` on `main` (ADR-19); before the merge, only the Config Server has its metrics exposed.
