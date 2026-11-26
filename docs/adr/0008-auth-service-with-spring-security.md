# ADR-08: Own auth-service with Spring Security

- **Status:** Accepted
- **Date:** 2026-11-14
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** Proposal §8.3, §10.1

## Context

The system needs account registration, login, roles and JWTs for customers, agents, managers and administrators. The stack is Spring only, and the local cluster has limited resources.

## Decision

A thin `auth-service`, built with Spring Security:

- registers accounts, authenticates and issues JWTs signed with RS256;
- stores the passwords with BCrypt;
- publishes the public key at `/auth/jwks`;
- has no business rules.

The gateway and each service validate the JWT as an OAuth2 Resource Server and read the roles from the claims. The `customerId` is the `sub` of the JWT.

## Alternatives considered

- **Keycloak** — is a complete identity provider, but it is heavy for k3d and adds one more product to configure.

## Consequences

### Positive

- A Spring-only stack and a simple login flow.
- Defence in depth: the token is validated at the gateway and in each service.

### Negative

- We maintain the authentication code.
- No SSO, MFA or password recovery out of the box.
