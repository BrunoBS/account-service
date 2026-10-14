# Revisão de feature.onboarding

Data: 13/10/2026. Critério: [padrão de revisão](../../REVIEW-STANDARD.md).

Revisão e refatoração do controller, responses, facade, use cases, validator,
adapter/port, histórico, catálogo, migrations e integrações com Workspace/Application.

## Decisões e contratos

| Regra | Implementação |
| --- | --- |
| Aplicabilidade | Somente workspace MANAGER ativo e visível para o usuário. |
| Auditoria | Onboarding não gera eventos de auditoria. O histórico de fases é dado funcional; os demais comandos de core mantêm suas regras próprias de auditoria. |
| Responsável | `requester` vem de `UserContext`/`UserSession.getUserName()`, com até 10 caracteres. Não é recebido no request nem truncado. |
| Histórico | Uma conclusão por workspace/fase; requester e data originais são preservados. |
| Fases obrigatórias | As quatro fases precisam estar configuradas e ativas. Uma fase ausente/inativa representa configuração inválida; não é ignorada. |
| Ordem | Conclusões fora de ordem são permitidas. A ordem de exibição é sortOrder e, em empate, code. |
| Conclusão final | Exige as quatro conclusões, retorna 204 e é idempotente. `readyToComplete` permanece distinto de `completed`. |

O contrato HTTP expõe `phases[].requester` em lugar de `userIdentifier`.
A V60 renomeia a coluna e verifica dados antigos antes de reduzir seu limite:
identidades vazias ou acima de 10 caracteres exigem correção explícita, sem perda
silenciosa de dados.

## Fluxo dos endpoints

| Endpoint | Caminho e regra | Resultado |
| --- | --- | --- |
| `GET /api/v1/workspaces/{workspaceIdentifier}/onboarding` | Controller → facade DEV/READ → query → serviço público de Workspace com visibilidade/estado → valida MANAGER → serviço público do catálogo → histórico próprio → status. | 200 com progresso; 404 para inexistente, invisível ou inativo; 400 para tipo não suportado; 500 para configuração inválida. |
| `PATCH /api/v1/workspaces/{workspaceIdentifier}/onboarding` | Controller → facade DEV/UPDATE → command transacional → query do progresso → valida readiness → comando público de Workspace → lock/releitura → conclusão. | 204 sem body; 400 se incompleto; 404 para recurso indisponível; 500 para configuração inválida. |

Controller cuida do transporte e responses. Facade cuida de autorização e
delegação. Regras e transações ficam nos use cases. O PATCH não recebe payload,
portanto não precisa de um JSON Schema de request.

## Registro automático das fases

| Fase | Gatilho atual | Integridade |
| --- | --- | --- |
| `WORKSPACE_REGISTRATION` | Criação de workspace MANAGER. | Registro na mesma transação do workspace e tags; falha obrigatória reverte a criação. |
| `WORKSPACE_FIRST_ENVIRONMENT` | Configuração de ambiente do workspace, ainda não implementada. | Integração futura, conforme decisão anterior. |
| `FIRST_APPLICATION_REGISTRATION` | Criação da primeira aplicação vinculada ao workspace. | Workspace pai bloqueado antes da verificação; falha reverte aplicação, tags e histórico. |
| `APPLICATION_FIRST_ENVIRONMENT` | Configuração de ambiente da aplicação, ainda não implementada. | Integração futura, conforme decisão anterior. |

A criação de um Environment isolado não conclui configuração de workspace ou
aplicação. Essa regra está na [decisão de implementação](../workspace/ONBOARDING-IMPLEMENTATION-ANALYSIS.md).
Sem as duas integrações futuras, as operações públicas atuais não permitem
registrar todas as fases. Os testes da conclusão preparam essas duas conclusões
como fixture; não demonstram gatilhos de configuração ainda inexistentes.

Core chama `OnboardingPhaseCompletionPort.registerPhaseCompletion`. O adapter
delega a `OnboardingPhaseCompletionCommandService`, que valida catálogo e sessão
e insere o histórico. A conclusão total usa `OnboardingCommandService`.
A separação evita a dependência circular que ocorreria com um único command:
WorkspaceCommandService → adapter → command → WorkspaceCommandService.

