# Execução — Migração de Catálogos para Foundation

- **Branch:** `feat/migrate-catalogs-to-foundation`
- **Origem:** `BrunoBS/account-api@main`
- **Destino:** `BrunoBS/account-service`
- **Status:** EM VALIDAÇÃO
- **Primeiro commit de implementação:** `ffde1d0c7a9cb96364d68bdd33f71af2e4d1c517`

## Escopo implementado inicialmente

- 16 CRUDs de catálogo migrados;
- `platform-catalog` consumido explicitamente;
- sem `platform-crud`;
- entidades/enums em `foundation/catalog/domain`;
- repositories em `foundation/catalog/repository`;
- services/DTOs/mappers/validators em `foundation/catalog/usecase`;
- controllers em `input/web/catalog`;
- migration V4 para tabelas `type_*`;
- `AccountType -> WorkspaceType`;
- scopes/fases de negócio `ACCOUNT -> WORKSPACE`;
- casos avançados preservados em `BaseCatalogService`;
- `FeatureScopeType` preservado em `DynamicCatalogService`.

## Dependências adicionais

### platform-catalog

Dependência explícita da Golden Platform Foundation.

### com.networknt:json-schema-validator:3.0.7

Migrada do legado porque os CRUDs de catálogo validam o campo `settings` com JSON
Schema Draft 2020-12.

Essa dependência está restrita ao consumidor nesta onda. Não foi movida para
`platform-libraries` nem para `platform-build`.

## Validação

Pendente de `mvn clean verify`/GitHub Actions. Findings de compilação, migration,
arquitetura ou comportamento serão registrados e corrigidos antes de concluir a onda.
