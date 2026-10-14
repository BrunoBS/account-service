# Revisão por classe e método — core.workspace

Data: 11/10/2026. Escopo: os 24 arquivos Java de `core/workspace`, incluindo métodos privados, construtores, interfaces e records. Revisão estática do estado atual, com leitura dos consumidores, schema, integração de visibilidade e testes relevantes. Nenhuma implementação foi alterada e nenhuma suíte foi executada nesta revisão.

## Encaminhamentos implementados após a revisão

- Confirmado pelo usuário: usuário comum cria somente MANAGER sem definir grupo autorizador. A ausência de acesso posterior até configuração por owner segue a regra aceita; não foi aberta visibilidade para grupo NULL.
- Troca de tipo bloqueada quando existe qualquer aplicação vinculada, incluindo INACTIVE e QUARANTINED. Demais alterações continuam permitidas conforme autorização. Consulta de vínculo só ocorre quando o tipo muda.
- Edição de workspace e criação de aplicação usam bloqueio pessimista na mesma linha do workspace, dentro de suas transações. A leitura do vínculo é uma leitura com bloqueio (`FOR SHARE`), para observar aplicações confirmadas após uma espera mesmo com snapshot anterior. O agregado é atualizado do banco após adquirir o bloqueio, inclusive quando a auditoria o carregou antes.
- A recarga do agregado usa outra consulta com bloqueio e EntityGraph de aprovadores. O teste de concorrência demonstrou que `EntityManager.refresh` pode emitir uma leitura sem bloqueio e recuperar o snapshot anterior no MySQL; por isso ele não é utilizado nesse fluxo. Detach não libera o bloqueio mantido pela transação.
- Consultas comuns de referências agora têm `ResourceVisibility(Workspace.class)`. Shared utiliza métodos explicitamente chamados `findReferenceIdentifierForShared`, `findReferenceIdentifiersForShared` e `findActiveIdentifiersForShared`, documentados como acessos internos após autorização do escopo compartilhado. O helper privado é reutilizado, sem duplicar queries/montagem de mapas.
- Máquina de estados simples na entidade: ACTIVE → INACTIVE, INACTIVE → ACTIVE ou QUARANTINED. QUARANTINED é terminal. Transições inválidas não alteram estado nem data. Finder reutiliza a validação de restauração/quarentena do domínio; o validator não contém mais essas regras.
- Lambda de `WorkspaceOutput.from` renomeada para `approver`.

Verificação dos encaminhamentos: `mvn --offline --batch-mode --no-transfer-progress clean verify` passou com 85 testes unitários/arquitetura e 159 testes de integração (244 no total), sem falhas, erros ou testes ignorados. Inclui concorrência nos dois sentidos entre criação de aplicação e mudança de tipo, vínculos em todos os estados, transições do domínio, visibilidade comum e regressão de Shared. Formatação e `git diff --check` passaram.

As tabelas abaixo registram o diagnóstico anterior a esses encaminhamentos.

Os acessores e métodos gerados de records (`equals`, `hashCode`, `toString`) e operações herdadas dos repositories são agrupados quando não possuem implementação própria.

## Decisões de negócio e cobertura residual

