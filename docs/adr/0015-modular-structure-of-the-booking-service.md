# ADR-15: Arquitetura hexagonal nos serviços e módulos do booking-service

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-01, ADR-06, ADR-07, ADR-12; backlog RP-09, RP-22

## Contexto

Cada serviço é um bounded context, exceto o `booking-service`, que junta três (ADR-01). Sem uma estrutura comum, cada serviço acabaria organizado à sua maneira, com as regras de negócio misturadas com o Spring, a base de dados e as chamadas a outros serviços. No `booking-service`, os três contextos acabariam a ler as tabelas uns dos outros e a partilhar entidades, que é exatamente o acoplamento do monolítico que a migração quer eliminar. O `booking-service` também tem de poder ser dividido mais tarde se a escala ou a equipa o justificarem.

## Decisão

### Camadas e regra de dependências

Cada serviço segue uma arquitetura hexagonal com quatro camadas:

```
pt/rentporto/{serviço}/
├── api/             # adaptador de entrada: contrato XxxApi, controllers, records de request e response (RP-06)
├── application/     # casos de uso transacionais e portas de saída (interfaces)
├── domain/          # agregados, value objects, portas dos repositórios e exceções de negócio
└── infrastructure/  # adaptadores de saída, um subpacote por tecnologia: persistence, security, messaging, client
```

- **Domínio:** Java sem Spring. As anotações `jakarta.persistence` são permitidas, como metadados de mapeamento; não há um modelo de persistência separado.
- **Aplicação:** orquestra o domínio e fala com o exterior só através de portas que ela própria define (por exemplo `PasswordHasher`, `DriverProfiles`, `CustomerEvents`). Pode usar `@Service` e `@Transactional`, mas não o Spring Data, o Spring Security, o Spring Web, o AMQP nem o Feign.
- **API e infraestrutura:** implementam os adaptadores. Nenhuma camada depende delas.
- **Dependências:** apontam para dentro: `api` e `infrastructure` → `application` → `domain`.
- **Teste:** um teste ArchUnit em cada serviço verifica estas regras.

### Convenções do domínio

- **Value objects** para os conceitos com regras (`Email`, `TaxNumber`, `Plate`, `Money`, `RentalPeriod`): validam e normalizam no construtor e são `@Embeddable` no JPA.
- **Agregados com comportamento:**
  - sem setters;
  - nascem por fábricas que impedem um estado inválido;
  - mudam de estado por métodos que validam a transição (por exemplo `vehicle.sendToMaintenance()`);
  - referem outros agregados só pelo id.
- **Outros serviços atrás de uma porta:** a aplicação define a interface, e um adaptador na infraestrutura (Feign com Resilience4j) traduz o DTO do outro serviço para o modelo próprio, como a Anti-Corruption Layer da ADR-12.
- **Eventos de integração só nos adaptadores:** o domínio não usa o `common-events`. Um adaptador traduz o estado do agregado para o contrato e escreve-o no outbox, na mesma transação (ADR-07).

### Módulos do booking-service

- O código está dividido em três pacotes de topo, um por contexto, cada um com as mesmas quatro camadas:

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
- Um teste com `ApplicationModules.of(BookingApplication.class).verify()` falha se alguma regra entre módulos for violada; o teste ArchUnit verifica as camadas dentro de cada módulo.
- Cada módulo tem as suas próprias filas no RabbitMQ.

O Spring Modulith usado é o 2.1.x, a linha compatível com o Spring Boot 4.1. Foi verificado com Java 25: o Spring Modulith 2.1.2 usa o ArchUnit 1.4.2, que lê classes de Java 25, e o `ApplicationModules.verify()` deteta violações entre módulos compilados com `--release 25`.

## Alternativas consideradas

- **Pastas por tipo (`controller`, `service`, `entity`, `repository`, `dto`)** — é a estrutura mais conhecida, mas tende a pôr as regras de negócio nos serviços, a deixar as entidades sem comportamento e a misturar o domínio com o Spring.
- **Pacotes por funcionalidade** (ex.: `account`, `token`, `security`) — agrupa o que muda junto e permite classes package-private, mas não separa o domínio da infraestrutura.
- **Arquitetura hexagonal pura, com modelo de persistência separado** — o domínio fica também livre do JPA, mas obriga a uma entidade JPA e a mappers nos dois sentidos por cada agregado, com pouco ganho enquanto os dois modelos forem iguais.
- **Pacotes sem regras verificadas** — as fronteiras degradam-se à primeira pressão de prazo.
- **Um submódulo Maven por contexto dentro do booking-service** — dá um isolamento mais forte em tempo de compilação, mas complica o build e o pipeline para pouco ganho com duas pessoas.

## Consequências

### Positivas

- As regras de negócio testam-se sem Spring, sem base de dados e sem os outros serviços; nos testes da aplicação, uma porta troca-se por uma implementação falsa.
- Trocar uma tecnologia (o Feign, o Stripe, o algoritmo das palavras-passe) só mexe num adaptador.
- Uma só estrutura em todo o repositório, verificada no CI e não só na revisão.
- Separar um módulo do booking-service num serviço próprio é trocar a interface `application` por um cliente remoto e os eventos internos por mensagens, sem reescrever o domínio.
- O registo de eventos garante que um evento interno não se perde se o serviço cair.

### Negativas

- Mais interfaces e adaptadores: uma porta por cada dependência externa.
- As classes usadas entre camadas têm de ser públicas.
- O domínio continua a ter as anotações do JPA, por isso uma mudança no mapeamento pode tocar nas classes do domínio.
- Mais uma dependência (Spring Modulith) e uma tabela de registo de eventos no booking-service.
- Exige disciplina na revisão dos pull requests, além dos testes de verificação.
