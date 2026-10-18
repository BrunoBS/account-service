> Documento histórico. Consulte [Service Context](../service-context/SERVICE-CONTEXT.md) para nomes e contratos atuais.

# Platform — revisão geral e fechamento

Data: 14/10/2026. Critérios: [padrão de revisão](../../REVIEW-STANDARD.md).

Após o merge, o diagnóstico local identificou que HEAD já continha reformatação
de migrations aplicadas. A referência histórica usada nesta rodada foi corrigida
conforme a [recuperação da verificação local](LOCAL-VERIFY-RECOVERY.md).

## Escopo e comportamento vigente

A revisão percorreu controller, fachada, use cases, validação, entidades,
repositórios, contratos JSON Schema, auditoria e integração com Shared. A
[estrutura atual](PLATFORM-STRUCTURE-REFACTOR.md) mantém quatro responsabilidades:

| Fluxo | Operações e regra |
| --- | --- |
| Microservice | Criar, consultar, editar, ativar e inativar; sem exclusão e sem nova transição para quarentena. |
| Feature | Criar, consultar, editar, ativar e inativar; sem exclusão e sem nova transição para quarentena. A consulta admite `contextIdentifier`. |
| Context | Criar, consultar, editar, ativar, inativar e purge físico somente sem nenhum vínculo. A consulta admite `featureIdentifier`. |
| FeatureContext | Associar e remover fisicamente um par pelas rotas de contexts. Não há replace nem estado independente do vínculo. |

Os quatro controllers somam 21 combinações método/caminho. Cada controller
depende da sua própria fachada, com autorização OWNER. Remover vínculo e fazer
purge retornam HTTP 204; os commands preservam o output necessário à auditoria.

Inativar uma feature preserva seus vínculos e a retira da listagem padrão de
features do contexto. Consultas administrativas com lifecycle INACTIVE permitem
localizar referências para limpeza. Novos vínculos exigem contexto, feature e
microservice ativos. Nenhum vínculo, mesmo de feature inativa, pode permanecer
quando um contexto é removido fisicamente.

Em Shared, feature inativa, não compartilhável ou com microservice inativo
impede a montagem dos quatro detalhes: contrato próprio, contrato disponível,
participação do proprietário e participação do participante. Após verificar
existência e autorização, a consulta lança `SharedMessageKeys.FEATURE_UNAVAILABLE`:
HTTP 409, código `SHARED-0013`, mensagem e solução localizadas. A resposta não
inclui histórico, mappings ou sourceEnvironments. Reativação restaura a consulta
dos registros preservados. Ações de limpeza continuam disponíveis.

## Achados resolvidos

| Local | Severidade | Evidência e impacto | Correção | Verificação |
| --- | --- | --- | --- | --- |
| ContextController → ContextFacade → ContextQueryService → ContextRepository | Média | A busca inversa tinha métodos distintos para coleção geral e por feature; UUID com espaços não era normalizado como na busca de features. | Um único `findAll(featureIdentifier, lifecycle, pageable)`, com filtro opcional, normalização no use case e existência validada quando preenchido. Página e contagem usam o mesmo predicado SQL. | FeatureContextApiIT cobre filtro ausente, branco, preenchido, com espaços e referência inexistente; FeatureContextQueryIT valida DTO fora da transação. |
| Migrations publicadas, V1–V36, em 35 arquivos alterados | Alta | A reformatação de `32ac504f` alterou checksums; a comparação com HEAD na revisão inicial não detectou que essa referência já havia sido reformatada. O diagnóstico posterior confirmou 35 divergências no banco local. | Recuperados os arquivos anteriores a `32ac504f`, sem alteração dos tokens SQL. As evoluções funcionais permanecem nas migrations V42–V65. | Os 44 checksums aplicados correspondem aos arquivos recuperados; PublishedMigrationChecksumTest protege esse histórico. Detalhes no relatório de recuperação local. |
| platform-messaging / ApiExceptionHandler | Alta | Método HTTP removido era tratado pelo handler genérico de erro 500. A correção existia somente no artefato instalado localmente. | Handler de HTTP 405 preserva Allow e usa mensagem localizada. Correção comitada na main da biblioteca: `917aa7b0`. | 73 testes da biblioteca aprovados, incluindo dois MockMvc para 405/localização; PlatformEndpointContractIT cobre os endpoints removidos. |
| WorkspaceFacade.findByIdentifier, identificado na regressão geral do commit | Alta | O detalhe exigia OPEN, divergindo do contrato DEV documentado e de WorkspaceAuthorizationIT. | Restaurada a política DEV para leitura individual, mantendo criação/listagem OPEN e mutações ADM. | WorkspaceAuthorizationIT verifica o header de policy e os cenários de acesso/visibilidade. |

