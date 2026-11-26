# ADR-14: Montantes e arredondamento

- **Estado:** Proposta
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §1.5, §10.1; backlog RP-24

## Contexto

Os preços combinam tarifa diária, multiplicadores de época, desconto de duração, extras com teto, cobertura, taxas e IVA de 23%. Sem uma regra de arredondamento única, a mesma cotação pode dar totais que diferem num cêntimo entre a cotação, a reserva e o recibo.

## Decisão

- Todos os montantes são guardados e trocados como `Money` em cêntimos (`long amountCents`, moeda `EUR`).
- Os cálculos intermédios usam `BigDecimal`.
- Cada linha de preço (aluguer, cada extra, cobertura, cada taxa) é arredondada uma única vez ao cêntimo com `RoundingMode.HALF_UP`.
- O IVA é calculado sobre a soma das linhas arredondadas e arredondado uma única vez com `HALF_UP`.
- A cotação guarda todas as linhas, por isso a reserva e o recibo nunca recalculam.

## Alternativas consideradas

- **Arredondar apenas o total** — faz com que as linhas mostradas ao cliente não somem o total.
- **Arredondamento bancário (`HALF_EVEN`)** — é menos intuitivo para os clientes e para os casos de teste calculados à mão.

## Consequências

### Positivas

- Totais reproduzíveis, que coincidem ao cêntimo com os casos de teste calculados à mão.

### Negativas

- Todas as linhas têm de ser arredondadas no mesmo sítio, no módulo `pricing` do `booking-service`.
