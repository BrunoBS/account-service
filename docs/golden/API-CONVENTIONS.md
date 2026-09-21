# Golden Reference — API Conventions

**Baseline:** G3 concluída.

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

Filtros de tag e resumo simplificado entram somente quando suas capabilities/domínios forem implementados.

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
