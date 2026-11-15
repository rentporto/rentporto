# ADR-10: Reservar por categoria e atribuir a viatura no levantamento

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §1.5, §9.5

## Contexto

As empresas de aluguer vendem uma categoria de viatura (ex.: Grupo B – Económico), não uma matrícula. O principal problema atual é o overbooking no aeroporto no verão.

## Decisão

Uma reserva ocupa uma `VehicleCategory` numa estação durante um período de aluguer. A disponibilidade é uma contagem por estação, categoria e dia: a capacidade menos as reservas ativas que ocupam a estação. O `fleet-service` atribui uma viatura concreta, de forma atómica, no levantamento.

## Alternativas consideradas

- **Reservar uma matrícula específica** — faz a disponibilidade depender de viaturas individuais e falha quando uma viatura vai para manutenção.

## Consequências

### Positivas

- A disponibilidade é uma contagem simples, verificada com lock pessimista.
- A frota pode ser reorganizada sem mexer nas reservas.

### Negativas

- A atribuição da viatura na Frota tem de ser atómica e idempotente.
- A vista de capacidade pode atrasar-se brevemente; a regra one-way faz com que erre para uma viatura a menos, nunca a mais.
