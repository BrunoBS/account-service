# Refinamento integral das features Golden

## Escopo e execução

Branch: `feature/segments-categories-v1`. Refatoração estrutural sem alteração de regras de negócio ou aliases legados. Alterações anteriores preservadas. Backup inicial: `/private/tmp/golden-features-before.tar.gz`. Inventário detalhado: `/private/tmp/golden-features-inventory.json`. Checksums de todas as migrations iniciais: `/private/tmp/golden-features-migrations.json`.

| Onda | Mapeamento | Estado |
|---|---|---|
| 0 | Inventário e baseline completo | Verde: 531 testes |
| 1 | message → dynamicbundle; MessageTranslation → MessageTranslator | Verde: 65 testes |
| 2 | platform → servicecontext; Context → Group; FeatureContext → FeatureGroup | Verde: 224 testes |
| 3 | segment → segmentation; SegmentCategory → Category | Verde: 69 testes |
| 4 | shared → resourcesharing; SharedContract → Contract; SharedParticipation → Participant | Verde: 97 testes |
| 5 | Onboarding, responsabilidades existentes | Verde: 68 testes |
| 6 | DynamicParameter, ParameterKey e integrações existentes | Verde: 70 testes |
| 7 | Build, integração, revisão consolidada | Verde: 537 testes |

## Onda 0

Inventário: message 23 classes, platform 50, segment 23, shared 31, onboarding 12, dynamicparameter 31. O modelo atual contém os conceitos MessageTranslation, Context, FeatureContextRelation, SegmentCategory e SharedParticipation, que serão alinhados aos nomes aprovados. Request/security contexts e capacidades das bibliotecas ficam fora dessas renomeações.

O primeiro build dentro do sandbox não conseguiu acessar o socket Docker (`Operation not permitted`); isso é impedimento de ambiente, anterior a qualquer refatoração. O build foi repetido fora do sandbox para executar Testcontainers com infraestrutura real. Nenhuma feature será alterada antes do baseline válido.

Migrations V1–V76 são imutáveis. Cada onda com mudança física terá migration incremental e teste de upgrade com dados. As migrations históricas e testes que deliberadamente inspecionam versões anteriores mantêm os nomes daquela versão.

Baseline aprovado: `mvn --offline --batch-mode --no-transfer-progress clean verify`, 131 testes unitários/arquitetura + 400 de integração, sem falhas, erros ou skips. Log: `/private/tmp/golden-features-baseline.log`. A segunda tentativa apresentou indisponibilidade de arquivos gerados durante a execução; repetição sem processos concorrentes confirmou baseline verde.

## Onda 1 — Dynamic Bundle

Packages por Message e MessageTranslator; API `/api/v1/dynamic-bundles/messages` e filhos `/translators`, composição `translators`, tabela `message_translators`, view `vw_dynamic_bundle_messages`, bindings e mensagens alinhados. V77 preserva dados/versões/FKs/cascata e publica novas versões de schema. Sem entidade Bundle artificial. Nenhuma migration anterior alterada.

Validação limpa: 27 testes unitários/arquitetura e 38 de integração (API, auditoria, concorrência, lifecycle, migrations históricas e upgrade 76→77 com dados), sem falhas/erros/skips. `BUILD SUCCESS`. Log: `/private/tmp/golden-wave1-verify.log`.

## Onda 2 — Service Context

Renomeação de feature, Group e FeatureGroupRelation; packages por responsabilidade; contratos HTTP, nomes físicos, views, índices, FKs, auditoria e binding de schemas alinhados. V78 preserva dados e referências de mensagens, parâmetros e contratos. O upgrade 77→78 compara todas as colunas de Microservice/Feature/Group e os pares da associação.

Validação: 38 testes unitários/arquitetura + 186 de integração. Três fixtures precisaram correção (aliases SQL reservados e exemplo de normalização); reteste dos três cenários e do upgrade passou com 19 testes de integração e BUILD SUCCESS. Nenhuma falha permanece. Logs `/private/tmp/golden-wave2-verify.log` e `/private/tmp/golden-wave2-retest.log`. Instalação limpa validada pelas APIs/JPA. Índices de FK são renomeados antes da recriação das constraints para preservar a ordem correta no MySQL.

Um relatório JaCoCo gerado impediu o clean; o target foi preservado em `/private/tmp/golden-wave2-target-before-clean` e a validação prosseguiu em diretório novo. Contextos de infraestrutura e propriedades platform.* permanecem com seus nomes corretos.

## Onda 3 — Segmentation

Segment/Category organizados por domínio; contrato `/api/v1/segmentation/segments`; V79 renomeia Category, preservando FK obrigatória e unique por segmento. Pré-verificação de órfãos/duplicidades e comparação integral no teste de upgrade. Bindings de Service Context limitados aos seus recursos, mantendo o ownership PLATFORM independente das features.

