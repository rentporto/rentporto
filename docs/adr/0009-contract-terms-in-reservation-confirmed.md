# ADR-09: Termos contratuais congelados no ReservationConfirmed

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §7, §11.2

## Contexto

As tarifas mudam todas as semanas. Na devolução, o contrato de aluguer tem de ser liquidado com as condições acordadas na reserva: tarifa diária, extras, cobertura, franquia, caução, preço do litro e taxa de serviço.

## Decisão

Os `ContractTerms` são congelados no momento da reserva, a partir da cotação, e viajam no evento `ReservationConfirmed`. O `rental-service` guarda-os no contrato e faz a liquidação sem chamar o `booking-service` nem o `customer-service`.

## Alternativas consideradas

- **O Aluguer consulta as Tarifas na devolução** — aplica as tarifas de hoje a um contrato acordado no passado e acopla o `rental-service` ao `booking-service`.

## Consequências

### Positivas

- Segue a regra real do negócio: uma alteração de preço nunca afeta uma reserva existente.
- Menos acoplamento entre o Aluguer e as Tarifas.

### Negativas

- Eventos maiores e dados duplicados de propósito.
- Alterações ao esquema do evento exigem versionamento.
