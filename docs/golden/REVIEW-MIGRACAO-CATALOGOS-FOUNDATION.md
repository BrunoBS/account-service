# Review — Migração dos Catálogos para Foundation

- **Data:** 2026-09-21/22
- **Branch:** `feat/migrate-catalogs-to-foundation`
- **Base lógica:** `refactor/architectural-alignment-post-g4`
- **Origem funcional:** `BrunoBS/account-api@main`
- **Status:** REVIEW CONCLUÍDO — SEM BLOQUEADORES CONHECIDOS

## 1. Objetivo do review

Validar que todos os CRUDs do legado baseados na capability de catálogo foram migrados
para a nova arquitetura sem copiar a lib, sem reintroduzir `platform-crud` e sem
distribuir catálogos por Core/Feature.

## 2. Completude

Foram encontrados e migrados 16 catálogos:

| Catálogo                           | Estratégia            |
|------------------------------------|-----------------------|
| WorkspaceType (legado AccountType) | EnumCatalogService    |
| ApplicationScopeType               | EnumCatalogService    |
| AuthorizationType                  | EnumCatalogService    |
| EnvironmentType                    | EnumCatalogService    |
| FeatureScopeType                   | DynamicCatalogService |
| FeatureType                        | BaseCatalogService    |
| InfrastructureType                 | EnumCatalogService    |
| LanguageType                       | EnumCatalogService    |
| LifecycleType                      | EnumCatalogService    |
| TagOriginType                      | EnumCatalogService    |
| VisibilityType                     | EnumCatalogService    |
| OnboardingPhase                    | BaseCatalogService    |
| PublisherScopeType                 | EnumCatalogService    |
| SchemaScopeType                    | EnumCatalogService    |
| SchemaType                         | BaseCatalogService    |
| ShareStatusType                    | EnumCatalogService    |

Resultado:

```text
12 Enum
1 Dynamic
3 Base/advanced
16 total
```

## 3. Estrutura arquitetural

Estrutura final:

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

A organização correta é **catalog-first** dentro de Catalog:
`catalog/<catalogo>/domain|repository|usecase`.

Não existem classes Java de catálogo ativas em Core/Feature.

## 4. Contratos preservados

Foram preservados:

- valores e comportamentos dos enums;
- filtros avançados de FeatureType;
- relação FeatureType -> FeatureScopeType;
- relação SchemaType -> SchemaScopeType;
- `FeatureType.available`;
- `OnboardingPhase.orientation`;
- soft delete/restore da lib;
- nomes dos endpoints, exceto a migração intencional
  `/account-type -> /workspace-type`;
- autorização OWNER dos endpoints administrativos.

Renomeações de negócio:

- `AccountType -> WorkspaceType`;
- `ACCOUNT -> WORKSPACE` nos scopes correspondentes;
- fases de onboarding Account -> Workspace.

## 5. Persistência

A V4 cria as tabelas de catálogo.

O review comparou as entidades com o legado e corrigiu dois excessos introduzidos na
primeira implementação:

1. `UNIQUE(name)` físico nos catálogos simples — removido;
2. `description NOT NULL` — removido.

A unicidade lógica simples continua sendo validada por `BaseCatalogValidator`.

Permanecem as unicidades compostas que já existiam no legado:

- FeatureType: `(featureScope, name)`;
- SchemaType: `(schemaScope, name)`.

## 6. Dependências

### platform-catalog

Consumido como capability da Golden Platform Foundation. Nenhuma implementação foi
copiada para o serviço.

### json-schema-validator

Dependência migrada junto por necessidade funcional:

```text
com.networknt:json-schema-validator:3.0.7
```

**Aviso:** é dependência direta nova do `account-service`.

Ela sustenta:

```text
foundation.schema.domain.SchemaDefaults
foundation.schema.usecase.SchemaValidator
```

Catalog adapta o resultado por
`foundation.catalog.support.CatalogSchemaValidationSupport`.

A direção é:

```text
Catalog -> Schema
Schema -X-> Catalog
```

## 7. Findings corrigidos

- imports residuais de `WorkspaceType`/`LifecycleType` antigos;
- teste Workspace que tratava `CATALOG` como inválido;
- mensagens de JSON Schema incompatíveis com o provider atual;
- entendimento incorreto do comportamento de `AuthorizationMock.allow`;
- constraints físicas mais restritivas que o legado;
- validator de Schema inicialmente colocado dentro de Catalog;
- dependência reversa intermediária `Schema -> Catalog`.

Nenhum desses findings permanece aberto no head validado.

## 8. Fitness functions

O head final possui 15 testes/regras arquiteturais.

Além das regras pós-G4, a migração adicionou proteção para:

- estrutura interna das capabilities Foundation;
- todo RestController em Input;
- policy OWNER dos controllers de Catalog;
- `Schema -X-> Catalog`.

## 9. Testes funcionais

`CatalogApiIT` cobre:

- os 13 endpoints padrão/dinâmicos no fluxo CRUD/restore;
- FeatureType;
- SchemaType;
- OnboardingPhase;
- enum inválido;
- settings inválido;
- policy OWNER.

Os 16 endpoints são exercitados.

## 10. Evidência

Verify #110 / run `35674577937`.

Head:

```text
3cd68f9aa0470b4689b8d360100747d13a21de31
```

Resultado:

- 121 fontes principais;
- 11 fontes de teste;
- 15 fitness functions;
- 3 testes unitários;
- 27 testes de integração;
- 45 testes totais;
- 0 falhas;
- 0 erros;
- 4 migrations aplicadas;
- upgrade V2 populada -> latest verde;
- **BUILD SUCCESS**.

## 11. Pendências

Não são bloqueadores desta migração:

- reavaliar a dependência direta `json-schema-validator:3.0.7` quando a capability
  Schema for consolidada;
- o CRUD completo de Schema não faz parte desta onda;
- o merge desta branch depende da conclusão/review da branch pós-G4.

## 12. Conclusão

A migração dos CRUDs de catálogo está tecnicamente concluída e aderente à estrutura
arquitetural definida. Não foi criado substituto para `platform-crud`; a Golden usa
`platform-catalog` como capability e mantém seus catálogos concretos na application
Foundation.
