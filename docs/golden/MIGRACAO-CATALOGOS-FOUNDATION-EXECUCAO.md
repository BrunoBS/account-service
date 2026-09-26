# Execução — Migração de Catálogos para Foundation

- **Branch:** `feat/migrate-catalogs-to-foundation`
- **Origem:** `BrunoBS/account-api@main`
- **Destino:** `BrunoBS/account-service`
- **Status:** CONCLUÍDA APÓS REVIEW
- **Baseline:** `21d3b0474be6057ea663800e6bcdb93447d5410e`
- **Head de código validado:** `3cd68f9aa0470b4689b8d360100747d13a21de31`

## Escopo executado

Foram migrados os 16 CRUDs do legado que utilizam a capability de catálogo.

### Estratégias

- 12 x `EnumCatalogService`;
- 1 x `DynamicCatalogService` — `FeatureScopeType`;
- 3 x `BaseCatalogService` — `FeatureType`, `SchemaType` e
  `OnboardingPhase`.

Os casos avançados permaneceram no abstrato, conforme decisão da execução; nenhum enum
artificial foi criado para forçá-los ao `EnumCatalogService`.

## Estrutura final

```text
br.com.itau.portalmanager.workspace
├── foundation
│   ├── catalog
│   │   └── <catalogo>
│   │       ├── domain
│   │       ├── repository
│   │       └── usecase
│   └── schema
│       ├── domain
│       └── usecase
└── input
    └── web
        └── catalog
```

Cada catálogo é um módulo interno de `foundation/catalog` e contém suas próprias layers `domain`, `repository` e
`usecase`.

Nenhum catálogo Java ativo ficou em Core ou Feature.

## Account -> Workspace

Foram migradas semanticamente as ocorrências de Account que representam o domínio:

- `AccountType -> WorkspaceType`;
- `AccountTypeEnum -> WorkspaceTypeEnum`;
- `ACCOUNT -> WORKSPACE` nos scopes correspondentes;
- `ACCOUNT_REGISTRATION -> WORKSPACE_REGISTRATION`;
- `ACCOUNT_FIRST_ENVIRONMENT -> WORKSPACE_FIRST_ENVIRONMENT`;
- tabela de tipo principal `type_accounts -> type_workspaces`.

O enum WorkspaceType passa a preservar o conjunto legado:

```text
ADMIN
MANAGER
CATALOG
```

Lifecycle também preserva:

```text
ACTIVE
INACTIVE
PENDING_DELETION
```

## Dependências

### Golden Platform Foundation

Foi adicionada a capability:

```text
br.com.portalmanager.platform:platform-catalog
```

A versão continua governada pelo `platform-libraries-bom`.

### Dependência externa migrada junto

```text
com.networknt:json-schema-validator:3.0.7
```

Ela é necessária para preservar a validação de `settings` do legado.

**Aviso:** é uma dependência direta nova do consumidor. Não foi promovida para
`platform-build` ou `platform-libraries` nesta atividade.

A dependência funcional correspondente foi organizada em:

```text
foundation.schema.domain.SchemaDefaults
foundation.schema.usecase.SchemaValidator
```

Catalog depende de Schema por meio de
`foundation.catalog.support.CatalogSchemaValidationSupport`.

## Persistência

Foi criada:

```text
V4__create_catalogs.sql
```

A migration cria as 16 tabelas de catálogo e expande os checks do agregado Workspace
para os valores de enum migrados.

O review removeu restrições físicas que não existiam no legado:

- catálogos simples não possuem `UNIQUE(name)` físico;
- `description` é nullable conforme `BaseCatalogEntity`.

Foram preservadas as identidades compostas reais:

- `FeatureType -> (featureScope, name)`;
- `SchemaType -> (schemaScope, name)`.

As migrations V1-V3 não foram alteradas.

## Contratos HTTP

Os 16 controllers residem em:

```text
input.web.catalog.<catalogo>
```

Os endpoints do legado foram preservados, com a mudança conceitual:

```text
/api/v1/account-type -> /api/v1/workspace-type
```

Todos os controllers declaram:

```text
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
```

A policy OWNER também é protegida por fitness function.

## Findings durante a execução

### Compilação

A primeira implementação revelou referências residuais aos enums anteriores. Foram
corrigidas para `WorkspaceTypeEnum` e `LifecycleTypeEnum`.

### Messaging

A validação JSON inicialmente repassava texto técnico como se fosse message key. O
provider atual exige chave qualificada e definição no bundle. As mensagens de Schema
foram migradas para o contrato `workspace-service.schema.*`, preservando o detalhe
técnico como parâmetro.

### Authorization

O teste inicial esperava 403 usando `AuthorizationMock.allow`. O contrato real do mock
autoriza a requisição e permite verificar a policy solicitada. O teste foi corrigido
para validar `verifyCalledWithPolicy("OWNER")`.

### Persistência

O review identificou `UNIQUE(name)` e `description NOT NULL` adicionados por
conveniência na primeira V4. Ambos foram corrigidos para preservar o modelo legado e o
contrato de `BaseCatalogEntity`.

### Catalog -> Schema

O validator de settings foi inicialmente colocado dentro de Catalog e depois movido
para `foundation.schema`, conforme a arquitetura oficial.

Uma versão intermediária criava dependência `Schema -> Catalog`; ela foi eliminada.
A direção final é:

```text
Catalog -> Schema
Schema -X-> Catalog
```

e existe fitness function para protegê-la.

## Fitness functions

`GoldenArchitectureTest` possui 15 regras/testes no head validado, incluindo:

- topologia de macrozonas;
- composition root;
- estrutura interna de `foundation.catalog` e `foundation.schema`;
- controllers somente em Input;
- `Schema -X-> Catalog`;
- direção Foundation/Core/Feature/Input;
- independência de Domain;
- Input sem bypass de internals Core/Feature;
- Catalog controllers com policy OWNER;
- controllers sem Repository/Integration;
- colaboração cross-module somente por contratos de Use Case;
- identificação de módulos aninhados.

## Testes funcionais de Catalog

`CatalogApiIT` possui 6 cenários e exercita os 16 endpoints:

- CRUD completo dos catálogos padrão e Dynamic;
- FeatureType relacionado;
- SchemaType relacionado;
- OnboardingPhase com `orientation`;
- validação de enum/settings;
- policy OWNER.

## Evidência final de código

Verify #110 / run `35674577937`:

- head: `3cd68f9aa0470b4689b8d360100747d13a21de31`;
- Java 25;
- 121 fontes principais;
- 11 fontes de teste;
- 15 fitness functions;
- 3 testes unitários;
- 27 testes de integração;
- 45 testes totais;
- 0 falhas;
- 0 erros;
- Flyway V4 aplicada com sucesso;
- upgrade V2 populada -> latest validado;
- **BUILD SUCCESS**.

## Pendências conscientes

- `json-schema-validator:3.0.7` permanece dependência direta do consumidor até a
  consolidação completa da capability Schema;
- esta atividade não migra o CRUD completo de Schema, somente a dependência de validação
  necessária aos catálogos;
- merge e checkpoint dependem do review da PR e da conclusão da branch pós-G4.
