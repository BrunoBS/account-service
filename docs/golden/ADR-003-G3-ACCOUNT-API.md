# ADR-003 — Contrato HTTP e ciclo de vida explícito de Account

## Status

Accepted.

## Data

2026-09-20

## Contexto

A G3 demonstra o ciclo de vida real de Account sem reconstruir CRUD genérico e sem antecipar capabilities transversais
previstas para G4/G5.

O legado expõe `/api/v1/accounts` e possui comportamentos funcionais que devem ser preservados. O projeto não aprovou
compatibilidade wire-level total com `account-api`.

## Decisão — compatibilidade

A Golden preserva **comportamento funcional aprovado**, não a estrutura DTO ou a implementação do legado.

O contrato HTTP usa DTOs explícitos por operação:

```text
CreateAccountRequest
UpdateAccountRequest
AccountResponse
ApproverRequest
ApproverResponse
```

Create não recebe `id`, `identifier` ou `version`.

Update exige `version` retornada pela última leitura.

## Decisão — endpoints G3

```text
POST   /api/v1/accounts
GET    /api/v1/accounts
GET    /api/v1/accounts/{accountId}
PUT    /api/v1/accounts/{accountId}
DELETE /api/v1/accounts/{accountId}
POST   /api/v1/accounts/{accountId}/restore
```

Listagem suporta nesta onda:

- `active`, default `true`;
- `typeName`, normalizado com trim e case-insensitive.

`tagName`, `simplify` e onboarding não entram na G3.

## Decisão — lifecycle

- create inicia `ACTIVE`;
- get por id retorna somente `ACTIVE`;
- update opera somente sobre `ACTIVE`;
- delete é deactivate para `INACTIVE`;
- restore aceita somente `INACTIVE`;
- listagem usa `active=true|false`.

## Decisão — normalização

Antes da validação:

- trim em name, description, requester, acronym e emailGroup;
- trim em dados de approvers;
- accountType/typeName com trim + uppercase;
- authorizerGroup vazio/branco é representado como `null`.

A conversão legada de `authorizerGroup=null` para string vazia não é preservada.

## Decisão — erros e mensagens

A Golden usa diretamente:

- `ValidationException`;
- `NotFoundException`;
- `ResourceVersionConflictException`;
- `ApiExceptionHandler`;

fornecidos por `platform-messaging`.

Mensagens específicas de Account são fornecidas por:

```text
META-INF/platform-messages/account-service_pt_BR.properties
```

Como o serviço possui DataSource próprio, a Golden registra explicitamente `NoOpApiMessageRepository`. Isso impede que o
DataSource de Account seja interpretado como catálogo JDBC de mensagens e mantém os bundles classpath como fonte desta
aplicação.

Não existe envelope de erro paralelo.

## Decisão — concorrência

A versão enviada no update é comparada com a versão atual antes da mutação.

JPA `@Version` permanece a proteção transacional contra races concorrentes.

## Arquitetura

```text
api
 ↓
application
 ↓
domain + persistence
```

Regras protegidas por ArchUnit:

- API não acessa persistence diretamente;
- application não depende de API;
- domain não depende de API, application, persistence ou Spring Web.

## Fora de escopo

A G3 não adiciona authorization, audit, tagging, catalog ou onboarding.

Essas capabilities permanecem nas ondas posteriores.
