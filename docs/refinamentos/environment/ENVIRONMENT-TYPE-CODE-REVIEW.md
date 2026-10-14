# Revisão de tipos de ambientes

Referência: [REVIEW-STANDARD.md](../../REVIEW-STANDARD.md). Fluxo separado de ambientes e compatibilidades; suas integrações são incluídas quando participam das invariantes de tipos.

## Contrato atual

O modelo vigente usa `code` como PK imutável, sem `id`, `identifier` ou `defaultScope` na entidade. Ambientes e compatibilidades referenciam tipos por código. Os registros históricos abaixo não definem o contrato atual.

| Ponto | Regra vigente |
| --- | --- |
| Códigos | DEFAULT, CUSTOM, SHARD e CELL; código obrigatório e imutável |
| Escopo | DEFAULT: rootAllowed=true/workspaceRequired=false; CUSTOM: true/true; SHARD e CELL: false/true |
| Create | Rejeita version não nula; Hibernate inicializa version=0. Validator encapsula existsByCode; não bloqueia o catálogo inteiro e a PK protege duplicidade concorrente |
| Update | Tipo ACTIVE, version obrigatória e atual, mesmo code da URL; snapshot após flush |
| allowsChildren=false | Exige ausência de compatibilidades ACTIVE como pai e de ambientes filhos em qualquer lifecycle; compatibilidades somente de entrada não bloqueiam |
| Inactivate | Permite tipo em uso; mantém referências e barra novos vínculos |
| Restore | Exige INACTIVE e retorna versão persistida |
| Delete | PURGE físico somente INACTIVE e sem ambientes/compatibilidades de qualquer lifecycle; retorna snapshot para auditoria |
| Consultas | find(code) aceita tipo existente de qualquer lifecycle; list(lifecycle) usa filtro central, ACTIVE por padrão, ordem displayOrder/code |
| Locks | Escritas de ambiente bloqueiam até dois tipos para leitura, por code; configuração/compatibilidade bloqueiam o catálogo em ordem; apenas @Transactional |
| Resposta | version, code, name, description, rootAllowed, allowsChildren, workspaceRequired, displayOrder e lifecycle; sem identifier UUID |

A verificação vigente está no [fechamento da revisão completa](ENVIRONMENT-FULL-CODE-REVIEW.md). As migrations V56/V57 migram a identidade e publicam os schemas; o teste EnvironmentTypeCodeMigrationIT verifica a atualização de dados existentes.

## Contrato e revisão iniciais — histórico

As seções seguintes registram decisões e verificações anteriores. Onde houver diferença, prevalecem a tabela acima, a seção atual do Finder e a revisão completa. A aceitação de version no create, a edição do code e as referências por ID numérico foram substituídas.

- Cadastro global: leitura autenticada OPEN; criação, edição e lifecycle OWNER. Não há grupo autorizador por tipo ou ResourceVisibility neste catálogo.
- Código editável, único entre todos os registros, incluindo inativos. O schema aceita DEFAULT, CUSTOM, SHARD ou CELL. A constraint única protege a corrida; a consulta prévia melhora a mensagem. ID e identifier não mudam na edição; os vínculos usam IDs internos.
- Um environment-type.json atende criação/edição. A biblioteca normaliza os textos conhecidos e valida o JSON original antes do binding, preservando tipos inválidos e campos desconhecidos para rejeição.
- Na criação, version enviada dentro dos tipos/limites do schema é ignorada na montagem da entidade. Hibernate inicializa @Version em 0. Na edição, version é obrigatória e comparada com a versão persistida, além da proteção de gravação do Hibernate.
- rootAllowed permite raízes; workspaceRequired define o escopo; allowsChildren permite filhos. A compatibilidade do par pai/filho continua necessária.
- Mudança de escopo é bloqueada quando existem ambientes daquele tipo. Desligar raiz/filhos é bloqueado quando há instâncias afetadas, inclusive históricas.
- Inativação exige ACTIVE, mesmo que o tipo tenha ambientes ou compatibilidades. Preserva os vínculos e impede novos vínculos. Edição de ambiente mantendo o tipo e restauração de ambiente existente permitem tipos inativos, inclusive o tipo pai. Criar ambiente ou trocar para outro tipo exige tipos ativos no novo par. Criar/reativar compatibilidade exige tipos ativos. Restauração de tipo exige INACTIVE. Exclusão física exige INACTIVE e ausência de qualquer ambiente ou compatibilidade.
- Listagem integral, inclusive inativos, ordenada por displayOrder/ID. O contrato limita o catálogo a quatro códigos.
- Limites atuais: code 50, name 100, description 250, identifier 36, lifecycle 50. Schema e colunas concordam. Não foi introduzida redução de limites sem definição de contrato.

