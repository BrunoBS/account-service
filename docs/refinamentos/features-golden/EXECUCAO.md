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

Organização por responsabilidades reais. Nesta revisão, os pacotes únicos de domain, facade, integration, repository e validation foram achatados; model e operations mantêm phase/progress e completion/progress, respectivamente. WorkspaceOnboardingCompletion e adaptadores reais foram preservados. Nenhum nome físico ou contrato estava divergente: tabela e endpoints preservados, sem migration vazia. 28 testes unitários/arquitetura + 40 de integração, sem falhas/erros/skips; BUILD SUCCESS. Inclui Workspace API, Onboarding API e migrations requester/schema de fase. Log `/private/tmp/golden-wave5-verify.log`.

## Onda 6 — Dynamic Parameter

Agregado ParameterKey mantido diretamente em domain, facade, repository, usecase.model e usecase.validation, pois cada camada tem um único domínio. O facade oferece command, query e command publisher. O agendamento de reconciliação fica no `ParameterKeyPublisherService`; o runtime concentra acesso ao Redis e a consulta paginada. Sem nova migration SQL: nomes físicos já canônicos em V75–V76. Namespace Redis alinhado para dynamic-parameter, mantendo envelope, chaves lógicas, tipos de negócio, batch, async e scheduler; chaves antigas de desenvolvimento não são apagadas. 36 testes unitários/arquitetura + 34 de integração, sem falhas/erros/skips; BUILD SUCCESS, MySQL/Redis reais. Log `/private/tmp/golden-wave6-verify.log`.

## Onda 7 — Validação geral

`mvn --offline --batch-mode --no-transfer-progress verify`: BUILD SUCCESS, 133 testes unitários/arquitetura + 404 testes de integração = 537; zero falhas, erros ou skips. A execução integrou Testcontainers com MySQL e Redis reais, incluindo banco limpo, APIs, concorrência, ciclo de vida, auditoria, publicação/reconciliação Redis e upgrades com dados nas migrations V77–V80. O endpoint atual de grupos de features é `/api/v1/service-contexts/feature-groups`, com `featureGroupIdentifier`; a associação persiste em `feature_group_feature`.

Integridade: hashes das 76 migrations anteriores conferidos (76/76 inalteradas); `git diff --check` limpo; packages Java alinhados aos diretórios; busca no código Java de imports/packages/rotas e classes públicas legadas sem referências ativas. Os schemas SQL/JSON em `target/classes` foram comparados aos fontes. O schema `dynamic-parameter.json`, que apresentou bytes extras transitórios durante uma execução, foi restaurado byte a byte do backup pré-tarefa e validado como JSON; a execução final completa passou com a cópia de recurso correta. Log final: `/private/tmp/golden-features-final-verify.log`.

Resultado consolidado: todas as ondas concluídas. Não há falhas ou pendências conhecidas. Nenhum commit, push, publicação de artefato ou merge foi feito.


## Ajuste posterior — FeatureGroup e validação de mensagens

Dynamic Bundle separa a validação de mensagens (`MessageValidator`) da validação de traduções (`MessageTranslatorValidator`). Service Context usa os agregados Microservice, Feature e FeatureGroup; a entidade FeatureGroup não declara nome JPQL explícito. V81 renomeia as tabelas `service_context_groups` e `service_context_feature_groups` para `feature_group` e `feature_group_feature`, incluindo a coluna `feature_group_id`, sem recriar dados.

A validação posterior foi executada com `mvn --offline --batch-mode --no-transfer-progress -Dit.test=ServiceContextRenameMigrationIT,FeatureGroupPersistenceIT,FeatureGroupQueryIT,FeatureGroupResourceSchemaMigrationIT,FeatureGroupApiIT,FeatureApiIT,ServiceContextApiIT,ServiceContextEndpointContractIT,FeatureConcurrencyIT,FeatureGroupConcurrencyIT verify`: BUILD SUCCESS, 135 testes unitários/arquitetura + 63 testes de integração, sem falhas ou skips. Incluiu a migration V81 com upgrade de dados, APIs, filtros, persistência, consultas JPQL e concorrência. Log: `/private/tmp/service-context-feature-group-final.log`.

## Revisão local posterior — schemas canônicos

A revalidação do estado local identificou 117 arquivos não rastreados que duplicavam os pacotes e contratos legados já substituídos pela organização aprovada. Após comparar as versões com os agregados refatorados, os arquivos foram preservados em `/private/tmp/account-service-local-refactor-preserved.tar.gz` e retirados das source roots; isso removeu os erros de compilação que eles introduziam.

A revisão de nomenclatura encontrou títulos e binding de schema ainda associados a `Segment Category` e `Feature Context`. V82 atualiza a definição/configuração de associação para `feature-group-feature`, publica os títulos canônicos `Feature Group associations request` e `Category request`, e mantém o payload e as versões publicadas anteriores. A referência V65 usada pelo teste histórico foi movida para `src/test/resources/migration/service-context/feature-context.json`.

