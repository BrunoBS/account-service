# ADR-002 — Baseline de persistência da G2

## Status

Accepted.

## Data

2026-09-20

## Contexto

A G2 da Golden Reference precisa introduzir persistência real para Account sem recriar CRUD genérico e sem antecipar capabilities previstas para ondas posteriores.

A G0 já definiu:

- MySQL como banco alvo;
- migrations SQL versionadas;
- Hibernate apenas para validação do schema;
- optimistic locking via JPA `@Version`;
- lifecycle ACTIVE/INACTIVE como estado candidato explícito;
- AccountType como conceito funcional restrito a ADMIN/MANAGER.

## Decisão — migrations

A ferramenta oficial da Golden Reference para G2 será **Flyway**.

O serviço utiliza:

```text
spring-boot-starter-flyway
flyway-mysql
```

As migrations ficam em:

```text
src/main/resources/db/migration
```

O fluxo esperado é:

```text
MySQL vazio
→ Flyway aplica migrations
→ Hibernate valida schema
→ aplicação inicia
```

Não usar `ddl-auto=update`, `schema.sql` ou instalação manual de schema.

## Decisão — lifecycle

O lifecycle de Account será estado explícito do domínio:

```text
ACTIVE
INACTIVE
```

Ele não será modelado como catálogo persistido.

Justificativa:

- é estado intrínseco do agregado;
- deactivate/restore dependem diretamente dele;
- não existe requisito de administrabilidade runtime;
- usar catálogo apenas porque o legado usava seria preservar arquitetura antiga sem evidência.

## Decisão — AccountType na G2

Na G2, AccountType será enum explícito:

```text
ADMIN
MANAGER
```

`CATALOG` não pertence aos tipos válidos de Account.

Essa decisão não antecipa a integração com `platform-catalog`. A onda G4 avaliará a capability somente com caso de uso concreto. Se houver necessidade real de catálogo persistido/administrável, a mudança será tratada como decisão e migration próprias.

## Persistência

A G2 cria somente:

```text
accounts
account_approvers
```

Account possui:

- id técnico;
- version JPA;
- identifier UUID textual;
- account type;
- campos funcionais do slice;
- onboarding flag;
- lifecycle;
- timestamps;
- approvers com cascade/orphan removal.

## Testes

A validação usa MySQL real via `@WithMySql` e dependências Testcontainers declaradas explicitamente no consumidor.

Devem ser provados:

- migration em banco vazio;
- Hibernate schema validation;
- persistência de Account/Approver;
- unique name;
- optimistic locking;
- arquitetura mínima;
- `mvn clean verify`.

## Foundation

Esta ADR não altera a Foundation.
