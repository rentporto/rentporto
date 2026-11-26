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
| Mailpit | SMTP em `localhost:1026`, caixa em http://localhost:8025; o SMTP não usa o 1025 de origem para não ir parar a outro Mailpit instalado na máquina | — |
| Prometheus | http://localhost:9090, raspa as aplicações que correm no host e as filas do RabbitMQ | — |
| Grafana | http://localhost:3000, com a datasource Prometheus e o painel RentPorto (http://localhost:3000/d/rentporto) provisionados | `admin` / `rentporto` |
| Zipkin | http://localhost:9411, recebe todos os rastos das aplicações que correm no host | — |
| WireMock | http://localhost:8089, faz de outro serviço nas execuções manuais (por exemplo, a caução do booking-service enquanto não existe); mapeamentos em `infra/wiremock/mappings` | — |

Portas dos serviços: gateway 8080, auth 8081, customer 8082, fleet 8083, booking 8084, rental 8085, notification 8086, config-server 8888.

## Config Server

Os serviços leem a configuração do Config Server e não arrancam sem ele, por isso arranca-se primeiro:

```
./mvnw -pl config-server spring-boot:run
```

No perfil `dev` (por omissão), o Config Server lê a pasta `config/` local: uma alteração aplica-se ao reiniciar o serviço, sem commit. No perfil `k8s`, lê o ramo `main` do GitHub (ADR-19).

## Gateway

A API pública fica em `http://localhost:8080/api/v1`: o gateway remove o `/api/v1` e encaminha cada recurso para o serviço dono (ADR-21). Arranca depois do Config Server, como os serviços; um serviço parado responde 503.

A documentação de todos os serviços fica numa só página, o Swagger UI do gateway: http://localhost:8080/api/docs (no k3d, http://rentporto.local/api/docs). Escolhe-se o serviço no topo; os endpoints internos não aparecem, e o "Try it out" envia os pedidos pelo gateway, com o token do "Authorize" (ADR-27).

## auth-service

No perfil `dev`, o auth-service cria no arranque o administrador `admin@rentporto.pt` com a password `rentporto-admin`, e assina os JWT com uma chave gerada nesse arranque: depois de o reiniciar, é preciso fazer login de novo. No `k8s`, a chave e o administrador vêm de um Secret (ADR-20).

## Pagamentos

Não há fornecedor de pagamentos externo: o módulo `payment` do booking-service simula-o (ADR-29). O cartão de teste que o cliente indica no `paymentMethodId` da reserva decide a resposta em todas as operações dessa reserva:

| Cartão | Resposta |
|---|---|
| `card_ok` | aprova tudo |
| `card_declined` | recusa tudo (`CARD_DECLINED`) |
| `card_no_extra` | aprova a cobrança e a caução; recusa a cobrança acima da caução, que fica em dívida |
| `card_slow` | não responde dentro do timeout (`PROVIDER_UNAVAILABLE`); faz abrir o circuit breaker `payment-provider` |
| outro qualquer | recusa (`UNKNOWN_CARD`) |

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

O Prometheus raspa cada pod das aplicações e as filas do RabbitMQ, e o Grafana já tem a datasource Prometheus e o painel RentPorto (ADR-24, ADR-26). Abrem-se com `port-forward svc/prometheus 9090` e `port-forward svc/grafana 3000`; o painel fica em http://localhost:3000/d/rentporto, o utilizador do Grafana é `admin` e a password está no Secret `grafana`. O Ingress só encaminha `/api`, por isso as métricas não saem do cluster.

O painel RentPorto está em `infra/grafana/dashboards/rentporto.json` e não se edita no Grafana: muda-se o ficheiro, por pull request. O `infra/verify.sh` e o `deploy.sh` confirmam que está provisionado e que o Prometheus aceita todas as suas consultas.

As aplicações enviam todos os rastos para o Zipkin do cluster, que os guarda em memória (ADR-25): perdem-se quando o pod reinicia. Abre-se com `port-forward svc/zipkin 9411`.

O booking-service escala sozinho entre 2 e 3 réplicas pela CPU (ADR-28); `kubectl --context k3d-rentporto -n rentporto get hpa` mostra a CPU e as réplicas. `./infra/k8s/load-booking.sh` põe carga no booking-service, de dentro do cluster, e mostra-o a chegar às 3 réplicas; com `--scale-down`, espera também que volte a 2, uns 5 minutos depois de a carga parar.

Os manifestos estão em `infra/k8s`: a base usa as imagens do GHCR e o overlay `k3d` as imagens locais (ADR-23). `./infra/k8s/check-manifests.sh` confirma que todos os contentores têm probes e limites.

## Recomeçar do zero

O `postgres/init.sh` só corre com o volume vazio. Depois de o alterar:

```
docker compose down -v
```
