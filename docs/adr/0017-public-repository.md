# ADR-17: Repositório público no GitHub

- **Estado:** Aceite
- **Data:** 2026-11-15
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-11; backlog RP-02

## Contexto

O RP-02 exige que um pull request com um teste a falhar fique bloqueado e que cada merge na `main` publique no GHCR uma imagem por serviço afetado. A ADR-11 previa um repositório privado e a organização `rentporto` está no plano GitHub Free. Neste plano, um repositório privado não tem branch protection nem rulesets (a API responde «Upgrade to GitHub Pro or make this repository public»), os pacotes privados partilham uma quota de 500 MB e o GitHub Actions tem um limite mensal de minutos. Oito imagens com um JRE 25 deixam pouca ou nenhuma margem nessa quota.

## Decisão

O repositório `rentporto/rentporto` passa a ser público. Esta ADR substitui apenas a parte da ADR-11 que o definia como privado; o resto da ADR-11 mantém-se.

- A `main` tem um ruleset que exige um pull request e o check `CI result` verde antes do merge.
- As imagens no GHCR (`ghcr.io/rentporto/<serviço>`) são públicas. Um pacote não herda a visibilidade do repositório, por isso cada pacote é tornado público uma vez, depois da primeira publicação.
- Nenhum segredo entra no repositório: chaves do Stripe, passwords das bases de dados e afins vêm de variáveis de ambiente ou de Secrets do Kubernetes. O histórico foi verificado antes da mudança e não continha segredos.

## Alternativas consideradas

- **Plano GitHub Team para a organização** — permite branch protection num repositório privado, mas tem um custo por utilizador.
- **Manter o repositório privado no plano Free** — o CI marca o pull request a vermelho, mas não impede o merge; o bloqueio dependeria só da disciplina da equipa, e as imagens ficariam limitadas pela quota de pacotes.
- **Mover o repositório para uma conta pessoal com GitHub Pro** — resolve a branch protection, mas tira o repositório da organização e mantém uma quota de pacotes privados.

## Consequências

### Positivas

- O merge na `main` só acontece com o CI verde.
- Os minutos do GitHub Actions e o armazenamento das imagens públicas deixam de ter quota.
- Os docentes acedem ao código e às imagens sem convite.

### Negativas

- O código, o histórico e as imagens ficam visíveis para qualquer pessoa.
- Um segredo enviado por engano fica exposto de imediato e tem de ser revogado; apagá-lo do histórico não chega.
- Cada novo serviço obriga a tornar o seu pacote público à mão depois da primeira publicação.
