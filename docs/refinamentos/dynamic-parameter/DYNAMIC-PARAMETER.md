# DynamicParameter — gestão de ParameterKey

## Refatoração do domínio

Feature: `feature.dynamicparameter`. Entidade principal: `domain.ParameterKey`.
Contratos e casos de uso recebem o prefixo ParameterKey; a facade e os componentes de runtime/integração recebem DynamicParameter. Catálogos seguem o ADR-008: `foundation.catalog.parameterkeytype` e `parameterkeystatustype`. Entry point: `entrypoint.web.dynamicparameter.ParameterKeyController`.

A feature ainda está sem consumidores, conforme decisão do usuário. Não foram mantidos aliases ou classes de compatibilidade. As regras existentes foram preservadas.

| Superfície | Nome atual |
| --- | --- |
| Coleção administrativa | `/api/v1/dynamic-parameters` |
| Publicação | `POST /api/v1/dynamic-parameters/publish` → 202 sem corpo |
| Catálogos | `/api/v1/parameter-key-type`, `/api/v1/parameter-key-status-type` |
| Propriedades | `platform.dynamic-parameter.*` |
| Entidade / tabela / coluna da chave | `ParameterKey` / `parameter_keys` / `parameter_key` |
| Tabelas de catálogo | `type_parameter_key_types`, `type_parameter_key_statuses` |
| Schemas | `PLATFORM/dynamic-parameter`, `PLATFORM/dynamic-parameter-publication` |
| Mensagens | chaves `parameter-key.*`, códigos `PARAMETER-KEY-*` |
| Auditoria | recurso `PARAMETER_KEY`, eventos `PARAMETER_KEY_*` |

Migrations novas V75 e V76 renomeiam tabelas/coluna/índices/constraints e identidades/bindings de schema, preservando ids, UUIDs, versões de entidades, valores/snapshots, status e referências. As migrations V1–V74 e as versões publicadas anteriores dos schemas permanecem intactas. Novas versões de schema alteram somente o título exibido. Recursos históricos JSON usados para validar V74 ficam em `src/test/resources/migration`; produção usa apenas os nomes atuais.

Continuam iguais: payloads HTTP e JSON Schema estrutural, limites, versionamento otimista, locks, lifecycle, autorização OWNER, transações/auditoria individuais, publicação assíncrona, MSET em lotes de 500, scheduler horário, snapshot publicado imutável durante edição e remoção após inativação/exclusão. A integração Redis mantém o envelope `{type,value}`, namespace `dynamic-parameter`, chaves lógicas e tipos serializados `CONFIGURATION`/`EXPRESSION`, sem TTL. Esses códigos de valor são semântica de negócio, não nomes de classes de infraestrutura.

## Validação desta refatoração

- `mvn --offline --batch-mode --no-transfer-progress clean verify`: **531 testes aprovados** (131 unitários/arquiteturais + 400 integrações), zero falhas, erros, skips ou flakes. **12min59s**. Log: `/private/tmp/dynamic-parameter-full.log`.
- Validação direcionada anterior: **24 testes aprovados** (5 de arquitetura, 17 da API e 2 de migração), incluindo as rotas exclusivamente canônicas e o upgrade V74 → V76. Log: `/private/tmp/dynamic-parameter-contracts.log`. Esses casos também estão incluídos na regressão completa.
- Integrações com MySQL e Redis reais via platform-testing/Testcontainers. Cobertura de preservação de dados/versões/snapshots, histórico publicado de schemas, constraints, publicação assíncrona/202, MSET 500/500/200, recuperação pelo scheduler, lifecycle idempotente, rollback/auditoria, autorização, JSON/expressões e concorrência.
- SHA-256 de todas as migrations V1–V74 conferido: nenhuma foi alterada. O teste de upgrade compara os dados antes/depois do rename, preserva o histórico do schema e verifica as constraints renomeadas.
- A regressão inicial identificou expectativas antigas nos testes de contagem de auditoria e de classificação JDBC para CHECK. Foram corrigidas para PARAMETER_KEY e o código MySQL 3819. Uma rota adicional remanescente da substituição automática também foi removida; o teste arquitetural impede aliases.
- Fora dos módulos renomeados, a única classe de produção alterada foi MicroserviceQueryService, apenas para renomear os métodos usados por DynamicParameter. Fontes das demais features e alterações anteriores foram preservados. Links do README/documento atual e git diff --check aprovados.


