# ADR-18: Lock partilhado em JPA em vez do ShedLock

- **Estado:** Aceite
- **Data:** 2026-11-15
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-06, ADR-07; backlog RP-05, RP-60

## Contexto

Com várias réplicas, o publisher do outbox (ADR-07) e os schedulers da reserva (ADR-06) têm de correr numa só réplica de cada vez. O ADR-06 previa o ShedLock sobre o PostgreSQL, mas o fornecedor do ShedLock para PostgreSQL usa JDBC, e a persistência do projeto é feita em JPA.

## Decisão

- A biblioteca `common-messaging` tem um lock partilhado em JPA: a entidade `SchedulerLock`, na tabela `scheduler_lock`, guarda o nome do lock, até quando está ocupado e que réplica o tem.
- Uma réplica obtém o lock com um `UPDATE` em JPQL que só altera a linha se o lock estiver livre ou expirado. Se a linha ainda não existir, a réplica cria-a, e a chave primária impede que duas réplicas a criem ao mesmo tempo.
- No fim da tarefa, a réplica liberta o lock. Se cair antes, o lock expira ao fim do tempo máximo indicado.
- O publisher do outbox e os schedulers do booking-service usam o mesmo lock (`SchedulerLocks.runOnOneReplica`).
- Esta decisão substitui o ShedLock referido no ADR-06. O resto do ADR-06 mantém-se.

## Alternativas consideradas

- **ShedLock sobre o PostgreSQL** — é uma biblioteca testada, mas o fornecedor para PostgreSQL usa JDBC, ao lado da persistência em JPA.
- **`SELECT … FOR UPDATE SKIP LOCKED` nas linhas do outbox** — dispensa o lock global, mas as réplicas publicam por ordens diferentes e a transação fica aberta enquanto se espera pela confirmação do broker.
- **Lock pessimista numa linha, mantido durante a tarefa** — é o mais simples, mas prende uma ligação à base de dados durante todo o envio.

## Consequências

### Positivas

- Toda a persistência do projeto fica em JPA.
- As transações são curtas, e as mensagens do outbox saem pela ordem em que foram gravadas.
- Menos uma dependência.

### Negativas

- O código do lock é nosso e precisa de testes próprios.
- O prazo do lock usa o relógio da aplicação, por isso as réplicas têm de ter os relógios acertados; no k3d correm na mesma máquina.
