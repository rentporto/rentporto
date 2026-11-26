# ADR-20: Chave de assinatura dos JWT e primeiro administrador

- **Estado:** Aceite
- **Data:** 2026-11-17
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-08, ADR-17, ADR-19; backlog RP-09, RP-11, RP-12

## Contexto

O auth-service assina os JWT com RS256 e publica a chave pública em `/auth/jwks` (ADR-08). A chave privada não pode estar no repositório, porque ele é público (ADR-17). Se cada arranque gerasse uma chave nova, um reinício invalidava todos os tokens e duas réplicas assinariam com chaves diferentes. Depois de instalado, o sistema também precisa de uma primeira conta ADMIN, para criar as contas de agentes e gestores.

## Decisão

- **Chave:** no perfil `k8s`, a chave privada RSA (PKCS#8 em PEM) vem do Secret do auth-service, pela variável `AUTH_SIGNING_KEY`; sem ela, o serviço não arranca. No `dev` e nos testes, o serviço gera uma chave em memória no arranque.
- **Identificador da chave:** o `kid` é o thumbprint da chave pública (RFC 7638), por isso a mesma chave tem o mesmo `kid` depois de um reinício.
- **Token:** header com `kid` e claims `iss`, `sub` (o id da conta), `email`, `roles`, `iat` e `exp`. Vale 1 hora, sem refresh token: quando expira, o cliente faz login de novo.
- **Validação no próprio auth-service:** valida os seus tokens com a chave local, sem chamar o seu `/auth/jwks`. Os outros serviços leem a chave em `/auth/jwks` (RP-11).
- **Primeiro administrador:** criado no arranque a partir de `AUTH_ADMIN_EMAIL` e `AUTH_ADMIN_PASSWORD`, que no `k8s` vêm do mesmo Secret. Se a conta já existir, fica como está; mudar a password no Secret não altera a conta.
- **Secret:** o `deploy.sh` do RP-12 cria-o com `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048`.

## Alternativas consideradas

- **Chave gerada em cada arranque, também no k8s** — dispensa o Secret, mas um reinício termina todas as sessões e impede ter duas réplicas.
- **Chave de desenvolvimento no repositório** — evita a geração, mas é uma chave privada num repositório público.
- **Spring Authorization Server** — emite tokens e gere chaves, mas traz os fluxos OAuth2 completos, que a RentPorto não usa.
- **Refresh tokens** — evitam um novo login de hora a hora, mas obrigam a guardá-los e a revogá-los.

## Consequências

### Positivas

- No k3d, os tokens continuam válidos depois de um reinício ou de um novo deploy do auth-service.
- O repositório não tem nenhuma chave privada nem nenhuma password de produção.
- O sistema arranca com um administrador, sem passos manuais na base de dados.

### Negativas

- Rodar a chave obriga a um reinício, e os tokens assinados com a chave anterior deixam de ser válidos.
- No `dev`, reiniciar o auth-service invalida os tokens emitidos antes.
- Um token não pode ser revogado antes de expirar.