Implementação na branch `feature/segments-categories-v1`, preservando as alterações anteriores. Nenhuma tabela de publicação foi criada e nenhuma migration foi alterada. O envio em lote consome a platform-redis V5; sua versão já validada foi instalada apenas no Maven local, sem publicação remota.

## Contrato administrativo

- Criação: active=true, DRAFT, publishedValue/publishedType nulos; não escreve no Redis.
- Edição de tipo/valor: DRAFT e preserva publishedValue/publishedType anteriores. Alterar somente descrição mantém o status. Identidade e chave permanecem imutáveis.
- Publicação: POST /api/v1/dynamic-parameters/publish, JSON `{"identifiers":["uuid"]}` com 1 ou N identifiers únicos. Retorno 202 sem corpo, sem identificador de acompanhamento.
- Validação estrutural e autorização OWNER/UPDATE ocorrem antes da aceitação. Validações de existência/estado são individuais no processamento em background.
- O executor gerenciado pelo Spring processa a lista fora da thread HTTP, com contexto de usuário/auditoria e correlação preservados e restaurados ao final. Cada identifier tem sua própria transação. Falhas individuais não interrompem os demais.
- Uma publicação válida copia value/type para publishedValue/publishedType, passa a PUBLISHED e usa saveAndFlush. Redis é acionado somente após commit. Não há transação distribuída: falha Redis não reverte o snapshot salvo.
- O snapshot é capturado quando o worker processa o item; esta etapa não registra o conteúdo no momento do 202.
- Inativação: active=false, DRAFT, snapshot nulo e remoção Redis imediatamente após commit. Reativação: active=true/DRAFT, sem snapshot e sem envio. Exclusão somente inativa e também remove Redis após commit.

## Scheduler e Redis

O scheduler em integration.scheduler usa intervalo e atraso inicial de uma hora, configuráveis. A consulta usa active=true e publishedValue IS NOT NULL, paginação por id e consultas de referência de microsserviço em lote. Cada página já traz os snapshots publicados completos, sem consulta individual por chave, e é enviada com RedisStore.setAll/MSET. Não há SCAN, remoção de órfãos, contadores de sincronização, leases ou Lua na feature. A chave lógica é <microservice.code>.<key> e o envelope é {type,value}; RedisStore aplica namespace e serialização, sem TTL.

Um JSON null é um valor JSON válido; ausência de publicação é SQL NULL. Objetos/listas vazios também são válidos conforme o schema. O tipo publicado acompanha o valor para preservar o snapshot após edição.

Runtime e scheduler habilitados por padrão. Configurações:

- platform.dynamic-parameter.runtime.enabled (true)
- platform.dynamic-parameter.reconciliation.enabled (true)
- platform.dynamic-parameter.reconciliation.interval (PT1H)
- platform.dynamic-parameter.runtime.batch-size (500): limite de chaves por lote manual e por página do scheduler

O tamanho de lote pertence ao consumidor e deve ser compatível com platform.redis.max-batch-size da lib (1000 por padrão). A antiga propriedade reconciliation.batch-size foi substituída por runtime.batch-size; configure a nova propriedade. Propriedades de lease e SCAN foram removidas. Configuração inválida de intervalo/tamanho falha na inicialização com PlatformConfigurationException. Testes sem Redis desabilitam runtime/scheduler; testes de integração da feature habilitam runtime com @WithRedis.

## Envio em lote — V5

