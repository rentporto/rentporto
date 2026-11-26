# ADR-30: Organização dos pacotes por tipo de ficheiro

- **Estado:** Aceite
- **Data:** 2026-11-26
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-08, ADR-15; backlog RP-06

## Contexto

O ADR-15 divide cada serviço nas camadas `api`, `application`, `domain` e `infrastructure`. Até aqui, dentro de cada camada, tudo ficava numa só pasta:

- **Tamanho:**
  - o `domain` do fleet-service tinha 33 ficheiros, 15 deles exceções;
  - o `domain` do rental-service tinha 22 e o do customer-service 13;
  - o `api` do fleet-service tinha 23 e o do módulo `pricing` do booking-service 18.
- **Mistura:**
  - no `domain` estavam os agregados, os value objects, as portas dos repositórios e as exceções;
  - na `application` estavam os casos de uso, as portas, as exceções e as tarefas de arranque;
  - no `api` estavam o contrato da API, a sua implementação e os dados que entram e saem.
- **Bibliotecas comuns:**
  - a raiz da `common-web` juntava o formato dos erros, a configuração do OpenAPI e a das observações;
  - a raiz da `common-events` juntava o envelope, os value objects partilhados e os nomes da topologia do RabbitMQ.
- **Testes:** os containers, as anotações dos testes de integração e os dados de teste estavam na raiz, misturados com os testes.
- **Repetição:**
  - a classe que lê do JWT a conta do pedido (`AuthenticatedUser`) estava copiada, com pequenas diferenças, em três serviços;
  - a configuração do relógio de Lisboa (`ClockConfiguration`) estava igual em quatro.

## Decisão

### Serviços

- **Uma pasta por tipo de ficheiro em cada camada**, igual em todos os serviços e nos módulos do booking-service:

  ```
  pt/rentporto/{serviço ou módulo}/
  ├── api/
  │   ├── contract/      # XxxApi: interfaces com os paths, os papéis (@PreAuthorize) e o OpenAPI
  │   ├── controller/    # XxxController e XxxAdvice, que implementam os contratos
  │   ├── request/       # XxxRequest: corpos dos pedidos
  │   ├── response/      # XxxResponse: corpos das respostas
  │   └── model/         # enums e records usados dentro dos pedidos e das respostas
  ├── application/
  │   ├── service/       # casos de uso e tarefas de arranque
  │   ├── port/          # interfaces que a aplicação usa e a infraestrutura implementa
  │   ├── exception/     # exceções da aplicação
  │   └── model/         # records que passam pelos casos de uso e pelas portas
  ├── domain/
  │   ├── model/         # agregados, entidades, value objects e enums
  │   ├── repository/    # portas dos repositórios
  │   ├── service/       # regras sem estado que não pertencem a um agregado
  │   └── exception/     # exceções do domínio
  └── infrastructure/    # uma pasta por tecnologia: persistence, messaging, security, client, mail, ...
  ```

- **O que vai para cada pasta:**
  - **`api`:** o sufixo diz a pasta. `Api` vai para `contract`, `Controller` e `Advice` para `controller`, `Request` para `request` e `Response` para `response`. O resto vai para `model`.
  - **`application`:** as exceções vão para `exception`, as interfaces para `port` e os records que passam pelas portas para `model`. O resto vai para `service`.
  - **`domain`:** as exceções vão para `exception`, as interfaces dos repositórios para `repository` e as regras sem estado, como o cálculo dos encargos da devolução (`ReturnCharges`), para `service`. O resto vai para `model`.
  - **Pastas e tipos aninhados:**
    - não há classes na raiz de uma camada;
    - uma pasta só existe quando tem ficheiros;
    - os tipos aninhados ficam com a sua classe, por exemplo o `Mailer.Email` fica com a porta `Mailer`.
- **Visibilidade:**
  - os controllers continuam visíveis só no seu pacote;
  - os métodos `from(...)` das respostas passam a públicos, porque o controller está noutro pacote. O ADR-15 já impede as outras camadas de usar o `api`;
  - nada mais muda de visibilidade. Os construtores dos casos de uso e as tarefas de arranque (`AdminBootstrap`, `ExampleCategories`) ficam visíveis só no pacote `service`, junto dos testes que os usam.

### Bibliotecas comuns

- **`common-web` por área.** A raiz fica só com o `CommonWebAutoConfiguration`:
  - `error`: `BusinessException`, `EndpointNotImplementedException` e `ProblemDetailsAdvice`;
  - `openapi`: `OpenApiContract` e `WebDefaults`;
  - `observation`: `NoActuatorObservations`;
  - `security`: a configuração do resource server e o `AuthenticatedUser`;
  - `time`: o `TimeAutoConfiguration`.
