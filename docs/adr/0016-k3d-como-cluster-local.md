# ADR-16: k3d como cluster Kubernetes local

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §13; backlog RP-12, RP-55, RP-59

## Contexto

O sistema tem de correr em Kubernetes numa máquina local, para a entrega e para a defesa, e os docentes têm de o conseguir reconstruir com facilidade. O cluster tem de aguentar 6 serviços Spring, o gateway, o Config Server, PostgreSQL, RabbitMQ, Prometheus, Grafana, Zipkin, Mailpit, WireMock e o Stripe CLI num orçamento de cerca de 10 GB de memória. A proposta previa o Minikube, mas o enunciado permite usar outras ferramentas.

## Decisão

Usar o k3d, que corre o k3s (uma distribuição Kubernetes leve) em contentores Docker:

- o `deploy.sh` cria o cluster com `k3d cluster create rentporto --agents 1 -p "80:80@loadbalancer"`;
- as imagens construídas localmente entram no cluster com `k3d image import <imagem> -c rentporto`;
- o Ingress usa o Traefik incluído no k3s, sem anotações específicas de um controller, e o `rentporto.local` aponta para `127.0.0.1`;
- os PersistentVolumes usam o provisioner `local-path` e o HPA usa o metrics-server, ambos incluídos no k3s;
- os manifestos Kustomize são os mesmos que serviriam noutro cluster; o docker-compose continua a ser usado no desenvolvimento diário.

## Alternativas consideradas

- **Minikube** — tem mais addons e documentação, mas é mais pesado (sobretudo com driver de VM) e obriga a ativar o Ingress e o metrics-server.
- **kind** — é igualmente leve, mas não traz Ingress controller, metrics-server nem mapeamento de portas para um load balancer sem configuração extra.
- **Só docker-compose** — não cumpre o requisito de disponibilizar o sistema em Kubernetes.

## Consequências

### Positivas

- Arranque mais rápido e menos memória gasta pelo próprio cluster.
- Ingress, armazenamento persistente e metrics-server disponíveis sem configuração.
- Os manifestos continuam portáveis para qualquer cluster Kubernetes.

### Negativas

- O limite de memória é o do Docker, não um parâmetro do cluster; no Docker Desktop tem de ser configurado nas definições.
- O Ingress controller é o Traefik, não o NGINX: os exemplos da internet com anotações do NGINX não se aplicam.
- O k3s difere ligeiramente de um Kubernetes upstream nos componentes incluídos, o que não afeta este projeto.
- A proposta (secção 13) e o README de entrega têm de refletir a troca.
