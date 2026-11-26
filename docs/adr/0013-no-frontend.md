# ADR-13: Sem frontend: interação pela API

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §1.6, §8.2

## Contexto

A cadeira avalia a arquitetura de microsserviços. O esforço disponível é de cerca de 28 dias-pessoa.

## Decisão

Não há frontend. Todos os atores usam a API através do Swagger UI agregado no gateway e de uma coleção Bruno com o fluxo completo de cada ator. Os pagamentos são confirmados no servidor com métodos de pagamento de teste do Stripe (`pm_card_visa`, `pm_card_chargeDeclined`).

## Alternativas consideradas

- **Frontend em React com TypeScript** — retiraria cerca de 3,5 dias-pessoa aos serviços core e à infraestrutura.

## Consequências

### Positivas

- Mais tempo para os serviços core, a resiliência e a observabilidade.

### Negativas

- Sem fluxo de 3-D Secure.
- O `paymentMethodId` no pedido é um atalho de teste: em produção viria do Stripe.js no browser, e os dados do cartão nunca passariam pela nossa API.
