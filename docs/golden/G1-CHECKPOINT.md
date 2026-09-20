# G1 Checkpoint — Skeleton + Foundation

**Estado:** bloqueado por permissão de consumo do GitHub Packages  
**Bootstrap commit:** `3a5eb731d121aacbc56e97deea744811fee491fb`

## Objetivo

Provar que `account-service` nasce como aplicação nova sobre `FOUNDATION-GOLDEN-V1`, sem dependência estrutural do legado.

## Baseline adotado

- repository: `BrunoBS/account-service`;
- package root: `com.empresa.golden`;
- artifact: `com.empresa.golden:account-service:0.1.0-SNAPSHOT`;
- parent: `com.empresa.platform:platform-parent:1.0.1`;
- runtime Foundation: `platform-starter`;
- test Foundation: `platform-test-support`;
- Java 25 / Spring Boot 4.1.1 herdados do parent.

## Guardrails confirmados

- zero dependências/imports de `platform-crud`;
- nenhum `BaseCrud*` ou equivalente introduzido;
- nenhum código de produção copiado de `account-api`;
- nenhuma persistência adicionada em G1;
- capabilities `audit`, `catalog` e `tagging` permanecem fora do POM até seus casos de uso serem implementados;
- integração oficial usa resolução remota via GitHub Packages.

## Evidências de CI

### Run #1 — 35533860119

Commit: `3a5eb731d121aacbc56e97deea744811fee491fb`

Resultado:

- checkout: sucesso;
- Java 25: sucesso;
- Maven Verify: falha;
- causa: `PLATFORM_PACKAGES_TOKEN` ausente, resultando em HTTP 401 ao resolver `platform-parent:1.0.1`.

### Run #3 — 35533896832

Commit: `9e15895cd77fce9542ab017efe92ac17db00c8c7`

O workflow passou a possuir `packages: read` e utiliza `github.token` como fallback quando `PLATFORM_PACKAGES_TOKEN` não estiver configurado.

Resultado:

- checkout: sucesso;
- Java 25: sucesso;
- token presente com `Packages: read`;
- Maven Verify: falha;
- causa: o package privado continua invisível ao token do repositório consumidor; Maven informa que `com.empresa.platform:platform-parent:pom:1.0.1` não foi encontrado no registry.

## Classificação da pendência

Esta pendência é de infraestrutura/permissão do GitHub Packages, não um gap técnico da Foundation.

A Foundation já provou a resolução remota de `platform-parent:1.0.1` no checkpoint `FOUNDATION-GOLDEN-V1`.

## Ação externa necessária

Uma destas opções precisa ser aplicada:

1. conceder ao repositório `BrunoBS/account-service` acesso de leitura ao package privado publicado pelo `platform-build`; ou
2. configurar no repositório `account-service` o secret `PLATFORM_PACKAGES_TOKEN` com token autorizado a ler os packages da Foundation.

O workflow já aceita as duas formas: prefere `PLATFORM_PACKAGES_TOKEN` quando existir e usa `github.token` como fallback.

## Critério de fechamento

A G1 somente será marcada como concluída quando um novo run provar:

```text
GitHub Packages
→ resolve platform-parent:1.0.1
→ resolve platform-starter
→ compila account-service
→ inicia contexto Spring Boot em teste
→ platform-test-support executa teste de integração
→ Maven Enforcer + dependency convergence
→ mvn clean verify
→ GitHub Actions verde
```

Até essa evidência, não iniciar G2.
