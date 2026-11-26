# Registos de decisões de arquitetura (ADR)

Decisões de arquitetura da RentPorto. Cada registo explica o contexto, a decisão, as alternativas e as consequências.

Para registar uma nova decisão, copiar `0000-modelo.md` para o número seguinte e abrir um pull request. Uma decisão aceite não se edita: é substituída por uma nova ADR.

| ADR | Decisão | Estado |
|---|---|---|
| [ADR-01](0001-decomposicao-em-microsservicos.md) | Decomposição em microsserviços por bounded context | Aceite |
| [ADR-02](0002-comunicacao-hibrida.md) | Comunicação híbrida: REST para decisões imediatas, mensagens para mudanças de estado | Aceite |
| [ADR-03](0003-rabbitmq-como-broker-de-mensagens.md) | RabbitMQ como broker de mensagens | Aceite |
| [ADR-04](0004-descoberta-de-servicos-pelo-kubernetes.md) | Descoberta e balanceamento pelo Kubernetes, sem Eureka | Aceite |
| [ADR-05](0005-postgresql-com-uma-base-por-servico.md) | Uma instância PostgreSQL com uma base lógica e um utilizador por serviço | Aceite |
| [ADR-06](0006-saga-orquestrada-na-reserva.md) | Saga orquestrada na reserva, coreografia nas reações | Aceite |
| [ADR-07](0007-outbox-e-consumidores-idempotentes.md) | Transactional outbox e consumidores idempotentes | Aceite |
| [ADR-08](0008-auth-service-com-spring-security.md) | auth-service próprio com Spring Security | Aceite |
| [ADR-09](0009-termos-contratuais-no-reservation-confirmed.md) | Termos contratuais congelados no ReservationConfirmed | Aceite |
| [ADR-10](0010-reservar-por-categoria.md) | Reservar por categoria e atribuir a viatura no levantamento | Aceite |
| [ADR-11](0011-monorepo.md) | Monorepo com um módulo, uma imagem e um pipeline por serviço | Aceite |
| [ADR-12](0012-stripe-com-anti-corruption-layer.md) | Stripe em modo de teste atrás de uma Anti-Corruption Layer, com um adaptador WireMock | Substituída pela ADR-29 |
| [ADR-13](0013-sem-frontend.md) | Sem frontend: interação pela API | Aceite |
| [ADR-14](0014-montantes-e-arredondamento.md) | Montantes e arredondamento | Proposta |
| [ADR-15](0015-estrutura-modular-do-booking-service.md) | Arquitetura hexagonal nos serviços e módulos do booking-service | Aceite |
| [ADR-16](0016-k3d-como-cluster-local.md) | k3d como cluster Kubernetes local | Aceite |
| [ADR-17](0017-repositorio-publico.md) | Repositório público no GitHub | Aceite |
| [ADR-18](0018-lock-partilhado-em-jpa.md) | Lock partilhado em JPA em vez do ShedLock | Aceite |
| [ADR-19](0019-configuracao-externa-no-config-server.md) | Configuração externa no Config Server | Aceite |
| [ADR-20](0020-chave-dos-jwt-e-primeiro-administrador.md) | Chave de assinatura dos JWT e primeiro administrador | Aceite |
| [ADR-21](0021-entrada-pelo-api-gateway.md) | Entrada pelo API Gateway: rotas por recurso, autenticação à entrada e rate limiting | Aceite |
| [ADR-22](0022-chamadas-entre-servicos-com-o-jwt-do-utilizador.md) | Chamadas entre serviços com o JWT do utilizador | Aceite |
| [ADR-23](0023-disponibilizacao-no-k3d.md) | Disponibilização no k3d: segredos gerados no deploy, imagens por overlay e ordem de arranque | Aceite |
| [ADR-24](0024-metricas-com-prometheus-e-grafana.md) | Métricas com Prometheus e Grafana: descoberta de pods e métricas só dentro do cluster | Aceite |
| [ADR-25](0025-rastreio-distribuido-com-zipkin.md) | Rastreio distribuído com Zipkin: propagação W3C e contexto guardado no outbox | Aceite |
| [ADR-26](0026-painel-do-grafana.md) | Painel do Grafana como código: histogramas, filas do RabbitMQ e estados do negócio | Aceite |
| [ADR-27](0027-swagger-ui-agregado-no-gateway.md) | Swagger UI agregado no gateway, sem os endpoints internos e com os pedidos pelo gateway | Aceite |
| [ADR-28](0028-escala-automatica-do-booking-service.md) | Escala automática do booking-service pela CPU, entre 2 e 3 réplicas | Aceite |
| [ADR-29](0029-pagamentos-simulados.md) | Pagamentos com um fornecedor simulado e cartões de teste, em vez do Stripe | Aceite |
