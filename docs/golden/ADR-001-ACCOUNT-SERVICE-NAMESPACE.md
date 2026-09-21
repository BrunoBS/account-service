# ADR-001 — Namespace Java do account-service

## Status

Superseded.

## Data

2026-09-20

## Supersessão

Esta decisão registra o namespace utilizado entre G1 e G4. Foi substituída em 2026-09-21 por `ADR-ADEQUACAO-ARQUITETURAL-POS-G4.md`, que adota `Workspace` como conceito de domínio e `br.com.itau.portalmanager.workspace` como root Java ativo. O texto abaixo permanece inalterado como evidência histórica.

## Contexto

A Golden Reference passou a ser implementada no repositório `BrunoBS/account-service`.

Durante G0/G1, o skeleton utilizou provisoriamente:

```text
com.empresa.golden
```

Esse namespace não representa a identidade definitiva do serviço e não deve ser propagado para entidades, repositories, application services, controllers ou regras arquiteturais da G2 em diante.

A Foundation consolidada possui namespace próprio e separado:

```text
br.com.portalmanager.platform
```

Account é domínio de aplicação e não pertence ao namespace `core` da Foundation.

## Decisão

O package root Java oficial do `account-service` é:

```text
br.com.portalmanager.account
```

A classe principal passa a residir em:

```text
br.com.portalmanager.account.AccountServiceApplication
```

Packages funcionais devem evoluir a partir desse root, por exemplo:

```text
br.com.portalmanager.account.api
br.com.portalmanager.account.application
br.com.portalmanager.account.domain
br.com.portalmanager.account.persistence
br.com.portalmanager.account.onboarding
br.com.portalmanager.account.configuration
```

## Separação de namespaces

```text
br.com.portalmanager.platform.*      → Foundation
br.com.portalmanager.account.*   → account-service
```

A Golden é o papel arquitetural deste serviço, não um namespace funcional. Portanto não será criado `br.com.portalmanager.golden.*`.

Também não será utilizado `br.com.portalmanager.platform.account.*`, pois Account não pertence à Foundation.

## Consequências

- todo novo código da G2 em diante deve nascer sob `br.com.portalmanager.account`;
- o skeleton G1 é migrado antes da implementação de persistência;
- testes arquiteturais futuros devem considerar esse root;
- nenhuma camada de compatibilidade com `com.empresa.golden` será mantida.

## Coordenada Maven da aplicação

Além do package Java, foi aprovada a identidade Maven do serviço:

```text
br.com.portalmanager:account-service:0.1.0-SNAPSHOT
```

Portanto:

```text
Maven groupId : br.com.portalmanager
artifactId    : account-service
Java root     : br.com.portalmanager.account
```

A Foundation permanece separada em `br.com.portalmanager.platform`.

## Consequência adicional

O namespace provisório `com.empresa.golden` é removido também da coordenada Maven da aplicação.
