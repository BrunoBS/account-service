# G4 Review — Capabilities transversais

**Estado:** CONCLUÍDA  
**Data:** 2026-09-21  
**Head funcional validado:** `a4c238eea24022d984aed49ba8a18eeb9d0a4853`

## Escopo entregue

### Authorization

- OPEN/DEV/ADM por endpoint;
- autenticação obrigatória mesmo em OPEN;
- `@ResourceVisibility` em get/list;
- OWNER bypass;
- authorizer group correto permitido;
- authorizer incompatível = forbidden;
- coleção filtrada.

### Tagging

- `platform-tagging` explícito;
- migration V2 para `tags`;
- tags manuais no contrato HTTP;
- normalização/deduplicação delegadas à Foundation;
- tags de sistema por identifier/name/authorizerGroup/acronym;
- tags de sistema não expostas como manuais;
- update recalcula tags de sistema;
- filtro `tagName` por tags manuais ou de sistema.

### Audit

- `platform-audit` explícito;
- INSERT/UPDATE/DELETE/RESTORE;
- resource id correto;
- actor/correlation id vindos do UserContext;
- teste com `AuditPublisher` capturável.

### Catalog

Avaliado e não adotado. AccountType continua enum fechado ADMIN/MANAGER.

### Messaging / Logging

Continuam pelo starter e foram exercitados no fluxo HTTP/autorização. Nenhuma implementação paralela foi criada.

## Ajuste de integração durante a G4

O profile de teste G3 fixava:

```text
platform.authorization.enabled=false
```

Isso fazia o interceptor mock guest da Foundation preencher o UserContext e impedia `@WithMockAuthorization` de
representar OWNER/authorizer groups.

A correção foi manter authorization ativo no profile de teste e deixar `@WithMockAuthorization` fornecer a URL dinâmica
do WireMock nos testes HTTP.

Isso foi correção do consumidor; nenhuma mudança na Foundation foi necessária.

## Evidência final

GitHub Actions Verify #66:

- run `35552052276`;
- repository Maven local isolado;
- Java 25;
- RequireJavaVersion: passed;
- DependencyConvergence: passed;
- Testcontainers 2.0.5;
- MySQL 8.0 real;
- Flyway aplicando migrations V1 + V2;
- `GoldenArchitectureTest`: 3/3;
- `AccountNormalizerTest`: 3/3;
- `AccountAuthorizationIT`: 3/3;
- `AccountApiIT`: 9/9;
- `AccountAuditIT`: 1/1;
- `AccountTaggingIT`: 2/2;
- `AccountServiceApplicationIT`: 1/1;
- `AccountPersistenceIT`: 3/3;
- `DatabaseMigrationIT`: 1/1;
- unit/architecture: 6 testes;
- integração: 20 testes;
- total: 26 testes;
- 0 falhas;
- 0 erros;
- **BUILD SUCCESS**.

## Guardrails

- zero platform-crud;
- nenhum BaseCrud equivalente;
- platform-catalog não introduzido sem caso real;
- migrations continuam sendo a fonte de schema;
- nenhuma alteração na Foundation;
- onboarding não iniciado.

## Decisão

A **G4 — Capabilities transversais está concluída**.

Próxima onda: **G5 — fluxo não-CRUD / onboarding mínimo**.
