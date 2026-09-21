# Golden Reference — API Conventions

**Baseline:** G4 concluída.

## Base path

```text
/api/v1/accounts
```

## Contratos

A Golden não reutiliza entidade JPA nem um DTO único para todas as operações.

```text
CreateAccountRequest
UpdateAccountRequest
AccountResponse
```

A entidade `Account` permanece interna.

## Create

```http
POST /api/v1/accounts
```

Retorno: `201 Created`.

O servidor gera id técnico, identifier, version, lifecycle ACTIVE, onboarding=false e timestamps.

## Get

```http
GET /api/v1/accounts/{accountId}
```

Somente Account ACTIVE é visível.

Inexistente ou INACTIVE retorna `404` com código `ACCOUNT-0001`.

## List

```http
GET /api/v1/accounts?active=true&typeName=ADMIN
```

Regras:

- `active` default = `true`;
- `active=false` lista INACTIVE;
- `typeName` é opcional;
- typeName ignora case e whitespace;
- tipo inválido retorna 400 com detalhe em `typeName`.

Filtro `tagName` foi implementado em G4 via `platform-tagging`. Resumo simplificado permanece fora do slice atual.

## Update

```http
PUT /api/v1/accounts/{accountId}
```

O request contém `version`.

Semântica:

- opera somente sobre Account ACTIVE;
- manter o próprio nome é permitido;
- nome usado por outra Account é rejeitado;
- approvers são substituídos;
- stale version retorna `409 / GLOBAL-0009`;
- JPA `@Version` continua protegendo concorrência real.

## Deactivate

```http
DELETE /api/v1/accounts/{accountId}
```

Não remove fisicamente o registro:

```text
ACTIVE → INACTIVE
204 No Content
```

## Restore

```http
POST /api/v1/accounts/{accountId}/restore
```

Somente Account INACTIVE pode ser restaurada.

Restore inválido retorna `400 / ACCOUNT-0002`.

## Normalização

Campos textuais funcionais são normalizados antes da validação.

Authorizer group ausente é `null`, não string vazia.

## Validation e erros

Erros de negócio usam `platform-messaging` e `ApiErrorResponse`.

Validações de campo retornam `400 / GLOBAL-0001` com `details[].field`.

Not found específico de Account usa `ACCOUNT-0001`.

## Capabilities futuras

G3 não define contratos de authorization, audit, tagging, catalog administrável ou onboarding.


## G4 — Authorization

Todos os endpoints passam pela capability `platform-authorization`.

| Operação | Policy |
|---|---|
| create | OPEN |
| list | OPEN |
| get por id | DEV |
| update | ADM |
| deactivate | ADM |
| restore | ADM |

No contrato atual da Foundation, `OPEN` não significa anônimo: o interceptor ainda exige `X-Correlation-Id` e token Bearer.

Leituras usam `@ResourceVisibility` sobre `AccountResult`:

- OWNER ignora filtro de visibilidade;
- usuário com authorizer compatível vê o Account;
- usuário sem authorizer compatível recebe 403 no recurso unitário;
- coleções são filtradas;
- Account sem `authorizerGroup` fica visível apenas para OWNER.

## G4 — Tagging

Create/update aceitam `tags` manuais.

A resposta devolve somente tags manuais normalizadas. Tags de sistema são mantidas internamente a partir de:

- identifier;
- name;
- authorizerGroup;
- acronym.

Owner técnico de tags:

```text
owner_type = ACCOUNT
owner_id   = Account.identifier
```

`tagName` usa a normalização de `platform-tagging` e pesquisa tags manuais e de sistema.

Update reconcilia tags, removendo valores de sistema obsoletos e criando os atuais. Restore também reconcilia as tags de sistema preservando as manuais.

## G4 — Audit

As mutações publicam eventos via `platform-audit`:

```text
create     → ACCOUNT / INSERT
update     → ACCOUNT / UPDATE
deactivate → ACCOUNT / DELETE
restore    → ACCOUNT / RESTORE
```

O serviço de audit é configurado externamente por `AUDIT_SERVICE_URL`.