| Prioridade | Classe / método | Observação e encaminhamento |
| --- | --- | --- |
| Fluxo de acesso após cadastro | `WorkspaceValidator.validateCreationPermissions`, `WorkspaceCommandService.create`, `WorkspaceFinder` | Está definido que usuário comum pode criar MANAGER sem `authorizerGroup`, e somente OWNER configura o grupo. Como o filtro por grupo não inclui valores NULL, o criador não terá leitura por grupo até um owner configurar um grupo ao qual ele pertença. Isso é consequência da regra aceita, não uma falha de autorização. Os testes cobrem criação sem grupo e visibilidade por grupo separadamente; ainda é útil um cenário integrado de criação sem grupo → configuração pelo owner → acesso pelo grupo atribuído, se esse for o fluxo esperado de onboarding. |
| Integridade ao mudar tipo | `WorkspaceValidator.validateSensitiveChanges`, `WorkspaceCommandService.update` | A mudança de tipo é permitida somente ao OWNER e recusada quando existe aplicação vinculada, em qualquer lifecycle. A consulta e a alteração usam o lock do workspace, protegendo a corrida com criação de aplicação. Coberto por cenários concorrentes e vínculos em estados diferentes; sem pendência de implementação. |
| Referências internas para Shared | `WorkspaceQueryService.findReferenceIdentifierByInternalId`, `findReferenceIdentifiersByInternalIds`, `findActiveIdentifiersByInternalIds` | Não usam `ResourceVisibility` porque Shared acessa referências entre workspaces após autorizar o contrato/participação. O ponto de entrada e a pré-condição estão documentados; adicionar o filtro comum impediria compartilhamentos legítimos. |
| Lifecycle e legibilidade | `Workspace.inactivate`, `restore`, `quarantine`, `WorkspaceOutput.from` | Transições estão protegidas pela entidade; a variável da conversão de aprovadores usa o nome `approver`. Nenhuma ação restante. |

## 1. WorkspaceCommandService

| Método | Observação |
| --- | --- |
| Construtor | Dependências explícitas e coerentes com orquestração. Não precisa criar abstrações adicionais. |
| `create` | Recebe input tipado já validado estruturalmente na entrada HTTP; validação de negócio agrupada, mapper, persistência e tags em uma transação. `saveAndFlush` antecipa conflito de integridade e produz versão para a saída. Sem leitura desnecessária das tags do agregado novo. A configuração posterior de grupo por OWNER segue a regra aceita. |
| `update` | Busca protegida do ativo; valida versão e permissões antes de consultas adicionais. Reutiliza mapper e reconcilia aprovadores/tags. `@Version` protege corrida posterior à validação. Troca de tipo com aplicações vinculadas é recusada sob lock. |
| `inactivate` | Exige ativo e grava INACTIVE; preserva tags. Retorno completo serve à auditoria mesmo com HTTP 204 na facade. Sem gap identificado no fluxo atual. |
| `restore` | Exige INACTIVE, preserva aprovadores e tags e evita reconciliar dados que não mudaram. Retorna estado e versão atualizados. |
| `delete` | Coloca somente INACTIVE em QUARANTINED, sem remoção física. Nome corresponde à operação pública; sem necessidade de expor o termo técnico na API. Retorno mantém snapshot completo de auditoria. |
| `reconcileApprovers` | Complexidade linear, mantém IDs dos pares inalterados, remove antes de inserir e usa flush para liberar valores UNIQUE, inclusive trocas de e-mail. Flush intermediário é necessário nesse cenário e continua dentro da mesma transação. Alterar somente a grafia do par substitui o aprovador: é comportamento atual, não bug comprovado. |
| Record privado `ApproverKey` / acessores gerados | Chave composta legível e restrita ao algoritmo. Não precisa virar classe externa. |

## 2. WorkspaceFinder

| Método | Observação |
| --- | --- |
| Construtor | Uma dependência, responsabilidade de localizar referências/agregados. |
| `findActiveReference(String)` | Projeção leve, ACTIVE, Optional → not found e visibilidade explícita. |
| `findActiveReference(Long)` | Mesmas garantias da sobrecarga por identificador externo. Não usa busca de entidade completa. |
| `findActive` | Carrega agregado com aprovadores e valida ativo. Adequado aos consumidores atuais que precisam dessa coleção. Para necessidade apenas de ID, usar as projeções existentes. |
| `findInactiveForRestore` | Visibilidade antes da validação de estado; QUARANTINED e ACTIVE não são restauráveis. |
| `findInactiveForDeletion` | Visibilidade e exigência de INACTIVE; impede quarentena repetida. |
| `findRequired` | Privado, concentra not found. Sem anotação adicional necessária: é chamado pelas entradas públicas anotadas. |

