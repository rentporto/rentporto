# ADR-28: Escala automática do booking-service

- **Estado:** Aceite
- **Data:** 2026-11-23
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** ADR-15, ADR-16, ADR-18, ADR-21, ADR-23; backlog RP-54

## Contexto

Queremos mostrar que um serviço escala sem os outros. O booking-service é o que recebe a pesquisa, as cotações e as reservas, por isso é o que mais carga terá; já corre com 2 réplicas, com os schedulers num lock partilhado (ADR-18).

- **Métricas:** o k3s, por baixo do k3d, já traz o metrics-server (ADR-16), por isso o Kubernetes conhece a CPU de cada pod sem instalar nada.
- **Arranque da JVM:** um serviço Spring gasta perto de um CPU durante os primeiros segundos, o que não é carga.
- **Orçamento:** o cluster local tem cerca de 10 GB de memória para tudo (ADR-16); cada réplica do booking-service pede 384 MiB e pode chegar a 768 MiB.
- **Gateway:** limita os pedidos de cada cliente (ADR-21), por isso não serve para gerar carga.

## Decisão

- **HorizontalPodAutoscaler pela CPU:** entre 2 e 3 réplicas do booking-service, para manter a CPU média abaixo de 70 % do que cada pod pede (100m).
- **Estabilização:**
  - sobe uma réplica de cada vez, só se a CPU ficar acima do alvo durante 60 segundos;
  - desce ao fim de 5 minutos sem carga.
  - O Kubernetes ignora a CPU dos pods que ainda não estão prontos, e a janela de 60 segundos cobre o resto do arranque da JVM.
- **O manifest mantém 2 réplicas**, igual ao mínimo do HPA, para um deploy nunca contrariar o HPA em repouso. O `check-manifests.sh` confirma que cada HPA escala um Deployment que pede CPU e que começa no mínimo.
- **Verificação:**
  - o `deploy.sh` confirma que o HPA mede a CPU e que, sem carga, fica no mínimo;
  - o `infra/k8s/load-booking.sh` põe carga num pod busybox dentro do cluster, direto ao Service do booking-service, e mostra o HPA a chegar às 3 réplicas.

## Alternativas consideradas

- **Escalar pela memória** — a JVM não devolve a memória quando a carga baixa, por isso o serviço nunca voltaria a descer.
- **Métricas personalizadas** (pedidos por segundo, pelo Prometheus Adapter) — medem melhor a procura, mas acrescentam mais um componente ao cluster para um HPA só.
- **KEDA ou o tamanho das filas do RabbitMQ** — servem a quem consome mensagens, não a um serviço que responde a pedidos HTTP.
- **Carga através do gateway** — é o caminho real dos clientes, mas o rate limiting responderia 429 antes de o booking-service aquecer.
- **Mais réplicas no máximo** — mostraria mais escala, mas cada réplica tira até 768 MiB ao orçamento do cluster.

## Consequências

### Positivas

- O booking-service escala sozinho com a carga e volta a 2 réplicas quando ela passa, sem mexer nos outros serviços.
- Os deploys e os reinícios não fazem o HPA escalar.
- A demonstração corre com um só script, sem ferramentas fora do cluster.

### Negativas

- O alvo de 70 % de 100m é baixo para um serviço Java: escala depressa, o que serve a demonstração, mas com tráfego real o pedido de CPU teria de subir.
- Enquanto os endpoints do booking-service respondem "não implementado", a carga é sobre o `/v3/api-docs`; quando houver cotações, a carga deve passar a ser sobre elas.
- Um `kubectl apply` com o HPA acima do mínimo repõe 2 réplicas até o HPA voltar a decidir.
