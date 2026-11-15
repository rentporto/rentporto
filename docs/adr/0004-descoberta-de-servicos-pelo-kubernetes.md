# ADR-04: Descoberta e balanceamento pelo Kubernetes, sem Eureka

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §8.3, §13; ADR-16

## Contexto

O ambiente de destino é um cluster k3s, criado com o k3d, que já fornece Services com nomes DNS e balanceamento entre réplicas pelo kube-proxy. Cada JVM a mais consome parte do orçamento de memória. Os mesmos nomes de serviço também resolvem no docker-compose.

## Decisão

Os clientes OpenFeign chamam os outros serviços pelo nome do Service do Kubernetes (ex.: `http://booking-service:8080`). O docker-compose usa os mesmos nomes. Não há servidor Eureka nem Spring Cloud LoadBalancer. O Config Server mantém-se para a configuração externa.

## Alternativas consideradas

- **Eureka com Spring Cloud LoadBalancer** — duplica o que o cluster já faz e acrescenta mais uma JVM a correr.

## Consequências

### Positivas

- Menos um componente para correr e configurar.
- Os mesmos nomes de serviço no docker-compose e no k3d.

### Negativas

- O balanceamento é feito no servidor (kube-proxy), não no cliente.
- Correr um serviço fora do compose ou do cluster obriga a configurar os URL explicitamente.
