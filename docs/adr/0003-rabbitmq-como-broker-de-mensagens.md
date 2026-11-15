# ADR-03: RabbitMQ como broker de mensagens

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §9.6, §11

## Contexto

O sistema precisa de comandos com um único consumidor, eventos com vários consumidores, retries e dead-letter queues. Tudo corre num cluster k3d local, com um orçamento de cerca de 10 GB de memória. Reproduzir o histórico de eventos não é um requisito.

## Decisão

Usar RabbitMQ com Spring AMQP:

- um topic exchange `rentporto.events`, com routing key `{contexto}.{evento}` (ex.: `reservation.confirmed`);
- uma fila e uma DLQ por consumidor; no `booking-service`, cada módulo tem as suas próprias filas;
- uma fila dedicada por comando;
- 3 tentativas com backoff antes de a mensagem ir para a DLQ.

## Alternativas consideradas

- **Apache Kafka** — oferece replay e débito elevado, mas usa mais memória, não tem DLQ por mensagem nem semântica de comandos nativas e acrescenta trabalho operacional de que o projeto não precisa.

## Consequências

### Positivas

- Filas de comandos e DLQ nativas.
- Consola de gestão que mostra as filas e as DLQ durante a defesa.
- Pouca memória no k3d.

### Negativas

- Sem histórico de eventos reproduzível: reconstruir um read model obriga o serviço de origem a republicar o seu estado.
