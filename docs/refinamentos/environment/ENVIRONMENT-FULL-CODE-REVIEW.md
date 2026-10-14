# Revisão completa de ambientes

Este arquivo registra etapas da revisão. O resultado vigente está em **Fechamento dos gaps da simulação**, ao final; as verificações anteriores de compilação e pendências de execução foram substituídas pela regressão final ali registrada.

## Organização dos comandos

`DefaultEnvironmentFacade` utiliza `DefaultEnvironmentCommandService`; `WorkspaceEnvironmentFacade` utiliza `WorkspaceEnvironmentCommandService`. Cada serviço expõe `create`, `update`, `inactivate`, `restore` e `delete`, com transação e evento de auditoria próprios do escopo. As permissões continuam nas facades: OWNER para alterações globais e ADM para alterações do workspace.

`EnvironmentCommandOperations` concentra normalização, validação, hierarquia, locks, persistência e montagem do output. Não possui eventos de auditoria: cada chamada publica somente o evento do comando de entrada. Utiliza apenas `@Transactional`, participando da mesma transação do comando, sem isolamento explícito ou `Propagation.MANDATORY`.

A resolução do workspace fica no comando de workspace; as buscas de escrita do Finder aplicam a visibilidade do ambiente e de seus ancestrais. Na criação, `WorkspaceReferencePort.resolveActiveForUpdate` bloqueia o workspace ativo até o fim da transação, coordenando o novo vínculo com mudanças de lifecycle. As consultas permanecem em `EnvironmentQueryService`; rotas e contratos HTTP foram preservados.

## Revisão por operação

| Operação | Regras e proteção |
| --- | --- |
| create | Normaliza, rejeita input nulo e versão informada, resolve tipo ativo e pai acessível, valida compatibilidade e nome no escopo. Constraint protege duplicidade concorrente. Hibernate inicializa a versão. Ordem automática não pode ultrapassar Integer.MAX_VALUE. |
| update | Exige versão do cliente, mantém pai imutável, verifica versão atual após lock. Mudança de tipo exige compatibilidade ativa e ausência de filhos. Vínculos existentes podem conservar tipo ou compatibilidade inativos. |
| inactivate | Exige estado ACTIVE. Preserva filhos e vínculos; descendentes deixam de estar acessíveis nas consultas ativas enquanto o ancestral estiver inativo. |
| restore | Exige estado INACTIVE e ancestrais ativos acessíveis. Compara a versão da leitura inicial com a versão após lock para detectar alteração concorrente do ambiente. Valida vínculo existente, permitindo tipo ou compatibilidade inativos. Não repete validação de criação. |
| delete | Exige INACTIVE e ausência de filhos de qualquer estado. Move para QUARANTINED e mantém registro e nome reservado. Evento DELETE, sem remoção física/PURGE. |

Todos os comandos retornam `EnvironmentOutput` após flush, incluindo inativação e exclusão. As facades mantêm HTTP 204 quando apropriado; o retorno do comando fornece o snapshot e a versão persistida à auditoria. Falha de auditoria reverte a alteração.

## Achados resolvidos

| Local | Severidade | Evidência e impacto | Ajuste e verificação |
| --- | --- | --- | --- |
| Finder e comandos | Alta | Entidade carregada antes da espera pelo lock podia representar versão ou lifecycle antigos. | Desanexar snapshot antes da consulta autoritativa; locks de ancestrais da raiz para o alvo. Testes com snapshot anterior e restauração simultânea. |
| Mudança de tipo e criação de filho | Alta | Validação sobre snapshot não protege alteração concorrente da hierarquia. | Tipos bloqueados em ordem antes dos ambientes, tipo do pai conferido novamente, filhos consultados com leitura corrente. Teste concorrente. |
| Visibilidade da hierarquia | Alta | Filho visível podia referenciar ancestral filtrado, expondo dados ou provocando erro de carregamento. | Consultas explícitas de ancestrais e projeções em lote; filtragem consistente em detalhe, listas, árvore e escrita. Teste de grupos e workspace invisível. |
| Auditoria de lifecycle | Alta | Comando sem representação não fornecia snapshot completo do estado persistido. | Retorno após flush nos dois escopos; testes de eventos, versões e rollback. |
| QueryService | Média | Leituras completas e consultas repetidas de ancestrais aumentavam o custo. | Roots filtradas no banco; árvore montada com consulta em lote; projeções para identificadores e hierarquia; CTE seletiva para referências Shared. Regressões de Shared e lifecycle. |
| Validator e domínio | Média | Pré-condições e transições dispersas; overflow na ordem automática. | Unicidade encapsulada no validator, lifecycle protegido no domínio, erro de input nulo e limite de ordem. Testes de versão, quarentena e overflow. |
| Commands | Baixa | Serviço único expunha dez métodos misturando escopos e eventos. | Serviços separados com cinco operações cada e regras comuns em EnvironmentCommandOperations. Mesma suíte de regressão após extração. |

