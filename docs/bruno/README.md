# Coleção Bruno

Abrir esta pasta no Bruno 4 ("Open Collection") e escolher o ambiente:

| Ambiente | API | Password do administrador |
|---|---|---|
| `compose` | `http://localhost:8080/api/v1`, com os serviços a correr localmente | a do perfil `dev` (`rentporto-admin`) |
| `k3d` | `http://rentporto.local/api/v1` (ver `infra/README.md`) | do `.env` desta pasta, que o `deploy.sh` escreve; o `.gitignore` exclui-o |

Há uma pasta por ator: `customer`, `administrator`, `agent`, `fleet-manager` e `pricing-manager`. O cliente regista-se a si próprio e o administrador cria o agente e os dois gestores, por isso, num ambiente novo, corre-se primeiro a coleção inteira (Run Collection), por ordem. Os pedidos de criação de contas têm uma `Idempotency-Key` fixa: correr a coleção outra vez devolve as mesmas contas.

Cada pasta faz login sozinha antes de um pedido do seu ator quando ainda não há token ou quando ele expira dentro de um minuto; o pedido Login de cada pasta faz o mesmo à vista. Os pedidos de negócio respondem 501 até a história de cada endpoint o implementar; o teste confirma que o papel do ator passa (nem 401 nem 403).

O gateway aceita 10 logins por minuto por IP (ADR-21) e cada execução da coleção faz 5, por isso duas execuções seguidas no mesmo minuto podem receber 429 no login.

## No terminal

```
cd docs/bruno
npx @usebruno/cli@4.3.0 run --env compose
npx @usebruno/cli@4.3.0 run agent --env k3d
```

## Outras ferramentas

A coleção Bruno é a única mantida no repositório.

- **Postman, Insomnia, Hoppscotch:** importam a especificação OpenAPI de cada serviço, em `docs/openapi`, com todos os endpoints e DTOs, mas sem os ambientes nem o login automático.
- **Coleção completa no Postman:** no Bruno, "Export" → "Postman Collection". Os scripts são traduzidos para a API `pm` do Postman, mas convém confirmar o login automático.
