# ADR-008 — Nomenclatura dos módulos de Catalog Type

- **Status:** ACEITO
- **Data:** 2026-09-22
- **Escopo:** Golden Reference / `foundation.catalog`

## Contexto

A ordem interna de Catalog já é definida pelo ADR-007 como catalog-first:

```text
foundation/catalog/<catalogo>/
├── domain
├── repository
└── usecase
```

Os catálogos concretos da Golden usam majoritariamente entidades cujo nome termina em
`Type`, como `ApplicationScopeType`, `FeatureType`, `LifecycleType` e
`WorkspaceType`.

Usar packages como `applicationscope`, `feature` ou `workspace` elimina do nome
do módulo justamente a informação de que a responsabilidade é um catálogo de tipos.

## Decisão

Quando a entidade principal de um catálogo termina em `Type`, o módulo dentro de
`foundation.catalog` deve usar o nome completo da entidade em lowercase, preservando
o sufixo `type`.

Exemplos:

```text
ApplicationScopeType -> foundation.catalog.applicationscopetype
AuthorizationType    -> foundation.catalog.authorizationtype
FeatureScopeType     -> foundation.catalog.featurescopetype
FeatureType          -> foundation.catalog.featuretype
LifecycleType        -> foundation.catalog.lifecycletype
SchemaType           -> foundation.catalog.schematype
WorkspaceType        -> foundation.catalog.workspacetype
```

A estrutura completa permanece:

```text
foundation/catalog/applicationscopetype/
├── domain
├── repository
└── usecase
```

## Exceções

Todos os módulos concretos de catálogo terminam em `type`, inclusive quando o nome
histórico da entidade principal não termina em `Type`.

Exemplo:

```text
OnboardingPhase -> foundation.catalog.onboardingphasetype
```

Packages compartilhados que não representam um catálogo concreto permanecem naturais:

```text
foundation.catalog.support
```

## Consequências

- packages antigos como `foundation.catalog.applicationscope`,
  `foundation.catalog.feature` e `foundation.catalog.workspace` deixam de ser usados;
- `schematype` permanece inalterado, pois já expressava corretamente a convenção;
- `onboarding` passa a `onboardingphasetype`;
- controllers Web não são renomeados por esta decisão; a regra é específica de
  `foundation.catalog`;
- fitness function deve falhar quando uma entidade de domínio `*Type` estiver em um
  módulo cujo nome não corresponda ao nome completo da entidade em lowercase.

## Relação com ADR-007

O ADR-007 continua definindo a **ordem**:

```text
catalogo -> layer
```

Este ADR define a **nomenclatura do módulo do catálogo**.
