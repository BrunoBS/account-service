# Governança — Catálogos + Platform Microservice/Feature Runtime

## 1. Objetivo

Este documento registra o modelo atual da Golden Reference para catálogos da `foundation` e para os recursos administrativos da Platform.

A regra central é separar claramente:

- **catálogos tipados**, estáveis e enumeráveis;
- **recursos Platform**, administráveis e persistidos como entidades;
- **configuração runtime**, exposta por contratos tabulares;
- **referências estruturais**, evitando relacionamento persistido por String quando existe entidade dona.

---

# 2. Catálogos da Foundation

Catálogos permanecem em:

```text
foundation.catalog.<catalog>type
```

Exemplos:

```text
lifecycletype
resourcescopetype
workspacetype
environmenttype
```

Os valores conhecidos de catálogo continuam em:

```text
UPPERCASE_UNDERSCORE
```

Exemplos:

```text
ACTIVE
INACTIVE
QUARANTINED

WORKSPACE
APPLICATION
```

A convenção de package permanece:

```text
foundation/catalog/<catalog>type/
├── domain
├── repository
└── usecase
```

---

# 3. Catálogos consolidados

## 3.1. ResourceScopeType

`PublisherScopeType` e o antigo `SchemaType` foram consolidados em:

```text
ResourceScopeType
```

Tabela:

```text
type_resource_scopes
```

Valores:

```text
WORKSPACE
APPLICATION
```

`SchemaScopeType` permanece separado porque representa outra semântica:

```text
PLATFORM
WORKSPACE
```

---

# 4. Recursos Platform

`Microservice`, `Feature` e `FeatureContext` não são mais tratados como catálogos simples.

São entidades administrativas persistidas em:

```text
platform_microservices
platform_features
platform_feature_contexts
platform_feature_context_relations
```

Relações principais:

```text
Microservice 1 ───── N Feature

Feature N ───── N FeatureContext
```

Semântica oficial:

```text
Microservice        = quem oferece
Feature        = o que é oferecido
FeatureContext = em qual contexto funcional a capacidade é disponibilizada
```

`FeatureContext` não representa ownership de Workspace/Application.

---

# 5. Convenção de code e name

## 5.1. Recursos Platform

Para:

```text
Microservice
Feature
FeatureContext
```

o `code` é o identificador técnico canônico.

Padrão:

```text
lowercase kebab-case
```

Regex:

```text
^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$
```

Exemplos:

```text
workspace-service
promotion-engine
manager-account
```

Regras:

- somente letras minúsculas, números e hífen;
- sem espaço;
- sem underscore;
- sem maiúsculas;
- sem normalização silenciosa;
- valor estável após criação.

## 5.2. Microservice.code

Para Microservice, o `code` deve ser o mesmo identificador técnico usado pela aplicação.

Exemplo:

```properties
spring.application.name=workspace-service
```

deve corresponder a:

```text
Microservice.code = workspace-service
```

## 5.3. name

`name` é amigável e voltado para apresentação.

Exemplos:

```text
code = workspace-service
name = Workspace Microservice

code = promotion-engine
name = Motor de Promoção

code = manager-account
name = Conta Manager
```

`name` não segue kebab-case e preserva o valor informado.

---

# 6. Lifecycle

Recursos Platform utilizam:

```text
ACTIVE
INACTIVE
QUARANTINED
```

Lifecycle continua sendo catálogo tipado da Foundation.

`PURGED` não é estado de lifecycle.

`PURGED` representa evento terminal de auditoria após remoção física.

---

# 7. Microservice

Tabela:

```text
platform_microservices
```

Campos principais:

```text
id
version
identifier
code
name
description
lifecycle_code
created_at
updated_at
```

Regras:

- `identifier` é UUID exposto para API;
- `id` é chave interna de relacionamento;
- `code` é único;
- `name` é único;
- `code` segue kebab-case;
- Feature pertence estruturalmente a Microservice;
- Microservice com Features vinculadas não pode ser colocado em quarentena sem resolver os vínculos.

---

# 8. Feature

Tabela:

```text
platform_features
```

Campos principais:

```text
id
version
identifier
code
name
description
microservice_id
lifecycle_code
settings
created_at
updated_at
```

Relação:

```text
platform_features.microservice_id
        ↓
platform_microservices.id
```

Feature deve possuir exatamente um Microservice ativo na criação.

O `settings` permanece reservado para configuração operacional da feature.

Exemplo:

```json
{
  "quarantine": {
    "enabled": true,
    "retentionDays": 30,
    "restoreAllowed": true
  },
  "audit": {
    "enabled": true,
    "snapshotOnPurge": true
  },
  "purge": {
    "enabled": true
  }
}
```

Ownership de Microservice não fica em JSON.

---

# 9. FeatureContext

Tabelas:

```text
platform_feature_contexts
platform_feature_context_relations
```

Exemplos de code:

```text
manager-account
catalog-account
administration
```

Regras:

- relação N:N com Feature;
- somente contexto ativo pode ser associado;
- contexto com Feature vinculada não pode ser colocado em quarentena;
- coleção exposta pelo domínio permanece somente leitura.

---

