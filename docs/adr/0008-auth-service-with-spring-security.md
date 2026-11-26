# ADR-08: auth-service próprio com Spring Security

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §8.3, §10.1

## Contexto

O sistema precisa de registo de contas, login, papéis e JWT para clientes, agentes, gestores e administradores. A stack é só Spring e os recursos do cluster local são limitados.

## Decisão

Um `auth-service` fino, construído com Spring Security:

- regista contas, autentica e emite JWT assinados com RS256;
- guarda as palavras-passe com BCrypt;
- publica a chave pública em `/auth/jwks`;
- não tem regras de negócio.

O gateway e cada serviço validam o JWT como OAuth2 Resource Server e leem os papéis das claims. O `customerId` é o `sub` do JWT.

## Alternativas consideradas

- **Keycloak** — é um fornecedor de identidade completo, mas é pesado para o k3d e acrescenta mais um produto para configurar.

## Consequências

### Positivas

- Stack só Spring e fluxo de login simples.
- Defesa em profundidade: o token é validado no gateway e em cada serviço.

### Negativas

- Mantemos o código de autenticação.
- Sem SSO, MFA nem recuperação de palavra-passe de origem.
