# ADR-007 — Ordem interna dos packages de Catalog

- **Status:** ACEITO
- **Data:** 2026-09-22
- **Escopo:** Golden Reference / application foundation macrozone

## Contexto

Os catálogos concretos pertencem à application Foundation e reutilizam a capability
`platform-catalog`.

Durante a migração dos 16 CRUDs, a estrutura foi inicialmente implementada como
layer-first:

```text
foundation/catalog/domain/<catalogo>
foundation/catalog/repository/<catalogo>
foundation/catalog/usecase/<catalogo>
```

Essa ordem não corresponde à organização definida para os módulos concretos de Catalog.

## Decisão

Catalog deve ser organizado **por catálogo primeiro** e por layer depois:

```text
foundation/catalog/<catalogo>/
├── domain
├── repository
└── usecase
```

Exemplo:

```text
foundation/catalog/workspacetype/
├── domain/
│   ├── WorkspaceType.java
│   └── WorkspaceTypeEnum.java
├── repository/
│   └── WorkspaceTypeRepository.java
└── usecase/
    └── WorkspaceTypeService.java
```

O mesmo padrão vale para todos os catálogos concretos.

Componentes compartilhados que não pertencem a um catálogo concreto podem residir
diretamente em um package explícito de suporte, por exemplo:

```text
foundation/catalog/support/CatalogSchemaValidationSupport.java
```

Controllers permanecem em:

```text
entrypoint/web/catalog/<catalogo>
```

## Consequências

- a estrutura física e o package Java devem refletir a mesma ordem;
- novos catálogos devem nascer em `foundation.catalog.<catalogo>.<layer>`;
- fica proibido reintroduzir `foundation.catalog.domain.<catalogo>`,
  `foundation.catalog.repository.<catalogo>` ou
  `foundation.catalog.usecase.<catalogo>`;
- a fitness function de arquitetura deve falhar caso a ordem layer-first reapareça.

## Relação com documentação anterior

Onde a árvore de referência genérica sugerir `catalog/domain|usecase|repository`,
este ADR especializa a estrutura interna de Catalog e prevalece para os catálogos
concretos da Golden Reference.