## Create

Command chama validateForCreate(input), converte os detalhes, cria a entidade e persiste com saveAndFlush. O validator encapsula existsByCode e rejeita input nulo sem consultar o banco. Não há retorno null inalcançável nem bloqueio de todo o catálogo para checar duplicidade.

Testes cobrem input nulo, duplicidade ativa/inativa, criação com version=999 resultando em version=0 no banco/response e criação concorrente preservando uma linha com resposta de erro sem SQL/constraint. Na execução inicial do teste concorrente, o log confirmou a violação real da constraint única e sua tradução HTTP. O teste inicia as chamadas simultaneamente; em outras execuções, a rejeição também pode ocorrer pela consulta prévia.

## Update

Command carrega o tipo com lock exclusivo, chama validateForUpdate(type, input), monta Details, aplica code/detalhes na entidade e retorna o output de saveAndFlush. Entidade exige ACTIVE ao atualizar; validator também verifica esse estado antes das demais regras.

Validator rejeita input nulo/version ausente, compara a versão enviada, verifica duplicidade de código excluindo o próprio ID e consulta vínculos somente se houver mudança relevante. Não carrega vínculos completos nem consulta todos os ambientes. Os erros contextuais de escopo, raiz e filhos continuam agregados.

As verificações de vínculos usam consultas nativas de leitura atual, com LIMIT 1 FOR SHARE. Isso evita depender do snapshot de consultas comuns sob REPEATABLE_READ. O tipo já está bloqueado exclusivamente pelo command; operações que usam suas regras precisam adquirir lock compartilhado antes de gravar ambientes.

Depois de adquirir um lock, entidades já gerenciadas são detached e recarregadas por nova consulta com lock, como em ApplicationFinder. Não se usa refresh para supor uma leitura atual: a tentativa anterior com refresh ainda reproduziu falhas nos cenários concorrentes sob isolamento padrão.

## Locks e ordem de aquisição

Operações mantêm somente @Transactional, sem isolamento explícito e sem mudar a configuração global de datasource.

Gravações de ambientes adquirem locks compartilhados apenas no tipo escolhido e no tipo pai, em ordem crescente de ID. Requisições podem usar simultaneamente os mesmos tipos; edição do tipo e mudanças de compatibilidade usam locks exclusivos e aguardam os leitores.

Na criação de filhos, primeiro se resolve o ID do tipo pai, depois se bloqueiam os tipos envolvidos e finalmente o ambiente pai. O pai é recarregado por consulta com lock. Se seu tipo mudou entre a resolução e o lock, a criação é rejeitada, evitando validar com um conjunto de locks diferente da relação atual. Essa ordem evita adquirir o pai antes de aguardar o tipo, o que poderia conflitar com as consultas de vínculos do update.

O lock exclusivo do ambiente pai durante criação de filhos foi preservado. Alterações no grafo de compatibilidade também preservam a serialização do grafo para impedir ciclos. Há contenção deliberada em operações relacionadas; não há garantia numérica de throughput nesta revisão.

## Análise por classe e método

