# G2 Review — Persistência e modelo mínimo

**Estado:** CONCLUÍDA  
**Data:** 2026-09-20  
**Commit funcional:** `e3c7ff42d438f7dfd6c226c5de7b1a333d0c5729`

## Escopo entregue

A G2 implementou somente o baseline de persistência aprovado:

- Spring Data JPA;
- MySQL;
- Flyway;
- migration V1;
- `Account`;
- `AccountApprover`;
- `AccountLifecycle`;
- `AccountType`;
- `AccountRepository`;
- optimistic locking com `@Version`;
- testes MySQL/Testcontainers;
- teste de migration;
- regra arquitetural mínima.

Não foram introduzidos controllers, onboarding, tagging, audit, catalog, autorização de domínio ou CRUD genérico.

## Decisões

Registradas em:

`docs/golden/ADR-002-G2-PERSISTENCE-BASELINE.md`

Resumo:

```text
Migrations  → Flyway
Lifecycle   → ACTIVE | INACTIVE
AccountType → ADMIN | MANAGER
```

`platform-catalog` não foi antecipado na G2.

## Schema V1

Migration:

`src/main/resources/db/migration/V1__create_account_schema.sql`

Tabelas:

```text
accounts
account_approvers
```

Proteções físicas relevantes:

- PK técnica de Account;
- identifier único;
- name único independentemente do lifecycle;
- FK approver → account;
- check de AccountType;
- check de lifecycle;
- índices de lifecycle/type/approver owner.

Hibernate está configurado com:

```text
ddl-auto=validate
```

Não existe `ddl-auto=update`.

## Evidência CI

GitHub Actions Verify #26:

- run `35547295205`;
- Maven repository local isolado;
- Java 25;
- Maven Enforcer: sucesso;
- DependencyConvergence: sucesso;
- MySQL 8.0 real via Testcontainers;
- Flyway criou `flyway_schema_history`;
- Flyway aplicou exatamente 1 migration;
- aplicação iniciou após migration + Hibernate validation;
- resultado final: **BUILD SUCCESS**.

### Testes

`GoldenArchitectureTest`:

- 1 teste;
- 0 falhas;
- 0 erros.

`AccountServiceApplicationIT`:

- 1 teste;
- 0 falhas;
- 0 erros.

`AccountPersistenceIT`:

- 3 testes;
- 0 falhas;
- 0 erros;
- Account + approver persistidos;
- unique name protegido;
- stale update rejeitado por JPA `@Version`.

`DatabaseMigrationIT`:

- 1 teste;
- 0 falhas;
- 0 erros;
- migration V1 confirmada em banco vazio.

Total de integração/persistência no run:

```text
5 testes
0 falhas
0 erros
```

## Guardrails revisados

Busca no repositório após a implementação:

- `platform-crud`: ausente;
- `BaseCrud*`: ausente;
- `GenericCrud*`: ausente;
- `CrudSupport*`: ausente;
- `ddl-auto=update`: ausente;
- namespace provisório `com.empresa`: ausente do código ativo.

## Observação de dependências — resolvida

O run #26 registrou `Testcontainers version: 2.0.5`.

Após a G2, a Foundation removeu a gestão duplicada de Testcontainers de `platform-dependencies` e manteve Spring Boot
4.1.1 como fonte tecnológica da versão efetiva.

Evidência da correção:

- Foundation commit `bd00859a066c3bd47350e12b240556d860472875`;
- Verify #92, run `35548702093`: sucesso;
- Publish #7, run `35548960128`: sucesso.

A versão efetiva permanece `2.0.5`. A observação `OBS-0001` está encerrada e não bloqueia a Golden.

## Critérios de saída

- [x] MySQL vazio sobe;
- [x] Flyway aplica migrations;
- [x] Hibernate valida o schema;
- [x] Account persiste;
- [x] approvers persistem;
- [x] unique name é protegido;
- [x] lifecycle ACTIVE é persistido;
- [x] optimistic locking é comprovado;
- [x] repository é específico;
- [x] testes usam infraestrutura opt-in;
- [x] arquitetura mínima testada;
- [x] `mvn clean verify` verde;
- [x] decisões documentadas.

## Decisão

A **G2 — Persistência e modelo mínimo está concluída**.

A próxima onda é **G3 — ciclo de vida explícito de Account/API**, sem reabrir a Foundation.
