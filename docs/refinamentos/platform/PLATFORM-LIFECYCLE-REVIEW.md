# Platform — ativação, inativação e purge de contexto

Data: 13/10/2026. Complemento à [separação dos quatro fluxos](PLATFORM-STRUCTURE-REFACTOR.md).

## Regras implementadas

| Recurso | Ativar / inativar | Excluir |
| --- | --- | --- |
| Microservice global | Sim, mantendo suas referências | Não existe endpoint ou operação de comando de exclusão |
| Feature global | Sim, mantendo suas referências | Não existe endpoint ou operação de comando de exclusão |
| Context | Sim | Purge físico somente quando não existir nenhum vínculo com feature |
| Vínculo feature/context | Inclusão e remoção pelo fluxo próprio | DELETE remove somente o vínculo; não exclui feature ou contexto |

Os cadastros de microservice e feature têm seis rotas cada: criar, listar,
detalhar, atualizar, ativar e inativar. FeatureController também filtra por
`contextIdentifier` em `findAll`. Foram removidos os DELETE por identificador,
os métodos correspondentes de fachada/command e as transições de quarentena no
domínio. DELETE nessas URLs retorna 405, independentemente do estado do registro,
sem mutação e sem evento de exclusão. A validação de dependências usada somente
para excluir microservice e sua consulta ao repositório de features também foram
removidas.

O código e o nome de um microservice ou feature inativos continuam reservados.
Inativação não remove features, mensagens, contratos ou vínculos de contexto.
Ativação de feature continua exigindo microservice elegível. As validações de
criação, edição, versão e autorização OWNER permanecem aplicadas.

## Purge de contexto

A rota permanece `DELETE /api/v1/platform/contexts/{contextIdentifier}`, agora
atendida por `purge` em controller, fachada e command. Resposta HTTP 204.

A regra é ausência de qualquer vínculo, inclusive com feature inativa. Não há
pré-condição adicional exigindo contexto INACTIVE: contexto ACTIVE ou INACTIVE
sem vínculos pode ser removido. Contexto inexistente retorna 404; contexto com
vínculo retorna erro de negócio 400. Para liberar o purge, remover as associações
pelo fluxo `featurecontext`. Nenhuma feature é removida em cascata.

O comando:

1. Busca e bloqueia o contexto pelo identificador público.
2. Verifica a primeira relação existente por leitura atual com lock.
3. Preserva o `ContextOutput` com a identidade, atributos, lifecycle e versão atuais.
4. Remove o contexto fisicamente e executa flush.
5. Retorna o snapshot preservado para o aspect de auditoria.

A auditoria usa `AuditAction.PURGE` e evento `FEATURE_CONTEXT_PURGED`, preservando
o resourceType histórico `FEATURE_CONTEXT`. Não há `AuditBeforeSnapshotProvider`,
retorno void ou snapshot montado depois de remover os dados. Falha na auditoria
reverte a remoção e seus eventos. O corpo HTTP continua vazio, enquanto facade e
command retornam o snapshot para uso interno.

O lock do contexto é compartilhado com todas as operações de vínculo. Se a
associação confirmar primeiro, o purge a detecta mesmo depois de um snapshot
antigo. Se o purge confirmar primeiro, uma inclusão que estava esperando retorna
404, sem criar relação órfã. A foreign key existente é uma proteção adicional.
Após purge, código e nome do contexto ficam disponíveis para novo cadastro, que
recebe outro UUID.

## Associação e consultas

`FeatureContextController` e `FeatureContextFacade` possuem apenas `associate`
e `remove`, sem replace. As duas escritas usam somente
`/api/v1/platform/contexts/{contextIdentifier}/features/{featureIdentifier}`,
sem body. O PUT de substituição foi removido; as escritas inversas a partir de
features também foram removidas.

O `findAll` de cada controller recebe o filtro opcional de vínculo. A cadeia
FeatureController → FeatureFacade → FeatureQueryService → FeatureRepository usa
somente `findAll`, com contextIdentifier opcional. ContextFacade → ContextQueryService
→ ContextRepository também usam `findAll`, com featureIdentifier opcional. A consulta de features usa
`GET /api/v1/platform/features?contextIdentifier=<UUID>`; a consulta inversa usa
`GET /api/v1/platform/contexts?featureIdentifier=<UUID>`. Ambas preservam
a autorização OWNER, a paginação no banco e o filtro central de lifecycle.
O fluxo de vínculo não possui query service, normalizador ou DTO de substituição.