| Local | Avaliação atual |
| --- | --- |
| EnvironmentTypeController: create/update | Requests tipados, 201/200 e conversão direta para input; sem regra de negócio |
| Controller: list/find/lifecycle | Representações públicas; 204 para inativação/exclusão; restauração retorna representação |
| Requests / Response / Output | Contratos explícitos e conversões puras; version e permissões expostas sem entidade JPA |
| EnvironmentTypeFacade | Policies OPEN/OWNER e delegação; nomes de parâmetros legíveis |
| Command.create | Uma chamada de validação; mapper e entidade; constraint fecha a corrida |
| Command.update | Uma chamada de validação com entidade/input; code editável; snapshot após flush |
| Command.inactivate/restore/delete | Inativação altera somente o lifecycle sem bloquear por uso; restore exige INACTIVE; delete continua impedido por referências, usa PURGE e retorna o output preservado antes da remoção para auditoria |
| Query.activeForEnvironment / forEnvironmentUpdate / existingForEnvironment | Locks compartilhados apenas nos tipos envolvidos, recarga por consulta bloqueada e distinção entre vínculo novo (tipos ativos) e manutenção (tipo inativo permitido) |
| Query.findForUpdate | Lock exclusivo e detach/recarga para obter estado atual após a espera |
| Query.find/findOutput/list | Leitura read-only, not found e ordenação estável |
| Validator.create/update | Regras e consultas no módulo dono; estrutura/enum no schema; version do update comparada explicitamente |
| Validator.lifecycle | Validação das pré-condições de uso/estado recebidas pelo command |
| Mapper / Details | Conversão pura dos atributos editáveis, sem repository ou parsing HTTP |
| EnvironmentType | Identidade gerada pelo servidor, ACTIVE inicial, code editável no update, detalhes comuns e guardas de lifecycle |
| TypeRepository | exists específico, unicidade no banco e consultas com locks por identidade |
| EnvironmentRepository: validação de tipo | Optional escalar, LIMIT 1 e leitura atual sob lock; sem materializar coleções |
| Normalizer | Trim/uppercase de textos conhecidos, Locale.ROOT e preservação de estrutura inválida |
| Schema / migrations | Contratos estruturais e persistência alinhados; nenhuma migration aplicada alterada nesta atividade |

## Verificação e limites

Execuções anteriores:

- Create refatorado: 90 testes unitários/arquiteturais e 11 integrações passaram.
- Contrato de version: 90 unitários/arquiteturais e 4 integrações passaram; create version=999 e update version=0 exercitados.
- Somente @Transactional com as consultas anteriores reproduziu duas falhas de concorrência (raiz/filhos). A primeira tentativa de leitura atual ainda falhou porque a recarga de entidade por refresh podia reutilizar o snapshot. A implementação atual usa detach e consulta com lock.
- Suíte completa anterior: 89 unitários/arquiteturais passaram; 183 integrações com 5 falhas em PublisherApiIT/WorkspaceApiIT. Publisher tem divergências de schema/expectativas e um teste de paginação tentando repetir KAFKA; Workspace espera aceitar version na criação contra seu contrato atual. Esses módulos não foram corrigidos nesta atividade.

Verificação final: mvn --offline --batch-mode --no-transfer-progress -Dit.test=EnvironmentApiIT,SharedApiIT verify. BUILD SUCCESS: 90 testes unitários/arquiteturais, 14 integrações de Environment e 51 de Shared, sem falhas, erros ou skips (155 no total). Passaram edição de code preservando ID/identifier/compatibilidades, version inicial e conflito de update, duplicidade, schema, policy, paralelismo com locks compartilhados, raiz/filhos concorrentes, ciclos e regressão Shared. git diff --check passou. Log: /private/tmp/environment-type-update-review.log. A suíte completa não foi repetida; as cinco falhas externas anteriormente registradas permanecem sem correção nesta atividade.

Não foi executada pipeline remota nem teste de carga. As pendências de exclusão e captura de snapshot desta etapa inicial foram resolvidas nas etapas de PURGE e fechamento do command, registradas abaixo.

## Inativação orientada a novos vínculos

