# Análise e decisão de implementação — onboarding

Referência funcional: `/Users/bruno/Documents/workspace/account-api`. Destino: arquitetura atual de workspace-service. A análise foi feita antes da implementação; as decisões confirmadas e o escopo entregue estão registrados ao final.

## Comportamento encontrado no account-api

| Etapa | Gatilho real |
| --- | --- |
| `ACCOUNT_REGISTRATION` | Após criar a conta. |
| `ACCOUNT_FIRST_ENVIRONMENT` | Salvar configuração da conta em ambiente de tipo DEFAULT e autorização DEV. Não é criar o ambiente. |
| `FIRST_APPLICATION_REGISTRATION` | Após criar uma aplicação da conta. |
| `APPLICATION_FIRST_ENVIRONMENT` | Salvar configuração de uma aplicação em ambiente de tipo DEFAULT e autorização DEV. |

Cada conclusão registra conta, fase, usuário autenticado e data. O serviço verifica existência antes de inserir; não foi encontrada proteção suficiente contra duplicidade concorrente nesse algoritmo.

GET de onboarding retorna fases ordenadas: concluídas são COMPLETED, a primeira fase ainda não concluída é IN_PROGRESS e as seguintes são PENDING. Registrar etapas não exige sequência obrigatória. O PATCH final exige todas as fases concluídas e marca `onboarding=true`; chamadas posteriores não alteram novamente o booleano. As conclusões representam histórico, sem reversão automática quando recursos são removidos.

Arquivos principais: `feature/onboarding/OnboardingService`, `OnboardingRepository`, `AccountOnboardingCompletion`, `core/account/AccountService`, `core/application/ApplicationService`, `feature/configuration/account/AccountConfigurationService`, `feature/configuration/application/ApplicationConfigurationService` e `web/account/AccountController`.

## Diferenças no projeto atual

1. O catálogo `foundation.catalog.onboardingphasetype` já existe, com códigos WORKSPACE_REGISTRATION, WORKSPACE_FIRST_ENVIRONMENT, FIRST_APPLICATION_REGISTRATION e APPLICATION_FIRST_ENVIRONMENT. Reutilizá-lo.
2. `Workspace.onboarding` já existe e inicia false, mas não possui operação de conclusão nem histórico de fases.
3. Não existem as configurações workspace–ambiente e aplicação–ambiente utilizadas pela referência. `Environment.workspaceId` identifica o dono do ambiente customizado; não representa configuração de uma aplicação ou associação de um workspace a um ambiente DEFAULT global.
4. A coluna `orientation` do catálogo legado foi removida pela V5. Se a resposta precisar de instruções por fase, usar metadado em settings do catálogo e validar sua estrutura, em vez de restaurar silenciosamente uma coluna antiga.
5. ADMIN não permite criar aplicações; exigir as quatro fases para esse tipo tornaria a conclusão impossível. A aplicabilidade das fases por tipo deve ser definida.
6. Usuário comum cria MANAGER sem grupo. O onboarding não deve contornar a regra de visibilidade aceita para esses registros; um owner configura o grupo antes do acesso protegido.

## Organização proposta

| Pacote | Responsabilidade |
| --- | --- |
| `feature.onboarding.domain` | Entidade de conclusão, enum de status e OnboardingMessageKeys. |
| `feature.onboarding.repository` | Persistência das conclusões e projeções de leitura próprias. |
| `feature.onboarding.usecase.operations` | Commands separados para registrar fase e concluir onboarding, evitando dependência circular; query para consultar progresso. |
| `feature.onboarding.usecase.validation` | Pré-condições de conclusão e aplicabilidade das fases. |
| `feature.onboarding.usecase.model` | Outputs de fases/progresso, com tipos explícitos. |
| `feature.onboarding.facade` | AuthorizationRequired, contexto de autorização e delegação. |
| `feature.onboarding.integration` | Adaptador para receber o registro automático dos módulos core. |
| `foundation.integration` | Contrato mínimo de integração para core comunicar os marcos, sem depender de feature. |
| `entrypoint.onboarding` | Controller e responses HTTP; sem expor entidade ou projeção de repository. |

`GoldenArchitectureTest` proíbe core depender de feature. Portanto WorkspaceCommandService e ApplicationCommandService não podem injetar diretamente um serviço de feature.onboarding. Usar o contrato `PhaseCompletionPort.registerPhaseCompletion`, seguindo a fronteira adotada para `WorkspaceReferencePort`. Não usar outro repository de módulo diretamente.

## Alterações necessárias