## Persistência e limites

Nome da entidade limitado a 50 caracteres, em acordo com schema e migration; pai marcado como não atualizável. A migration V56 substitui a chave surrogate de `environment_types` por `code` e migra as FKs de ambientes e compatibilidades preservando os vínculos existentes. V57 publica schemas com os campos de código. `code` é imutável e identifica o tipo; `id`, `identifier` e `defaultScope` não existem nessa entidade. A constraint do banco protege as combinações de escopo. Ordenação admite empates, com código como desempate.

A rota sem workspace exige `environmentTypeCode=DEFAULT`; a rota com workspace exige o código apropriado, como CUSTOM, SHARD ou CELL. O campo é obrigatório e os schemas aceitam somente DEFAULT, CUSTOM, SHARD e CELL. As flags `rootAllowed` e `workspaceRequired` validam esses tipos integrados: DEFAULT usa `true/false`, CUSTOM usa `true/true`, SHARD e CELL usam `false/true`. `parentIdentifier`, quando informado, deve ter formato UUID. A requisição não envia um escopo default separado. As compatibilidades guardam `parentTypeCode` e `childTypeCode`; seu próprio identifier continua identificando o vínculo. O DTO interno de auditoria fornece `identifier=code` para a biblioteca atual, enquanto a resposta HTTP expõe apenas `code`.

Listagens usam o filtro central ACTIVE, INACTIVE e QUARANTINED, com ACTIVE como padrão. Visibilidade do workspace e dos ambientes continua obrigatória nos fluxos de usuário; integrações Shared preservam seu contrato explícito de autorização e referências históricas.

## Verificação

A regressão abaixo passou antes da alteração mais recente de identidade e FKs:

```shell
mvn --offline --batch-mode --no-transfer-progress -Dit.test=EnvironmentApiIT,EnvironmentAuditIT,EnvironmentTypePurgeIT,EnvironmentTypeCompatibilityAuditIT,SharedApiIT,LifecycleListingIT verify
```

A compilação principal e dos testes com `mvn --offline --batch-mode --no-transfer-progress -DskipTests test-compile` passou após a mudança para `code` como PK. Os testes não foram executados nessa etapa; a migration e os schemas precisam de validação em integração com o banco.

## Revisão transversal de controller, facade e use cases — 2026-10-12

Escopo: os quatro controllers, suas facades e todos os commands, queries, finders, normalizer, mappers e validators de `core.environment`. Entidades, repositórios, schemas, migrations e implementações das bibliotecas de autorização e mensagens foram consultados para acompanhar os contratos até a persistência. Foram examinadas 29 operações HTTP.

### Contratos e responsabilidades

| Fluxo | Controller e facade | Use cases e proteções |
| --- | --- | --- |
| Ambientes globais | `/api/v1/environment-defaults`; leitura OPEN, escrita OWNER; requests tipados | `DefaultEnvironmentCommandService` e `EnvironmentQueryService`; `workspaceId=null`, tipo compatível com escopo global, auditoria específica |
| Ambientes de workspace | `/api/v1/workspaces/{workspaceIdentifier}/environments`; leitura DEV, escrita ADM | `WorkspaceEnvironmentCommandService`; referência ativa e visível de workspace, isolamento do escopo, visibilidade de ambiente e ancestrais |
| Tipos | `/api/v1/environment-types`; leitura OPEN, escrita OWNER; identidade pelo code | `EnvironmentTypeCommandService` e `EnvironmentTypeQueryService`; código imutável, versão obrigatória no update, proteção de usos e flags, purge somente inativo e sem uso |
| Compatibilidades | `/api/v1/environment-types/compatibilities`; leitura OPEN, escrita OWNER; identidade do vínculo por UUID | `EnvironmentTypeCompatibilityCommandService` e query; tipos ativos na criação, proteção contra ciclos, reativação do vínculo existente e purge somente inativo e sem uso |

Controllers traduzem HTTP e DTOs; facades aplicam políticas e delegam; commands coordenam transações e auditoria; queries montam modelos de leitura. Requests continuam usando a validação estrutural da biblioteca antes da desserialização. Nenhuma entidade JPA é devolvida pela API.

