# G2 — Persistência e modelo mínimo

**Fase:** G2 — Implementação  
**Estado:** EM EXECUÇÃO  
**Data:** 2026-09-20

## Entry gate

A G2 iniciou somente após a revalidação da Foundation consolidada e da identidade final do serviço.

Baseline:

- Maven `br.com.portalmanager:account-service:0.1.0-SNAPSHOT`;
- Java `br.com.portalmanager.account`;
- Foundation `br.com.portalmanager.core:*:1.0.0`;
- registry único `BrunoBS/platform-libraries`;
- CI com repository Maven isolado verde;
- nenhuma instalação local da Foundation.

## Decisões aprovadas

As decisões estruturais estão registradas em `ADR-002-G2-PERSISTENCE-BASELINE.md`.

### Migrations

```text
Flyway
```

Fluxo obrigatório:

```text
MySQL vazio
→ Flyway
→ Hibernate ddl-auto=validate
→ aplicação/testes
```

### Lifecycle

```text
AccountLifecycle
├── ACTIVE
└── INACTIVE
```

Lifecycle é estado explícito do domínio e não catálogo administrável.

### AccountType

Na G2:

```text
AccountType
├── ADMIN
└── MANAGER
```

A G2 não introduz `platform-catalog`. A integração com catálogo permanece reservada para G4 e exige caso de uso concreto.

## Escopo em implementação

- entidade `Account`;
- entidade `AccountApprover`;
- migration V1;
- Spring Data JPA;
- MySQL;
- repository específico;
- unique name;
- identifier único;
- lifecycle persistido;
- optimistic locking com `@Version`;
- testes MySQL/Testcontainers;
- teste de migration;
- regra arquitetural mínima.

## Fora de escopo

- controllers;
- casos de uso HTTP;
- onboarding;
- tagging;
- audit;
- integração catalog;
- matriz completa de autorização;
- CRUD genérico.

## Guardrails

- zero `platform-crud`;
- nenhuma classe `BaseCrud*`;
- nenhuma abstração genérica para esconder JPA;
- `ddl-auto=update` proibido;
- migrations são a única fonte de criação/evolução do schema;
- Hibernate apenas valida;
- MySQL real nos testes de persistência;
- infraestrutura Testcontainers declarada explicitamente no consumidor;
- entity não é contrato HTTP.

## Critério de saída

A G2 só fecha quando:

- MySQL vazio recebe a migration V1;
- Hibernate valida o schema criado por Flyway;
- Account e approvers persistem;
- nome único é protegido fisicamente;
- lifecycle inicial ACTIVE é persistido;
- stale update falha via `@Version`;
- repository permanece específico;
- testes de arquitetura passam;
- `mvn clean verify` fica verde no CI com Maven repository isolado;
- review e evidências são documentados.

## Foundation

Nenhuma alteração na Foundation está autorizada por esta onda.

Falhas encontradas na implementação devem ser classificadas primeiro como problema da Golden ou de integração. Somente evidência reproduzível pode reabrir uma discussão sobre Foundation.
