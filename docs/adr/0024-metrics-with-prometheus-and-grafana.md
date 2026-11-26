# ADR-24: Métricas com Prometheus e Grafana

- **Estado:** Aceite
- **Data:** 2026-11-17
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-16, ADR-21, ADR-23; backlog RP-14, RP-50, RP-51

## Contexto

Queremos métricas de todas as aplicações desde o início, no k3d e no compose, para as observar à medida que o sistema cresce e para os painéis do RP-51.

- **Réplicas:** o booking-service corre com 2 réplicas, por isso as métricas têm de ser recolhidas por pod e não pelo Service, que as misturaria.
- **Exposição:** as métricas revelam o funcionamento interno das aplicações, por isso não devem sair do cluster, e hoje o Ingress encaminha tudo para o gateway.
- **Recursos:** o cluster tem de caber nos cerca de 8 GB do Docker (ADR-16).

## Decisão

- **Endpoint das métricas:** cada aplicação expõe `/actuator/prometheus` com o Micrometer. As métricas levam a etiqueta `application` com o nome da aplicação.
- **Pull com descoberta de pods.**
  - No k3d, o Prometheus descobre os pods do namespace `rentporto` com `kubernetes_sd_configs` e raspa os que têm as anotações `prometheus.io/scrape`, `prometheus.io/port` e `prometheus.io/path`. Cada réplica é um target.
  - Um Role só de leitura de pods, no namespace, dá-lhe o acesso de que precisa.
- **Métricas dentro do cluster.**
  - O `/actuator/prometheus` não pede token, como o health.
  - O Ingress só encaminha `/api`, por isso não chega ao actuator do gateway.
- **Grafana provisionado por ficheiros:** a datasource Prometheus vem de um ficheiro de provisioning, partilhado pelo k3d e pelo compose. No k3d, a password do admin vem de um Secret que o `deploy.sh` gera (ADR-23), e o acesso faz-se por `kubectl port-forward`.
- **No compose:** também há Prometheus e Grafana, e o Prometheus raspa as aplicações que correm no host.
- **Sem o Prometheus Operator nem o kube-prometheus-stack:** um Deployment do Prometheus e um do Grafana chegam.

## Alternativas consideradas

- **kube-prometheus-stack (Helm)** — traz o operador, o Alertmanager, os exporters e dezenas de painéis, mas consome vários GB e acrescenta o Helm e CRDs a um cluster local com orçamento apertado.
- **Lista fixa de Services no Prometheus** — é mais simples, mas o Service distribui cada scrape por uma réplica diferente e as métricas das duas réplicas do booking misturam-se.
- **Porta de gestão separada** (`management.server.port`) — isola o actuator, mas obriga a mudar as probes, a segurança e os Services de todas as aplicações; com o Ingress limitado a `/api`, o resultado é o mesmo.

## Consequências

### Positivas

- Cada pod, incluindo cada réplica, é um target com o seu estado.
- Uma aplicação nova só precisa das três anotações para ser recolhida.
- O `deploy.sh` confirma que todas as aplicações estão UP e que a datasource do Grafana responde.

### Negativas

- As métricas guardam-se 3 dias num PVC de 2 GB; apagar o cluster apaga-as.
- Uma rota nova fora de `/api`, como o Swagger agregado do RP-52, obriga a acrescentar o caminho ao Ingress.
- No k3d, a exposição do endpoint vem do `config/` no `main` (ADR-19); antes do merge, só o Config Server tem as métricas expostas.