Todas as quatro listagens gerais usam `LifecycleFilter`, aceitando ACTIVE, INACTIVE e QUARANTINED, com ACTIVE quando o parâmetro não é informado. Roots, children e tree mantêm seu contrato de hierarquia ativa. O detalhe de ambiente continua restrito ao estado ativo; tipos e compatibilidades podem ser consultados por identidade independentemente do lifecycle.

### Achados desta revisão

| Local | Severidade | Evidência e impacto | Ajuste aplicado |
| --- | --- | --- | --- |
| `EnvironmentFinder.lockActiveAncestors` | Alta | A consulta nativa de bloqueio conferia o lifecycle, mas não recebia o filtro Hibernate de visibilidade. Escritas podiam atravessar um ancestral oculto. | Consulta JPQL com `PESSIMISTIC_READ`, lifecycle no predicado e filtro de visibilidade, mantendo a ordem raiz → alvo. |
| `WorkspaceEnvironmentCommandService.create` | Média | Resolver a referência sem lock permitia mudança simultânea do lifecycle do workspace entre a validação e a criação. | Usa o port existente `resolveActiveForUpdate`, seguindo o padrão de criação de aplicações. O bloqueio é da linha desse workspace; criações concorrentes no mesmo workspace aguardam o fim da transação. |
| `EnvironmentCommandOperations.restore` | Média | Uma sequência concorrente de restauração, edição e inativação podia fazer a validação usar o tipo de uma leitura anterior. | Captura a versão inicial e compara com a versão autoritativa após lock. Uma mudança concorrente é rejeitada com o conflito de versão da plataforma, sem exigir novo campo HTTP. |
| `EnvironmentTypeValidator.validateForCreate` | Média | O cliente podia informar version na criação, embora a versão inicial pertença ao Hibernate. | Rejeita version informada com a chave de mensagem já existente. |
| `EnvironmentQueryService.listWorkspace/listDefaults` | Média | A montagem de `parentIdentifier` podia inicializar um pai lazy por registro, especialmente quando o pai tem outro lifecycle. | Identificadores de pais buscados em lote por projeção; output recebe a referência resolvida sem carregar a entidade pai para essa conversão. |
| Finder, QueryService e facade de workspace | Baixa | Os nomes `Custom` também representavam SHARD e CELL. | Métodos internos usam `Workspace`, com chamadas da facade e dos commands atualizadas; rotas HTTP preservadas. |
| Controllers, facades, outputs e validator de compatibilidade | Baixa | Imports wildcard e variáveis como `e`, `value` e `TypeId` tornavam os contratos menos explícitos. | Imports explícitos e nomes referentes a ambiente, compatibilidade e código de tipo. |

### Concorrência, auditoria e erros

Tipos são bloqueados antes das linhas de ambientes, e ancestrais antes do alvo. Criação de filho e mudança de tipo continuam coordenadas; update mantém a comparação da versão recebida do cliente após lock. O vínculo existente pode conservar tipo ou compatibilidade inativos, enquanto novos vínculos exigem a elegibilidade atual.

As constraints continuam protegendo unicidade de nomes, códigos e pares de tipos. A biblioteca de mensagens traduz `DataIntegrityViolationException` em conflito da plataforma, sem devolver SQL ou nomes de constraints ao cliente. Não foi introduzida captura de exceção para continuar uma transação inválida.

`delete` de ambiente mantém quarentena e evento DELETE. Exclusões físicas de tipo e compatibilidade mantêm PURGE, snapshot montado antes da remoção, flush e retorno para o aspect. Inativação, restauração e atualização preservam o retorno para auditoria dentro da transação.

### Verificação e limites

Após os ajustes desta revisão, `mvn --offline --batch-mode --no-transfer-progress -DskipTests test-compile` passou, compilando 393 arquivos principais e 62 arquivos de teste. `git diff --check` passou. Foi corrigido também um ponto-e-vírgula ausente em `EnvironmentTypeFinder`, apontado pela primeira compilação.

Não foram adicionados nem executados testes nesta etapa. As correções de visibilidade, concorrência e busca em lote precisam de execução das regressões de API, autorização, auditoria e banco; compilação e inspeção estática não comprovam esses comportamentos em runtime. As migrations não foram alteradas nesta revisão.

As listagens de ambientes continuam sem paginação para preservar o contrato atual. A projeção em lote reduz consultas de pais, mas não limita o tamanho da resposta nem a carga da hierarquia. Introduzir paginação nas listagens gerais é uma evolução de contrato separada; a árvore completa exige tratamento próprio.

## Fechamento dos gaps da simulação — 2026-10-12