| Cenário | Status | readyToComplete | completed |
| --- | --- | --- | --- |
| Workspace recém-criado | Cadastro COMPLETED; primeira fase ausente IN_PROGRESS; demais PENDING. | false | false |
| Primeira aplicação antes da configuração do ambiente | Fases 1 e 3 COMPLETED; fase 2 IN_PROGRESS; fase 4 PENDING. | false | false |
| Quatro conclusões registradas | Todas COMPLETED. | true | false |
| PATCH após quatro conclusões | Histórico preservado; booleano do workspace alterado. | true | true |
| PATCH repetido ou simultâneo | Uma alteração de versão; requester/datas do histórico preservados. | true | true |

Inativação/quarentena de aplicações não desfaz conclusões históricas.

## Ajustes aplicados

| Ponto revisado | Resultado |
| --- | --- |
| Facade com orquestração e transação | Extraído `OnboardingCommandService.complete`; facade autoriza e delega. |
| Adapter com regras/persistência | Adapter passou a delegar ao command de conclusão de fase. |
| Repositories de outros módulos | Query e validator consomem `OnboardingPhaseTypeService`; core é acessado por seus serviços públicos. Regra de arquitetura impede acesso a repositories externos. |
| Exceção específica | Removida `OnboardingConfigurationException`; erros técnicos usam `ApiException` da plataforma com chaves, status e mensagens localizadas. |
| Nomenclatura | `OnboardingMilestonePort/Adapter` substituídos por `OnboardingPhaseCompletionPort/Adapter`; `register` por `registerPhaseCompletion`. |
| Validação final | `requireCompleted` renomeado para `requireReadyToComplete`, refletindo a pré-condição real. |
| Variáveis | Serviços e dados nomeados `workspaceQueryService`, `phaseCatalogService`, `completionRepository`, `phaseOutputs` e `inProgressPhaseCode`. |
| `findProgress` | Método principal concentra busca, validação e composição do progresso. Histórico por fase, montagem dos outputs e decisão de status ficam em métodos menores. A primeira fase sem conclusão é calculada explicitamente, sem booleano mutável. |
| Ordenação | sortOrder com desempate por code. |
| Settings | Schema de catálogo na V61 e validação defensiva do formato persistido; orientation numérico/booleano não é convertido em texto. |
| Teste de version no create de Workspace | Corrigida expectativa antiga: version informada recebe 400; create sem version inicia em zero e update usa a versão retornada. |
| Regressão de publicadores | Fixture passa a restaurar a versão 2 publicada pela V51; paginação usa códigos distintos e teste de settings prepara os dados inválidos/ válidos da ação correspondente. O teste de upgrade compara o schema real com a referência. |

## Mensagens da plataforma

`ApiException` permite resolver chave, idioma e HTTP no handler comum, preservando
a causa técnica quando existente. Não foi necessário alterar a biblioteca nem
criar um advice de onboarding. Erros de negócio continuam usando
`ValidationException`.

| Chave | Código | HTTP | Condição |
| --- | --- | --- | --- |
| WORKSPACE_TYPE_NOT_SUPPORTED | ONBOARDING-0001 | 400 | Workspace não MANAGER. |
| PHASES_NOT_CONFIGURED | ONBOARDING-0002 | 500 | Catálogo obrigatório ausente/inativo. |
| INCOMPLETE | ONBOARDING-0003 | 400 | Conclusão solicitada antes das quatro fases. |
| AUTHORIZATION_CONTEXT_UNAVAILABLE | ONBOARDING-0004 | 500 | Integração sem identidade autenticada utilizável. |
| SETTINGS_INVALID | ONBOARDING-0005 | 500 | Settings persistidos incompatíveis com o contrato. |
| REQUESTER_INVALID | ONBOARDING-0006 | 500 | Identidade autenticada acima de 10 caracteres. |

Mensagens disponíveis em pt_BR/en. Causas internas não são expostas na resposta.

## Settings do catálogo

A V61 publica e vincula `onboarding-phase-type-settings` ao catálogo
`onboarding-phase-type`. O contrato exige objeto JSON, aceita orientation
opcional como string/null e preserva outros metadados. Create/update do catálogo
usam a validação central de schemas.

A V30 já converteu settings de catálogos em coluna JSON: o banco rejeita sintaxe
inválida. A query detecta também formato estrutural inválido que possa existir
em registros anteriores ao novo schema. O mapper do catálogo faz o parse uma
vez por fase; a feature consome JsonNode, sem repetir a desserialização.