## Dados existentes e escopo

A alteração não exige migration: tabelas e constraints não mudaram. O contrato
de substituição foi removido; o schema histórico publicado na V65 permanece sem
consumidor HTTP. Migrations aplicadas permanecem imutáveis. Dados legados em quarentena
não são convertidos nem apagados automaticamente, e o catálogo global de lifecycle
continua atendendo outros módulos. Não há novas transições para quarentena nos
três cadastros de Platform.

O vínculo usa o lifecycle da feature global, e sua remoção é física, preservando
os cadastros globais. O detalhe da participação em Shared passou a verificar a
disponibilidade da feature e do microservice antes de montar a resposta. Quando
indisponíveis, as consultas individuais geram uma exceção de negócio pelo messaging,
conforme a decisão posterior registrada neste documento.

## Verificação

Rodada de lifecycle e simplificação em 13/10/2026, anterior ao ajuste da rota de
consulta, concluída às 21:22, sem falhas, erros ou skips:

| Verificação | Resultado |
| --- | --- |
| Unitários e arquitetura do serviço | 107 aprovados |
| Integração de Platform, Shared, lifecycle e queries | 137 aprovados em 12 classes |
| Biblioteca platform-messaging | 73 aprovados; build e install local concluídos |
| Inventário dos controllers nessa rodada | 23 combinações método/caminho sem colisões; FeatureContext com apenas duas escritas |
| git diff --check | Aprovado no serviço e na biblioteca |

A integração cobre purge ativo/inativo sem vínculos, bloqueio com feature inativa,
reutilização de código/nome depois do purge, snapshot e rollback de auditoria,
autorização, 405 dos DELETE globais/PUT removido, 404 das escritas inversas e
idempotência das associações. A consulta paginada de features realiza três
statements: existência do contexto, conteúdo com microservice e contagem.

A primeira rodada encontrou deadlocks ao consultar com lock um par inexistente
antes de inseri-lo. A inclusão foi corrigida para inserção atômica após bloqueio
dos pais, mantendo apenas `@Transactional`. Os seis cenários repetidos de vínculos
independentes passaram, assim como retry após snapshot antigo e purge concorrente.

A remoção dos endpoints também revelou que o handler comum tratava método HTTP
não permitido como 500. A `platform-messaging` passou a responder 405, mantendo
o header Allow e a mensagem localizada `PlatformMessageKeys.METHOD_NOT_ALLOWED`.
Os dois novos testes MockMvc da biblioteca e os testes HTTP do serviço validam
essa correção. A biblioteca foi instalada localmente; estas alterações não foram
commitadas ou publicadas nesta rodada.

Comando final:

```text
mvn --offline --batch-mode --no-transfer-progress -Dit.test=MicroserviceApiIT,FeatureApiIT,ContextApiIT,FeatureContextApiIT,PlatformEndpointContractIT,PlatformApiIT,MicroserviceConcurrencyIT,FeatureConcurrencyIT,ContextConcurrencyIT,SharedApiIT,LifecycleListingIT,FeatureContextQueryIT verify
```

Evidências locais: `/private/tmp/platform-lifecycle-removal-verify.log`,
`/private/tmp/platform-messaging-method-not-allowed-install.log` e
`/private/tmp/platform-controller-final-inventory.json`.

### Ajuste da rota de consulta de features — estado intermediário

`GET /features?contextIdentifier=<UUID>` seleciona `findAllByContextIdentifier`.
Sem o parâmetro, `GET /features` seleciona `findAll`, preservando `contextCode`.
Os mappings usam condições opostas de presença do parâmetro, sem ambiguidade.
As rotas de consulta aninhadas anteriores foram removidas; não há segmentos
`contexts` nem repetição de `features` na nova consulta. Como a antiga coleção de
vínculos não tem mais GET, seu PUT removido passa a retornar 404.

O inventário atualizado tem 23 handlers e 22 combinações método/caminho. Os dois
GET da coleção de features têm condições de parâmetro mutuamente exclusivas.