As chamadas atuais relevantes ocorrem dentro das transações dos serviços. Finder e resolver dependem desse contexto; novos consumidores devem manter a integração transacional necessária ao filtro Hibernate.

## 3. WorkspaceNormalizer

| Método | Observação |
| --- | --- |
| `normalizePayload` | Copia o JSON, normaliza somente campos conhecidos e preserva tipos inválidos/campos desconhecidos para o schema rejeitar. Não altera settings nem o payload original. Chamado pelo advice HTTP antes da desserialização. |
| `normalizeTypeFilter` | Delega à regra única de códigos; filtro vazio vira ausência. |
| `normalizeTagFilter` | Usa `TagName`, consistente com a biblioteca; evita implementar regra paralela de tags. |
| `trimText` | Não converte números/objetos em strings. Correto para normalização antes de validação. |
| `normalizeCode(ObjectNode, String)` | Preserva ausência e valores de tipo inválido; string vazia vira NULL. Schema rejeita tipo obrigatório NULL e aceita grupo opcional NULL. |
| `normalizeCode(String)` | Maiúsculas com `Locale.ROOT`; previsível entre ambientes. Sem mudança necessária. |

## 4. WorkspaceQueryService

| Método | Observação |
| --- | --- |
| Construtor | Responsabilidades claras; normalização do payload fica no advice HTTP, filtros na facade. |
| `findReferenceIdentifierByInternalId` | Projeção histórica pode incluir inativo/quarentena; uso atual em Shared. Documentar pré-condição de autorização do consumidor. |
| `findReferenceIdentifiersByInternalIds` | Resolve referências históricas em lote. Sem filtro de visibilidade intencional para compartilhamento; mesma documentação recomendada. |
| `findActiveIdentifiersByInternalIds` | Resolve apenas ACTIVE em lote; existência/atividade não equivale a autorização. Mesma pré-condição. |
| `findIdentifiersByInternalIds` | Privado; evita consulta para coleção vazia e centraliza montagem do mapa. Sem necessidade de limite arbitrário ou consulta por item. |
| `findByIdentifier` | Read-only, Finder protegido e tags manuais. Anotação de visibilidade também no Finder protege seus outros consumidores; sobreposição aqui não demonstra falha. |
| `findAll` | Page nativo, filtros no banco, desempate por ID e tags em lote. Aprovadores carregados por batch dentro da transação. `active` ausente significa ACTIVE; false significa INACTIVE, não inclui QUARANTINED. Comportamento deve constar no contrato da API. |

## 5. WorkspaceValidator

| Método | Observação |
| --- | --- |
| Construtor | Catálogo e repository são dependências adequadas para regras de negócio. |
| `requireActive` | Oculta recurso não ativo com not found; coerente com consulta operacional. |
| `requireRestorable` | Exige INACTIVE; erro de validação específico. |
| `requireDeletable` | Exige INACTIVE; não unificar com restore só porque hoje compartilham condição: mensagens e intenções são distintas. |
| `requireVersion` | Comparação null-safe e exceção de conflito dedicada. |
| `validateCreationPermissions` | Respeita regra owner para ADMIN/grupo; MANAGER sem grupo permitido. O acesso posterior depende da atribuição de grupo por OWNER, conforme regra aceita. |
| `requireOwner` | Ausência de sessão não concede privilégio. Não confiar em campo enviado pelo cliente. |
| `validateSensitiveChanges` | Só exige owner quando tipo/grupo realmente mudam. A troca de tipo consulta vínculos e bloqueia quando há aplicação, sob o lock do pai. |
| `requireUpdateVersion` | Versão obrigatória na edição, ainda que opcional no schema comum; falha antes de consultas adicionais. |
| `validatePagination` | Rejeita unpaged, offset incompatível com JPA e propriedades de ordenação fora da lista. Limite de tamanho HTTP pertence ao resolver Spring configurado; chamadas internas não recebem automaticamente esse limite. Extrair allowlist para constante seria apenas melhoria pequena. |
| `validateForCreate` | Ponto único de validação agrupada; autorização sensível antecede consulta global de nome. |
| `validateForUpdate` | Versão → permissão → nome se mudou → regras de negócio. Preserva unicidade global, inclusive inativos. Constraint no banco continua indispensável à concorrência. |
| `validateTypeFilter` | Valida tipo suportado/ativo somente quando informado. |
| `validateBusiness` | Catálogo e duplicidade de nome/aprovadores; tamanhos/estrutura ficam no schema. Sets com `Locale.ROOT` evitam duplicados por case, com erros por índice. Até cinco aprovadores: custo pequeno. |
| `isSupportedType` | Enum restrito ADMIN/MANAGER; custo desprezível. Sem motivo para cache adicional. |
| `rejectIfInvalid` | Centraliza lançamento da exceção com erros acumulados. |