Validação do estado antes de V82: `mvn --offline --batch-mode --no-transfer-progress verify`, 135 testes unitários/arquiteturais + 396 de integração = 531, BUILD SUCCESS com MySQL/Testcontainers. Após V82 e os ajustes das fixtures: os testes direcionados `CanonicalFeatureSchemaNamesMigrationIT` e `FeatureGroupResourceSchemaMigrationIT` passaram (135 testes unitários/arquiteturais + 2 integrações), BUILD SUCCESS. O socket do Docker Desktop foi informado somente à execução Maven via `DOCKER_HOST`; nenhuma configuração global foi alterada. Logs: `/private/tmp/account-service-golden-refactor-verify-docker.log` e `/private/tmp/account-service-feature-schema-migrations.log`.


## Revisão posterior — subpacotes por domínio

Regra aplicada em todas as features: criar subpacotes por domínio dentro de uma camada somente quando houver mais de um domínio real naquela camada. Se houver um único domínio, as classes ficam diretamente na camada; subpacotes por outros motivos claros, como `repository/projection`, continuam válidos. A regra evita caminhos redundantes como `domain/parameterkey/ParameterKey` sem misturar agregados diferentes.

Na revisão completa, `dynamicparameter` teve achatados domain, facade, repository, usecase/model, usecase/validation e o pacote HTTP `dynamicparameter/parameterkey`; `integration` e `usecase/operations` mantiveram seus subpacotes distintos. Em `onboarding`, domain, facade, integration, repository e usecase/validation ficaram diretos; model e operations mantêm seus conceitos separados. Em `resourcesharing`, a única política de validation foi movida para a camada, enquanto Contract e Participant continuam separados nas camadas com múltiplos domínios. `dynamicbundle`, `segmentation` e `servicecontext` já mantêm múltiplos domínios nas camadas agrupadas, então seus pacotes foram preservados.

Validação desta reorganização: compilação de 498 classes principais e 108 classes de teste; 24 testes unitários/arquitetura direcionados passaram, incluindo as regras de arquitetura atualizadas. Depois, `mvn --offline --batch-mode --no-transfer-progress -Dit.test=ParameterKeyApiIT,OnboardingApiIT,ResourceSharingApiIT verify` passou com 135 testes unitários/arquitetura + 79 integrações, zero falhas/erros/skips; BUILD SUCCESS. As integrações usaram MySQL real via Testcontainers. A primeira tentativa deste verify revelou a regra de arquitetura antiga e falhou antes das integrações; após atualizar a regra, a execução final passou.

## Ajuste posterior — nomes concisos dos módulos

Os packages das features foram alinhados ao padrão acordado: `feature.resourcesharing` passou a `feature.sharing` e `feature.dynamicparameter` passou a `feature.parameter`. `DynamicBundle` e `ServiceContext` permanecem compostos para evitar colisão/ambiguidade com `message` e com serviços de aplicação; `Segmentation` e `Onboarding` permanecem inalterados. O README e o teste arquitetural refletem os seis nomes finais.

A alteração se limita à identidade/pacote do módulo. Foram preservados nomes de domínio e contratos que precisam ser explícitos fora do contexto do package: classes `ResourceSharing*`/`DynamicParameter*`, rotas `/resource-sharing` e `/dynamic-parameters`, tabelas `resource_sharing_*` e `parameter_keys`, e namespace Redis `dynamic-parameter`. Nenhuma regra de negócio mudou.

Validação após a troca dos packages: `mvn --offline --batch-mode --no-transfer-progress -Dit.test=ParameterKeyApiIT,ParameterKeyPublicationIT,ParameterKeyReconciliationIT,ParameterKeyConcurrencyIT,ResourceSharingApiIT verify` passou com 137 testes unitários/arquitetura + 81 integrações, sem falhas/erros/skips; BUILD SUCCESS. Testou endpoints, scan do Spring, persistência, publicação/reconciliação Redis e concorrência. `git diff --check` passou e não há referências ativas aos packages antigos em `src` ou no README.

## Revisão geral da documentação — 2026-10-18

O estado atual foi comparado com os packages e configurações ativos. A estrutura
documentada usa o namespace `br.com.portalmanager.platform.workspace`, as seis
features `dynamicbundle`, `onboarding`, `parameter`, `segmentation`, `servicecontext`
e `sharing`, e controllers de catálogo diretamente em `entrypoint.catalog`. O código
tem 14 módulos concretos de catálogo em `foundation.catalog`.

O guia de Parameter foi atualizado para remover referências a packages antigos,
propriedades `platform.dynamic-parameter.*`, `DynamicParameterErrorLogger` e outros
componentes que já não existem. Ele agora descreve `WorkspaceProperties`,
`ParameterKeyPublisherService`, as flags reais de runtime/reconciliação, o contrato
HTTP assíncrono e o uso genérico de `RedisStore`. README e FOUNDATION-USAGE também
foram alinhados à configuração e dependências atuais; a ADR-007 reflete os controllers
genéricos diretamente em `entrypoint.catalog`. O ADR-006 está explicitamente marcado
como registro histórico, pois sua configuração de Messaging não existe mais no código.

Validação da contagem da última execução completa: os relatórios Surefire/Failsafe
presentes em `target` somam 538 testes, 0 falhas, 0 erros e 0 skips. O guia aponta para
esse resultado sem alterar os números dos checkpoints históricos anteriores. Nenhuma
migration, contrato de runtime ou código de produção foi alterado nesta revisão de
documentação.
