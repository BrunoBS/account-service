> Registro histórico. Consulte o [refinamento integral das features Golden](../features-golden/EXECUCAO.md) para nomenclatura e contratos atuais.

# Revisão dos filtros de estado nas listagens HTTP

## Contrato comum

Recursos com LifecycleTypeCode usam `?lifecycle=ACTIVE|INACTIVE|QUARANTINED`. Ausência do parâmetro seleciona ACTIVE. Texto é normalizado com trim e Locale.ROOT; valores vazios ou desconhecidos retornam 400, com a mensagem comum `validation.lifecycle.filter.invalid`.

O antigo parâmetro booleano active foi substituído nos recursos com lifecycle. Catálogos com is_active booleano continuam usando active=true|false pelo CatalogController/AbstractCatalogFacade/AbstractCatalogService da biblioteca, sem alteração desse contrato.

Controllers recebem o filtro textual e o encaminham à facade. Facades encaminham ao use case; FindAllWorkspacesInput também transporta lifecycle. O use case resolve o valor uma única vez por operação com LifecycleFilter.resolve, obtendo LifecycleTypeCode. O filtro é aplicado pelo repository no banco, antes da paginação e do mapeamento de outputs.

LifecycleFilter reutiliza LifecycleTypeEnum/LifecycleTypeCode do catálogo de lifecycle, sem consultar registros do catálogo a cada request. Os valores semânticos são definidos pelo enum existente; a existência de um registro ativo no catálogo é regra de referência para gravações, não pré-condição para consultar dados históricos em INACTIVE/QUARANTINED. Cada módulo continua acessando apenas seu próprio repository.

## Inventário dos controllers

| Controller | Listagem geral | Antes | Depois |
| --- | --- | --- | --- |
| WorkspaceController | /api/v1/workspaces | active booleano | lifecycle, paginação e visibilidade preservadas |
| ApplicationController | /api/v1/workspaces/{workspace}/applications | active booleano | lifecycle, escopo e paginação preservados |
| PublisherController | /api/v1/publishers | active booleano | lifecycle e scope |
| DefaultEnvironmentController | /api/v1/environment-defaults | active booleano | lifecycle |
| WorkspaceEnvironmentController | /api/v1/workspaces/{workspace}/environments | active booleano | lifecycle, visibilidade e acessibilidade dos ativos preservadas |
| EnvironmentTypeController | /api/v1/environment-types | todos os estados | lifecycle, ordenação displayOrder/id preservada |
| EnvironmentTypeCompatibilityController | /api/v1/environment-types/compatibilities | parser local ACTIVE/INACTIVE | resolver comum, incluindo QUARANTINED |
| MicroserviceController | /api/v1/platform/microservices | todos os estados | lifecycle, ordem por id |
| FeatureController | /api/v1/platform/features | todos os estados ou busca por contexto | Page, lifecycle + contextCode em uma consulta, sem duplicar features |
| FeatureController.findContexts | /api/v1/platform/features/{identifier}/contexts | coleção de contexts | Page e lifecycle dos contexts; feature existente em qualquer lifecycle para consulta histórica por OWNER |
| FeatureContextController | /api/v1/platform/contexts | todos os estados | Page, lifecycle, ordem por id e sort validado |
| MessageController | /api/v1/messages | active opcional; ausência listava todos | lifecycle, padrão ACTIVE, content/page; somente ACTIVE/INACTIVE persistidos |
| MessageTranslationController | /api/v1/messages/{message}/translations | active opcional; ausência listava todos | lifecycle, padrão ACTIVE, content/page; somente ACTIVE/INACTIVE persistidos |
| PlatformSchemaController | /api/v1/schemas | todos os estados | lifecycle + scope PLATFORM |
| WorkspaceSchemaController | /api/v1/workspaces/{workspace}/schemas | todos os estados | lifecycle + scope WORKSPACE + workspace |
| SchemaBindingController | /api/v1/schema-bindings | todos os estados | lifecycle, ordem por id |
| SharedOwnerController | /api/v1/workspaces/{workspace}/applications/{application}/shared-contracts | todos os estados | lifecycle, conteúdo/contagem paginados com o mesmo filtro |
| Controllers de catálogos | application-scope-type, authorization-type, onboarding-type, schema-version-status-type, share-status-type, lifecycle-type, publication-mode-type, resource-scope-type, schema-scope, tag-origin-type, workspace-type, visibility-type | active booleano | preservado |
| OnboardingController | progresso individual | sem findAll/listagem geral | preservado |
| SharedParticipantController | participações e contratos disponíveis | status de participação / somente contratos disponíveis ativos | preservado |

