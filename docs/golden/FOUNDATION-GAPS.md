# Golden Reference — Foundation Gaps

**Status:** 2 gaps confirmados e resolvidos durante G1.

Este arquivo registra somente deficiências reproduzíveis da Foundation encontradas durante a construção da Golden.

## GAP-0001 — Distribuição remota de platform-libraries para consumidores Golden

**Status:** RESOLVIDO  
**Detectado em:** G1 — Skeleton + consumo da Foundation  
**Data:** 2026-09-20

**Capability:** distribuição/build da Foundation.

**Cenário reproduzível:**  
Um serviço independente consome o parent e as libraries e executa `mvn clean verify` em runner limpo, autenticado no GitHub Packages.

**Comportamento esperado:**  
O parent deve ser resolvido pelo registry de `platform-build` e os JARs das capabilities pelo registry de `platform-libraries`.

**Comportamento encontrado:**  
O parent era resolvido remotamente, mas `platform-starter:1.0.0` e `platform-test-support:1.0.0` ainda não estavam publicados no registry correto.

**Correção:**  
Distribuição própria, settings e workflow de publicação para `platform-libraries`.

**Evidência:**  

- `platform-libraries` Verify #48 — run `35534990434`: **success**;
- `platform-libraries` Publish #2 — run `35534990418`: **BUILD SUCCESS**;
- `account-service` Verify #7 — run `35535402633`: **BUILD SUCCESS**.

**Decisão:** encerrado.

## GAP-0002 — platform-test-support impõe JDBC ao consumidor sem banco

**Status:** RESOLVIDO  
**Detectado em:** G1 — startup mínimo sem persistência  
**Data:** 2026-09-20

**Capability:** `platform-test-support`.

**Cenário reproduzível:**  
Um serviço Spring Boot sem persistência declara somente `platform-test-support` em test scope e executa um `@PlatformIntegrationTest`.

**Comportamento esperado:**  
Sem dependência de banco declarada pela aplicação, JDBC/MySQL/Testcontainers não devem entrar transitivamente nem provocar criação de `DataSource`.

**Comportamento encontrado:**  
`platform-test-support:1.0.0` declarava `spring-boot-starter-jdbc` e demais infraestruturas de teste como dependências não opcionais. O Spring Boot detectava JDBC e ativava `DataSourceAutoConfiguration`, causando falha por ausência de driver/configuração no skeleton G1.

**Workaround rejeitado:**  
Excluir manualmente `DataSourceAutoConfiguration` no consumidor. Esse workaround mascara o acoplamento e transfere para cada aplicação a responsabilidade de desligar infraestrutura que nunca pediu.

**Correção:**  
No `platform-test-support:1.0.1`, JDBC, driver MySQL, Kafka e Testcontainers foram marcados como dependências Maven opcionais. `@WithMySql` e `@WithKafka` continuam explícitos e exigem as dependências correspondentes somente nos serviços que usam essas capacidades.

Foi criado `InfrastructureDependencyOptionalityTest` para garantir o contrato.

**Versionamento:**  

- `platform-build/platform-parent/platform-dependencies:1.0.2`;
- release train `platform-libraries:1.0.1`.

Nenhum release existente foi sobrescrito.

**Evidência:**  

- `platform-build` Publish #12 — run `35535997475`: **BUILD SUCCESS**;
- `platform-libraries` Verify #50 — run `35536158485`: **BUILD SUCCESS**;
- `platform-libraries` Publish #3 — run `35536158484`: **BUILD SUCCESS**;
- `account-service` Verify #11 — run `35536464540`: **BUILD SUCCESS sem exclude de DataSource**.

**Decisão:** encerrado. O contrato passa a ser “infraestrutura ausente não é carregada implicitamente”.

## Regra para novos gaps

Para cada novo gap registrar capability, cenário reproduzível, comportamento esperado/atual, evidência, impacto, alternativas, recomendação e decisão.

Preferência arquitetural, redução de linhas ou conveniência local não constituem gap de Foundation.

Nenhuma nova alteração em `platform-build` ou `platform-libraries` deve ser iniciada sem evidência e decisão explícita.
