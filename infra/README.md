# Local infrastructure

```
docker compose up -d --wait
./infra/verify.sh
```

Without a `.env`, the default passwords of `.env.example` are used.

| Component | Address | Credentials |
|---|---|---|
| PostgreSQL | `localhost:5432` | one database and one user per service (`auth`, `customer`, `fleet`, `booking`, `rental`, `notification`); the default password is the name |
| RabbitMQ | `localhost:5672`, console at http://localhost:15672 | `rentporto` / `rentporto` |
| Mailpit | SMTP at `localhost:1026`, inbox at http://localhost:8025; the SMTP does not use the default 1025, so that the emails do not end up in another Mailpit installed on the machine | — |
| Prometheus | http://localhost:9090, scrapes the applications running on the host and the RabbitMQ queues | — |
| Grafana | http://localhost:3000, with the Prometheus datasource and the RentPorto dashboard (http://localhost:3000/d/rentporto) provisioned | `admin` / `rentporto` |
| Zipkin | http://localhost:9411, receives every trace of the applications running on the host | — |
| WireMock | http://localhost:8089, stands in for another service in manual runs (for example, the deposit of the booking-service while it does not exist); mappings in `infra/wiremock/mappings` | — |

Ports of the services: gateway 8080, auth 8081, customer 8082, fleet 8083, booking 8084, rental 8085, notification 8086, config-server 8888.

## Config Server

The services read their configuration from the Config Server and do not start without it, so it starts first:

```
./mvnw -pl config-server spring-boot:run
```

In the `dev` profile (the default), the Config Server reads the local `config/` folder: a change applies when the service restarts, without a commit. In the `k8s` profile, it reads the `main` branch from GitHub (ADR-19).

## Gateway

The public API is at `http://localhost:8080/api/v1`: the gateway removes the `/api/v1` and routes each resource to the service that owns it (ADR-21). It starts after the Config Server, like the services; a stopped service answers 503.

The documentation of every service is on a single page, the Swagger UI of the gateway: http://localhost:8080/api/docs (on k3d, http://rentporto.local/api/docs). The service is chosen at the top; the internal endpoints do not appear, and "Try it out" sends the requests through the gateway, with the token from "Authorize" (ADR-27).

## auth-service

In the `dev` profile, the auth-service creates the administrator `admin@rentporto.pt` with the password `rentporto-admin` at startup, and signs the JWT with a key generated at that startup: after restarting it, you have to log in again. In `k8s`, the key and the administrator come from a Secret (ADR-20).

## Payments

There is no external payment provider: the `payment` module of the booking-service simulates it (ADR-29). The test card that the customer gives in the `paymentMethodId` of the reservation decides the answer to every operation of that reservation:

| Card | Answer |
|---|---|
| `card_ok` | approves everything |
| `card_declined` | declines everything (`CARD_DECLINED`) |
| `card_no_extra` | approves the charge and the deposit; declines the charge above the deposit, which stays outstanding |
| `card_slow` | does not answer within the timeout (`PROVIDER_UNAVAILABLE`); opens the `payment-provider` circuit breaker |
| any other | declines (`UNKNOWN_CARD`) |

## RabbitMQ

The topology (exchange, queues, DLQs and bindings) is in `rabbitmq/definitions.json` and is loaded at startup; the services do not declare queues. The `RabbitTopologyTest` of `common-events` checks that every routing key has a queue and that every queue has its DLQ. The user is separate, in `rabbitmq/users.json`, which only compose uses; on k3d it comes from a Secret (ADR-23).

## Kubernetes (k3d)

With Docker and k3d installed:

```
./deploy.sh                 # creates the cluster, builds and imports the images and waits until everything is ready
./deploy.sh --skip-build    # imports the images already built, without compiling
k3d cluster delete rentporto
```

`deploy.sh` creates the Secrets that do not exist yet, with random passwords and the JWT key, finishes with a login of the administrator through the Ingress and writes the administrator password to `docs/bruno/.env`, for the `k3d` environment of the Bruno collection (`docs/bruno/README.md`). The administrator password:

```
kubectl --context k3d-rentporto -n rentporto get secret auth-service -o jsonpath='{.data.AUTH_ADMIN_PASSWORD}' | base64 --decode
```

To open `http://rentporto.local` in the browser, add the two lines `127.0.0.1 rentporto.local` and `::1 rentporto.local` to `/etc/hosts`; without the second, macOS looks the `.local` name up by mDNS and every request takes a few seconds.

The Config Server of the cluster reads the `main` branch from GitHub (ADR-19). To test the configuration of a branch already pushed to GitHub: `CONFIG_LABEL=<branch> ./deploy.sh`. The RabbitMQ console and the Mailpit inbox open with `kubectl --context k3d-rentporto -n rentporto port-forward svc/rabbitmq 15672` and `port-forward svc/mailpit 8025`; the RabbitMQ password is in the `rabbitmq` Secret.

Prometheus scrapes every application pod and the RabbitMQ queues, and Grafana already has the Prometheus datasource and the RentPorto dashboard (ADR-24, ADR-26). They open with `port-forward svc/prometheus 9090` and `port-forward svc/grafana 3000`; the dashboard is at http://localhost:3000/d/rentporto, the Grafana user is `admin` and the password is in the `grafana` Secret. The Ingress only routes `/api`, so the metrics do not leave the cluster.

The RentPorto dashboard is in `infra/grafana/dashboards/rentporto.json` and is not edited in Grafana: the file is changed, by pull request. `infra/verify.sh` and `deploy.sh` check that it is provisioned and that Prometheus accepts all its queries.

The applications send every trace to the Zipkin of the cluster, which keeps them in memory (ADR-25): they are lost when the pod restarts. It opens with `port-forward svc/zipkin 9411`.

The booking-service scales by itself between 2 and 3 replicas with its CPU (ADR-28); `kubectl --context k3d-rentporto -n rentporto get hpa` shows the CPU and the replicas. `./infra/k8s/load-booking.sh` puts load on the booking-service, from inside the cluster, and shows it reaching 3 replicas; with `--scale-down`, it also waits until it goes back to 2, about 5 minutes after the load stops.

The manifests are in `infra/k8s`: the base uses the GHCR images and the `k3d` overlay the local images (ADR-23). `./infra/k8s/check-manifests.sh` checks that every container has probes and limits.

## Starting from scratch

`postgres/init.sh` only runs with an empty volume. After changing it:

```
docker compose down -v
```
