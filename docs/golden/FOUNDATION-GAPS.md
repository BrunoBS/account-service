# Golden Reference — Foundation Gaps

**Status atual:** nenhum gap aberto contra a Foundation consolidada.

Este arquivo registra somente deficiências reproduzíveis da Foundation encontradas durante a construção da Golden Reference.

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

A Foundation consolidada incorporou:

- publicação unificada;
- BOM próprio das capabilities;
- namespace oficial;
- infraestrutura de teste pesada opt-in;
- remoção definitiva de `platform-crud`.

## Regra para novos gaps

Antes de propor qualquer mudança na Foundation:

1. reproduzir o problema no consumidor;
2. excluir erro de POM, import, settings, workflow ou uso incorreto da Golden;
3. confirmar que o comportamento pertence à Foundation;
4. registrar evidência, impacto e alternativas;
5. obter decisão explícita antes de alterar a Foundation.

Preferência arquitetural, conveniência local ou redução de linhas não constituem gap.

Enquanto a migração para a Foundation consolidada estiver em validação, qualquer falha deve ser tratada primeiro como problema de integração do consumidor até haver evidência em contrário.
