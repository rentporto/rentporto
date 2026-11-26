# ADR-25: Rastreio distribuído com Zipkin

- **Estado:** Aceite
- **Data:** 2026-11-22
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-07, ADR-16, ADR-19, ADR-22, ADR-24; backlog RP-50

## Contexto

Um pedido atravessa o gateway, um ou mais serviços por REST e, pelo RabbitMQ, os consumidores dos eventos. Para diagnosticar latências e falhas, queremos ver esse percurso como um só rasto, com o mesmo traceId do princípio ao fim.

- **Publicação assíncrona:** com o outbox (ADR-07), a mensagem é gravada na transação do pedido e enviada mais tarde pelo publisher, noutra thread e sem o contexto do pedido. Sem cuidado, cada mensagem começa um rasto novo.
- **Chamadas entre serviços:** o gateway encaminha os pedidos e o rental-service chama o fleet-service e os pagamentos por Feign (ADR-22).
- **Orçamento:** o cluster local tem cerca de 10 GB de memória para tudo (ADR-16).

## Decisão

- **Brave e Zipkin:** cada aplicação usa o `spring-boot-starter-zipkin`, que no Spring Boot 4 traz o Micrometer Tracing com o Brave e o envio dos spans para o Zipkin. O nome de cada serviço no Zipkin é o `spring.application.name`.
- **Propagação W3C:** o contexto viaja no header `traceparent`, nos pedidos HTTP e nas mensagens AMQP. Os logs levam o traceId e o spanId.
- **Amostragem de 100 %:** `management.tracing.sampling.probability: 1.0` no `config/application.yml`, para todos os perfis. Com o valor por omissão, 10 %, a maior parte dos pedidos não ficava registada.
- **O outbox guarda o contexto do pedido:**
  - o OutboxWriter grava o `traceparent` do span corrente na coluna `headers` do outbox, que já existia;
  - o OutboxPublisher publica cada mensagem dentro de um span `outbox publish`, filho desse contexto;
  - a observação do RabbitTemplate põe o `traceparent` na mensagem, e a dos listeners continua o rasto no consumidor. As duas ficam ligadas pelos defaults da `common-messaging`.
- **Feign:** um serviço que chame outro por Feign junta o `feign-micrometer`, que passa o rasto na chamada.
- **Actuator fora dos rastos:** a `common-web` não observa os pedidos ao `/actuator` nem o que acontece enquanto os serve. As probes do Kubernetes e o Prometheus chamam o actuator de cada pod a cada 10 a 15 segundos, e os seus rastos esconderiam os pedidos dos utilizadores e encheriam a memória do Zipkin.
- **Zipkin em memória:**
  - no compose e no k3d, o Zipkin guarda os rastos em memória; no k3d, até 50 000 spans;
  - no k3d abre-se por `kubectl port-forward`, como o Grafana (ADR-24);
  - o `deploy.sh` confirma que o Zipkin tem o rasto do login através do gateway e do auth-service.
- **Testes como em produção:** as anotações de teste dos serviços ligam o tracing com `@AutoConfigureTracing` e amostragem de 100 %, sem o envio para o Zipkin. Sem a anotação, o Spring Boot desliga nos testes o envio e, com ele, a propagação.

## Alternativas consideradas

- **OpenTelemetry com o exportador para o Zipkin** — permite trocar de backend sem mudar as aplicações, mas traz o SDK do OpenTelemetry e mais configuração, para o mesmo resultado com um só backend.
- **TraceId no envelope dos eventos** — dispensa o header, mas mistura a observabilidade com o contrato dos eventos e obriga cada listener a continuar o rasto à mão. O envelope mantém o `correlationId`, que liga as mensagens do mesmo fluxo de negócio (ADR-07).
- **Publicar sem guardar o contexto** — é o mais simples, mas cada mensagem começa um rasto novo e perde-se a ligação entre o pedido e as reações aos seus eventos.
- **Zipkin com armazenamento persistente** (Elasticsearch, Cassandra ou MySQL) — guarda os rastos entre reinícios, mas mais um servidor não cabe no orçamento do cluster local.

## Consequências

### Positivas

- Um pedido vê-se no Zipkin com o mesmo traceId desde o gateway até aos consumidores das mensagens, com a espera no outbox à vista.
- Os logs de todos os serviços levam o traceId, por isso juntam-se os logs de um pedido.
- Um serviço novo só precisa da dependência: o outbox e o RabbitMQ já levam o rasto.

### Negativas

- Com 100 % de amostragem, cada pedido gera spans; com tráfego real, a amostragem teria de baixar.
- Os rastos perdem-se quando o Zipkin reinicia.
- Uma mensagem gravada fora de um pedido, por exemplo por um scheduler, começa um rasto próprio.
- Uma chamada Feign sem o `feign-micrometer` fica num rasto à parte.
- As métricas `http.server.requests` deixam de contar os pedidos ao actuator.
- No k3d, o endpoint do Zipkin e a amostragem vêm do `config/` no `main` (ADR-19); antes do merge, o cluster não os tem.