Decisão do usuário: o estado do tipo controla disponibilidade para novos vínculos, sem invalidar ambientes/compatibilidades existentes. Inativar mantém as linhas e as referências, inclusive compatibilidades ativas. Os endpoints de consulta continuam expondo ambientes existentes. Update sem troca de tipo, inclusive enviando explicitamente o código atual, e restore de ambiente permitem tipo inativo; a topologia e a acessibilidade dos ambientes ancestrais continuam verificadas.

Criação e troca de tipo exigem tipos ativos, incluindo o tipo pai no novo vínculo. Criação/reativação de compatibilidade continua exigindo os dois tipos ativos; exclusão do tipo permanece bloqueada por qualquer referência. Nenhuma alteração em cascata de lifecycle foi introduzida.

Inativação usa o mesmo lock exclusivo da edição do tipo. Criações usam locks compartilhados e recarregam o estado após esperar. Uma criação que confirma primeiro pode permanecer vinculada; se a inativação confirmar primeiro, a criação é rejeitada. Depois da inativação, novas criações são rejeitadas. Manutenção do vínculo é resolvida pelo ID interno, sem depender de código antigo ou imutável.

Testes adicionados: inativação com ambientes e compatibilidades, leitura/edição/restauração dos ambientes existentes, bloqueio de criação/troca de tipo/novas compatibilidades, bloqueio de exclusão, retomada de criação depois de restaurar o tipo e concorrência entre criação e inativação. Verificação final: mvn --offline --batch-mode --no-transfer-progress -Dit.test=EnvironmentApiIT verify passou com 90 testes unitários/arquiteturais e 16 integrações de Environment, sem falhas/erros/skips. Os 51 testes de Shared passaram na execução anterior com o mesmo código de produção; não foram repetidos após a correção exclusiva do cabeçalho de correlação no novo teste de exclusão. A primeira execução de Environment falhou apenas por 401 nesse teste sem cabeçalho, já corrigido. Logs: /private/tmp/environment-type-inactivation-review.log e /private/tmp/environment-type-inactivation-final.log. git diff --check passou.

## Auditoria da exclusão física

EnvironmentTypeCommandService.delete passa de AuditAction.DELETE/ENVIRONMENT_TYPE_DELETED para AuditAction.PURGE/ENVIRONMENT_TYPE_PURGED. A remoção física e as pré-condições (INACTIVE, nenhuma referência) permanecem. O endpoint continua respondendo 204; o command retorna EnvironmentTypeOutput preservado antes da remoção; a biblioteca captura o retorno para PURGE.

O command busca o registro pelo identifier sob lock, valida, monta EnvironmentTypeOutput, remove da base com flush e retorna o output. O aspect publica esse snapshot apenas se a operação terminar com sucesso, na mesma transação da remoção. O provider de captura anterior foi removido. O evento terminal é PURGED; isso não cria um estado de lifecycle adicional.

EnvironmentTypePurgeIT habilita auditoria e verifica remoção física com snapshot INACTIVE/version=7/detalhes persistidos, evento e identifier corretos, ausência de evento e de remoção para estado ativo/referências/inexistência, e rollback da remoção se o appender falhar. A falha é simulada com spy do appender; persistência e rollback usam MySQL real. A primeira tentativa de simulação com trigger não executou por falta de privilégios no banco de teste e foi substituída, sem alterar privilégios ou configuração do banco.

Validação: os 16 testes de EnvironmentApiIT passaram com o código de produção final; depois da correção exclusiva do teste de falha do appender, 90 testes unitários/arquiteturais e 3 integrações de EnvironmentTypePurgeIT passaram (BUILD SUCCESS). Logs: /private/tmp/environment-type-purge-review.log e /private/tmp/environment-type-purge-final.log. git diff --check passou. Suíte completa não repetida; falhas externas anteriormente registradas permanecem fora desta alteração.

## Fechamento do command: restore e delete