## 6. WorkspaceFacade

| Método | Observação |
| --- | --- |
| Construtor | Injeção explícita de command, query e normalizer. |
| `create` | OPEN/CREATE mais regras de owner no validator; recebe input tipado após normalização/schema na entrada HTTP. OPEN não significa acesso anônimo na biblioteca. |
| `findByIdentifier` | DEV/READ e visibilidade no query/Finder. |
| `findAll` | OPEN/READ e visibilidade no query; monta input com filtros normalizados, sem regra de negócio duplicada. |
| `update` | ADM/UPDATE mais owner somente para campos sensíveis. |
| `inactivate` | ADM/DEACTIVATE; descarta saída do command porque a API retorna 204, mantendo auditoria interna. |
| `restore` | ADM/RESTORE; devolve saída do command. |
| `delete` | ADM/DELETE; mantém contrato void/204 e auditoria do command. |

`AuthorizationContext` é consumido pelo aspecto, mesmo sem uso explícito no corpo: não é parâmetro morto.

## 7. WorkspaceReferenceAdapter

| Método | Observação |
| --- | --- |
| Construtor | Depende do Finder, preservando visibilidade em todas as resoluções operacionais. |
| `resolveActive` | Uma projeção resolve ID, identificador e tipo juntos, evitando consultas repetidas. |
| `resolveInternalId` | Retorna somente ID da projeção ativa/protegida. |
| `resolveIdentifier` | Resolve sentido inverso, também exigindo ativo e visível. Não substitui resolução histórica de Shared. |

## 8. WorkspaceRepository

| Método | Observação |
| --- | --- |
| `findByIdentifier` | Optional e EntityGraph de aprovadores adequados à leitura do agregado. A visibilidade é aplicada nas entradas do Finder/query, não globalmente no repository. |
| `findFiltered` | Conteúdo/count com mesmos predicados; EXISTS de tag evita multiplicação de linhas. Sem fetch join de coleção na página. Índices adicionais devem ser decididos por EXPLAIN e volume real, não apenas pela aparência da query. |
| `findActiveReferenceByIdentifier` | Projeção Optional e predicado ACTIVE; sem List/limit 1. |
| `findReferenceById` | Projeção Optional; lifecycle NULL permite uso histórico intencional. |
| `findReferencesByIds` | Consulta em lote; guard de coleção vazia fica no query service. |
| `existsByName` | Verificação global sem visibilidade. Case-insensitive garantido pela collation V45; nomes inativos/quarentenados permanecem reservados. |
| `existsByNameAndIdNot` | Exclui o próprio registro; permite alterar grafia do nome sem colidir consigo. Constraint UNIQUE protege a corrida. |
| Operações herdadas de `JpaRepository` | Sem implementação local. Serviços controlam transação, visibilidade e ciclo de vida; consumidores não devem contornar essas entradas. |