| Componente | Alteração |
| --- | --- |
| Banco | Nova migration após V45 para `workspace_onboarding_completions`, com ID interno de workspace, código de fase, usuário e data; FK para workspaces/type_onboardings e UNIQUE(workspace_id, phase_code). Não editar migrations existentes. |
| Catálogo | Garantir as fases necessárias com ordenação determinística e política explícita para fases inativas/faltantes. Evitar concluir um onboarding vazio por ausência de configuração. |
| Workspace create | Registrar WORKSPACE_REGISTRATION na mesma transação da criação, usando a sessão autenticada. Falha no registro deve reverter a operação inteira. |
| Application create | Registrar FIRST_APPLICATION_REGISTRATION no workspace pai, preservando o bloqueio de integridade já implementado. |
| Configurações de ambiente | Implementar os vínculos/configurações necessários ou definir gatilhos equivalentes antes de registrar as duas fases de ambiente. Não marcar fases com base apenas no nome do ambiente. |
| Workspace | Adicionar método de conclusão que marque onboarding, atualize updatedAt e mantenha controle de versão; operação idempotente. Expor essa alteração por usecase público do próprio módulo, sem acesso externo ao repository. |
| Query de progresso | Validar workspace ativo/visível primeiro; consultar catálogo e conclusões em lote e calcular status em Java. Usar data tipada e NULL para fase não concluída, evitando DATE_FORMAT e strings "-" da referência. |
| API | Proposta: GET e PATCH `/api/v1/workspaces/{workspaceIdentifier}/onboarding`; PATCH sem body como na referência, retorna 204 e valida pré-condições no servidor. |
| Segurança | Leitura DEV/READ; a referência permite DEV no PATCH. Definir a ação de escrita explicitamente conforme a biblioteca; restringir a ADM se a regra de produto exigir. Aplicar ResourceVisibility ao acesso ao workspace, inclusive conclusão. Usuário/data nunca são informados pelo cliente. |
| Histórico e auditoria | Decisão atualizada em 13/10/2026: onboarding não gera eventos de auditoria. Preservar requester e data no histórico funcional das fases. Uma repetição idempotente não deve criar outra conclusão de fase. |
| Mensagens | Chaves no domain da feature; mensagens pt_BR/en amigáveis para onboarding incompleto, fase/configuração inválida e ausência de contexto. |

## Integridade e concorrência

- A UNIQUE da conclusão é obrigatória; `exists` seguido de `save` sozinho não impede corrida.
- Serializar o registro/conclusão por workspace usando o bloqueio existente e uma leitura atual após espera. Não usar um refresh que recupere snapshot antigo no MySQL.
- Reutilizar transação da operação de origem; não registrar marcos de operações que serão revertidas, nem capturar violação de integridade e continuar uma transação invalidada.
- Definir a ordem dos bloqueios entre workspace e configurações antes de adicionar os dois vínculos de ambiente.
- Manter comportamento histórico das fases, salvo decisão explícita de recalcular/resetar progresso após exclusões.

## Testes necessários

- Registro automático da criação de workspace/aplicação e rollback integral quando a origem falha.
- Registro repetido e concorrente sem duplicar fase ou sobrescrever o primeiro usuário/data.
- Progresso inicial, fases concluídas fora de ordem, desempate de ordenação e progresso completo.
- Conclusão incompleta recusada, conclusão válida, repetição idempotente, versionamento e ausência de eventos de auditoria do onboarding.
- Workspace inexistente, inativo, em quarentena e fora do grupo autorizador.
- Aplicabilidade de ADMIN/MANAGER e política para catálogo incompleto/inativo.
- Ambiente DEFAULT + DEV versus CUSTOM e outros níveis; vínculo real com workspace/aplicação correta.
- Regressão de Workspace, Application, Environment e arquitetura.

## Decisões confirmadas e escopo implementado

1. O onboarding aplica-se somente a workspaces MANAGER. Workspaces ADMIN não iniciam esse fluxo.
2. O fluxo possui quatro etapas e aplica-se somente a MANAGER. Nesta implementação entram os registros automáticos de cadastro do workspace e da primeira aplicação. As duas etapas de ambiente ficam visíveis como pendentes até a entrega que implementar os vínculos/configurações correspondentes; criar um Environment isolado não as conclui.
3. O progresso é exposto por `GET /api/v1/workspaces/{workspaceIdentifier}/onboarding`. O `PATCH` sem body só conclui o onboarding depois das quatro etapas; por enquanto ele será recusado até as duas etapas de ambiente serem integradas. O `onboarding` do workspace permanece distinto do indicador `readyToComplete`.
4. A criação registra cada marco na mesma transação de Workspace/Application. A chave única por workspace e fase preserva o primeiro usuário/data e evita duplicidade. A identidade vem da sessão autenticada.
5. Foi criada a migration V46, com histórico de conclusão e as quatro fases ativas no catálogo. Somente as fases de cadastro são registradas automaticamente nesta entrega; os gatilhos de ambiente serão implementados junto às configurações.
6. Na revisão de 13/10/2026, o responsável pela fase foi padronizado como `requester`, com limite de 10 caracteres. A V60 renomeia a coluna sem truncar identidades antigas. A conclusão final fica sem `@Auditable`, conforme a decisão de não auditar onboarding. Achados e evidências atuais estão em [ONBOARDING-CODE-REVIEW.md](../onboarding/ONBOARDING-CODE-REVIEW.md).
7. A refatoração da revisão foi implementada: facade e adapter delegam a commands, o catálogo é consumido por serviço público, erros usam `ApiException` e `OnboardingMessageKeys`, e a V61 publica o schema de settings das fases. Testes cobrem ordenação determinística, mensagens localizadas, rollback, concorrência, idempotência e ausência de auditoria.
