# Golden Reference — Foundation Gaps

**Status:** 1 gap confirmado e resolvido durante G1.

Este arquivo registra somente deficiências reproduzíveis da Foundation encontradas durante a construção da Golden.

## GAP-0001 — Distribuição remota de platform-libraries para consumidores Golden

**Status:** RESOLVIDO  
**Detectado em:** G1 — Skeleton + consumo da Foundation  
**Data:** 2026-09-20

**Capability:** distribuição/build da Foundation.

**Cenário reproduzível:**  
Um serviço independente usa `platform-parent:1.0.1`, `platform-starter:1.0.0` e `platform-test-support:1.0.0` e executa `mvn clean verify` em runner limpo, autenticado no GitHub Packages.

**Comportamento esperado:**  
O parent deve ser resolvido remotamente pelo registry de `platform-build` e os artefatos das libraries devem ser resolvidos remotamente pelo registry de `platform-libraries`.

**Comportamento encontrado:**  
O parent era resolvido remotamente, mas `platform-starter:1.0.0` e `platform-test-support:1.0.0` não existiam no registry remoto de `platform-libraries`. O reactor também não possuía `distributionManagement` próprio nem workflow de publicação das libraries.

**Evidência:**  
No `account-service`, com autenticação válida e ambos os registries configurados, Maven confirmou a ausência dos artefatos `1.0.0`. A versão foi preservada porque o probe remoto demonstrou que não havia release completo conflitante no registry correto.

**Impacto:**  
Uma aplicação Golden não conseguia consumir a Foundation completa remotamente sem uma publicação das libraries, contrariando o fluxo oficial que proíbe dependência de `mvn install` local entre repositórios.

**Correção executada:**  
`platform-libraries` recebeu:

- `distributionManagement` próprio para `https://maven.pkg.github.com/brunobs/platform-libraries`;
- configuração Maven de publicação;
- workflow de publicação manual;
- publicação do reactor Foundation `1.0.0`.

**Evidência da correção:**  

- `platform-libraries` Verify #48 — run `35534990434`: **success**;
- `platform-libraries` Publish Maven packages #2 — run `35534990418`: **success / BUILD SUCCESS**;
- `account-service` Verify #7 — run `35535402633`: **success / BUILD SUCCESS**.

**Autenticação:**  
O consumidor continua usando `PLATFORM_PACKAGES_TOKEN`. A publicação no registry do próprio `platform-libraries` usa o `GITHUB_TOKEN` efêmero do workflow com `packages: write`, sem exposição de tokens.

**Decisão:**  
Gap encerrado. Não houve alteração de versão das libraries nem mudança arquitetural das capabilities da Foundation.

## Regra para novos gaps

Para cada novo gap registrar:

- capability;
- cenário reproduzível;
- comportamento esperado;
- comportamento atual;
- evidência/teste;
- impacto;
- alternativas;
- recomendação;
- decisão.

Preferência arquitetural, redução de linhas ou conveniência local não constituem gap de Foundation.

Nenhuma nova alteração em `platform-build` ou `platform-libraries` deve ser iniciada sem evidência e decisão explícita.