## 9. WorkspaceTagRepository

Sem métodos declarados. Especialização de `TagRepository<WorkspaceTag, Workspace>` necessária ao bean e à persistência das tags; manter. Operações herdadas pertencem à biblioteca.

## 10. WorkspaceReference

| Método | Observação |
| --- | --- |
| `getId` | ID interno utilizado na integração, sem carregar entidade. |
| `getIdentifier` | Alias corresponde às queries; identificador externo. |
| `getWorkspaceType` | Alias corresponde às queries; elimina busca adicional de tipo. |

## 11. Workspace

| Método | Observação |
| --- | --- |
| Construtor protegido | Necessário ao JPA; não remover como “não usado”. |
| Construtor com `WorkspaceDetails` e data | Cria identidade, estado ativo e datas; recebe detalhes previamente validados pelo usecase. Não precisa builder adicional. |
| `update` | Reutiliza aplicação dos detalhes e atualiza data. Não valida permissões de sessão no domínio. Troca de tipo depende da regra discutida acima. |
| `applyDetails` | Única atribuição dos campos comuns a create/update; copia settings para impedir mutação externa. |
| `addApprover` | Constrói filho vinculado ao agregado. Validação de quantidade/duplicidade ocorre no schema/validator; Set por identidade Java sozinho não garante unicidade de funcional/e-mail. Banco e validator cobrem fluxo atual. |
| `removeApprover` | Remove filho; orphanRemoval efetiva exclusão no flush. |
| `inactivate` | Transição simples; estado anterior validado pelo Finder. Guard de domínio é opção de endurecimento. |
| `restore` | Mesma observação; não deve restaurar QUARANTINED fora do fluxo validado. |
| `quarantine` | Mesma observação; fluxo público exige INACTIVE. |
| `getSettings` | Deep copy impede alteração do JSON persistido por referência externa. Manter. |
| `getApprovers` | Visão não modificável do Set; filhos não expõem setters. Carregamento lazy exige contexto transacional no mapeamento de saída. |
| `getId`, `getVersion`, `getIdentifier`, `getWorkspaceType`, `getName`, `getDescription`, `getRequester`, `getAcronym`, `getAuthorizerGroup`, `getEmailGroup`, `isOnboarding`, `getLifecycle`, `getCreatedAt`, `getUpdatedAt` | Acessores simples, sem regra oculta ou consulta extra própria. `onboarding` inicia false e não tem transição neste pacote; esclarecer seu propósito antes de criar fluxo ou remover coluna. |

## 12. WorkspaceApprover

| Método | Observação |
| --- | --- |
| Construtor protegido | JPA. |
| Construtor de pacote | Cria UUID e vínculo com Workspace; restringe criação ao domínio. |
| `getId` | Identidade estável dos aprovadores preservados pela reconciliação. |
| `getFunctional`, `getEmail` | Acessores simples; sem setters, evitando alteração fora da reconciliação. |

## 13. WorkspaceTag

| Método | Observação |
| --- | --- |
| Construtor protegido | JPA. |
| Construtor público | Factory utilizada pelo TagManager; delega nome/origem à biblioteca. |
| `getOwner` | Implementa vínculo exigido pela abstração de tags. Associação lazy. |

Coluna de tags com 50 comporta nomes de Workspace e tags manuais, ambos limitados a 50 pelo schema.

## 14. WorkspaceDetails

Record com construtor/acessores gerados, sem métodos próprios. Separa dados de alteração das entradas HTTP. `JsonNode` é mutável: o record é imutável apenas quanto às referências. A cópia na entidade protege o estado persistido; não há necessidade demonstrada de adicionar cópias em todas as camadas.

## 15. WorkspaceSystemTags

| Método | Observação |
| --- | --- |
| Construtor privado | Impede instância da utilidade. |
| `resolve` | Centraliza identificador/nome/grupo/sigla. Grupo pode ser NULL; TagManager normaliza e descarta NULL. Não substituir `Arrays.asList` por `List.of` sem tratar NULL. |