O endpoint mantém 202 sem corpo. O worker particiona os identifiers recebidos em grupos de até runtime.batch-size, com padrão 500. Cada item confirma sua própria transação/auditoria; somente identifiers confirmados são reunidos para envio. Uma consulta em lote relê os snapshots ativos publicados antes do MSET. Se nenhum item do grupo for confirmado, não há consulta/envio Redis.

Para 1.200 identifiers válidos, são três envios: 500, 500 e 200. Falhas de validação/persistência excluem apenas o item afetado. MSET é atômico no seu lote: uma falha de envio gera um log sanitizado por snapshot afetado, mantém o publishedValue no banco e não interrompe os grupos/páginas seguintes. Não há fallback de SET individual. O scheduler poderá reenviar esses snapshots na próxima execução. Não existe atomicidade entre lotes nem entre banco e Redis.

O scheduler usa paginação por id e lê os valores completos diretamente do banco; o mesmo setAll envia cada página. Inativação/exclusão usam delete individual após commit, sem esperar completar lote ou o agendamento.

## Erros

Somente logs de erro da feature, sem logs de sucesso, início ou resumo. DynamicParameterErrorLogger resolve códigos/mensagens/soluções pelo ApiMessageResolver da platform-messaging e usa o campo estruturado platform.error da observability. Identifier, fullKey e publishedValue são registrados quando disponíveis; payload passa por DefaultLogSanitizer. Causas/mensagens cruas de drivers não são incluídas. Erros antes de localizar a chave e erros gerais de consulta não possuem chave/snapshot e registram esses campos nulos.

## Organização

A estrutura atual possui 37 arquivos Java de produção na feature e sua entrada HTTP. A renomeação não introduziu abstrações novas.

ParameterKeyPublicationService agenda e particiona o trabalho; ParameterKeyPublicationOperations aplica transação e auditoria de uma chave. ParameterKeyAfterCommitSynchronization registra a remoção após commit. DynamicParameterRuntimeService reúne envio/remoção pela lib e sincronização horária. ParameterKeyCommandService/QueryService, Finder e LifecycleOperations mantêm CRUD, bloqueio de escrita e auditoria idempotente; ParameterKeyNormalizer atende ao contrato da lib de schemas.

## Limites desta etapa

202 confirma aceitação em memória, não conclusão nem entrega durável. Ainda não existem tabelas de solicitações de publicação e itens, consulta de resultados, retries persistidos ou snapshot da solicitação. O scheduler recupera publicações já salvas, mas não solicitações perdidas antes do commit.

A sincronização horária é eventual, sem coordenação distribuída entre instâncias. SELECT e MSET não são atômicos com uma inativação concorrente. Uma falha na remoção pode deixar resíduo no Redis: como o job consulta apenas registros ativos publicados, ele não repara essas remoções; repetir inativação/exclusão tenta novamente. Recuperação durável e ordenação concorrente ficam para a fase das tabelas, sem alterar a lib genérica.

## Histórico

Os checkpoints anteriores à renomeação permanecem em [configuration](../configuration/CONFIGURATION-CHECKPOINT-3.md), identificados como históricos. As contagens e nomes de testes daqueles documentos descrevem as versões anteriores, não esta refatoração.

Sem commit, push, merge ou publicação de artefatos nesta tarefa.

## Organização por domínio — refinamento integral Golden

ParameterKey e seus modelos, facade, repository/projeções, validação e operações residem em `parameterkey` dentro de cada camada. Integração/runtime e scheduler mantêm suas responsabilidades existentes. Não há alteração física adicional: V75–V76 já alinham o banco a ParameterKey. A refatoração integral atual alinha também o namespace Redis para `dynamic-parameter`, sem alias ou exclusão automática das antigas chaves de desenvolvimento; a publicação e a reconciliação escrevem no namespace novo. Envelope, chave lógica, snapshots, operações em lote e regras permanecem iguais.

[Checkpoint atual das ondas](../features-golden/EXECUCAO.md).
