# ADR-005 — Migração da Golden para o namespace Platform

## Status

Accepted.

## Data

2026-09-21

## Atualização pós-G4

A decisão deste ADR sobre o namespace da **Foundation** (`br.com.portalmanager.platform`) permanece vigente. A identidade da aplicação `account-service` / `br.com.portalmanager.account` descrita abaixo foi posteriormente substituída pela decisão `Account → Workspace` registrada em `ADR-ADEQUACAO-ARQUITETURAL-POS-G4.md`. O trecho é preservado como evidência do estado da migração quando este ADR foi aceito.

## Contexto

A Foundation alterou sua identidade pública para evitar ambiguidade entre capacidades da plataforma e domínio de aplicação.

A decisão oficial da Foundation está registrada em:

`BrunoBS/platform-libraries/docs/adr/ADR-004-PLATFORM-NAMESPACE-AND-MODULE-NAMING.md`.

O namespace anterior:

```text
br.com.portalmanager.core
```

foi substituído por:

```text
br.com.portalmanager.platform
```

Também foram renomeados:

```text
platform-logging      → platform-observability
platform-test-support → platform-testing
```

Por decisão do projeto, todos os artifacts da Foundation permanecem em `1.0.0`.

## Decisão

A Golden Reference passa a consumir exclusivamente a Foundation em:

```text
br.com.portalmanager.platform
```

Coordenadas principais:

```text
br.com.portalmanager.platform:platform-parent:1.0.0
br.com.portalmanager.platform:platform-dependencies:1.0.0
br.com.portalmanager.platform:platform-libraries-bom:1.0.0
br.com.portalmanager.platform:platform-starter:1.0.0
br.com.portalmanager.platform:platform-observability:1.0.0
br.com.portalmanager.platform:platform-messaging:1.0.0
br.com.portalmanager.platform:platform-authorization:1.0.0
br.com.portalmanager.platform:platform-audit:1.0.0
br.com.portalmanager.platform:platform-catalog:1.0.0
br.com.portalmanager.platform:platform-tagging:1.0.0
br.com.portalmanager.platform:platform-testing:1.0.0
```

A aplicação continua com identidade própria:

```text
Maven: br.com.portalmanager:account-service:0.1.0-SNAPSHOT
Java:  br.com.portalmanager.account
```

## Observability

A Golden não declara `platform-observability` diretamente porque ele chega pelo `platform-starter`.

O prefixo vigente da capability é:

```text
platform.observability.logging
```

Não existe compatibilidade na Golden com `platform.logging`.

## Testing

A dependência de teste passa a ser:

```text
br.com.portalmanager.platform:platform-testing:1.0.0
```

Imports passam a usar:

```text
br.com.portalmanager.platform.testing.*
```

## Evidência da Foundation

Baseline publicado:

- Foundation head `44440ec61c96b410778669e1219d89377c13a837`;
- Verify #95: sucesso;
- Publish Maven packages #8, run `35630329890`: sucesso;
- versão publicada: `1.0.0`.

## Validação da Golden

A migração só é considerada concluída quando:

1. não há imports/coordenadas `br.com.portalmanager.core`;
2. não há `platform-test-support`;
3. não há `platform-logging`;
4. não há configuração `platform.logging`;
5. Maven resolve os artifacts somente do registry remoto `BrunoBS/platform-libraries`;
6. `mvn clean verify` passa com repository Maven local isolado;
7. os testes G1–G4 permanecem verdes.

## Consequência

Esta migração é um realinhamento da Foundation já validada e não inicia G5.

O próximo checkpoint funcional permanece G5 somente após a revalidação do consumidor.