## Consultas específicas

Consultas de disponibilidade do Shared permanecem limitadas a recursos ativos, pois INACTIVE/QUARANTINED não podem oferecer novos vínculos. Participações usam ShareStatusTypeCode, um estado próprio (REQUESTED/APPROVED etc.), não lifecycle. O filtro status permanece com sua regra específica.

Resumo de aplicações e consultas de topologia (roots/children/tree) continuam representando a seleção ativa prevista por esses endpoints. Listagens de versões de schema usam SchemaVersionStatusTypeCode. Consulta de contextos vinculados à feature é uma projeção da associação, e não findAll do recurso. Esses endpoints não receberam um filtro de lifecycle que alteraria o significado do resultado.

Nenhuma transição de domínio foi adicionada: quando um recurso não possui registros QUARANTINED, a listagem desse estado simplesmente retorna vazia. Links históricos, visibilidade e regras para referências ativas não foram relaxados.

## Validação

LifecycleListingIT cobre as 16 listagens gerais com dados nos estados permitidos por cada recurso, padrão ACTIVE, normalização e erro para filtro vazio/desconhecido. Após a revisão de message, mensagens e traduções persistem somente ACTIVE/INACTIVE e o teste confirma página vazia para QUARANTINED nessas duas rotas. Também verifica combinação de contextCode com lifecycle e contagem paginada de quarentena nos recursos que possuem esse estado.

A revisão final de message manteve o filtro centralizado e validou essa distinção junto com paginação, autorização e exclusão física. A verificação passou com 95 testes unitários/de arquitetura e 40 integrações; log: `/private/tmp/message-physical-deletion-verify.log`. O contrato atualizado está em [MESSAGE-INTEGRATION.md](../message/MESSAGE-INTEGRATION.md).

Testes existentes de ApplicationApiIT, EnvironmentApiIT, MessageApiIT e WorkspaceApiIT foram atualizados para o contrato lifecycle; testes de catálogos continuam com active booleano.

Verificação realizada:

- Regressão dos módulos: 90 testes unitários/de arquitetura e 106 testes de integração existentes passaram (CatalogApiIT, PlatformApiIT, PlatformQueryIT, MessageApiIT, SharedApiIT, SchemaApiIT, EnvironmentApiIT e ApplicationApiIT). A primeira execução também incluiu os 2 testes novos de LifecycleListingIT, que falharam na preparação da fixture; os dados de teste foram corrigidos. Log: /private/tmp/lifecycle-listings-review.log.
- LifecycleListingIT após correção da fixture: BUILD SUCCESS, 90 unitários/de arquitetura e 2 testes de integração, cobrindo as 16 rotas gerais. Log: /private/tmp/lifecycle-listings-focused.log.
- Compilação final de produção e testes: BUILD SUCCESS com `mvn --offline --batch-mode --no-transfer-progress -DskipTests test-compile`. Log: /private/tmp/lifecycle-final-compile.log.
- Regressão focada de filtro/paginação/autorização de workspaces: BUILD SUCCESS, 90 unitários/de arquitetura e 13 integrações (3 métodos de WorkspaceApiIT e 10 WorkspaceAuthorizationIT). Log: /private/tmp/lifecycle-workspace-regression.log.
- Resultado consolidado: 90 testes unitários/de arquitetura e 121 integrações distintas passaram nas verificações focadas. git diff --check também passou.

A suíte completa do serviço não foi reexecutada nesta etapa. Falhas preexistentes de outros cenários de PublisherApiIT/WorkspaceApiIT estão registradas nas revisões anteriores; esta etapa valida os filtros de publishers também pela matriz HTTP de LifecycleListingIT.
