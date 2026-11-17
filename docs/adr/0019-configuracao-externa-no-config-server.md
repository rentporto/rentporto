# ADR-19: Configuração externa no Config Server

- **Estado:** Aceite
- **Data:** 2026-11-17
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-04, ADR-11, ADR-16, ADR-17; backlog RP-07, RP-08

## Contexto

Os serviços precisam de endereços, bases de dados e parâmetros diferentes no desenvolvimento e no k3d, e esses valores têm de mudar sem reconstruir as imagens. No desenvolvimento, o docker-compose só sobe a infraestrutura e os serviços correm no IDE, em `localhost` com portas fixas (RP-07); no k3d, chamam-se pelos nomes dos Services (ADR-04). O repositório é público (ADR-17).

## Decisão

- A configuração vive na pasta `config/` do monorepo (ADR-11): ficheiros comuns (`application.yml`, `application-dev.yml`, `application-k8s.yml`) e um `{serviço}.yml` por serviço. Muda por pull request, como o código.
- O Config Server serve essa pasta com dois backends, escolhidos pelo perfil:
  - `dev`: backend `native`, sobre a pasta local, para uma alteração se aplicar ao reiniciar o serviço, sem push;
  - `k8s`: backend `git`, sobre o ramo `main` do repositório no GitHub.
- O gateway e os seis serviços importam a configuração do Config Server com fail-fast e algumas tentativas: sem Config Server, não arrancam e, no k3d, o pod não fica ready.
- Os endereços dependem do perfil: `localhost` e as portas fixas no `dev`, os nomes dos Services no `k8s`. Isto substitui a frase do ADR-04 segundo a qual o docker-compose usa os nomes dos Services.
- O repositório não guarda segredos: as passwords do perfil `k8s` são placeholders preenchidos por Secrets.

## Alternativas consideradas

- **Repositório Git só para a configuração** — separa a configuração do código, mas obriga a dois repositórios e a dois fluxos de revisão.
- **Backend `git` também no desenvolvimento** — obriga a fazer push para testar uma alteração de configuração.
- **ConfigMaps do Kubernetes em vez do Config Server** — dispensa um componente, mas não serve o desenvolvimento local com a mesma configuração.

## Consequências

### Positivas

- A mesma configuração, versionada, serve os dois ambientes; só mudam os endereços.
- Mudar um parâmetro no k3d é um commit no `main` e o reinício do serviço, sem nova imagem.

### Negativas

- No k3d, o Config Server precisa de acesso à internet para ler o GitHub; sem rede, os serviços não arrancam.
- No desenvolvimento, o Config Server tem de estar a correr antes dos serviços.
