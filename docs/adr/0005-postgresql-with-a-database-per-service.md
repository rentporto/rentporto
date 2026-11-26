# ADR-05: Uma instância PostgreSQL com uma base lógica e um utilizador por serviço

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §8.3, §13.1

## Contexto

É obrigatório ter uma base de dados por serviço, para que nenhum serviço leia os dados de outro. Correr seis instâncias PostgreSQL no k3d gastaria uma grande parte do orçamento de memória.

## Decisão

Correr um único StatefulSet de PostgreSQL. Um script de arranque cria seis bases de dados e seis utilizadores, um por serviço, e cada utilizador só tem privilégios sobre a sua base. Cada serviço gere o seu esquema com Flyway.

A base `booking` tem um esquema por módulo (`reservation`, `pricing`, `payment`), sem chaves estrangeiras nem joins entre esquemas (ADR-15).

## Alternativas consideradas

- **Uma instância PostgreSQL por serviço** — dá isolamento físico, mas custa demasiada memória no k3d.
- **Esquema partilhado** — traz de volta o acoplamento do monolítico.

## Consequências

### Positivas

- Isolamento dos dados garantido pelos privilégios da base de dados.
- Pouco consumo de memória.
- Os esquemas por módulo permitem separar o `booking-service` mais tarde mudando apenas a ligação.

### Negativas

- O isolamento é lógico, não físico; a instância é um ponto único de falha.
- Em produção, cada serviço teria a sua própria instância.
