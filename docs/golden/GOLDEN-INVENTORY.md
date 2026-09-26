# Golden Reference — Inventário G0

**Fase:** G0 — Inventário funcional e contratos  
**Status:** Em revisão  
**Data:** 2026-09-20

## 1. Objetivo

Este documento inventaria o que existe hoje nas fontes oficiais necessárias para construir a Golden Reference sem copiar
a arquitetura antiga.

A classificação segue a governança:

```text
REGRA DE NEGÓCIO
COMPORTAMENTO OBSERVADO
DECISÃO ARQUITETURAL ANTIGA
DÉBITO TÉCNICO
PADRÃO A SER REAVALIADO
```

## 2. Foundation disponível

Checkpoint técnico confirmado: `FOUNDATION-GOLDEN-V1`.

Evidência oficial:

- `BrunoBS/platform-libraries`
- branch `main`
- `docs/foundation/FOUNDATION-CHECKPOINT.md`

Baseline:

- Java 25;
- Spring Boot 4.1.1;
- Maven >= 3.9.9;
- `br.com.portalmanager.platform:platform-parent:1.0.0`;
- `br.com.portalmanager.platform:platform-libraries-bom:1.0.0`;
- GitHub Packages via `https://maven.pkg.github.com/brunobs/platform-libraries`;
- `platform-starter:1.0.0` agrega logging + messaging + authorization;
- audit/catalog/tagging explícitos;
- test-support para testes;
- `platform-crud` removido.

## 3. account-api — estado atual relevante

Repositório funcional de referência:

```text
BrunoBS/account-api
branch: main
```

O `pom.xml` atual usa:

- `platform-parent:1.0.0`;
- Spring Boot plugin 4.1.1;
- Spring Web;
- Spring Data JPA;
- MySQL;
- validation;
- platform messaging;
- authorization;
- logging;
- audit;
- catalog;
- tagging;
- test-support;
- **platform-crud**.

### Classificação

`platform-crud` no legado:

```text
DECISÃO ARQUITETURAL ANTIGA
```

Não deve aparecer na Golden.

## 4. Estrutura arquitetural do legado

Estrutura observada:

```text
com.brunobs
├── common
├── core
│   ├── account
│   ├── application
│   ├── catalog
│   ├── environment
│   └── tagging
├── feature
│   ├── configuration
│   ├── equalizer
│   ├── onboarding
│   ├── publisher
│   ├── schema
│   └── sharing
├── message
└── web
```

### Classificação

A estrutura `common/core/feature/message/web` é:

```text
DECISÃO ARQUITETURAL ANTIGA
PADRÃO A SER REAVALIADO
```

Não será copiada automaticamente.

## 5. Modelo Account observado

`Account` possui:

- id técnico;
- `@Version`;
- identifier UUID/string;
- account type;
- name;
- description;
- requester;
- acronym;
- settings;
- authorizer group;
- email group;
- onboarding;
- timestamps;
- lifecycle;
- approvers.

A entidade implementa `OptimisticLockable` do antigo `platform-crud`.

### Classificação

`@Version` e rejeição de update obsoleto:

```text
COMPORTAMENTO OBSERVADO
candidato a PRESERVAR
```

`OptimisticLockable`:

```text
DECISÃO ARQUITETURAL ANTIGA
DESCARTAR
```

Na Golden, optimistic locking deve ser demonstrado diretamente com JPA `@Version`.

## 6. Serviço Account observado

`AccountService` herda `BaseCrudService` e implementa `CrudNormalizer`.

Comportamentos identificados:

- busca por id somente de Account ACTIVE;
- listagem por estado ativo/inativo;
- filtro por tipo;
- filtro por tag;
- criação inicia lifecycle ACTIVE;
- criação inicia onboarding como falso;
- criação registra `ACCOUNT_REGISTRATION`;
- atualização valida autorização por owner ou authorizer group;
- delete é soft delete para INACTIVE;
- restore só aceita conta INACTIVE;
- busca por nome considera apenas ACTIVE;
- tags manuais e de sistema são reconciliadas;
- authorizer group nulo é convertido para string vazia;
- conclusão de onboarding exige progresso não vazio e todos os estágios `COMPLETED`.

