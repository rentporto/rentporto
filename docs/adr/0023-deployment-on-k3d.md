# ADR-23: Disponibilização no k3d

- **Estado:** Aceite
- **Data:** 2026-11-17
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-04, ADR-16, ADR-17, ADR-19, ADR-20; backlog RP-07, RP-12

## Contexto

O sistema tem de arrancar no k3d com um só comando (ADR-16), a partir de uma máquina com Docker e k3d. Para isso, é preciso decidir quatro coisas:

- **Segredos:** os serviços precisam de passwords, de uma chave de assinatura (ADR-20) e de um administrador, e o repositório é público (ADR-17).
- **Imagens:** as do CI ficam no GHCR, mas no k3d também se quer correr o código que ainda não foi publicado.
- **Ordem de arranque:** sem o Config Server, os serviços não arrancam (ADR-19), e o Kubernetes reinicia-os com esperas cada vez maiores.
- **RabbitMQ:** o ficheiro de definições do RP-07 trazia o utilizador e o hash da sua password.

## Decisão

- **Segredos gerados no deploy.**
  - O `deploy.sh` cria os Secrets `auth-service`, `postgres` e `rabbitmq` só se ainda não existirem, com passwords aleatórias e a chave gerada com `openssl`.
  - Nenhum segredo entra no repositório.
  - Voltar a correr o `deploy.sh` não os muda, por isso continuam a coincidir com os dados guardados nos volumes.
- **Imagens por overlay.**
  - Os manifestos ficam em `infra/k8s`, com Kustomize.
  - A base aponta para as imagens do GHCR. O overlay `k3d` troca-as pelas imagens construídas localmente, que o `deploy.sh` importa com `k3d image import`.
- **Uma fonte para a configuração do RP-07:** a topologia do RabbitMQ e o `init.sh` do PostgreSQL entram no cluster por ConfigMaps gerados a partir dos ficheiros do compose.
- **Utilizadores do RabbitMQ fora das definições.**
  - O RabbitMQ carrega uma pasta com dois ficheiros: a topologia, em comum, e os utilizadores.
  - No compose, os utilizadores vêm do `users.json` do repositório; no k3d, vêm do Secret `rabbitmq`.
- **Ordem de arranque explícita.**
  - Os serviços têm um initContainer que espera pelo Config Server, pelo PostgreSQL e pelo RabbitMQ.
  - Uma startupProbe dá margem ao arranque da JVM.
  - As probes de readiness e de liveness usam o Actuator, e todos os contentores têm limites de CPU e memória.
- **Portas:** cada Service expõe a porta fixa do desenvolvimento, por isso só os nomes mudam entre perfis (ADR-19).

## Alternativas consideradas

- **Secrets no repositório, cifrados com SOPS ou Sealed Secrets** — dá segredos estáveis entre máquinas, mas obriga a gerir chaves de cifra para um cluster que se cria e destrói localmente.
- **Helm** — dá templates e valores por ambiente, mas acrescenta uma ferramenta e uma linguagem de templates para manifestos que o Kustomize já cobre.
- **Imagens sempre do GHCR** — dispensa a compilação local, mas só testa o código que já está no `main`.
- **Deixar o Kubernetes reiniciar os serviços até o Config Server arrancar** — funciona, mas o backoff atrasa o arranque em minutos e enche os eventos de falsos erros.

## Consequências

### Positivas

- `./deploy.sh` cria tudo de raiz e termina com um login pelo Ingress.
- O repositório não tem nenhuma password nem chave do cluster.
- O compose e o k3d partilham a topologia do RabbitMQ e a criação das bases.
- O CI valida os manifestos e verifica as probes e os limites.

### Negativas

- Os segredos vivem só no cluster: apagar o cluster apaga-os, e os dados com eles.
- O Config Server do k3d lê o `main` do GitHub; para testar a configuração de um branch, é preciso enviá-lo e usar `CONFIG_LABEL`.
- Uma alteração ao ConfigMap da topologia só chega ao RabbitMQ quando o pod reinicia.
