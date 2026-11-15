# ADR-02: Comunicação híbrida: REST para decisões imediatas, mensagens para mudanças de estado

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §9.1, §9.4

## Contexto

Algumas interações precisam de uma resposta antes de quem pede poder continuar: o cliente espera que a reserva seja aceite, ou o agente espera ao balcão pela matrícula. Outras apenas propagam uma mudança de estado que pode interessar a outros contextos. Com os pagamentos online, um fornecedor externo entra no fluxo, e um fornecedor lento não pode bloquear a pesquisa nem as cotações.

## Decisão

Entre serviços:

- **REST síncrono (OpenFeign)** apenas quando quem chama precisa da resposta para decidir agora: o módulo `reservation` pede o perfil do condutor ao `customer-service`; no levantamento, o `rental-service` pede a atribuição da viatura ao `fleet-service` e a pré-autorização da caução ao `booking-service`.
- **Comandos** numa fila dedicada com um único consumidor quando um serviço pede a outro que faça algo: `SettleDeposit` (do `rental-service` para o `booking-service`).
- **Eventos** no topic exchange para tudo o que aconteceu: `ReservationConfirmed`, `VehicleReturned`, etc.

Dentro do `booking-service`, os módulos comunicam por chamadas às interfaces `application` e por eventos internos (ADR-15), não pelo RabbitMQ.

Todas as chamadas síncronas entre serviços têm timeout e circuit breaker (Resilience4j).

## Alternativas consideradas

- **Tudo por REST** — cria acoplamento temporal e falhas em cascata; um fornecedor de pagamentos lento prendia threads e parava a pesquisa.
- **Tudo assíncrono** — obriga o utilizador a consultar o estado de decisões que têm de ser tomadas de imediato, como a elegibilidade do condutor, e acrescenta complexidade sem benefício.

## Consequências

### Positivas

- Pouco acoplamento temporal, sem tornar assíncrono o que o utilizador espera ver de imediato.
- As falhas do fornecedor de pagamentos ficam contidas no módulo `payment`.

### Negativas

- Consistência eventual entre serviços.
- Exige mensagens fiáveis (ADR-07) e consumidores idempotentes.
