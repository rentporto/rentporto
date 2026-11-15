# ADR-12: Stripe em modo de teste atrás de uma Anti-Corruption Layer, com um adaptador WireMock

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §5, §8.3, §9.2

## Contexto

O negócio quer pagamento antecipado online e uma caução cativada no cartão no levantamento. Na defesa é preciso mostrar pagamentos realistas e também falhas reproduzíveis (fornecedor lento ou indisponível). Como os Pagamentos vivem no mesmo serviço que a pesquisa e as cotações (ADR-01), um Stripe lento não pode consumir os recursos que servem esses pedidos.

## Decisão

O módulo `payment` do `booking-service` define uma porta `PaymentProvider` com dois adaptadores, escolhidos por configuração:

- **Stripe** em modo de teste, com o Java SDK; os webhooks chegam ao cluster através do Stripe CLI;
- **WireMock**, com latência e falhas configuráveis.

Os tipos do Stripe (`PaymentIntent`, `PaymentMethod`) nunca saem do adaptador.

As chamadas ao fornecedor correm num thread pool próprio e limitado (bulkhead), com timeout, 3 retries com backoff e chave de idempotência, e circuit breaker. Nunca usam as threads que servem `/availability` e `/quotes`.

## Alternativas consideradas

- **Apenas um gateway simulado** — é menos realista e não exercita os webhooks nem as pré-autorizações reais.
- **Pagamentos num serviço próprio** — isola a falha ao nível do processo, mas obriga a saga a atravessar o broker (ver ADR-01).

## Consequências

### Positivas

- Pagamentos online realistas, visíveis no dashboard do Stripe.
- Trocar de adaptador demonstra a Anti-Corruption Layer e os cenários de circuit breaker.
- Com o bulkhead, um fornecedor lento não afeta a pesquisa nem as cotações.

### Negativas

- O Stripe CLI é necessário no cluster, porque o k3d não é acessível a partir da internet.
- O isolamento é ao nível das threads, não do processo: uma falha grave na JVM afeta os três módulos.
- As pré-autorizações de cartão duram cerca de 7 dias, por isso os alugueres longos precisam de renovação.
- Sem 3-D Secure (ver ADR-13).
