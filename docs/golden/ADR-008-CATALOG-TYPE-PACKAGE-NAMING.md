# ADR-008 — Nomenclatura dos módulos de Catalog Type

- **Status:** ACEITO
- **Data:** 2026-09-22
- **Escopo:** Golden Reference / application foundation macrozone

## Contexto

O ADR-007 definiu a organização catalog-first:

```text
foundation/catalog/<catalogo>/{domain,repository,usecase}
```

Os módulos de catálogo representam tipos administráveis. O nome do package deve tornar
essa natureza explícita e evitar nomes ambíguos como `applicationscope`,
`workspace` ou `lifecycle`.

## Decisão

Todo módulo concreto dentro de `foundation.catalog` deve terminar em `type`.

Quando a entidade principal já termina em `Type`, o nome do módulo deve corresponder
ao nome completo da entidade em minúsculas:

```text
ApplicationScopeType -> applicationscopetype
AuthorizationType    -> authorizationtype
FeatureScopeType     -> featurescopetype
WorkspaceType        -> workspacetype
SchemaType           -> schematype
```

A estrutura permanece catalog-first:

```text
foundation/catalog/applicationscopetype/
├── domain
├── repository
└── usecase
```

Módulos que já atendem à convenção permanecem inalterados. Por exemplo:

```text
SchemaType       -> schematype
OnboardingPhase  -> onboardingphasetype
```

A decisão não altera os packages HTTP:

```text
input/web/catalog/<catalogo>
```

Packages técnicos compartilhados, como `foundation.catalog.support`, não representam
catálogos concretos e ficam fora dessa convenção.

## Proteção arquitetural

A Golden mantém duas fitness functions complementares:

- `concreteCatalogModulesMustBeExplicitTypes`: todo módulo concreto termina em
  `type`;
- `typeDomainNameMustMatchCatalogModuleName`: quando a entidade é `*Type`, o módulo
  corresponde ao nome completo da entidade em minúsculas.

Essas regras devem falhar o build caso a nomenclatura volte a ficar ambígua.
