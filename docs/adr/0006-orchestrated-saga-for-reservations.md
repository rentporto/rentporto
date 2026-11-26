# ADR-06: Saga orquestrada na reserva, coreografia nas reações

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §9.2, §9.3

## Contexto

Criar uma reserva envolve a capacidade, a cotação e um pagamento num fornecedor externo. O processo precisa de compensações: pagamento recusado, fornecedor indisponível, pagamento não concluído em 15 minutos ou pagamento que chega depois de a reserva expirar. A chamada ao Stripe não pode ficar dentro da transação que bloqueia a capacidade, senão as linhas ficam presas enquanto se espera pelo fornecedor. Outros fluxos, como enviar notificações ou atualizar a frota depois de uma devolução, são reações simples, sem decisões a tomar.

## Decisão

O módulo `reservation` do `booking-service` orquestra as sagas de reserva e de cancelamento. O estado da saga é o estado da `Reservation` (`PENDING_PAYMENT`, `CONFIRMED`, `PAYMENT_FAILED`, `EXPIRED`, `CANCELLED`…).

Fluxo de criação:

1. Numa só transação: bloqueia a capacidade, valida e marca a cotação como usada, cria a reserva em `PENDING_PAYMENT` e regista o evento interno `ReservationPaymentRequested`.
2. Depois do commit, o módulo `payment` trata o evento e cria o pagamento no Stripe, num thread pool próprio (ADR-12).
3. O webhook do Stripe gera o evento interno `PaymentCompleted` ou `PaymentFailed`.
4. A reserva passa a `CONFIRMED` e o `ReservationConfirmed` é publicado no RabbitMQ pela outbox, ou passa a `PAYMENT_FAILED` e liberta a capacidade.

Um scheduler trata da expiração, do no-show e de mensagens perdidas. Com várias réplicas, os schedulers usam um lock partilhado (ShedLock sobre o PostgreSQL), para correrem numa só réplica. As reações simples (notificações, atualização da frota, criação do contrato) são coreografadas por eventos.

## Alternativas consideradas

- **Só coreografia** — espalha o estado da saga e torna as compensações mais difíceis de seguir e de testar.
- **Chamar o Stripe dentro do pedido de reserva** — segura os locks de capacidade enquanto espera pelo fornecedor e liga a latência da reserva à do Stripe.
- **Two-phase commit (2PC)** — não é suportado pelo broker nem pelo Stripe e bloqueia recursos enquanto espera.

## Consequências

### Positivas

- Estado da saga num só sítio, com compensações explícitas e testáveis.
- Como a saga é interna ao `booking-service`, as compensações não dependem do broker.
- As reações continuam desacopladas.

### Negativas

- O módulo `reservation` conhece os passos da saga.
- Exige schedulers com lock partilhado e cuidado com eventos que chegam tarde.