## Integridade, concorrência e desempenho

- Código imutável, identificador único, constraints de unicidade, foreign keys e
  `@Version` protegem a persistência. Validar existência antes de gravar complementa
  essas proteções e não as substitui.
- Commands de Platform usam `@Transactional`; queries usam readOnly. Os finders
  carregam o estado atual após o lock e removem entidades antigas do contexto de
  persistência quando necessário. Os testes cobrem snapshots anteriores, versões
  antigas e espera por inativação concorrente.
- Vínculos seguem ordem de locks definida: contexto WRITE, microservice READ na
  associação, feature WRITE e operação do par. Inserção atômica após validar os
  pais evita o deadlock observado com lock de intervalo de um par inexistente.
- Repetir uma operação sem alteração real não incrementa versões nem duplica
  auditoria. Falha obrigatória de auditoria reverte vínculo, versões ou purge.
- Consultas são paginadas no banco, com validação central de lifecycle, tamanho,
  offset e sort. A resposta de features carrega o microservice necessário sem
  N+1; página e contagem usam o mesmo filtro.
- Requests tipados passam pela validação do JSON original antes da desserialização.
  Normalizadores preservam propriedades desconhecidas e tipos para permitir sua
  rejeição pelo schema. Mappers não aplicam regras nem consultam repositórios.

## Limites e entrega

A biblioteca está comitada e instalada localmente como 1.0.0. A publicação do
artefato consumido por CI e outros ambientes ainda precisa acompanhar a entrega:
o commit por si só não atualiza o registry. Esta revisão não publicou artefatos
nem executou push ou deploy.

O filtro QUARANTINED continua disponível para dados históricos. Não há novas
transições para esse estado em Platform. Eventuais microservices/features já
nesse estado permanecem consultáveis administrativamente; não foi criada
migration de conversão de dados legados sem uma decisão específica. A revisão
não consultou uma base de produção para confirmar a existência desses registros.

## Validação

Antes do último ajuste de Context, a regressão de Platform/Shared aprovou 107
testes unitários/arquitetura e 138 testes de integração em 12 classes.

A regressão final do workspace executou as 57 classes de integração em processos
Maven com lotes de até 16 classes. OnboardingApiIT é executado em processo próprio
por interferência previamente reproduzida no mock de autorização em lote. O
consolidado por classe fica em `/private/tmp/workspace-precommit-results.json` e
os logs em `/private/tmp/workspace-precommit-batch-{1..5}.log`.

A primeira execução do lote 2 identificou a divergência OPEN/DEV de workspace.
Após restaurar DEV, as cinco classes de testes HTTP de workspace foram repetidas em
`/private/tmp/workspace-precommit-batch-workspace-policy-recheck.log`. Seus
resultados finais substituem os anteriores no consolidado, sem contar cenários
repetidos como novos testes. O log original da falha foi preservado.

| Verificação final | Resultado |
| --- | --- |
| Unitários e arquitetura do serviço | 107 aprovados |
| Integração do workspace | 340 aprovados em 57 classes |
| Falhas, erros e skips nos resultados finais | Zero |
| Biblioteca platform-messaging | 73 aprovados, build/install local e commit concluídos |
| Migrations já publicadas | 41 arquivos idênticos byte a byte a HEAD |
| git diff --check | Aprovado |

Os cenários incluem API, autorização, paginação, schema, persistência, upgrade,
auditoria/rollback, disponibilidade Shared, idempotência e concorrência. Não
restaram gaps funcionais bloqueadores identificados nesta revisão. A publicação
da biblioteca e a eventual conversão de dados legados permanecem explicitadas
acima como condições de entrega e decisão de dados, respectivamente.
