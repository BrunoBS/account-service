# G1 Checkpoint — Skeleton + Foundation

**Estado:** CONCLUÍDA E REVALIDADA APÓS PATCH DA FOUNDATION  
**Data:** 2026-09-20  
**Bootstrap commit:** `3a5eb731d121aacbc56e97deea744811fee491fb`

## Objetivo

Provar que `account-service` nasce como aplicação nova sobre a Foundation, sem dependência estrutural do legado, consumindo todos os artefatos remotamente e sem infraestrutura implícita que o serviço não escolheu.

## Baseline corrente validado

- repository: `BrunoBS/account-service`;
- package root: `com.empresa.golden`;
- artifact: `com.empresa.golden:account-service:0.1.0-SNAPSHOT`;
- Java 25;
- Spring Boot 4.1.1;
- `com.empresa.platform:platform-parent:1.0.2`;
- `com.empresa.platform:platform-dependencies:1.0.2`;
- `com.empresa.platform:platform-starter:1.0.1`;
- `com.empresa.platform:platform-test-support:1.0.1`;
- GitHub Packages como integração remota;
- autenticação de leitura via `PLATFORM_PACKAGES_TOKEN`.

## Guardrails confirmados

- zero dependências/imports de `platform-crud`;
- nenhum `BaseCrud*` ou equivalente introduzido;
- nenhum código de produção copiado de `account-api`;
- nenhuma persistência adicionada em G1;
- capabilities opcionais permanecem fora do POM até existirem casos de uso reais;
- nenhum `mvn install` local entre repositórios foi usado como solução ou evidência de integração;
- `platform-test-support` básico não deve ativar JDBC, MySQL, Kafka ou Testcontainers no consumidor.

## GAP-0001 — distribuição remota das libraries

A primeira validação da Golden comprovou que `platform-parent:1.0.1` era resolvido remotamente, mas os JARs `platform-starter:1.0.0` e `platform-test-support:1.0.0` ainda não estavam publicados no registry de `platform-libraries`.

A correção adicionou distribuição própria e workflow de publicação ao `platform-libraries`.

Evidências históricas:

- `platform-libraries` Publish #2 — run `35534990418`: **success**;
- `platform-libraries` Verify #48 — run `35534990434`: **success**;
- `account-service` Verify #7 — run `35535402633`: **BUILD SUCCESS**.

## GAP-0002 — infraestrutura de teste carregada implicitamente

Após o GAP-0001, o startup do skeleton chegou aos testes e revelou que `platform-test-support:1.0.0` exportava transitivamente infraestrutura como `spring-boot-starter-jdbc`.

Como a G1 não possui banco, o Spring Boot detectou JDBC no classpath e tentou criar um `DataSource`. Foi usado temporariamente no profile de teste:

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
```

Esse workaround foi rejeitado como padrão da Golden. O comportamento correto é:

```text
não declarou infraestrutura
→ infraestrutura não entra no classpath
→ auto-configuração correspondente não é ativada
```

### Correção da Foundation

Foi realizado patch versionado, sem sobrescrever releases Maven existentes:

```text
platform-build          1.0.1 → 1.0.2
platform-parent         1.0.1 → 1.0.2
platform-dependencies   1.0.1 → 1.0.2
platform-libraries      1.0.0 → 1.0.1
capabilities libraries  1.0.0 → 1.0.1
```

No `platform-test-support:1.0.1`, passaram a ser dependências Maven opcionais:

- `spring-boot-starter-jdbc`;
- `mysql-connector-j`;
- `spring-boot-testcontainers`;
- `spring-boot-starter-kafka`;
- Testcontainers MySQL;
- Testcontainers Kafka;
- Testcontainers JUnit Jupiter.

As capacidades `@WithMySql` e `@WithKafka` permanecem disponíveis, mas a aplicação que realmente as usar declara a infraestrutura correspondente.

Foi adicionado o teste `InfrastructureDependencyOptionalityTest` para proteger esse contrato.

### Evidências da Foundation

`platform-build` Publish #12:

- run `35535997475`;
- release `1.0.2`;
- Maven Enforcer e dependency convergence: sucesso;
- reactor: **BUILD SUCCESS**.

`platform-libraries` Verify #50:

- run `35536158485`;
- release train `1.0.1`;
- `InfrastructureDependencyOptionalityTest`: 1 teste, 0 falhas, 0 erros;
- `platform-test-support`: 35 testes, 0 falhas, 0 erros;
- reactor: **BUILD SUCCESS**.

`platform-libraries` Publish #3:

- run `35536158484`;
- release train `1.0.1`;
- reactor: **BUILD SUCCESS**.

Os workflows de publicação foram novamente estabilizados em `workflow_dispatch` após os releases.

## Prova final no consumidor sem banco

Commit:

`ba2b7fcdd90fa9a35b5e0ff01a14a0b38423fbf8`

Alterações relevantes no `account-service`:

- parent atualizado para `platform-parent:1.0.2`;
- versions das libraries herdadas do parent como `1.0.1`;
- removido completamente o exclude de `DataSourceAutoConfiguration`;
- nenhuma dependência JDBC/MySQL foi adicionada.

GitHub Actions Verify #11:

- run `35536464540`;
- Java 25: sucesso;
- `RequireJavaVersion`: passou;
- `RequireMavenVersion`: passou;
- `DependencyConvergence`: passou;
- `AccountServiceApplicationIT`: 1 teste, 0 falhas, 0 erros;
- `mvn clean verify`: **BUILD SUCCESS**.

Portanto o consumidor sem persistência inicia normalmente sem configuração negativa de JDBC.

## Topologia remota corrente validada

```text
platform-build 1.0.2
  ↓ publish
GitHub Packages / platform-build
  ↓ resolve platform-parent:1.0.2
platform-libraries 1.0.1
  ↓ publish
GitHub Packages / platform-libraries
  ↓ resolve libraries 1.0.1
account-service
  ↓
mvn clean verify
  ↓
BUILD SUCCESS
```

## Critérios de saída G1

- [x] Java 25 / Spring Boot 4.1.1;
- [x] `platform-parent:1.0.2` remoto;
- [x] `platform-starter:1.0.1` remoto;
- [x] `platform-test-support:1.0.1` remoto;
- [x] aplicação mínima compila;
- [x] contexto Spring Boot inicia sem banco;
- [x] nenhuma exclusão manual de DataSource necessária;
- [x] infraestrutura de teste pesada é opt-in;
- [x] Maven Enforcer verde;
- [x] dependency convergence verde;
- [x] GitHub Actions verde;
- [x] nenhum `mvn install` local entre repositórios;
- [x] gaps encontrados pela G1 documentados e corrigidos.

## Decisão

A **G1 — Skeleton + consumo da Foundation permanece concluída após revalidação do patch**.

A G2 está tecnicamente desbloqueada, mas não foi iniciada por esta correção.
