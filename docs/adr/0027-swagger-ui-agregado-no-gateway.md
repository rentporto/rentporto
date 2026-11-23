# ADR-27: Swagger UI agregado no gateway

- **Estado:** Aceite
- **Data:** 2026-11-23
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-21, ADR-24; backlog RP-52

## Contexto

Cada serviço publica a sua especificação OpenAPI, mas os clientes só chegam ao gateway (ADR-21). Queremos a documentação de todos os serviços numa só página, para explorar a API sem saber onde está cada serviço.

- **Endpoints internos:** o customer-service, o fleet-service e o booking-service têm endpoints `/internal/**`, chamados entre serviços, que estão nas suas especificações mas não se alcançam de fora.
- **Prefixo público:** o gateway tira o `/api/v1` antes de encaminhar, por isso os caminhos das especificações não o têm. Sem mais nada, o "Try it out" chamaria `/stations` em vez de `/api/v1/stations`.
- **Ingress:** no k3d, o Ingress só encaminha `/api` (ADR-24).

## Decisão

- **O gateway serve as especificações:**
  - `GET /api/docs/services/{serviço}` lê o `/v3/api-docs` do serviço no momento do pedido, por isso mostra sempre a versão que está a correr;
  - tira os caminhos `/internal/**` e os esquemas que só eles usam;
  - põe `/api/v1` como servidor, para o "Try it out" passar pelo gateway, com o token do "Authorize".
- **Swagger UI do gateway:** o springdoc do gateway lista os seis serviços com API. A página abre em `/api/docs`, que redireciona para `/api/swagger-ui`, sem token.
- **Tudo debaixo de `/api`:** o Ingress continua a encaminhar só `/api`, e a página abre no k3d sem lhe mexer.
- **Um serviço parado** responde 503 na sua especificação; os outros continuam disponíveis.
- **Os contratos completos** continuam em `docs/openapi/`, com os endpoints internos, para a equipa.

## Alternativas consideradas

- **Grupos do springdoc em cada serviço**, um público sem `/internal/**` — cada serviço filtrava a sua especificação, mas o servidor continuava errado quando lida através do gateway e a mesma configuração repetia-se em seis serviços.
- **Rotas do gateway para o `/v3/api-docs` de cada serviço** — dispensa código, mas não tira os endpoints internos e o caminho reescrito perde o prefixo que o springdoc usa para o servidor.
- **Copiar os contratos de `docs/openapi/` para o gateway no build** — dispensa os pedidos aos serviços, mas a página mostra a versão do build do gateway, não a dos serviços que estão a correr.
- **Acrescentar o `/swagger-ui` e o `/v3/api-docs` ao Ingress** — é o caminho por omissão do springdoc, mas abre caminhos fora de `/api`, que o ADR-24 quer evitar.

## Consequências

### Positivas

- Uma só página para toda a API, igual no compose e no k3d.
- Os endpoints internos e os seus esquemas não se mostram para fora.
- O "Try it out" funciona contra a API real, através do gateway.

### Negativas

- O gateway passa a saber quais são os serviços com API, numa lista no código e outra na configuração do springdoc; um teste confirma que as duas coincidem.
- Cada abertura de uma especificação faz um pedido ao serviço.
- O filtro conhece a convenção `/internal/**`: um endpoint interno noutro caminho apareceria.
