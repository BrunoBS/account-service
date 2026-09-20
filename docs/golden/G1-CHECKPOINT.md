# G1 Checkpoint — Skeleton + Foundation

**Estado:** CONCLUÍDA  
**Data de conclusão:** 2026-09-20  
**Bootstrap commit:** `3a5eb731d121aacbc56e97deea744811fee491fb`

## Objetivo

Provar que `account-service` nasce como aplicação nova sobre `FOUNDATION-GOLDEN-V1`, sem dependência estrutural do legado e consumindo a Foundation publicada remotamente.

## Baseline validado

- repository: `BrunoBS/account-service`;
- package root: `com.empresa.golden`;
- artifact: `com.empresa.golden:account-service:0.1.0-SNAPSHOT`;
- Java 25;
- Spring Boot 4.1.1;
- parent: `com.empresa.platform:platform-parent:1.0.1`;
- runtime Foundation: `platform-starter:1.0.0`;
- test Foundation: `platform-test-support:1.0.0`;
- GitHub Packages como integração remota;
- autenticação do consumidor via `PLATFORM_PACKAGES_TOKEN`.

## Guardrails confirmados

- zero dependências/imports de `platform-crud`;
- nenhum `BaseCrud*` ou equivalente introduzido;
- nenhum código de produção copiado de `account-api`;
- nenhuma persistência adicionada em G1;
- capabilities `audit`, `catalog` e `tagging` permanecem fora do POM até existirem casos de uso reais;
- nenhum `mvn install` local entre repositórios foi utilizado como solução ou evidência de integração.

## Gap de distribuição revelado pela G1

Depois de resolvida a autenticação do GitHub Packages, o Maven conseguiu resolver remotamente:

`com.empresa.platform:platform-parent:1.0.1`

O próximo erro revelou ausência dos artefatos de `platform-libraries`:

```text
com.empresa.platform:platform-starter:jar:1.0.0
com.empresa.platform:platform-test-support:jar:1.0.0
```

O `account-service` foi configurado para consultar os dois registries:

```text
https://maven.pkg.github.com/brunobs/platform-build
https://maven.pkg.github.com/brunobs/platform-libraries
```

Com autenticação válida, o probe confirmou que os artefatos `1.0.0` não existiam no registry correto de `platform-libraries`. Isso eliminou risco de conflito com uma publicação Maven já existente e permitiu manter a versão oficial `1.0.0`.

A causa estava na distribuição da Foundation: `platform-libraries` não possuía publicação própria para seu GitHub Packages.

## Correção na Foundation

Branch:

`BrunoBS/platform-libraries:refactor/golden-foundation`

A correção:

- adicionou `distributionManagement` para o registry de `platform-libraries`;
- adicionou settings de publicação separado;
- manteve `PLATFORM_PACKAGES_TOKEN` para leitura do `platform-build`;
- utiliza o `GITHUB_TOKEN` efêmero do workflow, com `packages: write`, exclusivamente para publicar no registry do próprio `platform-libraries`;
- publicou o reactor oficial `1.0.0`;
- estabilizou o workflow de release em `workflow_dispatch`, seguindo o padrão de `platform-build`.

### Evidência de publicação

Commit de publicação:

`9bcf1e0bf6efa58dd1141e75e8e8a9f8ffd2a44c`

GitHub Actions `Publish Maven packages #2`:

- run: `35534990418`;
- resultado: **success**;
- comando: `mvn ... clean deploy`;
- reactor: **BUILD SUCCESS**.

Artefatos publicados incluem:

- `platform-messaging:1.0.0`;
- `platform-authorization:1.0.0`;
- `platform-audit:1.0.0`;
- `platform-logging:1.0.0`;
- `platform-starter:1.0.0`;
- `platform-test-support:1.0.0`;
- `platform-catalog:1.0.0`;
- `platform-tagging:1.0.0`.

GitHub Actions `Verify #48`:

- run: `35534990434`;
- resultado: **success**.

A correção foi também registrada no `platform-libraries/docs/foundation/FOUNDATION-CHECKPOINT.md`.

## Ajuste do skeleton G1

Após a publicação, o `account-service` passou a resolver a Foundation remotamente e avançou até o teste de startup.

O primeiro teste pós-publicação falhou porque `platform-test-support` inclui infraestrutura JDBC no classpath e a G1 deliberadamente ainda não possui persistência. O Spring tentou autoconfigurar `DataSource`.

Isso foi tratado como configuração do skeleton da Golden, não como novo gap de Foundation.

No profile `test`, a G1 desabilita:

```text
org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration
```

Esse isolamento é temporário ao skeleton sem banco. A persistência real será introduzida somente em G2.

## Evidência final do consumidor

Commit:

`1fb65a7e99103591b36a88465a3d18ff93ff4089`

GitHub Actions `Verify #7`:

- run: `35535402633`;
- runner limpo;
- Java 25: sucesso;
- `platform-parent:1.0.1` resolvido remotamente;
- `platform-starter:1.0.0` resolvido remotamente;
- `platform-test-support:1.0.0` resolvido remotamente;
- Maven Enforcer `RequireJavaVersion`: passou;
- Maven Enforcer `RequireMavenVersion`: passou;
- Maven Enforcer `DependencyConvergence`: passou;
- `AccountServiceApplicationIT`: 1 teste, 0 falhas, 0 erros;
- `mvn clean verify`: **BUILD SUCCESS**.

O commit subsequente `4324ba78ada874f2e9d09f738f6ab6fb79fce649` mantém a mesma árvore funcional do ajuste de teste.

## Topologia remota validada

```text
platform-build
  ↓ publish
GitHub Packages / platform-build
  ↓ resolve platform-parent:1.0.1
platform-libraries
  ↓ publish
GitHub Packages / platform-libraries
  ↓ resolve libraries 1.0.0
account-service
  ↓
mvn clean verify
  ↓
BUILD SUCCESS
```

## Critérios de saída G1

- [x] `platform-parent:1.0.1`;
- [x] resolução remota do parent;
- [x] `platform-starter:1.0.0` remoto;
- [x] `platform-test-support:1.0.0` remoto;
- [x] aplicação mínima compila;
- [x] contexto Spring Boot inicia em teste;
- [x] `platform-test-support` efetivamente utilizado;
- [x] Maven Enforcer verde;
- [x] dependency convergence verde;
- [x] `mvn clean verify` verde;
- [x] GitHub Actions verde;
- [x] nenhuma integração baseada em `mvn install` local;
- [x] gap de distribuição documentado e corrigido.

## Decisão

A **G1 — Skeleton + consumo da Foundation está concluída**.

A G2 fica tecnicamente desbloqueada, mas não é iniciada por este checkpoint.