Restore faz saveAndFlush antes de montar o output, retornando a versão efetivamente persistida e incluindo essa versão no snapshot de auditoria. Teste restaura version=7 para 8, verifica resposta/banco/evento e usa imediatamente version=8 no update, que grava 9.

Delete chama validateForDelete(type). O validator concentra a exigência de INACTIVE e a ausência de referências; consultas escalares LIMIT 1 FOR SHARE verificam ambientes e compatibilidades com leitura atual. Command não depende mais dos repositories de ambientes/compatibilidades nem calcula booleans de validação. Constraints FK permanecem protegendo a integridade. Testes cobrem exclusão rejeitada com ambiente ou compatibilidade e ausência de evento para rejeições.

Validação: mvn --offline --batch-mode --no-transfer-progress -Dit.test=EnvironmentApiIT,EnvironmentTypePurgeIT verify. BUILD SUCCESS: 90 testes unitários/arquiteturais e 21 integrações (16 Environment, 5 auditoria/PURGE), sem falhas, erros ou skips. Log: /private/tmp/environment-type-command-final.log.

Regra complementar definida em 2026-10-12: configurar `allowsChildren=false` também é rejeitado quando houver compatibilidades ACTIVE nas quais o tipo é pai. A consulta é escalar, corrente e com bloqueio compartilhado; vínculos inativos e referências ao tipo somente como filho não impedem a flag falsa. A proteção existente para ambientes filhos permanece. Ver [coerência de tipos e compatibilidades](ENVIRONMENT-TYPE-COMPATIBILITY-CODE-REVIEW.md).

## Avaliação inicial do EnvironmentTypeQueryService (antes da extração do Finder)

| Método | Resultado |
| --- | --- |
| activeForEnvironment | Resolve ID pelo código e adquire locks compartilhados ordenados; exige atividade do tipo e do tipo pai para novos vínculos. A comparação do código após lock impede usar resolução anterior a uma renomeação |
| existingForEnvironment | Resolve pelo ID estável e permite lifecycle inativo na manutenção/restauração do vínculo; não libera regra de topologia ou acessibilidade dos ambientes |
| forEnvironmentUpdate | Distingue manter tipo (inativo permitido) de trocar tipo (ativos exigidos); seleciona todos os IDs antes de adquirir locks, evitando adquirir conjuntos em ordem invertida |
| lockForEnvironment | No máximo dois IDs, deduplicados/ordenados; detach e nova consulta bloqueada recuperam estado atual. A segunda consulta tem propósito de integridade e não deve ser removida sem preservar/testar a recarga |
| find | Retorna entidade do próprio módulo; usado somente pelo findOutput. Não há exposição da entidade no HTTP, mas o método público é desnecessário para esse consumidor |
| findForUpdate | Lock exclusivo e recarga atual, usado pelos comandos. Seus consumidores atuais têm transação aberta; quando chamado sozinho, a transação encerra e libera o lock antes do uso da entidade pelo chamador |
| findOutput | Read-only, representação pública e not found; delega à busca de entidade sem consulta extra |
| list | Read-only, ordem estável displayOrder/ID, quatro códigos permitidos pelo contrato e nenhum fetch de coleção/N+1 |

Ajustes identificados na avaliação inicial e implementados na etapa seguinte:

1. Carregamentos de entidade e locks movidos para EnvironmentTypeFinder; QueryService mantém find/list retornando outputs, seguindo a separação usada em Application/Publisher.
2. QueryService.find agora retorna EnvironmentTypeOutput; findOutput removido e facade atualizada. A busca de entidade fica em Finder.find.
3. Finder usa apenas @Transactional, conforme solicitado, sem propagação ou isolamento explícitos. QueryService abre transação read-only para consultas; comandos e PURGE fornecem a transação de escrita.

Não identificado novo bug bloqueador no QueryService nos fluxos atuais. Na implementação anterior de PURGE, provider e command repetiam findForUpdate; a captura pelo retorno do command elimina essa repetição. Não se recomenda eliminar a recarga baseada em estado gerenciado sem teste de leitura desatualizada, pois esse problema já foi reproduzido. A avaliação inicial foi por leitura; a implementação e sua validação estão registradas abaixo.

