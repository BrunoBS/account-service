# G3 Review — Ciclo de vida explícito de Account

**Estado:** CONCLUÍDA  
**Data:** 2026-09-20  
**Commit funcional inicial:** `6be7019ed3b44d94b334971f8824ac968e49d4c7`  
**Head funcional validado:** `794b1efdd6d0ea7ec45a0c39dd67ee5ad5891288`

## Escopo entregue

A G3 implementou create, get, list/filter, update, deactivate e restore com contratos HTTP explícitos, normalização,
validação, transações e tratamento de erros.

Não foram antecipados authorization, audit, tagging, catalog ou onboarding.

## Contratos e arquitetura

Foram adicionados requests/responses separados e modelos de aplicação próprios.

Fluxo:

```text
HTTP
 ↓
api
 ↓
application
 ↓
domain + persistence
```

As regras ArchUnit garantem fronteiras entre API, application, domain e persistence.

Não existe BaseCrud, GenericCrud, BaseService ou equivalente.

## Comportamentos comprovados

A integração HTTP prova:

- criação com identidade gerada pelo servidor;
- normalização antes da validação;
- get somente para ACTIVE;
- listagem ACTIVE/INACTIVE;
- filtro de AccountType case/whitespace-insensitive;
- update explícito;
- substituição de approvers;
- nome único após normalização;
- stale version → conflict;
- deactivate → INACTIVE;
- Account INACTIVE não aparece no get;
- restore de INACTIVE;
- restore inválido de ACTIVE;
- not found em update/delete inexistentes;
- detalhes de validação via platform-messaging.

## Integrações corrigidas durante a execução

### Testcontainers 2

A Golden usava a coordenada legada `org.testcontainers:mysql`.

Com a Foundation consolidada, a coordenada correta passou a ser:

```text
org.testcontainers:testcontainers-mysql
```

Correção: commit `fe866936eb95f4e7701530f77f786a51d27b424f`.

### ArchUnit

Os padrões iniciais `..api..` e `..persistence..` colidiam com packages externos como AssertJ API e Jakarta Persistence.

As regras foram restringidas ao namespace completo da Golden, sem relaxar as fronteiras arquiteturais.

Correção: commit `3fe68e70a9d5d25c5bc25c3a39451995bdf4a21d`.

### Messaging com DataSource

Com JPA/MySQL presente, `platform-messaging` detecta `JdbcTemplate` e pode selecionar o repository JDBC de mensagens.

O banco de Account não é um catálogo corporativo de mensagens. A Golden registra explicitamente
`NoOpApiMessageRepository` e usa bundles classpath.

Correção: commit `7a36db1c3ffb5b78c252845ce6c7dbb92a38e540`.

### Query parameter

O último falso negativo de teste vinha de double encoding de `%20admin%20` no RestAssured.

O teste passou a enviar `" admin "` como query parameter real.

Correção: commit `794b1efdd6d0ea7ec45a0c39dd67ee5ad5891288`.

## Evidência final

GitHub Actions Verify #34:

- run `35550319996`;
- repository Maven local isolado;
- Java 25;
- Maven Enforcer verde;
- dependency convergence verde;
- Testcontainers 2.0.5;
- MySQL real;
- Flyway V1 aplicada;
- `GoldenArchitectureTest`: 3/3;
- `AccountNormalizerTest`: 2/2;
- `AccountApiIT`: 9/9;
- `AccountServiceApplicationIT`: 1/1;
- `AccountPersistenceIT`: 3/3;
- `DatabaseMigrationIT`: 1/1;
- total: 19 testes, 0 falhas, 0 erros;
- **BUILD SUCCESS**.

## Critérios de saída

- [x] create;
- [x] get;
- [x] list/filter lifecycle/type;
- [x] update;
- [x] deactivate;
- [x] restore;
- [x] DTOs explícitos;
- [x] normalização;
- [x] validação;
- [x] transações;
- [x] mensagens/erros padronizados;
- [x] stale version 409;
- [x] regras arquiteturais;
- [x] MySQL real;
- [x] Maven repository isolado;
- [x] `mvn clean verify` verde.

## Decisão

A **G3 — Ciclo de vida explícito de Account está concluída**.

A G4 — capabilities transversais — não foi iniciada.
