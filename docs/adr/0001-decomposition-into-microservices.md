# ADR-01: Decomposição em microsserviços por bounded context

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §3, §5, §7

## Contexto

O sistema atual é um monolítico modular com oito módulos sobre uma única base de dados PostgreSQL. Os módulos leem e escrevem nas tabelas uns dos outros, por isso não podem ser escalados nem instalados separadamente. Um Event Storming simplificado e a análise da linguagem ubíqua identificaram oito bounded contexts. O sinal de fronteira mais forte foi a polissemia: «viatura» e «cliente» querem dizer coisas diferentes na Frota, nas Reservas, no Aluguer, nas Tarifas e nos Pagamentos.

Reservas, Tarifas e Pagamentos participam no mesmo fluxo: cotação, reserva e pagamento antecipado. Com um serviço por contexto, cada reserva atravessaria três processos, com chamadas síncronas, comandos e compensações entre eles. O argumento para instalar as Tarifas à parte (as tarifas mudam todas as semanas) também não obriga a um serviço próprio: as tarifas são dados alterados pela API, não código, por isso mudam sem novo deploy. A equipa tem duas pessoas e o cluster local tem um orçamento de cerca de 10 GB de memória.

## Decisão

Os oito bounded contexts ficam em seis microsserviços. Cada serviço é dono dos seus dados:

| Service | Bounded contexts |
|---|---|
| `auth-service` | Identity and Access |
| `customer-service` | Customers |
| `fleet-service` | Fleet |
| `booking-service` | Reservations, Pricing, Payments |
| `rental-service` | Rental |
| `notification-service` | Notifications |

Dentro do `booking-service`, cada contexto é um módulo (`reservation`, `pricing`, `payment`) com fronteiras verificadas por testes e um esquema próprio na base de dados (ADR-15). A inspeção e os danos ficam dentro do `rental-service`, e os recibos dentro do módulo `payment`, porque partilham o ciclo de vida do respetivo agregado. A faturação certificada pela AT fica no monolítico residual e está fora de âmbito.

## Alternativas consideradas

- **Um serviço por bounded context (8 serviços)** — isola os Pagamentos e permite instalar as Tarifas sozinhas, mas cada reserva atravessa três processos, com chamadas remotas, comandos e compensações, e há mais duas JVM no k3d.
- **Um serviço por entidade (Vehicle, Inspection, Receipt…)** — cria nano serviços que se chamam constantemente e precisam de transações distribuídas para um único ciclo de vida.
- **Divisão por camada técnica (API, negócio, dados)** — mantém o acoplamento pela base de dados partilhada, que é o problema de origem.

## Consequências

### Positivas

- As fronteiras seguem a linguagem do domínio, por isso são estáveis.
- O fluxo cotação, reserva e pagamento corre num só processo: a validação da cotação e a sua marcação como usada acontecem na mesma transação da reserva.
- Menos chamadas remotas, menos mensagens e menos peças para instalar e observar.
- Os contextos continuam separados como módulos, por isso o `booking-service` pode ser dividido mais tarde sem reescrever o domínio.

### Negativas

- Os Pagamentos, um subdomínio genérico com dependência externa, vivem junto do core; ficam protegidos apenas pela Anti-Corruption Layer e pelo bulkhead (ADR-12).
- Reservas e Tarifas escalam e são instaladas em conjunto.
- As fronteiras entre módulos dependem de disciplina e de testes de arquitetura (ADR-15).
- Os dois elementos do grupo partilham o mesmo deployable: Nuno nos módulos `reservation` e `payment`, Vitor no módulo `pricing`.