### Classificação da herança CRUD

```text
DECISÃO ARQUITETURAL ANTIGA
DESCARTAR
```

### Classificação dos comportamentos

Devem ser avaliados individualmente em `ACCOUNT-BEHAVIOR-MATRIX.md`.

## 7. Validação Account observada

Regras presentes no legado:

- `accountType`: somente ADMIN ou MANAGER;
- CATALOG é rejeitado;
- name obrigatório, 3 a 100;
- description entre 10 e 500;
- requester mínimo 5;
- acronym obrigatório, máximo 5;
- email group válido;
- approvers obrigatórios;
- approver funcional obrigatório;
- approver email válido;
- nome único independentemente do lifecycle;
- normalização faz trim antes da validação.

O validador herda `BaseCrudValidator`.

### Classificação

Regras de campos e integridade:

```text
REGRA DE NEGÓCIO / COMPORTAMENTO OBSERVADO
candidato a PRESERVAR
```

Herança `BaseCrudValidator`:

```text
DECISÃO ARQUITETURAL ANTIGA
DESCARTAR
```

## 8. API observada

Base path:

```text
/api/v1/accounts
```

Operações:

```text
POST   /api/v1/accounts
GET    /api/v1/accounts
GET    /api/v1/accounts/{accountId}
PUT    /api/v1/accounts/{accountId}
DELETE /api/v1/accounts/{accountId}
POST   /api/v1/accounts/{accountId}/restore
GET    /api/v1/accounts/{accountId}/onboarding
PATCH  /api/v1/accounts/{accountId}/onboarding
```

A listagem aceita:

- `active`;
- `typeName`;
- `simplify`;
- `tagName`.

Níveis observados:

- create: OPEN;
- list: OPEN;
- get by id: DEV;
- onboarding get/patch: DEV;
- update/delete/restore: ADM.

### Classificação

Paths e payloads:

```text
COMPORTAMENTO OBSERVADO
compatibilidade wire-level ainda não aprovada
```

Níveis de autorização:

```text
COMPORTAMENTO OBSERVADO
PADRÃO A SER REAVALIADO
```

O objetivo da Golden é preservar necessidade funcional comprovada, não congelar automaticamente a matriz antiga.

## 9. Resumo simplify=true

O resumo legado depende de:

- Environment;
- Publisher;
- Application.

Calcula:

- `totalEnvironments`;
- `totalPublishers`;
- `totalApplications`.

### Classificação

```text
COMPORTAMENTO OBSERVADO
ADIAR NA GOLDEN V1
```

Motivo: incluir esse resumo obrigaria a trazer para a Golden V1 três domínios explicitamente fora do escopo mínimo.

A Golden não deve criar implementações artificiais desses domínios somente para manter esse endpoint.

## 10. Tagging observado

A Account possui tags manuais e tags de sistema.

Tags de sistema candidatas do legado:

- identifier;
- name;
- authorizer group;
- acronym.

Comportamento observado em testes:

- tags manuais retornam no DTO;
- tags de sistema são usadas para busca, mas não são expostas como tags manuais;
- normalização e deduplicação são responsabilidade da capability de tagging;
- atualização recalcula tags de sistema e preserva as tags manuais.

### Classificação

```text
COMPORTAMENTO OBSERVADO
caso real para platform-tagging
PRESERVAR INTENÇÃO
```

A forma de implementação será redesenhada usando a API atual de `platform-tagging`.

## 11. Authorization observado

O legado utiliza:

- `@AuthorizationRequired`;
- níveis `OPEN`, `DEV`, `ADM`;
- `UserContext`;
- `UserSession.isOwner()`;
- `UserSession.hasAuthorizer(...)`;
- `@ResourceVisibility`.

