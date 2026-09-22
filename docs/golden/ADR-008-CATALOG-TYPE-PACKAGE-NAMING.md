# ADR-008 — Naming dos módulos concretos de Catalog como Type

- **Status:** ACEITO
- **Data:** 2026-09-22
- **Escopo:** Golden Reference / application foundation macrozone

## Contexto

Os módulos sob `foundation.catalog` representam catálogos administráveis de tipos.
A estrutura catalog-first definida pelo ADR-007 resolve ownership e organização, mas
nomes como `applicationscope`, `feature` ou `workspace` não deixam essa natureza
explícita na leitura do package.

## Decisão

Todo módulo concreto de catálogo abaixo de `foundation.catalog` deve explicitar
`type` no próprio nome do package.

Exemplos:

```text
foundation.catalog.applicationscopetype
foundation.catalog.authorizationtype
foundation.catalog.environmenttype
foundation.catalog.featurescopetype
foundation.catalog.featuretype
foundation.catalog.lifecycletype
foundation.catalog.workspacetype
```

Quando o package já explicita essa semântica, ele permanece como está:

```text
foundation.catalog.schematype
```

A estrutura interna continua:

```text
foundation/catalog/<catalogtype>/
├── domain
├── repository
└── usecase
```

## Exceções

`foundation.catalog.support` não representa catálogo concreto e, portanto, não recebe
o sufixo `type`.

A decisão é específica para `foundation.catalog`; ela não obriga renomear packages de
controllers em `input.web.catalog`.

## Consequências

- entidades `*Type` ficam agrupadas em módulos cujo nome termina em `type`;
- a leitura do package distingue imediatamente catálogo de domínio comum;
- novos catálogos concretos devem seguir a mesma convenção;
- a fitness function arquitetural falha se surgir módulo concreto de Catalog sem
  `type` no primeiro segmento;
- nomes de endpoints HTTP não são afetados.
