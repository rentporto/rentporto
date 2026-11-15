# ADR-07: Transactional outbox e consumidores idempotentes

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §9.6

## Contexto

Um serviço que atualiza a sua base de dados e depois publica no broker pode falhar entre as duas escritas: perde o evento, ou publica um evento de uma alteração que foi revertida. O RabbitMQ entrega pelo menos uma vez, por isso um consumidor pode receber a mesma mensagem mais de uma vez. Os webhooks do Stripe também podem chegar repetidos.

## Decisão

- Cada serviço grava os eventos e comandos que saem para o RabbitMQ numa tabela `outbox`, na mesma transação da mudança de estado; um publisher envia-os para o broker.
- Os eventos internos entre módulos do `booking-service` são persistidos pelo registo de eventos do Spring Modulith e reprocessados se o serviço cair antes de os tratar (ADR-15).
- Cada consumidor regista o `eventId` em `processed_events` e ignora repetidos. Os webhooks do Stripe são deduplicados pelo id do evento do Stripe.
- Todas as mensagens externas usam o envelope comum: `eventId`, `type`, `version`, `occurredAt`, `correlationId`, `payload`. O `traceId` do Micrometer viaja nos headers.

## Alternativas consideradas

- **Publicar diretamente depois do commit** — perde o evento se o serviço cair entre o commit e a publicação.
- **Publicar antes do commit** — pode publicar eventos de alterações que nunca aconteceram.

## Consequências

### Positivas

- Nenhuma mensagem perdida ou inventada, entre serviços e entre módulos.
- Retries seguros em todo o sistema.

### Negativas

- Uma tabela e um publisher por serviço.
- Pequeno atraso entre o commit e a publicação.
- As tabelas `outbox`, `processed_events` e o registo de eventos precisam de limpeza periódica.
