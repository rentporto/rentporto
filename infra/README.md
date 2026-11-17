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
| Prometheus | http://localhost:9090, raspa as aplicações que correm no host | — |
| Grafana | http://localhost:3000, com a datasource Prometheus provisionada | `admin` / `rentporto` |
| Zipkin | http://localhost:9411 | — |
| WireMock | http://localhost:8089, mapeamentos em `infra/wiremock/mappings` | — |

Portas dos serviços: gateway 8080, auth 8081, customer 8082, fleet 8083, booking 8084, rental 8085, notification 8086, config-server 8888.

## Config Server

Os serviços leem a configuração do Config Server e não arrancam sem ele, por isso arranca-se primeiro:

```
./mvnw -pl config-server spring-boot:run
```

No perfil `dev` (por omissão), o Config Server lê a pasta `config/` local: uma alteração aplica-se ao reiniciar o serviço, sem commit. No perfil `k8s`, lê o ramo `main` do GitHub (ADR-19).

## Gateway

A API pública fica em `http://localhost:8080/api/v1`: o gateway remove o `/api/v1` e encaminha cada recurso para o serviço dono (ADR-21). Arranca depois do Config Server, como os serviços; um serviço parado responde 503.

## auth-service

No perfil `dev`, o auth-service cria no arranque o administrador `admin@rentporto.pt` com a password `rentporto-admin`, e assina os JWT com uma chave gerada nesse arranque: depois de o reiniciar, é preciso fazer login de novo. No `k8s`, a chave e o administrador vêm de um Secret (ADR-20).

## Stripe

Copiar o `.env.example` para `.env`, pôr a chave de teste em `STRIPE_API_KEY` e arrancar com o perfil `stripe`:

```
docker compose --profile stripe up -d --wait
docker compose logs stripe-cli
```

O Stripe CLI reencaminha os webhooks para o booking-service em `localhost:8084`; o segredo do webhook (`whsec_...`) aparece no log.

## RabbitMQ

A topologia (exchange, filas, DLQ e bindings) está em `rabbitmq/definitions.json` e é carregada no arranque; os serviços não declaram filas. O `RabbitTopologyTest` do `common-events` confirma que cada routing key tem fila e que cada fila tem a sua DLQ. O utilizador está à parte, em `rabbitmq/users.json`, que só o compose usa; no k3d vem de um Secret (ADR-23).

## Kubernetes (k3d)

Com Docker e k3d instalados:

```
./deploy.sh                 # cria o cluster, constrói e importa as imagens e espera que tudo fique ready
./deploy.sh --skip-build    # importa as imagens já construídas, sem compilar
k3d cluster delete rentporto
```

O `deploy.sh` cria os Secrets que ainda não existem, com passwords aleatórias e a chave dos JWT, termina com um login do administrador pelo Ingress e escreve a password do administrador em `docs/bruno/.env`, para o ambiente `k3d` da coleção Bruno (`docs/bruno/README.md`). A password do administrador:

```
kubectl --context k3d-rentporto -n rentporto get secret auth-service -o jsonpath='{.data.AUTH_ADMIN_PASSWORD}' | base64 --decode
```

Para abrir `http://rentporto.local` no browser, acrescentar ao `/etc/hosts` as duas linhas `127.0.0.1 rentporto.local` e `::1 rentporto.local`; sem a segunda, o macOS procura o nome `.local` por mDNS e cada pedido demora alguns segundos.

O Config Server do cluster lê o ramo `main` do GitHub (ADR-19). Para testar a configuração de um ramo já enviado para o GitHub: `CONFIG_LABEL=<ramo> ./deploy.sh`. A consola do RabbitMQ e a caixa do Mailpit abrem-se com `kubectl --context k3d-rentporto -n rentporto port-forward svc/rabbitmq 15672` e `port-forward svc/mailpit 8025`; a password do RabbitMQ está no Secret `rabbitmq`.

O Prometheus raspa cada pod das aplicações e o Grafana já tem a datasource Prometheus (ADR-24). Abrem-se com `port-forward svc/prometheus 9090` e `port-forward svc/grafana 3000`; o utilizador do Grafana é `admin` e a password está no Secret `grafana`. O Ingress só encaminha `/api`, por isso as métricas não saem do cluster.

Os manifestos estão em `infra/k8s`: a base usa as imagens do GHCR e o overlay `k3d` as imagens locais (ADR-23). `./infra/k8s/check-manifests.sh` confirma que todos os contentores têm probes e limites.

## Recomeçar do zero

O `postgres/init.sh` só corre com o volume vazio. Depois de o alterar:

```
docker compose down -v
```
