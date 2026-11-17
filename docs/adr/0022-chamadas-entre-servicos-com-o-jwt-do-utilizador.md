# ADR-22: Chamadas entre serviços com o JWT do utilizador

- **Estado:** Aceite
- **Data:** 2026-11-17
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-02, ADR-08, ADR-21; backlog RP-11, RP-27, RP-45

## Contexto

Cada serviço valida o JWT e os papéis, mesmo nos pedidos que não passam pelo gateway (ADR-08). Os endpoints `/internal/**` não estão expostos pelo gateway (ADR-21), mas ficam acessíveis dentro do cluster e precisam de proteção. Há três:

| Endpoint | Quem chama | Quem está à espera |
|---|---|---|
| `GET /internal/customers/{id}/driver-profile` | booking-service, ao criar a reserva | o cliente |
| `POST /internal/vehicles/assignments` | rental-service, no levantamento | o agente |
| `POST /internal/deposits` | rental-service, no levantamento | o agente |

O REST síncrono só é usado quando alguém está à espera da resposta (ADR-02). Tudo o resto vai por mensagens, por isso há sempre um utilizador por trás de uma chamada interna.

## Decisão

- **Quem chama reencaminha o token:** o serviço que chama reencaminha o `Authorization` do pedido que recebeu, com um interceptor do Feign.
- **O endpoint interno é tratado como os outros:** valida o JWT e exige o papel de quem está à espera, com `@PreAuthorize`:
  - CUSTOMER no perfil do condutor;
  - AGENT na atribuição de viatura e na caução.
- **Sem credenciais próprias:** não há tokens de serviço nem segredos partilhados entre serviços.

## Alternativas consideradas

- **Client credentials no auth-service** — cada serviço teria um token próprio, mas isso obriga a um endpoint, a um segredo por serviço e a renovar os tokens, e só faz falta em chamadas sem utilizador, que o ADR-02 não prevê.
- **Uma API key partilhada** — é simples, mas um só segredo abre todos os endpoints internos, e o serviço chamado deixa de saber quem originou o pedido.
- **Confiar só na rede do cluster** — qualquer pod comprometido chamaria os endpoints internos sem restrições.

## Consequências

### Positivas

- Um endpoint interno só faz o que o utilizador por trás do pedido pode fazer.
- O serviço chamado sabe quem originou o pedido, o que dá rastreabilidade e permite verificar se o recurso é do utilizador.
- Não há credenciais de serviço para gerir.

### Negativas

- Um token prestes a expirar pode expirar a meio de uma cadeia de chamadas; o cliente repete o pedido depois de um novo login.
- Se um dia uma chamada interna acontecer sem utilizador (por exemplo num job agendado), esta decisão tem de ser revista.
