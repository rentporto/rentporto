# Infraestrutura local

```
docker compose up -d --wait
./infra/verify.sh
```

Sem `.env`, usam-se as passwords por omissão do `.env.example`.

| Componente | Endereço | Credenciais |
|---|---|---|
| PostgreSQL | `localhost:5432` | uma base e um utilizador por serviço (`auth`, `customer`, `fleet`, `booking`, `rental`, `notification`); a password por omissão é o nome |
| RabbitMQ | `localhost:5672`, consola em http://localhost:15672 | `rentporto` / `rentporto` |
| Mailpit | SMTP em `localhost:1025`, caixa em http://localhost:8025 | — |
| Zipkin | http://localhost:9411 | — |
| WireMock | http://localhost:8089, mapeamentos em `infra/wiremock/mappings` | — |

Portas dos serviços: gateway 8080, auth 8081, customer 8082, fleet 8083, booking 8084, rental 8085, notification 8086, config-server 8888.

## Stripe

Copiar o `.env.example` para `.env`, pôr a chave de teste em `STRIPE_API_KEY` e arrancar com o perfil `stripe`:

```
docker compose --profile stripe up -d --wait
docker compose logs stripe-cli
```

O Stripe CLI reencaminha os webhooks para o booking-service em `localhost:8084`; o segredo do webhook (`whsec_...`) aparece no log.

## RabbitMQ

A topologia (exchange, filas, DLQ e bindings) está em `rabbitmq/definitions.json` e é carregada no arranque; os serviços não declaram filas. O `RabbitTopologyTest` do `common-events` confirma que cada routing key tem fila e que cada fila tem a sua DLQ.

## Recomeçar do zero

O `postgres/init.sh` só corre com o volume vazio. Depois de o alterar:

```
docker compose down -v
```