## Separação QueryService / Finder

EnvironmentTypeQueryService expõe apenas find(identifier) e list(), ambos read-only e retornando outputs. EnvironmentTypeFinder resolve entidades e integrações de escrita: find, findForUpdate, activeForEnvironment, existingForEnvironment e forEnvironmentUpdate. Preserva ordem dos locks, recarga atual e distinção de vínculo novo versus existente.

Comandos de tipo/ambiente consomem Finder; a facade de consulta continua usando QueryService. Finder usa apenas @Transactional (propagação padrão REQUIRED): participa da transação do chamador ou abre uma quando necessário. Nos fluxos de escrita atuais, os comandos mantêm a transação durante busca, validação e alteração. O nível de isolamento continua definido pelo banco/conexão; não foi reintroduzido READ_COMMITTED nas anotações. O teste específico de rejeição de chamadas sem transação foi removido; os testes de paralelismo continuam mantendo a transação aberta.

Verificação concluída com BUILD SUCCESS: 90 testes unitários/de arquitetura e 73 de integração (17 EnvironmentApiIT, 5 EnvironmentTypePurgeIT e 51 SharedApiIT), sem falhas ou erros. Log: /private/tmp/environment-type-finder-review.log. git diff --check também passou.

Após a remoção de MANDATORY, compilação de produção e testes passou com `mvn --offline --batch-mode --no-transfer-progress -DskipTests test-compile`, assim como `git diff --check`. A suíte de integração acima foi executada antes dessa remoção.

## Nomes da facade e controller de compatibilidade

EnvironmentTypeCompatibilityFacade e EnvironmentTypeCompatibilityController expõem create, list, find, inactivate e delete. create e inactivate continuam delegando às operações allow/disallow do command, preservando a regra existente. Os parâmetros abreviados c/q/f/l/i/r foram substituídos por command/query/facade/lifecycle/identifier/request.

Adicionado GET /api/v1/environment-types/compatibilities/{identifier}, com autorização READ/OPEN e consulta read-only. Retorna a compatibilidade por identifier, inclusive inativa, ou 404 quando ausente. Teste de integração cobre a consulta e o identifier inexistente.

Verificação dessa etapa: BUILD SUCCESS, 90 testes unitários/de arquitetura e 16 EnvironmentApiIT, sem falhas ou erros. Log: /private/tmp/environment-compatibility-naming-review.log. git diff --check passou.

## Padronização transversal do filtro de lifecycle

As listagens de tipos e compatibilidades agora recebem lifecycle e usam LifecycleFilter.resolve, compartilhado com as demais listagens gerais. O padrão é ACTIVE, com suporte a INACTIVE/QUARANTINED e erro 400 para filtro inválido. A ordenação dos tipos permanece displayOrder/id e a filtragem ocorre no banco. Ver inventário e validação em [LIFECYCLE-LISTING-REVIEW.md](../lifecycle/LIFECYCLE-LISTING-REVIEW.md).

## Fechamento das compatibilidades

A revisão de compatibilidades também ajustou o TypeFinder para descartar entidades de snapshot antes da consulta com lock, incluindo instâncias com versão antiga já gerenciadas. Os dois commands PURGE retornam o objeto removido para captura pelo aspect. Ver [revisão das compatibilidades](ENVIRONMENT-TYPE-COMPATIBILITY-CODE-REVIEW.md).

## Revisão de nomes, desempenho e segurança do Finder — 2026-10-12

Esta seção descreve a implementação atual com `code` como chave primária. As avaliações anteriores de IDs numéricos e renomeação de código acima são registros históricos; o código do tipo agora é imutável.

