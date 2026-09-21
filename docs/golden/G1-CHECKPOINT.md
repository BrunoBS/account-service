# G1 Checkpoint — Skeleton + Foundation consolidada

**Estado:** CONCLUÍDA E REVALIDADA  
**Data da revalidação:** 2026-09-20

## Objetivo

Provar que `account-service` nasce como aplicação nova sobre a Foundation consolidada, sem dependência estrutural do legado, sem instalação local da Foundation e consumindo exclusivamente os artifacts oficiais publicados remotamente.

## Baseline oficial validado

- repository consumidor: `BrunoBS/account-service`;
- package root da aplicação: `br.com.portalmanager.account`;
- artifact da aplicação: `com.empresa.golden:account-service:0.1.0-SNAPSHOT`;
- Java 25;
- Spring Boot 4.1.1;
- Maven >= 3.9.9;
- `br.com.portalmanager.core:platform-parent:1.0.0`;
- `br.com.portalmanager.core:platform-dependencies:1.0.0`;
- `br.com.portalmanager.core:platform-libraries-bom:1.0.0`;
- `br.com.portalmanager.core:platform-starter:1.0.0`;
- `br.com.portalmanager.core:platform-test-support:1.0.0`.

## Repositório e registry da Foundation

Fonte oficial:

```text
BrunoBS/platform-libraries
```

Registry único do consumidor:

```text
https://maven.pkg.github.com/brunobs/platform-libraries
```

O `account-service` não consulta outro registry da Foundation.

## Modelo Maven validado

O parent governa build, Java, plugins, testes e gates.

O BOM das capabilities é importado explicitamente:

```text
platform-parent:1.0.0
        +
platform-libraries-bom:1.0.0
        ↓
platform-starter:1.0.0
platform-test-support:1.0.0
```

As capabilities são declaradas sem versão individual.

## Namespace

O consumidor utiliza o namespace oficial da Foundation:

```text
br.com.portalmanager.core
```

O import de `PlatformIntegrationTest` foi migrado para:

```java
import br.com.portalmanager.core.testing.annotation.PlatformIntegrationTest;
```

Não existe camada de compatibilidade com o namespace provisório anterior.

## Guardrails confirmados

- zero dependências/imports de `platform-crud`;
- nenhum `BaseCrud*` ou equivalente introduzido;
- nenhum código de produção copiado de `account-api`;
- nenhuma persistência adicionada durante G1;
- capabilities opcionais permanecem explícitas;
- infraestrutura pesada de teste continua opt-in;
- nenhuma exclusão manual de `DataSourceAutoConfiguration` é necessária;
- nenhuma instalação local da Foundation é aceita como prova de integração.

## Evidência de migração do consumidor

Commit de migração:

`eb1004e9b05325f6755838a9a5daef7c5af51586`

GitHub Actions Verify #16:

- run `35546160937`;
- parent no namespace oficial;
- BOM das capabilities importado;
- starter/test-support no namespace oficial;
- registry único;
- resultado: **success**.

## Prova com repository Maven isolado

Commit de CI:

`ee0f0a053b457eeba613398258cb81eee1f46ce0`

GitHub Actions Verify #17:

- run `35546171874`;
- repository Maven local isolado em `${RUNNER_TEMP}/account-service-m2`;
- nenhum cache Maven do workflow utilizado;
- comando:

```text
mvn --settings .github/maven-settings.xml \
    --batch-mode \
    --no-transfer-progress \
    -Dmaven.repo.local="${RUNNER_TEMP}/account-service-m2" \
    clean verify
```

Resultado:

- `RequireJavaVersion`: passed;
- `RequireMavenVersion`: passed;
- `RequireProperty`: passed;
- `BanDuplicatePomDependencyVersions`: passed;
- `DependencyConvergence`: passed;
- `AccountServiceApplicationIT`: 1 teste, 0 falhas, 0 erros;
- execução do test-support registrada sob `br.com.portalmanager.core.testing`;
- `BUILD SUCCESS`.

Como o repository Maven utilizado estava isolado e o build não possui checkout/install local da Foundation, o sucesso comprova resolução remota do parent, BOM e capabilities necessários ao consumidor.

## Topologia remota validada

```text
BrunoBS/platform-libraries
        ↓ publish
GitHub Packages / platform-libraries
        ↓
platform-parent:1.0.0
platform-dependencies:1.0.0
platform-libraries-bom:1.0.0
platform-starter:1.0.0
platform-test-support:1.0.0
        ↓
account-service
        ↓
repository Maven local isolado
        ↓
mvn clean verify
        ↓
BUILD SUCCESS
```

## Critérios de saída G1

- [x] namespace oficial `br.com.portalmanager.core`;
- [x] `platform-parent:1.0.0`;
- [x] `platform-libraries-bom:1.0.0`;
- [x] `platform-starter:1.0.0`;
- [x] `platform-test-support:1.0.0`;
- [x] apenas o registry oficial de `platform-libraries`;
- [x] resolução remota em repository Maven isolado;
- [x] aplicação mínima compila;
- [x] contexto Spring Boot inicia;
- [x] Maven Enforcer verde;
- [x] dependency convergence verde;
- [x] GitHub Actions verde;
- [x] nenhum `mvn install` local da Foundation;
- [x] nenhum `platform-crud`;
- [x] documentação de consumo atualizada.

## FOUNDATION-GOLDEN-V1

A Golden Reference fornece a prova downstream final de que a Foundation consolidada publicada pode ser consumida remotamente por uma aplicação real.

Para o consumidor Golden, o checkpoint `FOUNDATION-GOLDEN-V1` está formalmente validado.

## Próxima etapa

A **G1 — Skeleton + consumo da Foundation está concluída**.

A próxima onda do plano é G2 — Persistência e modelo mínimo. Antes de implementar persistência, permanecem as decisões específicas da Golden já registradas no plano, especialmente ferramenta de migrations e modelagem final do lifecycle de Account.

Nenhuma alteração na Foundation é autorizada por este checkpoint.