27 testes unitários/arquitetura + 42 de integração, sem falhas/erros/skips; BUILD SUCCESS. Inclui API, concorrência, migration histórica, upgrade 78→79 com dados, Group API e upgrade de Service Context. Log `/private/tmp/golden-wave3-verify.log`.

## Onda 4 — Resource Sharing

Contract/Participant em domain, facade, repository, model, operações e contratos web; EnvironmentMapping pertence a Participant. V80 renomeia tabelas, coluna participant_id, índices e FKs, preservando dados/versões/relacionamentos/cascatas. Referências internas usam ResourceSharing; enum ApplicationScope.SHARED preservado.

23 testes unitários/arquitetura + 74 de integração, sem falhas/erros/skips; BUILD SUCCESS. Inclui 52 cenários HTTP, Feature API, migration histórica e upgrade 79→80 com comparação integral e cascata. A fixture foi ajustada ao limite existente de requester. Um recurso gerado truncado foi regenerado; todos os recursos compilados foram comparados aos fontes. Log `/private/tmp/golden-wave4-final.log`.

## Onda 5 — Onboarding

Organização por completion/progress/phase, preservando WorkspaceOnboardingCompletion e os adaptadores reais. Nenhum nome físico ou contrato estava divergente: tabela e endpoints preservados, sem migration vazia. 28 testes unitários/arquitetura + 40 de integração, sem falhas/erros/skips; BUILD SUCCESS. Inclui Workspace API, Onboarding API e migrations requester/schema de fase. Log `/private/tmp/golden-wave5-verify.log`.

## Onda 6 — Dynamic Parameter

Agregado ParameterKey organizado em subpacotes por camada; runtime/scheduler preservados. Sem nova migration SQL: nomes físicos já canônicos em V75–V76. Namespace Redis alinhado para dynamic-parameter, mantendo envelope, chaves lógicas, tipos de negócio, batch, async e scheduler; chaves antigas de desenvolvimento não são apagadas. 36 testes unitários/arquitetura + 34 de integração, sem falhas/erros/skips; BUILD SUCCESS, MySQL/Redis reais. Log `/private/tmp/golden-wave6-verify.log`.

## Onda 7 — Validação geral

`mvn --offline --batch-mode --no-transfer-progress verify`: BUILD SUCCESS, 133 testes unitários/arquitetura + 404 testes de integração = 537; zero falhas, erros ou skips. A execução integrou Testcontainers com MySQL e Redis reais, incluindo banco limpo, APIs, concorrência, ciclo de vida, auditoria, publicação/reconciliação Redis e upgrades com dados nas migrations V77–V80. O endpoint atual de grupos de features é `/api/v1/service-contexts/feature-groups`, com `featureGroupIdentifier`; a associação persiste em `feature_group_feature`.

Integridade: hashes das 76 migrations anteriores conferidos (76/76 inalteradas); `git diff --check` limpo; packages Java alinhados aos diretórios; busca no código Java de imports/packages/rotas e classes públicas legadas sem referências ativas. Os schemas SQL/JSON em `target/classes` foram comparados aos fontes. O schema `dynamic-parameter.json`, que apresentou bytes extras transitórios durante uma execução, foi restaurado byte a byte do backup pré-tarefa e validado como JSON; a execução final completa passou com a cópia de recurso correta. Log final: `/private/tmp/golden-features-final-verify.log`.

Resultado consolidado: todas as ondas concluídas. Não há falhas ou pendências conhecidas. Nenhum commit, push, publicação de artefato ou merge foi feito.


## Ajuste posterior — FeatureGroup e validação de mensagens

Dynamic Bundle separa a validação de mensagens (`MessageValidator`) da validação de traduções (`MessageTranslatorValidator`). Service Context usa os agregados Microservice, Feature e FeatureGroup; a entidade FeatureGroup não declara nome JPQL explícito. V81 renomeia as tabelas `service_context_groups` e `service_context_feature_groups` para `feature_group` e `feature_group_feature`, incluindo a coluna `feature_group_id`, sem recriar dados.

A validação posterior foi executada com `mvn --offline --batch-mode --no-transfer-progress -Dit.test=ServiceContextRenameMigrationIT,FeatureGroupPersistenceIT,FeatureGroupQueryIT,FeatureGroupResourceSchemaMigrationIT,FeatureGroupApiIT,FeatureApiIT,ServiceContextApiIT,ServiceContextEndpointContractIT,FeatureConcurrencyIT,FeatureGroupConcurrencyIT verify`: BUILD SUCCESS, 135 testes unitários/arquitetura + 63 testes de integração, sem falhas ou skips. Incluiu a migration V81 com upgrade de dados, APIs, filtros, persistência, consultas JPQL e concorrência. Log: `/private/tmp/service-context-feature-group-final.log`.
