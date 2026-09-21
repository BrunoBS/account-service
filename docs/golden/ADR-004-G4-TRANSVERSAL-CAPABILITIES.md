# ADR-004 — Capabilities transversais da G4

## Status

Accepted.

## Data

2026-09-21

## Contexto

A G4 deve demonstrar capacidades transversais da Foundation somente quando existe um caso real no domínio Account.

A matriz funcional exige autorização por nível e por authorizer group, tags manuais/sistema e auditoria das mutações. AccountType permanece um conjunto fechado ADMIN/MANAGER, sem requisito de administrabilidade runtime.

## Decisão — Authorization

A Golden usa `platform-authorization` recebido pelo starter.

Policies:

```text
POST   /api/v1/accounts              → OPEN
GET    /api/v1/accounts              → OPEN
GET    /api/v1/accounts/{id}         → DEV
PUT    /api/v1/accounts/{id}         → ADM
DELETE /api/v1/accounts/{id}         → ADM
POST   /api/v1/accounts/{id}/restore → ADM
```

OPEN continua exigindo correlation id e Bearer token porque essa é a semântica operacional da capability atual.

Leituras usam `@ResourceVisibility`. `AccountResult` implementa `AuthorizableResource` usando `authorizerGroup`.

OWNER ignora a filtragem. Um usuário comum precisa possuir authorizer compatível. Conta sem authorizer group não é aberta automaticamente; somente OWNER a enxerga.

## Decisão — Tagging

A Golden declara `platform-tagging` explicitamente.

```text
owner_type = ACCOUNT
owner_id   = Account.identifier
```

Tags manuais entram no request e são devolvidas no response.

Tags de sistema são:

- identifier;
- name;
- authorizerGroup;
- acronym.

`TagManager.reconcile` é usado em create/update/restore. A capability é responsável por normalização/deduplicação; a aplicação não recria essas regras.

A tabela `tags` é criada por migration da aplicação, não por schema implícito da Foundation.

## Decisão — Audit

A Golden declara `platform-audit` explicitamente e audita:

```text
create     → INSERT
update     → UPDATE
deactivate → DELETE
restore    → RESTORE
```

Resource = `ACCOUNT`.

O endpoint externo é fornecido por `AUDIT_SERVICE_URL`. `fail-on-error=false` mantém audit desacoplado da transação funcional no runtime padrão.

## Decisão — Catalog

`platform-catalog` não entra na G4.

AccountType permanece enum explícito `ADMIN | MANAGER`. Não há requisito de administrar tipos em runtime, portanto um catálogo persistido seria abstração/comportamento sem caso real.

## Decisão — Messaging e Logging

Messaging e logging permanecem pelo starter.

A Golden reutiliza os contratos existentes e não cria mecanismo paralelo.

## Consequências

- capabilities opcionais aparecem no POM somente quando utilizadas;
- domínio continua explícito;
- nenhuma alteração na Foundation foi necessária;
- onboarding permanece fora da G4 e será tratado na G5.
