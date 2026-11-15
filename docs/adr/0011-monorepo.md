# ADR-11: Monorepo com um módulo, uma imagem e um pipeline por serviço

- **Estado:** Aceite
- **Data:** 2026-11-14
- **Decisores:** Nuno Soares, Vitor Sousa
- **Referências:** Proposta §12.1, §14.3; backlog RP-01

## Contexto

A equipa tem duas pessoas e os docentes precisam de um acesso simples para a avaliação. Mesmo assim, os serviços têm de ser construídos e instalados de forma independente. Como todos os módulos herdam do mesmo POM pai, as versões do Java e do Spring são as mesmas em todo o sistema, e o Spring Cloud só é compatível com algumas linhas do Spring Boot.

## Decisão

Um repositório privado no GitHub com um build Maven multi-módulo: um POM pai, um módulo por serviço, o gateway, o Config Server e o `common-events`.

- Cada serviço gera a sua própria imagem e é instalado de forma independente.
- Um único workflow do GitHub Actions deteta os módulos alterados e constrói apenas esses, com `mvn -pl <service> -am verify`.
- O único código partilhado é o `common-events` (contratos das mensagens e constantes). Não se partilha código de domínio.
- O POM pai fixa Java 25, Spring Boot 4.1.x e Spring Cloud 2025.1.x, uma combinação da matriz oficial (`start.spring.io`). O Java 25 serve para compilar (`--release 25`) e para correr: as imagens usam um JRE 25.
- O build só aceita o JDK 25: o `maven-enforcer-plugin` exige `[25,26)`. O `.sdkmanrc` na raiz fixa o JDK local e o CI usa `actions/setup-java` com a versão 25.
- O Maven Wrapper (`mvnw`) fixa a versão do Maven, por isso quem clona o repositório, incluindo os docentes, só precisa do JDK 25.

## Alternativas consideradas

- **Um repositório por serviço** — acrescenta trabalho para uma equipa de dois e para a avaliação, e torna mais difíceis as alterações de contratos entre serviços.
- **Java 21** — é a LTS anterior e também é suportada, mas obrigava a instalar outro JDK e perdia as melhorias do Java 25.
- **Aceitar qualquer JDK a partir do 25 (`[25,)`)** — um JDK 26 entrava no build sem uma decisão explícita, e o código podia comportar-se de forma diferente em cada máquina.
- **Spring Boot 4.0.x** — também é compatível com o Spring Cloud 2025.1, mas é a linha mais antiga e deixa de ter suporte mais cedo.

## Consequências

### Positivas

- Um só sítio para o código, os ADR, as especificações da API e os manifestos Kubernetes.
- Alterações de contratos ficam atómicas num único pull request.
- O mesmo JDK no desenvolvimento, no CI e nas imagens. O Java 25 traz compact object headers (opcionais) e melhorias no arranque com AOT, que ajudam no orçamento de memória do k3d (ADR-16).

### Negativas

- Exige disciplina para não partilhar código de domínio.
- Uma alteração no `common-events` ou no POM pai reconstrói todos os serviços.
- Quem não tiver o JDK 25 tem de o instalar antes de construir.
- Mudar de JDK obriga a uma nova ADR e a atualizar o enforcer, o `.sdkmanrc`, o CI e as imagens.
