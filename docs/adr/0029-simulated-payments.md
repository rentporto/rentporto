# ADR-29: Pagamentos com um fornecedor simulado

- **Estado:** Aceite
- **Data:** 2026-11-26
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** substitui o ADR-12; ADR-01, ADR-02, ADR-06, ADR-07, ADR-13, ADR-15, ADR-16, ADR-17, ADR-26; backlog RP-36 a RP-43, RP-53

## Contexto

O ADR-12 pôs o Stripe em modo de teste atrás de uma porta `PaymentProvider`, com um segundo adaptador em WireMock. O sistema não processa pagamentos reais: o que interessa mostrar é o fluxo dos pagamentos (a cobrança antecipada na saga da reserva, a caução no levantamento, a sua liquidação e os reembolsos) e a resiliência perante um fornecedor lento, não a integração com um fornecedor concreto.

- **Custo do Stripe:** uma conta e chaves de teste para cada membro da equipa, o Stripe CLI a correr no cluster para os webhooks chegarem ao k3d (ADR-16), a verificação das assinaturas dos webhooks e a renovação das pré-autorizações dos alugueres longos.
- **Reprodutibilidade:** os cenários da demonstração (pagamento recusado, cobrança acima da caução recusada, fornecedor lento) têm de se repetir sempre da mesma maneira, sem depender de um serviço externo nem da rede.
- **Estado do código:** o módulo `payment` do booking-service ainda só tem os contratos da API; o rental-service e o notification-service já usam os contratos de pagamento (`POST /internal/deposits`, `SettleDeposit`, `DepositSettled`, `PaymentFailed`, `PaymentRefunded`), que não dependem do fornecedor.

## Decisão

- **Um fornecedor simulado:** o módulo `payment` do booking-service mantém a porta `PaymentProvider` (ADR-15), com um só adaptador, `SimulatedPaymentProvider`, que corre no próprio serviço. Não há Stripe, webhooks, Stripe CLI nem WireMock como fornecedor.
- **Cartões de teste:** o cliente indica no `paymentMethodId` da reserva um cartão de teste, que decide a resposta do fornecedor em todas as operações dessa reserva (cobrança, caução, cobrança acima da caução e reembolso):

  | Cartão | Resposta |
  |---|---|
  | `card_ok` | aprova tudo |
  | `card_declined` | recusa tudo (`CARD_DECLINED`) |
  | `card_no_extra` | aprova a cobrança e a caução; recusa a cobrança acima da caução, que fica em dívida (`outstanding` no `DepositSettled`) |
  | `card_slow` | não responde dentro do timeout (`PROVIDER_UNAVAILABLE`) |
  | outro qualquer | recusa (`UNKNOWN_CARD`) |

- **Cobrança da reserva:** o módulo `payment` trata o `ReservationPaymentRequested` (ADR-15), chama o fornecedor e publica o evento interno `PaymentCompleted` ou `PaymentFailed`. O resultado vem na resposta do fornecedor, sem webhook. Cada reserva é cobrada uma só vez, mesmo que o pedido chegue repetido.
- **Caução:** `POST /internal/deposits` retém a caução no cartão da reserva, sem débito. O `SettleDeposit` captura o devido, liberta o resto e cobra a diferença ao mesmo cartão; se essa cobrança for recusada, o `DepositSettled` indica o valor em dívida. A retenção não expira.
- **Reembolsos:** o fornecedor regista o reembolso total ou parcial e o módulo publica `PaymentRefunded`. Não há cobranças pendentes para cancelar, porque o fornecedor responde dentro do timeout; um pagamento concluído depois de a reserva expirar é reembolsado.
- **Resiliência:** as chamadas ao fornecedor correm num thread pool próprio e limitado (bulkhead), com timeout e um circuit breaker `payment-provider`, criado no arranque para aparecer no painel do Grafana (ADR-26). O `card_slow` faz abrir o circuit breaker, enquanto a pesquisa e as cotações continuam a responder. Não há retries: um fornecedor no próprio processo não tem falhas de rede transitórias.
- **O que muda nos outros ADRs** (o resto de cada um mantém-se):
  - **ADR-01 e ADR-02:** o fornecedor de pagamentos deixa de ser uma dependência externa; o bulkhead e o circuit breaker protegem a pesquisa e as cotações de um fornecedor lento simulado.
  - **ADR-06:** no passo 2 o pagamento é feito no fornecedor simulado, e no passo 3 o `PaymentCompleted` ou o `PaymentFailed` vêm da resposta do fornecedor, não de um webhook.
  - **ADR-07:** deixa de haver webhooks do Stripe a deduplicar.
  - **ADR-13:** os métodos de pagamento de teste passam a ser os cartões acima, em vez de `pm_card_visa` e `pm_card_chargeDeclined`.
  - **ADR-15:** a porta `PaymentProvider` isola o fornecedor simulado; trocá-lo por um fornecedor real só mexe num adaptador.
  - **ADR-16:** o cluster deixa de contar com o Stripe CLI e com o WireMock.
  - **ADR-17:** deixa de haver chaves do Stripe a proteger.

## Alternativas consideradas

- **Stripe em modo de teste (ADR-12)** — mostra pagamentos reais no dashboard do Stripe, mas obriga a contas, chaves, ao Stripe CLI no cluster e à renovação das pré-autorizações, e os cenários de falha dependem do Stripe e da rede.
- **WireMock como fornecedor externo** — dá latência e falhas por configuração, mas é mais um serviço a correr e a configurar, e as respostas teriam de ser montadas à mão para cada operação.
- **Aprovar sempre** — é o mais simples, mas perde os pagamentos recusados, a dívida na liquidação da caução e a demonstração do circuit breaker.
- **Falhas e latência por configuração** — dispensa os cartões de teste, mas um cenário da demonstração obriga a mudar a configuração e a reiniciar o serviço, e afeta todas as reservas ao mesmo tempo.

## Consequências

### Positivas

- Os cenários de pagamento repetem-se sempre da mesma maneira, sem rede, contas nem chaves.
- Menos infraestrutura: sem Stripe CLI, sem segredos do Stripe e sem webhooks a expor.
- A saga, a caução, os reembolsos e o circuit breaker continuam a demonstrar-se como antes.
- Os contratos que o rental-service e o notification-service já usam não mudam.

### Negativas

- Não se mostra uma integração com um fornecedor de pagamentos real, nem a verificação de webhooks.
- Os resultados dependem de cartões combinados entre nós, não do comportamento de um fornecedor real.
- O isolamento continua ao nível das threads, não do processo: uma falha grave na JVM afeta os três módulos do booking-service.