Verificação específica: 107 unitários/arquitetura e 33 testes HTTP em quatro
classes aprovados, sem falhas, erros ou skips. Os cenários verificam o isolamento
do filtro (incluindo feature não vinculada), contexto vazio/inexistente, lifecycle,
paginação, sort, autorização, consulta geral e remoção das URLs antigas.
`git diff --check` aprovado.

```text
mvn --offline --batch-mode --no-transfer-progress -Dit.test=FeatureApiIT,FeatureContextApiIT,PlatformApiIT,PlatformEndpointContractIT verify
```

Log: `/private/tmp/platform-feature-query-route-verify.log`.

### Padronização posterior do findAll de ContextController

Cada controller concentra a consulta da coleção em `findAll`:

| Coleção | Filtro opcional de vínculo |
| --- | --- |
| GET /api/v1/platform/features | contextIdentifier |
| GET /api/v1/platform/contexts | featureIdentifier |

O identificador ausente ou em branco usa a consulta geral, seguindo o padrão atual
de FeatureController. Quando informado, usa a consulta específica da fachada,
com autorização OWNER, paginação e lifecycle do recurso retornado. Identificador
não encontrado retorna 404; recurso existente sem vínculo retorna página vazia.
A rota GET /features/{featureIdentifier}/contexts foi removida.

O inventário atual tem 21 handlers com combinações método/caminho únicas. A base de
ContextController agora é /api/v1/platform/contexts; create, update, lifecycle e
purge mantêm as URLs públicas anteriores.

Verificação específica: 107 testes unitários/arquitetura e 47 testes HTTP em cinco
classes aprovados, sem falhas, erros ou skips. Inclui listagem geral e filtrada,
identificador vazio/em branco, contexto sem vínculo, referência inexistente,
lifecycle, paginação, sort, autorização, remoção da rota antiga e os demais
endpoints de contexts. Inventário e `git diff --check` aprovados.

```text
mvn --offline --batch-mode --no-transfer-progress -Dit.test=ContextApiIT,FeatureApiIT,FeatureContextApiIT,PlatformApiIT,PlatformEndpointContractIT verify
```

Log: `/private/tmp/platform-context-query-route-verify.log`.

### Remoção de vínculo com HTTP 204

`FeatureContextController.remove` retorna `ResponseEntity<Void>` com 204 e corpo
vazio depois de concluir a operação. O retorno interno da fachada, command e
operação auditada foi preservado. POST de associação continua retornando 200 com
FeatureResponse. DELETE remove apenas o vínculo e mantém os cadastros globais.
Retries de remoção de vínculo ausente, com os dois recursos existentes, retornam
204 sem incrementar versões ou duplicar eventos. Referência inexistente continua
retornando 404 e falhas de autorização preservam seus erros.

Verificação específica: 107 testes unitários/arquitetura e 47 testes HTTP em cinco
classes aprovados, sem falhas, erros ou skips. Os testes confirmam 204 com corpo
vazio, exclusão do vínculo, preservação de feature/context, idempotência das
versões e eventos e auditoria com os snapshots internos. `git diff --check` aprovado.

Log: `/private/tmp/platform-feature-context-remove-verify.log`.

### Busca de features concentrada em findAll

O único filtro de vínculo em GET /api/v1/platform/features é `contextIdentifier`.
`contextCode` e os métodos `findAllByContextIdentifier` foram removidos das camadas
de Feature. Controller, fachada, query service e repositório usam `findAll`.
O controller somente encaminha parâmetros; normalização e seleção ficam no query
service e no banco, sem escolha entre dois caminhos de consulta.

Sem identificador ou com valor em branco, retorna a página geral. Identificador
preenchido é normalizado com trim e exige a existência do contexto, inclusive
inativo para consultas administrativas. Contexto inexistente retorna 404; contexto
existente sem relações retorna página vazia. Lifecycle, sort, conteúdo e contagem
seguem os filtros já centralizados, com microservice carregado na mesma consulta.
Nenhuma entidade, identidade persistida ou migration foi alterada.

Os contratos intermediários das seções anteriores permanecem como registro
histórico; o contrato atual de Feature não possui filtro por código nem método
separado de consulta por identificador de contexto.

Verificação específica: 107 testes unitários/arquitetura e 51 testes de integração
em sete classes aprovados, sem falhas, erros ou skips. Cobrem busca geral e por
contexto ativo/inativo, valores vazios e espaços, referência inexistente, ausência
de vínculos, autorização, lifecycle, sort e paginação. A busca paginada com filtro
continua usando três statements (existência, conteúdo com microservice e contagem);
a busca geral usa dois. `git diff --check` aprovado.

