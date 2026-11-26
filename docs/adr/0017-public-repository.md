# ADR-17: Public repository on GitHub

- **Status:** Accepted
- **Date:** 2026-11-15
- **Deciders:** Nuno Soares, Vitor Sousa
- **References:** ADR-11; backlog RP-02

## Context

RP-02 requires that a pull request with a failing test is blocked and that every merge into `main` publishes to GHCR one image per affected service. ADR-11 planned a private repository, and the `rentporto` organisation is on the GitHub Free plan. On this plan, a private repository has no branch protection and no rulesets (the API answers "Upgrade to GitHub Pro or make this repository public"), private packages share a 500 MB quota, and GitHub Actions has a monthly limit of minutes. Eight images with a JRE 25 leave little or no headroom in that quota.

## Decision

The `rentporto/rentporto` repository becomes public. This ADR supersedes only the part of ADR-11 that defined it as private; the rest of ADR-11 still stands.

- `main` has a ruleset that requires a pull request and a green `CI result` check before the merge.
- The images in GHCR (`ghcr.io/rentporto/<service>`) are public. A package does not inherit the visibility of the repository, so each package is made public once, after its first publication.
- No secret enters the repository: Stripe keys, database passwords and the like come from environment variables or Kubernetes Secrets. The history was checked before the change and contained no secrets.

## Alternatives considered

- **GitHub Team plan for the organisation** — allows branch protection on a private repository, but has a per-user cost.
- **Keep the repository private on the Free plan** — the CI marks the pull request red, but does not prevent the merge; blocking would depend only on the team's discipline, and the images would be limited by the package quota.
- **Move the repository to a personal account with GitHub Pro** — solves branch protection, but takes the repository out of the organisation and keeps a quota on private packages.

## Consequences

### Positive

- A merge into `main` only happens with a green CI.
- GitHub Actions minutes and the storage of public images no longer have a quota.
- Anyone can access the code and the images without an invitation.

### Negative

- The code, the history and the images are visible to anyone.
- A secret pushed by mistake is exposed at once and must be revoked; deleting it from the history is not enough.
- Each new service requires making its package public by hand after the first publication.
