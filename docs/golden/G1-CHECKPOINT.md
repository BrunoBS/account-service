# G1 Checkpoint — Skeleton + Foundation

**Estado:** em validação

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

## Guardrails

- zero dependências/imports de `platform-crud`;
- nenhum `BaseCrud*` ou equivalente introduzido;
- nenhum código de produção copiado de `account-api`;
- nenhuma persistência adicionada em G1;
- capabilities `audit`, `catalog` e `tagging` permanecem fora do POM até seus casos de uso serem implementados;
- integração oficial depende de resolução remota via GitHub Packages.

## Evidência esperada para fechamento

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

A conclusão da G1 deve registrar o run do GitHub Actions que comprovar o fluxo acima.
