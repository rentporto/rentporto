# ADR-26: Painel do Grafana como código

- **Estado:** Aceite
- **Data:** 2026-11-23
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-03, ADR-16, ADR-24, ADR-25; backlog RP-51

## Contexto

O Prometheus já recolhe as métricas de todas as aplicações (ADR-24), mas não há painéis: para mostrar o comportamento do sistema, alguém teria de os montar à mão no Grafana, em cada cluster, e perdiam-se quando o pod reiniciasse.

- **O que mostrar:** a latência por endpoint, o estado dos circuit breakers, as filas e as DLQs do RabbitMQ, e os contratos e as reservas por estado.
- **Fontes:** a latência e os circuit breakers vêm das aplicações; as filas só o RabbitMQ as conhece; os estados do negócio estão nas bases de dados dos serviços.
- **Reservas:** o booking-service ainda não tem as reservas, por isso o painel tem de estar pronto antes de haver dados.

## Decisão

- **Painel como código:** um só painel, RentPorto, em `infra/grafana/dashboards/rentporto.json`, provisionado por ficheiro no compose e no k3d, como a datasource (ADR-24). Não se edita na UI; muda-se o ficheiro por pull request.
- **Latência:** o `http.server.requests` regista um histograma (`config/application.yml`), para o Prometheus calcular o p50, o p95 e o p99 de cada endpoint. A mesma métrica dá os pedidos por segundo e os erros 5xx.
- **Circuit breakers:** as métricas `resilience4j_circuitbreaker_*` do Micrometer. Um serviço cria os seus circuit breakers no arranque, para o painel os mostrar antes da primeira chamada.
- **Filas e DLQs:** o plugin `rabbitmq_prometheus`, que já vem ligado na imagem do RabbitMQ. O Prometheus lê as métricas de cada fila em `/metrics/detailed` (porta 15692), no compose e no k3d.
- **Negócio:**
  - cada serviço expõe um gauge por estado, que conta na base de dados quando o Prometheus o lê: `rentporto.rental.agreements` no rental-service e, quando houver reservas, `rentporto.booking.reservations` no booking-service;
  - com várias réplicas, todas contam a mesma base, por isso o painel usa `max by (status)`.
- **Verificação:** o `infra/verify.sh` e o `deploy.sh` confirmam que o painel está provisionado e pedem ao Prometheus cada consulta do painel, para apanhar um nome de métrica ou uma consulta errada.

## Alternativas consideradas

- **Painéis feitos na UI e exportados de vez em quando** — é mais rápido no início, mas o que está no cluster deixa de coincidir com o repositório e perde-se num deploy limpo.
- **Painéis da comunidade** (Spring Boot, RabbitMQ) — mostram dezenas de métricas genéricas, mas não os estados do negócio nem as DLQs, e dependem de nomes e etiquetas que não são os nossos.
- **Contadores atualizados pelos eventos** — evitam ler a base de dados, mas começam do zero quando o serviço reinicia e divergem entre réplicas. Um `COUNT` por estado a cada 15 segundos é barato e dá sempre o valor certo.
- **Grafonnet ou outro gerador de JSON** — torna o painel mais legível como código, mas acrescenta uma ferramenta e um passo de geração para um só painel.

## Consequências

### Positivas

- Um deploy limpo já tem o painel, igual no compose e no k3d.
- O painel mostra as filas sem consumidor e as mensagens nas DLQs, que de outra forma só se viam na consola do RabbitMQ.
- Um circuit breaker ou uma fila nova aparecem no painel sem mudar o JSON.
- O booking-service só tem de expor o gauge das reservas para o gráfico ficar com dados.

### Negativas

- O JSON do painel é longo e mudá-lo à mão pede cuidado; a verificação apanha as consultas inválidas, não os gráficos feios.
- Os histogramas multiplicam as séries do `http.server.requests` por cada intervalo de latência.
- Cada leitura do Prometheus faz uma consulta à base de dados por estado, em cada réplica.
- Até o booking-service expor as reservas, o gráfico delas fica sem dados.
