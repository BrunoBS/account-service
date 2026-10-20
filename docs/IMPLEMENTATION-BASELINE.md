# Workspace Service — baseline implementado e fontes de verdade

> Revisão documental de 2026-10-20. Repositório Git: `BrunoBS/account-service`; artifact: `workspace-service`. O nome do repositório é histórico, **não** o nome do serviço.

## Stack confirmada

`pom.xml`: Java 25 via `br.com.portalmanager.platform.library:platform-parent:1.0.0`, Spring Boot 4.1.1 pelo parent/BOM tecnológico, `platform-libraries-bom:1.0.0`, JPA, Flyway/MySQL, JSON Schema Validator e capabilities de runtime da Foundation. Testes via `platform-testing-http/database/authorization/redis`.

`application.yml`: Flyway habilitado; `spring.jpa.hibernate.ddl-auto=validate`; `spring.jpa.open-in-view=false`.

## Arquitetura verificada

O teste `GoldenArchitectureTest` impõe zonas `config`, `foundation`, `core`, `feature`, `entrypoint`. Controllers REST pertencem a `entrypoint`; `foundation` não depende de `core/feature/entrypoint`; `core` não depende de `feature/entrypoint`; classes internas não dependem de `entrypoint`. Os endpoints exigem `AuthorizationContext`.

Responsabilidades internas incluem `domain`, `usecase`, `repository`, `facade`, `integration`, com organização por domínio. A implementação não usa o modelo genérico `controller/service/repository/model` em cada feature.

## Segurança

`@AuthorizationRequired` autoriza a operação; `@ResourceVisibility` limita registros de entidades JPA anotadas com `@AuthorizerGroup`. A lib recusa sessão ausente e grupos ausentes, exceto OWNER. Métodos especializados de sharing podem consultar recursos entre escopos após validação de contrato/participação; não devem ser substituídos mecanicamente por consultas comuns com filtro.

Consultas SQL nativas devem aplicar escopo explicitamente: não presumir que o filtro Hibernate opere nelas.

## Domínio e schemas

`Workspace` encapsula transições de lifecycle; operações de sharing e outras features mantêm validações específicas de estado no domínio/caso de uso. `SchemaDefinitionValidationAdapter` usa JSON Schema Draft 2020-12; `SchemaVersionCommandService` implementa draft, publish e delete draft com locking transacional.

JSON Schema não substitui validação de invariantes e autorização de negócio.

## Persistência

O repositório contém migrations Flyway `V1` até `V83`. `PublishedMigrationChecksumTest` protege checksums listados na fixture aplicada; `DatabaseUpgradeMigrationIT` testa migração de dados de V2 ao esquema mais recente. **Não editar migration já aplicada**. `@Version`, unique constraints, locks pessimistas, paginação e projections são usados conforme a necessidade.

## Testes e CI

A árvore atual contém 109 arquivos Java em `src/test` (71 `*IT.java`, 35 `*Test.java` e classes auxiliares). Há testes de arquitetura, API, autorização, schema, migrations e concorrência. `.github/workflows/verify.yml` configura Java 25, MySQL 8.4.11 e `mvn clean verify` com repositório Maven isolado. A existência do workflow não comprova o resultado de uma execução específica.

## Documentos vigentes e históricos

- [README do serviço](../README.md): introdução, features e APIs.
- [Foundation Usage](golden/FOUNDATION-USAGE.md): integração com as libraries.
- [API Conventions](golden/API-CONVENTIONS.md): convenções e exemplos do Workspace.
- [Review Standard](REVIEW-STANDARD.md): revisão técnica.
- [Refinamentos](refinamentos/): registros de decisões, hipóteses e versões; **não são contratos atuais por padrão**.
- [ADRs](golden/): decisões históricas; conferir supersessão e implementação antes de aplicar.

A documentação do `guideline-java` não pode contrariar os testes arquiteturais da Golden. Para parent Maven vigente, usar `platform-libraries/platform-parent`, não o repositório legado `platform-build`.