## Persistência, concorrência e desempenho

| Ponto | Avaliação |
| --- | --- |
| Registro único | UNIQUE(workspace_id, onboarding_phase_code) com INSERT ON DUPLICATE KEY UPDATE id=id garante inserção atômica sem sobrescrever requester/data. |
| Primeira aplicação | Verificação protegida pelo bloqueio do workspace pai; teste de criações concorrentes mantém um registro de fase. |
| Conclusão final | Lock e releitura de WorkspaceFinder recuperam o estado atual. Domínio evita nova alteração em repetição; @Version acompanha a primeira gravação. |
| Transações | `@Transactional` padrão, sem isolation explícito nem Propagation.MANDATORY. Commands participam da transação externa quando existentes. |
| Histórico | ID interno, FKs e unique adequados para registro subordinado e append-only. Sem lifecycle ou @Version artificial. |
| Consulta | Workspace, quatro fases em lote e histórico em lote; sem N+1. Autorização pode acrescentar suas consultas. |
| Índice | A unique começa por workspace_id e atende à busca do histórico. Nenhum índice adicional necessário demonstrado. |
| Tipo do workspace | A feature exige MANAGER. Pelas operações atuais, aplicações vinculadas, inclusive em quarentena, impedem mudança de tipo; não foi constatada corrida de tipo no fluxo legítimo de conclusão. |

Inativar uma fase necessária bloqueia o progresso; inativar a fase de cadastro
bloqueia a criação correspondente que precisa registrá-la. Essa política de
configuração obrigatória foi preservada.

## Verificação

A seleção de regressão validou 104 testes unitários e 73 integrações distintas
de onboarding, catálogo, Workspace, Application, autorização, auditoria e upgrades
V60/V61. O rerun de `OnboardingApiIT` terminou com seus 10 testes aprovados após
corrigir uma fixture que tentava inserir sintaxe inválida em coluna JSON.

Cobertura inclui ausência de auditoria, repetição/conclusão simultânea sem nova
versão, acesso por visibilidade, estado inativo, requester ausente/longo com
rollback, mensagens localizadas, catálogo incompleto, settings inválidos, empate
de sortOrder, schema no create/update do catálogo e preservação do histórico nas
migrations.

Logs das seleções:
- `/private/tmp/onboarding-refactor-verify.log`
- `/private/tmp/onboarding-refactor-api-final.log`

A primeira execução completa foi interrompida após o Docker atingir o limite de
I/O assíncrono do MySQL (`io_setup: EAGAIN`) com contêineres acumulados. Nela foram
identificadas quatro falhas nas fixtures/expectativas de publicadores, corrigidas
conforme a tabela de ajustes. Log: `/private/tmp/onboarding-full-verify.log`.

A tentativa com `-DreuseForks=false` confirmou a liberação dos contêineres,
mas foi substituída por lotes para evitar o custo de inicialização do Docker
por classe. Log dessa tentativa: `/private/tmp/onboarding-full-final-verify.log`.

Verificação final da suíte completa concluída: 104 testes unitários distintos
e 255 testes de integração, sem falhas, erros ou skips. As 46 classes de integração
foram distribuídas em três lotes de até 16 classes, cada um executado com
`mvn --offline --batch-mode --no-transfer-progress -Dit.test=<classes_do_lote> verify`.
Os três lotes terminaram com BUILD SUCCESS (82, 158 e 15 integrações).
Os testes unitários também são executados em cada lote. Os contêineres são
liberados ao final de cada processo Maven, sem alterar a aplicação ou suas
transações. Logs: `/private/tmp/onboarding-final-batch-1.log`,
`/private/tmp/onboarding-final-batch-2.log` e
`/private/tmp/onboarding-final-batch-3.log`.

Após simplificar `findProgress`, os 104 testes unitários e os dez cenários de
`OnboardingApiIT` passaram novamente, com BUILD SUCCESS. Log:
`/private/tmp/onboarding-find-progress-refactor.log`.

## Resultado

Os ajustes de arquitetura, nomes, mensagens, validação, transações e ausência de
auditoria desta revisão foram implementados. As duas integrações futuras de
configuração de ambientes permanecem conforme o escopo previamente definido e
precisarão de seus testes quando essa funcionalidade for implementada.
