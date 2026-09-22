# Plano — Migração dos CRUDs de Catálogo para a Foundation da Golden

- **Data:** 2026-09-21
- **Branch:** `feat/migrate-catalogs-to-foundation`
- **Baseline:** `21d3b0474be6057ea663800e6bcdb93447d5410e`
- **Origem funcional:** `BrunoBS/account-api@main`
- **Destino:** `BrunoBS/account-service`
- **Status:** EM EXECUÇÃO

## 1. Objetivo

Migrar para o `account-service` todos os CRUDs do `account-api` que utilizam a
capability `platform-catalog`, posicionando-os na macrozona `foundation/catalog`
definida pela arquitetura vigente.

A migração reutiliza a lib `platform-catalog`. Não copia a implementação da lib e não
reintroduz `platform-crud`.

## 2. Regra de ownership

Todo catálogo concreto da Golden Reference pertence à application foundation macrozone:

```text
platform-libraries/platform-catalog
        ↓ fornece infraestrutura
account-service/foundation/catalog
        ↓ declara os catálogos concretos
input/web/catalog
        ↓ expõe os endpoints HTTP
```

## 3. Estrutura alvo

```text
br.com.itau.portalmanager.workspace
├── foundation
│   └── catalog
│       ├── domain
│       │   └── <catalog>
│       ├── repository
│       │   └── <catalog>
│       └── usecase
│           └── <catalog>
└── input
    └── web
        └── catalog
            └── <catalog>
```

Entidades e enums ficam em `domain`; repositories em `repository`; services e
contratos auxiliares do CRUD em `usecase`; controllers ficam exclusivamente em Input.

## 4. Inventário — 16 catálogos

| Catálogo legado | Destino | Estratégia da lib | Endpoint destino |
|---|---|---|---|
| AccountType | WorkspaceType | EnumCatalogService | /api/v1/workspace-type |
| ApplicationScopeType | ApplicationScopeType | EnumCatalogService | /api/v1/application-scope-type |
| AuthorizationType | AuthorizationType | EnumCatalogService | /api/v1/authorization-type |
| EnvironmentType | EnvironmentType | EnumCatalogService | /api/v1/environment-type |
| FeatureScopeType | FeatureScopeType | DynamicCatalogService | /api/v1/feature-scope |
| FeatureType | FeatureType | BaseCatalogService | /api/v1/feature-type |
| InfrastructureType | InfrastructureType | EnumCatalogService | /api/v1/infrastructure-type |
| LanguageType | LanguageType | EnumCatalogService | /api/v1/language-type |
| LifecycleType | LifecycleType | EnumCatalogService | /api/v1/lifecycle-type |
| TagOriginType | TagOriginType | EnumCatalogService | /api/v1/tag-origin-type |
| VisibilityType | VisibilityType | EnumCatalogService | /api/v1/visibility-type |
| OnboardingPhase | OnboardingPhase | BaseCatalogService | /api/v1/onboarding-type |
| PublisherScopeType | PublisherScopeType | EnumCatalogService | /api/v1/publisher-scope-type |
| SchemaScopeType | SchemaScopeType | EnumCatalogService | /api/v1/schema-scope |
| SchemaType | SchemaType | BaseCatalogService | /api/v1/schema-type |
| ShareStatusType | ShareStatusType | EnumCatalogService | /api/v1/share-status-type |

## 5. Regras de migração dos services

- catálogos que já usam `EnumCatalogService` continuam usando `EnumCatalogService`;
- `FeatureScopeType`, que usa `DynamicCatalogService`, continua dinâmico;
- `FeatureType`, `SchemaType` e `OnboardingPhase` continuam usando
  `BaseCatalogService`, pois possuem DTO/campos/relações adicionais;
- nenhum catálogo será forçado a Enum apenas para eliminar `BaseCatalogService`;
- não existe dependência de `platform-crud` no destino.

## 6. Account -> Workspace

Ocorrências de Account que representam o domínio principal serão migradas
conceitualmente:

- `AccountType` -> `WorkspaceType`;
- `AccountTypeEnum` -> `WorkspaceTypeEnum`;
- valores de scope `ACCOUNT` -> `WORKSPACE` quando o significado for ownership do
  domínio principal;
- fases `ACCOUNT_REGISTRATION` e `ACCOUNT_FIRST_ENVIRONMENT` ->
  `WORKSPACE_REGISTRATION` e `WORKSPACE_FIRST_ENVIRONMENT`;
- schema type/scope que representam o domínio principal usam `WORKSPACE`.

Ocorrências técnicas externas legítimas não são renomeadas por substituição textual.

## 7. Dependências migradas junto

### platform-catalog

O `account-service` passará a declarar explicitamente:

```xml
br.com.portalmanager.platform:platform-catalog
```

A versão permanece fornecida pelo BOM da Golden Platform Foundation.

### JSON Schema validator

O legado valida `settings` dos catálogos com JSON Schema Draft 2020-12. O
`account-service` ainda não possui essa dependência.

Para preservar esse comportamento será migrada a dependência:

```text
com.networknt:json-schema-validator:3.0.7
```

e um validator local compatível com `CatalogValidationResult`.

**Atenção:** esta é uma nova dependência direta do `account-service`. Ela não altera
`platform-libraries` nem `platform-build` nesta atividade. Deve ser reavaliada quando
a capability Foundation Schema for implementada de forma completa.

## 8. Persistência

Será criada uma nova migration Flyway após V3 contendo as tabelas dos 16 catálogos,
com os campos padrão de `BaseCatalogEntity`:

```text
id
name
label
description
sort_order
is_active
settings
```

e campos adicionais:

- `FeatureType.featureScope`;
- `FeatureType.available`;
- `SchemaType.schemaScope`;
- `OnboardingPhase.orientation`.

Constraints físicas preservarão a identidade real, incluindo unicidade composta para
FeatureType e SchemaType.

As migrations históricas V1-V3 não serão reescritas.

## 9. Domínio Workspace atual

Os enums simples já existentes serão adequados para coexistir com os catálogos
persistidos:

- `WorkspaceType` enum atual -> `WorkspaceTypeEnum`;
- `LifecycleType` enum atual -> `LifecycleTypeEnum`;
- novas entidades de catálogo recebem os nomes `WorkspaceType` e `LifecycleType`.

O agregado Workspace continua persistindo seus valores enum como STRING nesta onda.
O CRUD administrativo do catálogo é persistido separadamente pelas tabelas `type_*`.

## 10. Autorização

Todos os endpoints de administração de catálogo preservam a regra do legado:

```text
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
```

## 11. Testes

A migração deve provar:

1. aplicação sobe com `platform-catalog`;
2. migration cria as tabelas;
3. CRUD padrão funciona para catálogo Enum;
4. catálogo Dynamic aceita names em runtime;
5. FeatureType preserva relação, filtros e `available`;
6. SchemaType preserva scope e unicidade composta;
7. OnboardingPhase preserva `orientation` e enum validation;
8. soft delete/restore continuam funcionando;
9. endpoints exigem autorização OWNER;
10. arquitetura mantém Catalog em Foundation e controllers em Input;
11. `mvn clean verify` e GitHub Actions ficam verdes.

## 12. Fora de escopo

- copiar código de `platform-catalog` para o serviço;
- recriar `platform-crud`;
- alterar a implementação da Golden Platform Foundation sem evidência concreta;
- implementar a capability completa de Schema;
- migrar CRUDs do legado que não sejam catálogos nesta onda.
