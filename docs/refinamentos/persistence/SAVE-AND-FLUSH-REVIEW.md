> Registro histórico. Consulte o [refinamento integral das features Golden](../features-golden/EXECUCAO.md) para nomenclatura e contratos atuais.

# Persistência explícita em core e feature

Revisão de 14/10/2026, conforme [padrão do projeto](../../REVIEW-STANDARD.md).

## Regra

Atualizações de entidades usam `repository.saveAndFlush(entity)`, incluindo mudanças de lifecycle, status, configuração e alterações do agregado durante associações. Outputs e snapshots são construídos com a entidade retornada após o flush. Criações individuais seguem a mesma convenção; operações em lote podem usar `saveAllAndFlush`.

A revisão cobre todas as operações de escrita em `core` e `feature`, inclusive métodos auxiliares que coordenam modificações de entidades. Não muda regras de domínio, contratos HTTP, locks, limites transacionais nem migrations.

## Resultado por módulo

| Módulo | Resultado |
| --- | --- |
| Core Workspace | Já utiliza saveAndFlush em create/update, lifecycle e conclusão de onboarding. |
| Core Application | Já utiliza saveAndFlush em create/update e lifecycle. |
| Core Publisher | Já utiliza saveAndFlush em create/update e lifecycle. |
| Core Environment | Tipos, compatibilidade e operações compartilhadas dos ambientes já utilizam saveAndFlush em create/update e lifecycle. |
| Feature Platform — microservice/feature/context | Update, activate e inactivate passam de flush isolado para saveAndFlush. Create de context passa de save + flush para saveAndFlush. |
| Feature Platform — featurecontext | O contexto alterado por uma associação é salvo explicitamente; os fluxos auditados salvam explicitamente a feature alterada. Retentativas sem mudança continuam retornando sem persistência/evento adicional. |
| Feature Message — message/translation | Update, activate e inactivate passam de flush isolado para saveAndFlush; create individual e em lote já seguem a convenção. |
| Feature Shared — contract/participation | Criações passam de save para saveAndFlush. Update/lifecycle do contrato e reenvio, aprovação, configuração, rejeição e revogação de participação salvam explicitamente antes do output. Mapeamentos são persistidos pela cascata do agregado de participação. |
| Feature Segment — segment/category | Update, activate e inactivate usam saveAndFlush; create já segue a convenção. |
| Feature Onboarding | Conclusão delega ao command de Workspace, já conforme. Registro de fase mantém insertIfAbsent SQL atômico para preservar idempotência e o requester original. |

Foram ajustadas 11 classes de produção: 33 pontos de atualização e 3 de criação. A fixture unitária de `FeatureContextCommandService` recebe o repository de contexto obrigatório; verificações existentes de rejeição passam a observar o método de persistência utilizado.

## Flushes com finalidade própria

Os flushes restantes em core/feature não substituem o saveAndFlush de uma atualização:

- Workspace: remoção de approvers libera valores únicos antes de inserir substitutos; o agregado continua salvo por saveAndFlush ao final.
- Exclusões físicas: EnvironmentType, EnvironmentTypeCompatibility, Context, Message e traduções usam delete/deleteAll com flush, preservando os snapshots de PURGE.
- Inserções SQL atômicas de relações e registros de onboarding mantêm seus contratos de concorrência/idempotência. A atualização de estado dos agregados envolvidos é explicitamente persistida por saveAndFlush.

O tratamento de violações de integridade usa o handler central da plataforma. Os commands de Segment/SegmentCategory não interpretam código de erro do driver nem nome de constraint. Validadores aplicam mensagens de negócio; violações do banco propagam com rollback e resposta 409/GLOBAL-0004, sem expor SQL.

## Validação

`mvn --offline --batch-mode --no-transfer-progress test`: **BUILD SUCCESS**, 115 testes unitários/arquiteturais, sem falhas, erros ou skips. Log local: `/private/tmp/save-and-flush-unit.log`.

`mvn --offline --batch-mode --no-transfer-progress -Dit.test=SharedApiIT,FeatureContextApiIT,MessageAuditIT verify`: **BUILD SUCCESS**, 115 unitários/arquiteturais + 65 integrações (52 Shared, 8 feature/context, 5 auditoria de mensagens), sem falhas, erros ou skips. Log: `/private/tmp/save-and-flush-focused.log`.

`mvn --offline --batch-mode --no-transfer-progress clean verify`: **BUILD SUCCESS**, **479 testes** (115 unitários/arquiteturais + 364 integrações), sem falhas, erros ou skips; tempo de 10min51s. Inclui persistência real, versões, auditoria/rollback, substituição de mapeamentos, idempotência e concorrência, além da regressão de todas as APIs e migrations. Log local: `/private/tmp/save-and-flush-full.log`. `git diff --check` também passou.

A [nova revisão do refinamento de segmentos](../segment/SEGMENT-CATEGORIES-V1.md) corrigiu a disponibilidade da categoria após mudança concorrente do pai e a mensagem de filtro de lifecycle. Seu build completo também valida todos os ajustes de persistência deste registro: **481 testes** (115 unitários/arquiteturais + 366 integrações), sem falhas, erros, skips ou flakes. Log local: `/private/tmp/segments-full-verify.log`.
