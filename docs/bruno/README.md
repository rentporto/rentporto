# Bruno collection

Open this folder in Bruno 4 ("Open Collection") and choose the environment:

| Environment | API | Administrator password |
|---|---|---|
| `compose` | `http://localhost:8080/api/v1`, with the services running locally | the one of the `dev` profile (`rentporto-admin`) |
| `k3d` | `http://rentporto.local/api/v1` (see `infra/README.md`) | from the `.env` in this folder, which `deploy.sh` writes; `.gitignore` excludes it |

There is one folder per actor: `customer`, `administrator`, `agent`, `fleet-manager` and `pricing-manager`. The customer registers themselves and the administrator creates the agent and the two managers, so, in a new environment, run the whole collection first (Run Collection), in order. The requests that create accounts have a fixed `Idempotency-Key`: running the collection again returns the same accounts.

Each folder logs in by itself before a request of its actor when there is no token yet or when it expires within a minute; the Login request of each folder does the same in plain sight. The business requests answer 501 until the story of each endpoint implements it; the test checks that the role of the actor gets through (neither 401 nor 403).

The gateway accepts 10 logins per minute per IP (ADR-21) and each run of the collection does 5, so two runs in a row within the same minute may get 429 on the login.

## In the terminal

```
cd docs/bruno
npx @usebruno/cli@4.3.0 run --env compose
npx @usebruno/cli@4.3.0 run agent --env k3d
```

## Other tools

The Bruno collection is the only one kept in the repository.

- **Postman, Insomnia, Hoppscotch:** they import the OpenAPI specification of each service, in `docs/openapi`, with every endpoint and DTO, but without the environments or the automatic login.
- **Full collection in Postman:** in Bruno, "Export" → "Postman Collection". The scripts are translated to the `pm` API of Postman, but check the automatic login.