| Método atual | Responsabilidade |
| --- | --- |
| `findForEnvironmentCreation` | Busca e bloqueia o tipo solicitado e o tipo do pai para leitura; exige ambos ativos e confere o escopo global/workspace. Substitui `activeForEnvironment`. |
| `findForEnvironmentUpdate` | Distingue manutenção do tipo de troca de tipo por `changingType`; somente a troca exige tipos ativos. Substitui `forEnvironmentUpdate`. |
| `findForEnvironmentRestoration` | Busca os tipos para manutenção de vínculo existente, permitindo inativos. Substitui `existingForEnvironment`. |
| `find` | Consulta simples pelo código, com transação read-only e erro de domínio para código ausente. |
| `findForUpdate` | Busca um tipo com lock exclusivo e estado recarregado; usado em inativação, restauração e purge do tipo. |
| `findForConfigurationUpdate` | Bloqueia todos os tipos em ordem e seleciona o solicitado, sem repetir sua consulta com lock. Usado na edição de configuração do tipo. |
| `findAllForUpdate` | Retorna os tipos atuais sob locks exclusivos ordenados; mantém a coordenação com alterações de compatibilidade e validação de ciclos. |

O helper `findTypeAndParentWithReadLock` substitui `lockForEnvironment`. A sequência fica explícita: validar os códigos, deduplicar e ordenar os códigos envolvidos, buscar cada tipo com bloqueio compartilhado, conferir atividade quando exigida e selecionar o tipo solicitado. O loop interrompe a busca ao encontrar um tipo inválido. Não há acumulador de retorno nulo.

### Ajustes de desempenho

- `detachCachedType` usa `EntityManager.getReference` e detach para retirar a instância gerenciada sem executar um SELECT preliminar. A consulta com lock continua responsável pela existência e pelo estado atual. O comportamento foi conferido no fonte local do Hibernate 7.4.5.Final, inclusive o tratamento de proxy não inicializado no detach; nenhum getter é chamado na referência antes de desanexá-la.
- A busca compartilhada consulta somente o tipo solicitado e, quando diferente, o tipo do pai: no máximo dois tipos. Não carrega todo o catálogo para operações de ambiente.
- Antes dos locks de todos os tipos, `findAllCodes` lê somente os códigos, substituindo o carregamento completo das entidades apenas para desanexá-las. A preparação e a busca bloqueada continuam sendo duas consultas; a otimização reduz os dados carregados na preparação.
- O update de configuração reutiliza a entidade retornada pelo bloqueio de todos os tipos, eliminando a nova consulta exclusiva do tipo selecionado.

Não foram removidos os locks globais de configuração/compatibilidade: eles têm propósito de integridade e ordem do grafo. Essa revisão não mediu latência, throughput ou consultas em execução; os ganhos descritos são de consultas e carregamentos eliminados no código.

### Segurança e integração

Código nulo ou em branco é rejeitado antes de consultar ou criar a referência JPA. Tipos ausentes e tipos inelegíveis retornam `TYPE_NOT_FOUND`, preservando o contrato de erro. Novos vínculos exigem tipos ativos; restauração e manutenção mantêm a regra de tipos inativos permitidos. A validação de compatibilidade, hierarquia e uso continua nos validators.

Os tipos envolvidos continuam sendo bloqueados em ordem de código e antes das linhas dos ambientes. A instância gerenciada é desanexada antes da consulta bloqueada para evitar validar um estado anterior à espera pelo lock. Mantidos `@Transactional` com propagação/isolation padrão e os commands como responsáveis pela transação que abrange busca, validação, gravação e auditoria. O Finder não substitui a autorização das facades.

Chamadas do `EnvironmentCommandOperations` e as duas referências existentes no `EnvironmentApiIT` foram atualizadas para os nomes novos. Não foram adicionados testes nem alterados seus cenários.

Verificação: `mvn --offline --batch-mode --no-transfer-progress -DskipTests test-compile` passou, compilando 393 arquivos principais e 62 arquivos de teste; `git diff --check` passou. Não foram executados testes nesta etapa. A recarga após lock, o erro de tipo inexistente após getReference/detach e as regressões de concorrência precisam de validação em integração com o banco.