Testes demonstram:

- sem autenticação, criação é rejeitada;
- owner `PM5_OWNER` acessa o recurso;
- grupo authorizer adequado acessa;
- usuário sem grupo específico recebe forbidden.

### Classificação

Autorização de acesso real:

```text
COMPORTAMENTO OBSERVADO
caso real para platform-authorization
```

`ResourceVisibility`:

```text
PADRÃO A SER REAVALIADO
não portar automaticamente
```

A Foundation possui a annotation, mas o plano histórico explicitamente tratou essa frente como PoC separada. Sua
existência técnica não torna seu uso obrigatório na Golden.

## 12. Audit observado

Operações mutáveis possuem `@Auditable`:

- create / INSERT;
- onboarding update / UPDATE;
- update / UPDATE;
- delete / DELETE;
- restore / RESTORE.

### Classificação

```text
COMPORTAMENTO OBSERVADO
caso real para platform-audit
```

A Golden deverá demonstrar auditoria em mutações relevantes, sem inventar eventos artificiais.

## 13. Catalog observado

O legado usa múltiplos catálogos.

Para o slice Account, os relevantes encontrados são:

- AccountType;
- LifecycleType;
- OnboardingPhase;
- TagOriginType.

### Avaliação inicial

`AccountType`:

```text
candidato forte a uso real de platform-catalog
```

`OnboardingPhase`:

```text
candidato, se o fluxo mínimo de onboarding permanecer configurável
```

`TagOriginType`:

```text
não pertence ao domínio da aplicação; platform-tagging já modela MANUAL/SYSTEM
não duplicar na Golden sem necessidade comprovada
```

`LifecycleType`:

```text
PADRÃO A SER REAVALIADO
```

O fato de ACTIVE/INACTIVE serem catálogo no legado não prova que o estado de Account deva ser administrável. A
alternativa preferencial para avaliação é um estado explícito de domínio, mantendo `platform-catalog` para conceitos
realmente administráveis.

## 14. Persistência observada

O legado usa:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

Não foi identificado no slice analisado um mecanismo versionado de migrations para o schema principal.

### Classificação

```text
DÉBITO TÉCNICO para a Golden
DESCARTAR como padrão
```

A Golden deverá usar migrations versionadas e schema validation.

## 15. Testes legados úteis como oráculo

Foram identificados como fontes funcionais relevantes:

- `AccountValidatorTest`;
- `AccountServiceHardeningTest`;
- `AccountSystemTagProviderTest`;
- `AccountIntegrationTest`;
- `AccountOnboardingIntegrationTest`;
- `AccountOptimisticLockIntegrationTest`;
- `AccountSummaryIntegrationTest`.

Os testes devem ser utilizados para extrair comportamento, não copiados estruturalmente.

## 16. Escopo recomendado da Golden V1 após inventário

### Incluir

- Account;
- AccountApprover;
- create/read/list/update/deactivate/restore;
- filtro active;
- filtro type;
- filtro tag;
- validação;
- normalização;
- uniqueness;
- optimistic locking;
- tagging manual + system;
- autorização real;
- auditoria em mutações;
- account type catalog;
- onboarding mínimo real.

### Adiar

- resumo dependente de Application/Environment/Publisher;
- demais domínios da `account-api`;
- todos os catálogos não exigidos pelo slice;
- ResourceVisibility até existir decisão concreta.

### Descartar como padrão

- platform-crud;
- BaseCrud*;
- CrudNormalizer;
- OptimisticLockable;
- estrutura antiga de pacotes;
- DTO único para todas as operações;
- `ddl-auto=update`.

## 17. Pendências para fechamento de G0

1. nome do novo repositório;
2. package root;
3. ferramenta de migrations;
4. compatibilidade de API: comportamento vs wire contract;
5. ResourceVisibility;
6. modelagem de Account lifecycle;
7. definição mínima das fases de onboarding.