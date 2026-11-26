# ADR-21: Entrada pelo API Gateway

- **Estado:** Aceite
- **Data:** 2026-11-17
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-04, ADR-08, ADR-13, ADR-16, ADR-19, ADR-20; backlog RP-10, RP-11

## Contexto

Todo o tráfego externo entra pelo gateway, o único serviço que o Ingress expõe. O gateway encaminha para seis serviços. Os clientes não devem conhecer essa topologia: se um recurso mudar de serviço, a URL não pode mudar. A segurança tem de ser aplicada à entrada sem duplicar as regras de papéis de cada serviço, e é preciso travar abusos, como tentativas de adivinhar palavras-passe. Não há frontend (ADR-13): a API é usada pelo Swagger UI e pela coleção Bruno.

## Decisão

- **Rotas por recurso.**
  - A API pública vive em `/api/v1/{recurso}`. O gateway remove `/api/v1` e encaminha para o serviço dono do recurso (ex.: `/api/v1/reservations/...` → booking-service `/reservations/...`).
  - A tabela de rotas é código do gateway. Os endereços dos serviços vêm do Config Server por perfil (ADR-19), com as mesmas portas no `dev` e no `k8s`.
- **Endpoints internos:** `/internal/**` não tem rota, por isso de fora do cluster responde 404.
- **Autenticação no gateway, autorização nos serviços.**
  - O gateway valida a assinatura do JWT com a chave de `/auth/jwks` (ADR-20), a validade e o `iss`, e reencaminha o `Authorization`.
  - Os papéis verificam-se com `@PreAuthorize` em cada serviço, que volta a validar o token (ADR-08).
  - As rotas públicas do catálogo passam sem token e ignoram um token antigo.
- **Topologia escondida.**
  - O gateway envia `X-Forwarded-Host`, `X-Forwarded-Proto` e `X-Forwarded-Prefix`.
  - Com `server.forward-headers-strategy: framework`, os serviços geram os links e o `Location` com o endereço externo.
  - O gateway só confia no `X-Forwarded-For` que vem dos proxies internos, como o Traefik.
- **Erros em ProblemDetail com `code`:** 401 sem token válido, 404 sem rota, 429 com `Retry-After` acima do limite e 503 quando o serviço não responde.
- **Rate limiting com Bucket4j, em memória:**
  - um limite por cliente, que é o `sub` do JWT ou, sem token, o IP;
  - um limite mais baixo para o login, por IP.
- **CORS:** só para as origens configuradas, e nenhuma por omissão.
- **Segurança comum no `common-web`:** o 401 e o 403 em ProblemDetail, o conversor da claim `roles` e o token ignorado nas rotas públicas. Usam-na o gateway e os serviços.

## Alternativas consideradas

- **Rate limiting com Redis** (o `RequestRateLimiter` do Spring Cloud Gateway reativo) — partilha o limite entre réplicas, mas acrescenta um componente à stack; com uma réplica do gateway não traz nada.
- **Autorização por papel também no gateway** — duplicaria as regras de cada serviço e obrigaria a mudar o gateway sempre que um endpoint mudasse de papéis.
- **Prefixo por serviço** (ex.: `/api/v1/booking/reservations`) — expõe a decomposição em serviços, e mover um recurso de serviço mudaria a URL.
- **Rotas em propriedades no Config Server** — mudariam sem nova imagem, mas a tabela de rotas faz parte do desenho da API e não do ambiente, e seria mais difícil de testar.

## Consequências

### Positivas

- O cliente vê uma só API, e a decomposição em serviços pode mudar sem mudar as URLs.
- Um pedido sem token válido não chega a nenhum serviço.
- Os limites e o formato dos erros são iguais em toda a API.

### Negativas

- Com mais de uma réplica do gateway, cada réplica aplica o limite por si.
- Um recurso novo obriga a acrescentar a rota no gateway e, se for público, a regra de segurança.
- O gateway é o ponto único de entrada: se falhar, a API fica indisponível até o Kubernetes o reiniciar.
