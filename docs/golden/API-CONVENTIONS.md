# Golden Reference — API Conventions

**Baseline funcional:** G4 concluída.  
**Adequação arquitetural:** pós-G4 / domínio ativo `Workspace`.

## Base path

```text
/api/v1/workspaces
```

O contrato `/api/v1/accounts` pertence ao baseline histórico G4 e não é mantido como alias na nova Golden Reference.

## Contratos Web

A Golden não reutiliza entidade JPA nem um DTO único para todas as operações.

```text
CreateWorkspaceRequest
UpdateWorkspaceRequest
WorkspaceResponse
```

A entidade `Workspace` permanece interna ao Core. Request/Response pertencem a `input.web.workspace`; Input/Output pertencem aos Use Cases.

## Create

```http
POST /api/v1/workspaces
```

Retorno: `201 Created`.

O servidor gera id técnico, identifier, version, lifecycle ACTIVE, onboarding=false e timestamps.

Payload usa `workspaceType` com os valores atualmente suportados `ADMIN | MANAGER`.

## Get

```http
GET /api/v1/workspaces/{workspaceId}
```

Somente Workspace ACTIVE é visível.

Inexistente ou INACTIVE retorna `404` com código `WORKSPACE-0001`.

## List

```http
GET /api/v1/workspaces?active=true&typeName=ADMIN
```

Regras:

- `active` default = `true`;
- `active=false` lista INACTIVE;
- `typeName` é opcional;
- `typeName` ignora case e whitespace;
- tipo inválido retorna 400 com detalhe em `typeName`;
- `tagName` pesquisa tags manuais e de sistema após normalização por `platform-tagging`.

## Update

```http
PUT /api/v1/workspaces/{workspaceId}
```

O request contém `version`.

Semântica:

- opera somente sobre Workspace ACTIVE;
- manter o próprio nome é permitido;
- nome usado por outro Workspace é rejeitado;
- approvers são substituídos;
- stale version retorna `409 / GLOBAL-0009`;
- JPA `@Version` continua protegendo concorrência real.

## Inactivate

```http
DELETE /api/v1/workspaces/{workspaceId}
```

Não remove fisicamente o registro:

```text
ACTIVE → INACTIVE
204 No Content
```

## Restore

```http
POST /api/v1/workspaces/{workspaceId}/restore
```

Somente Workspace INACTIVE pode ser restaurado.

Restore inválido retorna `400 / WORKSPACE-0002`.

## Normalização

Campos textuais funcionais são normalizados antes da validação.

Authorizer group ausente é `null`, não string vazia.

## Validation e erros

Erros de negócio usam `platform-messaging` e `ApiErrorResponse`.

Validações de campo retornam `400 / GLOBAL-0001` com `details[].field`.

Not found específico de Workspace usa `WORKSPACE-0001`.

## Authorization

Todos os endpoints passam pela capability `platform-authorization`.

| Operação | Policy |
|---|---|
| create | OPEN |
| list | OPEN |
| get por id | DEV |
| update | ADM |
| inactivate | ADM |
| restore | ADM |

No contrato atual da Foundation, `OPEN` não significa anônimo: o interceptor ainda exige `X-Correlation-Id` e token Bearer.

Leituras usam `@ResourceVisibility` sobre `WorkspaceOutput`:

- OWNER ignora filtro de visibilidade;
- usuário com authorizer compatível vê o Workspace;
- usuário sem authorizer compatível recebe 403 no recurso unitário;
- coleções são filtradas;
- Workspace sem `authorizerGroup` fica visível apenas para OWNER.

O campo técnico `accountId` do UserContext/Authorization pertence ao contrato da Foundation e não representa o agregado Workspace; por isso não é renomeado nesta atividade.

## Tagging

Create/update aceitam `tags` manuais.

A resposta devolve somente tags manuais normalizadas. Tags de sistema são mantidas internamente a partir de:

- identifier;
- name;
- authorizerGroup;
- acronym.

Owner técnico ativo de tags:

```text
owner_type = WORKSPACE
owner_id   = Workspace.identifier
```

A migration V3 converte registros existentes de `owner_type = ACCOUNT` para `WORKSPACE`.

`tagName` usa a normalização de `platform-tagging` e pesquisa tags manuais e de sistema. A busca reversa por owner é encapsulada em `core.workspace.integration.tagging`, pois essa operação não existe na API pública atual de `TagManager`.

Update reconcilia tags, removendo valores de sistema obsoletos e criando os atuais. Restore também reconcilia as tags de sistema preservando as manuais.

## Audit

As mutações publicam eventos via `platform-audit`:

```text
create     → WORKSPACE / INSERT
update     → WORKSPACE / UPDATE
inactivate → WORKSPACE / DELETE
restore    → WORKSPACE / RESTORE
```

Novos eventos usam `workspace-service` como service name. Eventos históricos `ACCOUNT` não são reescritos.

O serviço de audit é configurado externamente por `AUDIT_SERVICE_URL`.