# 10. Message Management e Microservice

Message Management não deve persistir uma String como relacionamento estrutural com Microservice.

A API pode receber o identificador técnico:

```text
microserviceIdentifier = <uuid-do-workspace-service>
```

O use case resolve esse valor para a referência de Microservice.

Persistência:

```text
messages.microservice_id
        ↓
platform_microservices.id
```

A coluna antiga:

```text
messages.service_code
```

foi removida do modelo final.

Regras de unicidade:

```text
(microservice_id, message_key)
(microservice_id, code)
```

O output pode continuar expondo:

```text
microserviceIdentifier = <uuid-do-workspace-service>
```

porque esse valor é derivado do relacionamento com `platform_microservices`.

---

# 11. Referência de Microservice dentro do módulo Message

Para preservar a independência arquitetural entre módulos, o domínio de Message não referencia diretamente a classe interna do domínio Platform.

O módulo Message guarda a referência estrutural pelo identificador interno:

```text
Message.microserviceId
```

mapeada sobre:

```text
platform_microservices
```

Objetivo:

- manter FK estrutural por `microservice_id`;
- evitar relacionamento persistido por String;
- evitar dependência direta de `feature.message.domain` em `feature.platform.domain`;
- respeitar as regras de arquitetura da Golden.

Validação funcional de Microservice ativo continua sendo feita por contrato público de use case.

---

# 12. View de runtime

A view:

```text
vw_feature_runtime_config
```

expõe o contrato operacional das Features sem obrigar consumidores a interpretar diretamente o JSON.

Colunas atuais:

```text
feature_code
feature_label
microservice_code
microservice_label

quarantine_enabled
quarantine_retention_days
quarantine_restore_allowed

audit_enabled
audit_snapshot_on_purge

purge_enabled

feature_active
microservice_active
```

Exemplo:

```text
feature_code     | microservice_code      | retention_days | audit | purge
-----------------+-------------------+----------------+-------+------
workspace        | workspace-service | 30             | true  | true
message          | workspace-service | 0              | true  | true
promotion-engine | workspace-service | 30             | true  | true
```

A auditoria e demais consumidores devem usar a view como contrato estável.

---

# 13. Runtime de mensagens

A view:

```text
vw_platform_messages
```

resolve o Microservice através de:

```text
messages.microservice_id
        ↓
platform_microservices.id
```

A chave runtime resultante utiliza o `Microservice.code` canônico.

Exemplo:

```text
workspace-service.workspace.not-found
```

Somente registros ativos de Microservice, Message e Translation são expostos.

---

# 14. Migrações relevantes

## V12

Promoveu Microservice e Feature de catálogo para entidades Platform:

```text
platform_microservices
platform_features
```

## V13

Introduziu `FeatureContext` e removeu o antigo `FeatureScopeType`.

## V14

Consolidou os catálogos duplicados em:

```text
ResourceScopeType
```

## V15

Padronizou:

```text
Microservice.code
Feature.code
FeatureContext.code
```

para lowercase kebab-case.

Exemplos:

```text
WORKSPACE_SERVICE → workspace-service
PROMOTION_ENGINE  → promotion-engine
MANAGER_ACCOUNT   → manager-account
```

## V16

Substituiu a referência textual de Message para Microservice:

```text
messages.service_code
```

por relacionamento estrutural:

```text
messages.service_id → platform_services.id
```

---

# 15. Contrato com auditoria, quarentena e purge

Feature continua sendo a chave de contexto operacional.

Fluxo conceitual:

```text
Feature
   ↓
Microservice
   ↓
vw_feature_runtime_config
   ↓
quarantine policy
   ↓
audit policy
   ↓
purge policy
```

Fluxo de remoção:

```text
ACTIVE / INACTIVE
       ↓
QUARANTINED
       ↓
retentionDays
       ↓
PURGE
       ↓
audit event
```

`PURGED` permanece evento de auditoria, não lifecycle.

---

# 16. Critérios de governança

O modelo deve preservar:

- códigos de catálogo em `UPPERCASE_UNDERSCORE`;
- códigos de recursos Platform em lowercase kebab-case;
- `Microservice.code == spring.application.name` para serviços administrados;
- relacionamentos internos usando IDs numéricos;
- UUID `identifier` para contratos externos;
- ausência de FK estrutural baseada em String quando existe entidade dona;
- independência entre módulos de negócio;
- validação de lifecycle no domínio;
- migrations históricas imutáveis;
- novas mudanças somente por nova migration;
- testes de upgrade de schema;
- `mvn clean verify` e CI verdes antes de merge.

---

# 17. Estado atual

O modelo de referência atual é:

```text
foundation.catalog
├── lifecycletype
├── resourcescopetype
└── demais catálogos tipados

feature.platform
├── Microservice
├── Feature
└── FeatureContext

feature.message
├── Message
├── MessageTranslation
└── Message.microserviceId
```

Com persistência:

```text
platform_microservices
      │
      ├────────< platform_features
      │
      └────────< messages
                     │
                     └────────< message_translations

platform_features >────────< platform_feature_contexts
```

Esse é o padrão base para as próximas evoluções da Golden Reference.