| Gap | Ajuste aplicado |
| --- | --- |
| Mensagens de purge ausentes | Cadastradas environment.compatibility.delete-requires-inactive (ENVIRONMENT-145) e environment.compatibility.in-use (ENVIRONMENT-146) em português e inglês |
| Create concorrente de tipo retornando 500 | Removido o bloqueio integral do catálogo no create, que reproduziu deadlock. existsByCode melhora a rejeição prévia; a PK(code) garante unicidade na corrida e a transação malsucedida é revertida |
| Testes de contratos antigos | Create de tipo com version não nula passa a esperar rejeição; flags fixas não são alteradas para fabricar cenários de concorrência; ciclos usam pares elegíveis entre tipos de workspace |
| Fixtures e schema de referência | Fixtures publicam V57 e usam code nas FKs atuais. Fixtures de upgrade anteriores a V56 continuam usando id/identifier e environment_type_id; o schema local de compatibilidade foi alinhado ao publicado |
| Auditoria | Fixtures cadastram tipos válidos e informam environmentTypeCode conforme a rota; mantida cobertura de snapshot, versão após flush e rollback da outbox |
| Quarentena | Teste comprova preservação de pai/tipo/compatibilidade e bloqueio dos purges. Mensagem de filhos não sugere que colocá-los em quarentena libere o pai |
| Upgrade de dados existentes | Novo EnvironmentTypeCodeMigrationIT parte de V55, verifica versões, lifecycle, settings, hierarquia, FKs, identidade da compatibilidade, PK(code), remoção das colunas antigas e equivalência dos três schemas |

A criação de tipo não precisa bloquear o grafo: uma nova linha ainda não possui arestas. Alterações de configuração e compatibilidades continuam bloqueando os tipos em ordem para proteger suas invariantes. Não foi removida proteção transacional para continuar após erro de banco.

O contrato de DELETE de ambientes permanece INACTIVE → QUARANTINED sem filhos. A remoção física posterior de ambientes e a paginação das listas/limites da árvore são evoluções específicas, registradas com requisitos em [limites operacionais](ENVIRONMENT-OPERATIONAL-LIMITS.md). Não existe rotina de retenção de ambientes implementada no serviço e não foi medida capacidade sob carga.

### Verificação final do contrato atual

```shell
mvn --offline --batch-mode --no-transfer-progress -Dit.test=EnvironmentApiIT,EnvironmentAuditIT,EnvironmentTypePurgeIT,EnvironmentTypeCompatibilityAuditIT,EnvironmentTypeCodeMigrationIT,SharedApiIT,SharedInternalReferencesMigrationIT,LifecycleListingIT clean verify
```

BUILD SUCCESS: 187 testes, sem falhas, erros ou skips. Executados com MySQL 8.4.11 isolado e mocks locais de autorização; nenhum banco de ambiente do usuário foi alterado.

| Suíte | Testes | Resultado |
| --- | --- | --- |
| Unitários e arquitetura | 90 | Passaram |
| EnvironmentApiIT | 32 | Passaram: contratos, hierarquia, visibilidade, lifecycle, versão, ciclos, locks, concorrência e novas proteções |
| EnvironmentAuditIT | 2 | Passaram: snapshots, versões persistidas e rollback |
| EnvironmentTypePurgeIT | 5 | Passaram: uso, snapshot, restore e rollback do purge |
| EnvironmentTypeCompatibilityAuditIT | 3 | Passaram: inativação, purge, rejeições e rollback |
| EnvironmentTypeCodeMigrationIT | 1 | Passou: upgrade V55 → V57 com dados existentes e equivalência dos schemas |
| SharedApiIT | 51 | Passaram: referências e regressões do compartilhamento |
| SharedInternalReferencesMigrationIT | 1 | Passou: preservação de referências históricas no upgrade |
| LifecycleListingIT | 2 | Passaram: filtro central e listas |

Log local: `/private/tmp/environment-review-final-verify.log`. `git diff --check` passou. A conferência estática confirmou o cadastro de todas as chaves usadas pelo core de ambientes nos dois idiomas e a validade dos schemas JSON.

A regressão exigiu build limpo porque target continha arquivos antigos com versões de migration duplicadas. Depois, corrigiu-se o deadlock de create, fixtures anteriores ao contrato por code e divergência do schema local. As migrations de produção existentes não foram reescritas; o novo teste verifica sua aplicação.

A revisão funcional do contrato atual pode ser encerrada. Essa conclusão não certifica volume ilimitado e não implementa retenção física de ambientes; esses limites estão explicitados no documento operacional. A suíte completa de todas as integrações do repositório e testes de carga não foram executados nesta etapa.
