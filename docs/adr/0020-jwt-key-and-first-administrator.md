# ADR-20: JWT signing key and first administrator

- **Status:** Accepted
- **Date:** 2026-11-17
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-08, ADR-17, ADR-19; backlog RP-09, RP-11, RP-12

## Context

The auth-service signs the JWTs with RS256 and publishes the public key at `/auth/jwks` (ADR-08). The private key cannot be in the repository, because the repository is public (ADR-17). If each startup generated a new key, a restart would invalidate every token and two replicas would sign with different keys. Once deployed, the system also needs a first ADMIN account, to create the accounts of agents and managers.

## Decision

- **Key:** in the `k8s` profile, the RSA private key (PKCS#8 in PEM) comes from the auth-service Secret, through the `AUTH_SIGNING_KEY` variable; without it, the service does not start. In `dev` and in the tests, the service generates an in-memory key at startup.
- **Key identifier:** the `kid` is the thumbprint of the public key (RFC 7638), so the same key keeps the same `kid` after a restart.
- **Token:** a header with `kid` and the claims `iss`, `sub` (the account id), `email`, `roles`, `iat` and `exp`. It is valid for 1 hour, with no refresh token: when it expires, the client logs in again.
- **Validation in the auth-service itself:** it validates its own tokens with the local key, without calling its own `/auth/jwks`. The other services read the key from `/auth/jwks` (RP-11).
- **First administrator:** created at startup from `AUTH_ADMIN_EMAIL` and `AUTH_ADMIN_PASSWORD`, which in `k8s` come from the same Secret. If the account already exists, it stays as it is; changing the password in the Secret does not change the account.
- **Secret:** the `deploy.sh` of RP-12 creates it with `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048`.

## Alternatives considered

- **A key generated at each startup, in k8s too** — removes the need for the Secret, but a restart ends every session and rules out two replicas.
- **A development key in the repository** — avoids generating one, but it is a private key in a public repository.
- **Spring Authorization Server** — issues tokens and manages keys, but brings the complete OAuth2 flows, which RentPorto does not use.
- **Refresh tokens** — avoid a new login every hour, but require storing and revoking them.

## Consequences

### Positive

- In k3d, the tokens remain valid after a restart or a new deployment of the auth-service.
- The repository has no private key and no production password.
- The system starts with an administrator, without manual steps in the database.

### Negative

- Rotating the key requires a restart, and the tokens signed with the previous key are no longer valid.
- In `dev`, restarting the auth-service invalidates the tokens issued before.
- A token cannot be revoked before it expires.