- **`AuthenticatedUser`** em `pt.rentporto.web.security`: uma só classe, com `subject()`, `id()`, `email()` e `hasRole()`, lida do JWT (ADR-08). Substitui as três cópias.
- **`TimeAutoConfiguration`:** dá a todos os serviços um `Clock` com a hora de Lisboa e substitui os quatro `ClockConfiguration`. Um serviço ou um teste pode declarar o seu próprio `Clock`.
- **`common-events`:**
  - os nomes da topologia do RabbitMQ (`Exchanges`, `RoutingKeys`, `CommandQueues`) vão para `topology`;
  - o `EventEnvelope`, o `Money` e o `RentalPeriod` ficam na raiz;
  - os eventos continuam numa pasta por contexto;
  - as mensagens e a topologia não mudam: mudam os pacotes das constantes, não os seus valores.
- **Sem camadas:** a `common-messaging` (`consumer`, `lock`, `outbox`) e o gateway (`docs`, `ratelimit`, `routing`, `security`) continuam organizados por funcionalidade.

### Testes

- **Cada teste fica no pacote da classe que testa**, para poder usar o que é visível só no pacote:
  - os testes dos endpoints vão para `api/controller`;
  - os testes dos casos de uso e as suas implementações falsas das portas vão para `application/service`;
  - os testes do domínio vão para `domain/model` ou `domain/service`;
  - os testes da infraestrutura ficam na pasta da tecnologia.
- **O `OpenApiContractTest` fica na raiz do `api`**, porque cobre a API do serviço toda. No booking-service fica na raiz do serviço, porque cobre os três módulos.
- **Os helpers partilhados pelos testes vão para `{serviço}/testing`**, como já faziam a `common-web` e a `common-messaging`. São os containers, as anotações `@XxxServiceTest` e os dados de teste.
- **Na raiz ficam só os testes do serviço inteiro:** `ArchitectureTest`, `AuthorizationTest` e `XxxApplicationTests`.

### Verificação automática

As `ArchitectureRules` da `common-web`, que todos os serviços já correm, verificam que:

- no `api`, na `application` e no `domain` não há classes fora das pastas acima, e na raiz da `infrastructure` não há classes;
- no `api`, cada classe está na pasta do seu sufixo, e só os controllers dependem dos controllers;
- as exceções estão só nas pastas `exception`, e nessas pastas só há exceções;
- as pastas `port` e `repository` só têm interfaces, e a `application/service` não tem nenhuma.

O resto do ADR-15 mantém-se.

## Alternativas consideradas

- **Uma pasta por recurso ou agregado em cada camada** (por exemplo `vehicle`, `transfer` e `category`) — junta os ficheiros que mudam juntos e deixa mais classes visíveis só no pacote. Mas cada pasta volta a misturar tipos de ficheiro, e os tipos partilhados por vários recursos precisam de mais uma pasta. A equipa preferiu ver de relance o que é contrato, porta, caso de uso, entidade ou exceção.
- **Manter uma pasta por camada** — não dá trabalho, mas as pastas maiores continuam a crescer com cada funcionalidade nova.
- **Separar só as exceções** — é a pasta que mais cresce no fleet-service, mas as portas e os repositórios ficariam misturados com os casos de uso e as entidades.
- **Mappers à parte** (por exemplo MapStruct) em vez dos `from(...)` nas respostas — mais uma dependência e mais uma pasta para conversões de uma linha.

## Consequências

### Positivas

- Cada pasta tem um só tipo de ficheiro e o mesmo nome em todos os serviços. As portas, os repositórios, as exceções ou o contrato de qualquer serviço encontram-se no mesmo sítio.
- O contrato da API fica separado da sua implementação, e as portas da aplicação ficam separadas dos casos de uso.
- A organização é verificada nos testes de cada serviço, por isso não se perde com o tempo.
- O `AuthenticatedUser` e o relógio de Lisboa têm uma só versão, testada uma vez.
- Os helpers dos testes ficam separados dos testes.

### Negativas

- Uma funcionalidade mexe em várias pastas: por exemplo, uma regra nova pode tocar no agregado, na exceção e no caso de uso.
- Há mais imports entre as pastas da mesma camada.
- Os `from(...)` das respostas ficam públicos.
- Um teste muda de pasta quando a classe que testa muda, para continuar no mesmo pacote.
