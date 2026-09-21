# Golden Reference — Foundation Gaps

**Status atual:** nenhum gap aberto contra a Foundation consolidada.

Este arquivo registra deficiências reproduzíveis e observações técnicas encontradas durante a construção da Golden Reference.

## Baseline oficial atual

A Foundation consolidada é consumida a partir de:

```text
BrunoBS/platform-libraries
br.com.portalmanager.core
platform-parent:1.0.0
platform-libraries-bom:1.0.0
capabilities:1.0.0
```

Registry:

```text
https://maven.pkg.github.com/brunobs/platform-libraries
```

## Histórico pré-consolidação

Durante a G1, o consumidor revelou dois problemas no baseline provisório anterior:

1. publicação incompleta das libraries para consumidores downstream;
2. infraestrutura JDBC/Kafka/Testcontainers exportada de forma excessiva pelo suporte de testes.

Esses problemas foram corrigidos antes da consolidação definitiva e não representam gaps abertos do baseline oficial atual.

## OBS-0001 — versão efetiva do Testcontainers

**Estado:** OBSERVAÇÃO; não classificada como gap aberto  
**Detectada em:** G2  
**Evidência:** account-service Verify #26, run `35547295205`

O runtime registrou:

```text
Testcontainers version: 2.0.5
```

No código atual da Foundation, `platform-dependencies:1.0.0` declara:

```text
testcontainers.version = 1.21.4
```

Spring Boot 4.1.1, por sua vez, gerencia Testcontainers 2.0.5.

### Impacto observado

Nenhum impacto funcional na G2:

- MySQL Testcontainer iniciou;
- migration Flyway passou;
- testes de persistência passaram;
- dependency convergence passou;
- build ficou verde.

### Classificação atual

Não alterar a Foundation a partir desta observação isolada.

Antes de qualquer proposta de correção, deve ser verificado separadamente se a propriedade da Foundation é intencional/efetiva no BOM consolidado e qual versão constitui o contrato oficial desejado.

## Regra para novos gaps

Antes de propor qualquer mudança na Foundation:

1. reproduzir o problema no consumidor;
2. excluir erro de POM, import, settings, workflow ou uso incorreto da Golden;
3. confirmar que o comportamento pertence à Foundation;
4. registrar evidência, impacto e alternativas;
5. obter decisão explícita antes de alterar a Foundation.

Preferência arquitetural, conveniência local ou redução de linhas não constituem gap.
