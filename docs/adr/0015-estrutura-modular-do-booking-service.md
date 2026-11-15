# ADR-15: Estrutura modular do booking-service

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-01, ADR-06, ADR-07; backlog RP-22

## Contexto

O `booking-service` junta três bounded contexts (ADR-01). Sem regras explícitas, os três contextos acabariam a ler as tabelas uns dos outros e a partilhar entidades, que é exatamente o acoplamento do monolítico que a migração quer eliminar. O serviço também tem de poder ser dividido mais tarde se a escala ou a equipa o justificarem.

## Decisão

- O código está dividido em três pacotes de topo, um por contexto, cada um com as camadas `api`, `application`, `domain` e `infrastructure`:

```
booking-service/src/main/java/pt/rentporto/booking/
├── reservation/
├── pricing/
├── payment/
└── shared/        # outbox, messaging config
```

- Um módulo só usa outro através da interface pública da camada `application` (`PricingApi`, `PaymentApi`). Nunca acede a repositórios, entidades ou controllers de outro módulo.
- Cada módulo tem o seu esquema na base `booking`, com as suas migrações Flyway, sem chaves estrangeiras nem joins entre esquemas. Uma operação pode abranger dois módulos na mesma transação apenas através dessas interfaces (ex.: `PricingApi.markQuoteUsed` dentro da criação da reserva).
- Os módulos reagem uns aos outros por eventos internos (`ReservationPaymentRequested`, `PaymentCompleted`, `PaymentFailed`), tratados depois do commit com `@ApplicationModuleListener` e persistidos pelo registo de eventos do Spring Modulith.
- Um teste com `ApplicationModules.of(BookingApplication.class).verify()` falha se alguma regra for violada.
- Cada módulo tem as suas próprias filas no RabbitMQ.

O Spring Modulith usado é o 2.1.x, a linha compatível com o Spring Boot 4.1. Foi verificado com Java 25: o Spring Modulith 2.1.2 usa o ArchUnit 1.4.2, que lê classes de Java 25, e o `ApplicationModules.verify()` deteta violações entre módulos compilados com `--release 25`.

## Alternativas consideradas

- **Pacotes sem regras verificadas** — as fronteiras degradam-se à primeira pressão de prazo.
- **Um submódulo Maven por contexto dentro do serviço** — dá um isolamento mais forte em tempo de compilação, mas complica o build e o pipeline para pouco ganho com duas pessoas.

## Consequências

### Positivas

- As fronteiras dos bounded contexts continuam visíveis no código e são verificadas no CI.
- Separar um módulo num serviço próprio é trocar a interface `application` por um cliente remoto e os eventos internos por mensagens, sem reescrever o domínio.
- O registo de eventos garante que um evento interno não se perde se o serviço cair.

### Negativas

- Mais uma dependência (Spring Modulith) e uma tabela de registo de eventos.
- Exige disciplina na revisão dos pull requests, além do teste de verificação.
