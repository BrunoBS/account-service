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

**Estado:** RESOLVIDA  
**Detectada em:** G2  
**Evidência original:** account-service Verify #26, run `35547295205`

O runtime da Golden registrou Testcontainers `2.0.5`, gerenciado por Spring Boot 4.1.1.

A Foundation possuía gestão duplicada por meio de uma propriedade/import explícito do BOM do Testcontainers. Essa duplicidade foi removida no `main` de `BrunoBS/platform-libraries`.

### Correção consolidada

`platform-dependencies:1.0.0` agora:

- mantém `spring-boot-dependencies:4.1.1` como fonte tecnológica;
- não declara `testcontainers.version`;
- não importa `testcontainers-bom` separadamente.

A versão efetiva permanece `2.0.5`, alinhada ao Spring Boot 4.1.1.

### Evidência da Foundation

- commit `bd00859a066c3bd47350e12b240556d860472875`;
- Verify #92, run `35548702093`: **success**;
- Publish Maven packages #7, run `35548960128`: **BUILD SUCCESS**.

### Correção final no consumidor

Após a remoção do BOM explícito da Foundation, a Golden também migrou a dependência de teste da coordenada legada:

```text
org.testcontainers:mysql
```

para a coordenada do Testcontainers 2.x gerenciada pelo Spring Boot 4.1.1:

```text
org.testcontainers:testcontainers-mysql
```

Commit do consumidor:

`fe866936eb95f4e7701530f77f786a51d27b424f`

A validação final da G3, Verify #34 — run `35550319996`, registrou Testcontainers `2.0.5` e terminou em **BUILD SUCCESS**.

### Decisão

Observação encerrada. Não há gap aberto relacionado a Testcontainers.

## Regra para novos gaps

Antes de propor qualquer mudança na Foundation:

1. reproduzir o problema no consumidor;
2. excluir erro de POM, import, settings, workflow ou uso incorreto da Golden;
3. confirmar que o comportamento pertence à Foundation;
4. registrar evidência, impacto e alternativas;
5. obter decisão explícita antes de alterar a Foundation.

Preferência arquitetural, conveniência local ou redução de linhas não constituem gap.