```text
mvn --offline --batch-mode --no-transfer-progress -Dit.test=ContextApiIT,FeatureApiIT,FeatureContextApiIT,PlatformApiIT,PlatformEndpointContractIT,LifecycleListingIT,FeatureContextQueryIT verify
```

Log: `/private/tmp/platform-feature-find-all-verify.log`.

### Disponibilidade dos ambientes de origem em Shared — estado intermediário

No GET de detalhe da participação pela visão do proprietário,
`SharedParticipationQueryService.findOwnerParticipationDetails` oferece
sourceEnvironments somente quando o contrato está ativo, a feature está ativa e
compartilhável, seu microservice está ativo e a aplicação/workspace participante
estão ativos. A disponibilidade da feature usa a mesma consulta central já aplicada
à descoberta e às novas operações de compartilhamento.

Se uma referência estiver indisponível, o detalhe continua consultável, preservando
contrato, status, histórico e mappings, e retorna sourceEnvironments como lista
vazia. A reativação restaura a oferta de ambientes quando as demais condições
continuam atendidas. Essa leitura não altera o estado persistido do contrato ou da
participação e não gera eventos de auditoria.

O item de ativação individual do vínculo com contexto foi encerrado por decisão de
produto: manter a relação e usar o estado global da feature, com remoção explícita
quando necessário. O filtro administrativo INACTIVE permanece disponível para limpeza.

Verificação específica: 107 testes unitários/arquitetura e 52 testes da API Shared
aprovados, sem falhas, erros ou skips. O novo cenário usa inativação/reativação
reais da feature e do microservice, mantém o contrato ativo e a participação aprovada,
preserva mappings e confirma sourceEnvironments vazio/restaurado. A regressão
inclui contratos/participantes inativos, histórico, autorização, ambientes e
concorrência existentes. `git diff --check` aprovado.

```text
mvn --offline --batch-mode --no-transfer-progress -Dit.test=SharedApiIT verify
```

Log: `/private/tmp/shared-feature-availability-verify.log`.

### Exceção de negócio ao consultar contrato/participação indisponível

A decisão final substitui a resposta de histórico com sourceEnvironments vazio
quando a feature estiver indisponível. O contrato e as participações permanecem
persistidos, mas os detalhes individuais retornam erro de negócio 409:

- contrato pela visão do proprietário;
- contrato disponível pela visão do participante, após verificar escopo e proprietário;
- participação pela visão do proprietário e do participante.

SharedContractAccessService.requireFeatureForDetails centraliza a regra e usa
SharedReferenceAdapter.isFeatureAvailable, considerando feature ativa e
compartilhável e microservice ativo. A exceção usa SharedMessageKeys.FEATURE_UNAVAILABLE,
chave shared.contract.feature-unavailable e código SHARED-0013. O messaging
resolve as mensagens e soluções em português/inglês no ApiErrorResponse padrão.

O erro substitui o DTO de detalhe inteiro: sourceEnvironments, mappings e histórico
não são devolvidos nessa consulta. Nenhum campo availability foi adicionado aos
records. Registros/escopos inexistentes continuam 404 e autorização é verificada
antes do acesso. As consultas administrativas de listagem e as operações de
inativação/exclusão continuam disponíveis para gestão e limpeza dos vínculos.

Reativar a feature/microservice restaura os detalhes se as demais referências
continuarem elegíveis. Contrato inativo ou participante inativo com feature
válida seguem as regras anteriores de histórico e de oferta de ambientes.

Verificação específica: 107 testes unitários/arquitetura e 52 testes de Shared
aprovados, sem falhas, erros ou skips. Cobrem erro 409 com código/mensagem do
messaging e ausência de histórico/mappings/sourceEnvironments no corpo de erro,
inativação da feature e do microservice, reativação, escopo incorreto (404),
autorização (403), ausência de eventos de auditoria nas leituras e limpeza por
inativação/exclusão do contrato com a feature inativa. `git diff --check` aprovado.

```text
mvn --offline --batch-mode --no-transfer-progress -Dit.test=SharedApiIT verify
```

Log: `/private/tmp/shared-feature-business-error-verify.log`.
