# Plano — Migração dos CRUDs de Catálogo para a Foundation da Golden

- **Data:** 2026-09-21
- **Branch:** `feat/migrate-catalogs-to-foundation`
- **Baseline:** `21d3b0474be6057ea663800e6bcdb93447d5410e`
- **Origem funcional:** `BrunoBS/account-api@main`
- **Destino:** `BrunoBS/account-service`
- **Status:** CONCLUÍDO APÓS REVIEW

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
│       └── <catalogo>
│           ├── domain
│           ├── repository
│           └── usecase
└── input
    └── web
        └── catalog
            └── <catalog>
```

Entidades e enums ficam em `domain`; repositories em `repository`; services e
contratos auxiliares do CRUD em `usecase`; controllers ficam exclusivamente em Input.

## 4. Inventário — 16 catálogos

| Catálogo legado      | Destino              | Estratégia da lib     | Endpoint destino               |
| -------------------- | -------------------- | --------------------- | ------------------------------ |
| AccountType          | WorkspaceType        | EnumCatalogService    | /api/v1/workspace-type         |
| ApplicationScopeType | ApplicationScopeType | EnumCatalogService    | /api/v1/application-scope-type |
| AuthorizationType    | AuthorizationType    | EnumCatalogService    | /api/v1/authorization-type     |
| EnvironmentType      | EnvironmentType      | EnumCatalogService    | /api/v1/environment-type       |
| FeatureScopeType     | FeatureScopeType     | DynamicCatalogService | /api/v1/feature-scope          |
| FeatureType          | FeatureType          | BaseCatalogService    | /api/v1/feature-type           |
| InfrastructureType   | InfrastructureType   | EnumCatalogService    | /api/v1/infrastructure-type    |
| LanguageType         | LanguageType         | EnumCatalogService    | /api/v1/language-type          |
| LifecycleType        | LifecycleType        | EnumCatalogService    | /api/v1/lifecycle-type         |
| TagOriginType        | TagOriginType        | EnumCatalogService    | /api/v1/tag-origin-type        |
| VisibilityType       | VisibilityType       | EnumCatalogService    | /api/v1/visibility-type        |
| OnboardingPhase      | OnboardingPhase      | BaseCatalogService    | /api/v1/onboarding-type        |
| PublisherScopeType   | PublisherScopeType   | EnumCatalogService    | /api/v1/publisher-scope-type   |
| SchemaScopeType      | SchemaScopeType      | EnumCatalogService    | /api/v1/schema-scope           |
| SchemaType           | SchemaType           | BaseCatalogService    | /api/v1/schema-type            |
| ShareStatusType      | ShareStatusType      | EnumCatalogService    | /api/v1/share-status-type      |

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

O legado valida `settings` dos catálogos com JSON Schema Draft 2020-12.

Para preservar esse comportamento foi migrada a dependência direta:

```text
com.networknt:json-schema-validator:3.0.7
```

A dependência funcional foi organizada conforme a arquitetura vigente:

```text
foundation.catalog
        ↓
foundation.schema
        ↓
com.networknt:json-schema-validator
```

Foram migrados para a application Foundation:

- `foundation/schema/domain/SchemaDefaults`;
- `foundation/schema/usecase/SchemaValidator`.

Catalog possui somente o adapter
`foundation/catalog/support/CatalogSchemaValidationSupport`, que converte o
resultado de Schema para `CatalogValidationResult`.

**Atenção:** `json-schema-validator:3.0.7` é uma nova dependência direta do
`account-service`. Ela não foi adicionada a `platform-libraries` nem a
`platform-build`. Deve ser reavaliada quando a capability Schema for consolidada de
forma completa.

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

## 13. Resultado final do review

A implementação foi revisada contra o padrão arquitetural vigente e contra o
`account-api` funcional.

### Estrutura confirmada

```text
foundation
├── catalog
│   └── <catalogo>
│       ├── domain
│       ├── repository
│       └── usecase
└── schema
    ├── domain
    └── usecase

input
└── web
    └── catalog/<catalogo>
```

A organização adotada é **catalog-first**: cada catálogo é um módulo interno de
`foundation/catalog` e, dentro dele, aparecem as layers
`domain|repository|usecase`.

### Estratégias finais

- 12 catálogos usam `EnumCatalogService`;
- 1 catálogo, `FeatureScopeType`, usa `DynamicCatalogService`;
- 3 catálogos, `FeatureType`, `SchemaType` e `OnboardingPhase`, permanecem em
  `BaseCatalogService` por possuírem contrato/campos/relações adicionais.

### Findings corrigidos durante o review

- referências residuais aos enums antigos foram removidas;
- o contrato de messaging das mensagens de Schema foi alinhado ao provider classpath;
- a policy OWNER foi validada pelo contrato real do `AuthorizationMock`;
- `UNIQUE(name)` físico introduzido inicialmente nos catálogos simples foi removido
  para preservar o legado; somente FeatureType e SchemaType mantêm unicidade composta;
- `description` voltou a ser nullable na migration, conforme `BaseCatalogEntity`;
- a dependência de validação JSON foi movida para `foundation.schema`;
- a direção interna `Schema -> Catalog` foi eliminada e passou a ser protegida por
  fitness function;
- não existe dependência ou import de `platform-crud`.

### Evidência técnica

Verify #110 / run `35674577937`, head
`3cd68f9aa0470b4689b8d360100747d13a21de31`:

- 121 fontes principais;
- 11 fontes de teste;
- 15 fitness functions arquiteturais;
- 3 testes unitários;
- 27 testes de integração;
- 45 testes totais;
- 0 falhas;
- 0 erros;
- Flyway V1 -> V4 validado;
- upgrade V2 populada -> V4 validado;
- 16 endpoints de catálogo exercitados;
- **BUILD SUCCESS**.

**Status:** implementação, testes e review concluídos. Merge/checkpoint permanecem fora
desta execução.