## 16. WorkspaceMessageKeys

Construtor privado e constantes, sem métodos de negócio. Localização em domain segue a regra registrada do projeto. Mensagens específicas de restore/delete são adequadas; não incorporar nomes de constraints ou SQL nas chaves/respostas.

## 17. WorkspaceMapper

| Método | Observação |
| --- | --- |
| `toDetails(CreateWorkspaceInput)` | Conversão explícita; sem persistência/validação. |
| `toDetails(UpdateWorkspaceInput)` | Mesma conversão para edição, mantendo contratos distintos. |
| `toDetails` privado com campos | Centraliza construção do código de tipo e do record. As duas listas explícitas de campos são pequenas; não justificam herança/interface genérica apenas para eliminá-las. |

## 18. WorkspaceTagConfiguration

| Método | Observação |
| --- | --- |
| `workspaceTagManager` | Bean tipado com repository e factory; configuração necessária. Classe/método de pacote e proxyBeanMethods=false são adequados. Não misturar configuração em command ou entidade. |

## 19. CreateWorkspaceInput

Record sem métodos próprios. Construtor/acessores representam payload validado. `version` é aceito estruturalmente pelo schema comum, mas o validator rejeita o campo quando não nulo na criação e o exige na edição. Listas/JSON não são cópias defensivas: uso atual local à operação não evidencia problema.

## 20. UpdateWorkspaceInput

Record sem métodos próprios. Versão obrigatória por regra de negócio; campos completos caracterizam edição por substituição, não patch parcial. Omitir settings/grupo/tags opcionais pode limpar esses dados; documentar esse comportamento para consumidores.

## 21. FindAllWorkspacesInput

Record sem métodos próprios. `active` NULL significa ativos, conforme query service; type/tag chegam normalizados pela facade. Não precisa carregar Pageable para dentro deste record.

## 22. ApproverInput

Record sem métodos próprios. Estrutura/tamanho no schema, duplicidade no validator e unicidade no banco; sem necessidade de validação repetida nos acessores.

## 23. ApproverOutput

Record sem métodos próprios. Saída mínima de funcional/e-mail; não expõe ID interno nem a entidade pai.

## 24. WorkspaceOutput

| Método | Observação |
| --- | --- |
| `from` | Mapeia dentro da transação, ordena aprovadores e copia lista de tags. Settings vem de getter com deep copy. Renomear lambda `value` para `approver` é melhoria de legibilidade. |
| Construtor/acessores gerados | DTO de saída sem regra de persistência. JSON retornado é independente da entidade, embora o próprio DTO contenha objeto mutável. |

## Evidência e limites da revisão

- A inspeção considerou `schemas/workspace.json`, limites das entidades, V43–V45, consumidores de Application/Shared e implementação local de `ResourceVisibilityAspect`/`ResourceVisibilityMappingRegistrar` da biblioteca.
- Testes existentes inspecionados cobrem permissões de criação/edição, visibilidade por grupo, referências, batching, settings, versões, concorrência de nomes, aprovadores, tags e auditoria. O caso criação sem grupo → acesso posterior pelo criador precisa de cobertura específica.
- Não foram identificados métodos locais claramente sem consumidor que devam ser removidos nesta revisão. Construtores JPA, projeções, parâmetros de aspectos e métodos herdados não devem ser classificados como mortos por ausência de chamada direta.
- A revisão estática inicial não executou testes, benchmarks, EXPLAIN nem validação de inicialização. Os ajustes posteriores foram validados separadamente pela suíte indicada no início deste relatório; não houve benchmark nem EXPLAIN de consultas.
- Não restam decisões de integridade ou autorização bloqueadoras. A cobertura integrada do ciclo de atribuição de grupo após criação pode ser acrescentada para documentar explicitamente o fluxo já definido.
