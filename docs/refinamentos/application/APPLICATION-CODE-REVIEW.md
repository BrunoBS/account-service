# Revisão do módulo Application

Referência: [REVIEW-STANDARD.md](../../REVIEW-STANDARD.md). Revisão das classes de `core/application` e das fronteiras HTTP, schema, persistência e integrações necessárias ao fluxo. A organização segue o padrão adotado para os módulos core: domínio, repository, facade e usecase com model, operations, validation, mapping e configuration.

## Regras preservadas e contratos ajustados

- Aplicações pertencem a um workspace MANAGER ativo e visível. O ID interno do pai é persistido; identificadores públicos são utilizados nas rotas e respostas.
- Nome é único dentro do workspace, ignorando case, inclusive para aplicações inativas/quarentenadas. Outro workspace pode usar o mesmo nome. Constraint e collation protegem a corrida de gravação.
- Escopo precisa estar ativo no catálogo. Uma aplicação SHARED com contratos vinculados não pode mudar para outro escopo; a verificação e a criação de contratos compartilham o bloqueio da aplicação.
- Criação usa ADM; consulta e edição usam DEV; inativação, restauração e exclusão usam ADM. Estas policies são regras atuais de Application. As restrições específicas de criação de Workspace não são transferidas automaticamente a outro recurso.
- Lifecycle: ACTIVE → INACTIVE → ACTIVE, ou INACTIVE → QUARANTINED. Transições inválidas são rejeitadas, inclusive em chamadas diretas à entidade.
- JSON de settings permanece estruturado do request até o banco e response, com cópias defensivas na entidade. Settings é obrigatório e deve ser objeto com language e infrastructure válidos.
- Normalização dos textos e códigos ocorre no advice HTTP antes do schema. Tipos inválidos e propriedades desconhecidas permanecem no payload para rejeição estrutural.
- O schema limita textos conforme as colunas e aceita até 20 tags manuais. A capability normaliza e deduplica tags; clientes não precisam enviar apenas o formato já normalizado. Os opcionais authorizerGroup e tags aceitam ausência/null como em Workspace; o schema compartilhado aceita `version` por causa do update, mas o validator a rejeita quando não nula na criação e a exige na edição.
- A listagem passa a usar `Pageable` e retornar `PagedModel`: `content` e `page` (`number`, `size`, `totalElements`, `totalPages`). Default e tamanho máximo são definidos pela configuração Spring Data. Filtros de estado e tag continuam disponíveis. Ordenação recebe desempate por ID e aceita apenas campos simples autorizados pelo validator.
- O endpoint de resumo continua retornando lista de representações pequenas, com consulta por projeção. Ele é um contrato de seleção distinto da listagem de detalhes paginada.
- Operações de lifecycle preservam output para snapshot de auditoria, mesmo quando o controller responde `204`.
- Primeiro cadastro de aplicação registra marco na mesma transação. A gravação do marco é atômica/idempotente e mantém o primeiro usuário/data.

## Limites alinhados a Workspace

| Campo | Coluna / schema de Application | Critério |
| --- | --- | --- |
| name | 50; mínimo 3 no schema | Mesmo limite do nome de Workspace |
| alias | 50; mínimo 1 no schema | Nome alternativo, com mesmo limite do nome |
| description | 200, opcional/null | Texto descritivo persistido e retornado no detalhe |
| acronym | 5; mínimo 1 no schema | Mesmo limite da sigla |
| authorizerGroup | 10, opcional/null | Mesmo limite do grupo |
| applicationScope / lifecycle | 20 | Mesmo tamanho dos códigos de catálogo |
| tags manuais | Até 20 itens, até 50 caracteres cada | Mesmo contrato de tags manuais |
| application_tag.name | 50 | Comporta tags de sistema derivadas de nome/alias, além das manuais |
| settings | JSON NOT NULL | Objeto obrigatório; exige language e infrastructure válidos |

A validação de tamanhos fica no schema, sem duplicação no validator. A migration V48 preserva as foreign keys, publica um novo schema e mantém as migrations anteriores imutáveis.

## Achados corrigidos

| Local | Severidade | Evidência/impacto | Ajuste e verificação |
| --- | --- | --- | --- |
| Normalizer / facade / schema | Média | Schema precedia normalização; arquivo de referência continha BOM/texto residual e não correspondia ao contrato publicado | Normalização de JsonNode no facade, schema válido e publicação v2 por nova migration; teste de tipos/propriedades e códigos normalizados |
| Finder / comandos de lifecycle | Média | Comando consultava query para visibilidade e carregava novamente o recurso | Visibilidade no Finder; comandos carregam uma vez e usam guardas do domínio; testes HTTP de acesso e lifecycle |
| Query de escopo sob lock | Alta | `refresh` podia reutilizar estado anterior à espera sob isolamento MySQL | Recarga por consulta com lock após detach, como no Finder de referência; cenários de contrato versus alteração de escopo |
| Application / outputs / response | Média | JSON era String persistida como TEXT e reparsed por mapper estático no response | JsonNode + Hibernate JSON, getter/entrada com cópia defensiva e migration; teste de persistência HTTP e mutação externa |
| Listagem | Média | Materialização integral e consulta adicional de todos os identificadores da tag | Page Spring Data, EXISTS para tag e busca de tags da página em lote; testes de páginas, ordenação e contagem autorizada |
| Referências / resumo | Baixa | Carregamento de entidade completa para dados pequenos | Projeções e consulta escalar do identificador; regressão de Shared e visibilidade do resumo |
| Validator / Command | Média | Validações espalhadas entre command e validator | Uma entrada de validação por criação/edição, incluindo versão e integridade de escopo; erro de contrato vinculado mantém 409 |
| Application / Command | Baixa | Listas repetidas de atributos em construção/edição | ApplicationDetails + ApplicationMapper; comando permanece responsável por coordenar persistência |
| Configuração / construtores / queries | Baixa | Configuração em operations, fallback de dependências e métodos sem consumidor | Configuration em pacote próprio, dependências obrigatórias, remoção de métodos e noop sem uso; compilação e arquitetura |
| Nome / persistência | Média | Case-insensitive dependia da collation ambiente | Collation explícita em nova migration; testes de nome por workspace, reserva após inativação e corrida com case diferente |

## Análise por classe e método

### ApplicationCommandService

| Método | Avaliação final |
| --- | --- |
| Construtor | Dependências obrigatórias explícitas; sem fallback que desabilite integrações silenciosamente |
| create | Schema estrutural no proxy, referência ativa/visível do pai sob lock, validação agrupada e mapper. saveAndFlush revela violações antes de retornar. Tags iniciais e marco do primeiro cadastro participam da mesma transação |
| update | Carrega aplicação ativa/visível sob lock, valida versão/nome/catálogo/contratos, aplica detalhes e reconcilia tags retornando as manuais sem segunda leitura |
| inactivate | Resolve pai e Finder visível, transição protegida na entidade e persistência com versão; snapshot para auditoria |
| restore | Exige INACTIVE. Tags manuais existentes são reutilizadas; não recalcula tags de sistema quando os atributos não mudaram |
| delete | Exige INACTIVE e faz quarentena; não remove linhas/relacionamentos históricos; snapshot auditável |

### ApplicationFinder

| Método | Avaliação final |
| --- | --- |
| findActive | Visibilidade aplicada; inexistente e estado não ativo retornam not found |
| findActiveForUpdate | Lock pessimista e recarga atual; valida estado após adquirir o lock; usado em alteração e integridade de contratos |
| findInactiveForRestore | Visibilidade e guarda restorable da entidade; estado inválido tem erro específico |
| findInactiveForDeletion | Visibilidade e guarda deletable da entidade |

O Finder usa a transação do use case chamador. Consultas de Shared com autorização própria não passam por esta entrada filtrada.

### ApplicationQueryService

| Método | Avaliação final |
| --- | --- |
| findActiveScope | Resolve pai visível e aplicação ativa/visível; não expõe entidade |
| findActiveScopeForUpdate | Reutiliza Finder sob lock; mantém contrato de referência usado pela integração |
| findActiveScopeForShared | Usa projeção com IDs e scope, sem carregar settings/description; confere vínculo com workspace e atividade de ambos. Entrada entre aplicações após o chamador validar contrato/participação; não aplica o grupo da aplicação de origem ao participante |
| findIdentifierByInternalId | Consulta escalar e vínculo com workspace. Uso histórico autorizado por Shared; pode resolver aplicação inativa |
| findReferencesForShared | Projeções em lote, referências históricas e identificadores do pai em lote; guard para coleção vazia |
| findActiveReferencesForShared | Projeções ativas e pais ativos em lote; exclui referências de pais não ativos |
| findByIdentifier | Pai e aplicação ativos/visíveis, output de detalhe e tags manuais |
| findAll | Page no banco; filtro EXISTS de tag, ordenação estável, validação de sort/offset e tags em lote. Conteúdo e count submetidos à visibilidade |
| summary | Projeção leve de aplicações ativas/visíveis do pai; não carrega settings nem tags |

Métodos sem consumidor removidos: findInternalIdByIdentifier, findActiveForShared, findInactiveForShared, findInactiveByIdentifier e findInactiveForDeletion. A consulta histórica usada pelo Shared permanece explícita.

### ApplicationNormalizer

| Método | Avaliação final |
| --- | --- |
| normalizePayload | Copia o objeto, normaliza apenas campos textuais conhecidos e preserva estrutura inválida para o schema |
| normalizeTag | Delegação à normalização da biblioteca para filtro de leitura |
| trim / code / group | Helpers pequenos; códigos e grupo em uppercase com Locale.ROOT; preserva prefixo A- único |

### ApplicationValidator

| Método | Avaliação final |
| --- | --- |
| validateForCreate | Regras agrupadas: tipo MANAGER, unicidade por pai e escopo ativo. Estrutura fica no schema |
| validateForUpdate | Inclui versão obrigatória/conflito, unicidade excluindo a própria aplicação e integridade de contratos. Não consulta duplicidade se o nome não mudou, nem contratos se não há mudança relevante de escopo |
| validatePagination | Rejeita unpaged/offset fora da faixa JPA e campos de sort não suportados |
| requireVersion | Comparação explícita; JPA @Version continua protegendo a corrida real |
| validateBusinessRules / reject | Helpers locais para regras comuns e agregação de mensagens |

### ApplicationRepository

| Método | Avaliação final |
| --- | --- |
| findByIdentifierAndWorkspaceId | Optional e escopo do pai explícito |
| findByIdentifierAndWorkspaceIdForUpdate | Mesma identidade com lock pessimista, utilizado pelo Finder |
| existsByWorkspaceId | Detecta primeiro cadastro incluindo histórico de aplicações, não apenas ativas |
| existsByWorkspaceIdAndName | Unicidade por workspace sem filtro de visibilidade/lifecycle; não libera nomes de registros invisíveis/inativos |
| existsByWorkspaceIdAndNameAndIdNot | Mesmo critério excluindo o próprio registro |
| findFiltered | Page, lifecycle e EXISTS de tag. Count derivado usa mesmos predicados; sem fetch de coleção |
| findReferences | Projeções em lote, com lifecycle opcional para histórico versus descoberta |
| findActiveSummaries | Projeção sem JSON e ordem por ID |
| findIdentifier | Optional escalar por IDs de aplicação/pai |

Repositório não aplica ResourceVisibility globalmente: queries históricas/autorizadas por contrato têm requisitos diferentes. Entradas públicas de acesso filtrado ficam no Finder/query. Métodos herdados de JpaRepository são usados pelo próprio módulo.

### Application

| Método | Avaliação final |
| --- | --- |
| Construtor JPA | Necessário para persistência |
| Construtor público | Identidade gerada pelo servidor, pai imutável, ACTIVE e datas; aplica ApplicationDetails |
| update / applyDetails | Atribuição comum centralizada e cópia de settings; use case valida regras contextuais antes da alteração |
| inactivate / restore / quarantine | Transições simples com guarda de origem; máquina de estados adicional não se justifica para três estados |
| requireRestorable / requireDeletable / requireLifecycle | Pré-condições reutilizadas pelo Finder e pela transição |
| getSettings | Cópia defensiva do JsonNode |
| Demais getters | Acessores simples sem consulta ou regra oculta; identidade e version permanecem internas ao ciclo de persistência |

### Contratos, configuração e utilidades

| Classe / métodos | Avaliação final |
| --- | --- |
| ApplicationDetails | Record do domínio para dados comuns a criação/edição; não conhece HTTP ou repository |
| ApplicationMapper.toDetails (create/update) | Conversão explícita para details/código VO; sem validação ou consultas |
| CreateApplicationInput / UpdateApplicationInput | Contratos de use case; edição contém version. Schema compartilhado e validator estabelecem pré-condições por operação |
| ApplicationOutput.from | Representação de detalhe tipada, settings estruturado e cópia da lista de tags |
| ApplicationSummary | Record pequeno, sem factory inutilizada |
| ApplicationReferenceOutput / ApplicationScopeReferenceOutput | Contratos mínimos de integração com IDs internos e identificadores de retorno pertinentes |
| ApplicationReference | Projeção repository própria, sem settings; não é retornada pelo controller |
| ApplicationScopeReference | Projeção mínima para scope ativo de Application no workspace solicitado |
| ApplicationTag: construtores/getOwner | Entidade dependente, vínculo lazy; factory usada pelo TagManager. A migration limita a coluna name a 50 |
| ApplicationTagRepository | Especialização tipada necessária à biblioteca; sem CRUD paralelo |
| ApplicationSystemTags.resolve | Centraliza tags derivadas de atributos e pai; Arrays.asList aceita grupo null, tratado pelo TagManager |
| ApplicationTagConfiguration.applicationTagManager | Bean tipado, configuration própria e proxyBeanMethods=false |
| ApplicationMessageKeys | Chaves em domain; erro de paginação incluído. Resolução/tradução permanece na plataforma |
| ApplicationFacade: create/update | AuthorizationRequired e normalização antes de command/schema |
| ApplicationFacade: find/findAll/summary | Policy READ e delegação; parâmetros legíveis e Pageable explícito |
| ApplicationFacade: inactivate/restore/delete | Policies de lifecycle mantidas; delegação sem regra adicional |

### Fronteira Web e schema

Controller conserva rotas, statuses e políticas funcionais; recebe requests tipados com validação anterior à desserialização, e a listagem usa PagedModel. Response traduz output sem parse estático de settings. Resumo permanece específico. V47 altera settings para JSON e fixa collation do nome. V48 alinha tamanhos/nulabilidade e publica a versão 3 do contrato; migrations já aplicadas permanecem intactas. A fixture carrega o arquivo de referência, e um teste de migration compara esse arquivo à versão publicada para evitar divergência de contrato.

## Verificação

Verificação local concluída com `BUILD SUCCESS`:

```bash
mvn --offline --batch-mode --no-transfer-progress clean verify
```

- 87 testes unitários e arquiteturais; 168 testes de integração; 255 testes no total, sem falhas, erros ou skips.
- `git diff --check` sem problemas de whitespace.
- Log local: `/private/tmp/typed-requests-full-verify.log`.
- Os testes de upgrade iniciados em schemas antigos desabilitam a validação Hibernate da estrutura atual apenas nesse contexto de teste. O cenário de Application confirma preservação de dados JSON, tipo da coluna, collation e igualdade do schema publicado com o arquivo de referência.
- A fixture de catálogo foi ajustada para não conflitar com as fases pré-carregadas para os fluxos de domínio. A fixture estrutural usa o schema de referência sem reaplicar DDL em cada cenário.

Não restaram achados bloqueadores ou de alta severidade identificados nesta revisão. A mudança de formato da listagem e a normalização/deduplicação de tags são mudanças deliberadas do contrato, registradas acima e no README. Não foi executada uma pipeline remota nesta atividade.

Cenários exercitados: arquitetura; lifecycle e JSON defensivo; schema e normalização; tags; paginação/sort; contagem autorizada; persistência e unicidade com case; primeiro cadastro concorrente; integridade de contrato concorrendo com mudança de escopo; autorização do workspace pai e aplicação.

Settings de Application preserva regras específicas no schema: language aceita JAVA, C#, NODE ou GO; infrastructure aceita VM, ECS ou EKS. Quando settings é um objeto, ambos são obrigatórios. Settings é obrigatório: ausência e null são rejeitados.

Settings rejeita atributos não mapeados (`additionalProperties: false`); apenas language e infrastructure são aceitos.

V49 reduz nome/alias para 50 e grupo autorizador para 10 (incluindo o prefixo A- normalizado), adiciona description opcional de até 200 caracteres à Application e publica Application Resource v4 e Workspace Resource v2. E-mails de Workspace passam a 100.

CreateApplicationRequest e UpdateApplicationRequest são agora contratos tipados usados pelo controller. ResourceSchemaRequestBodyAdvice da biblioteca normaliza e valida o JSON recebido antes da desserialização, usando o SchemaValidator da biblioteca. O request traduz para o input tipado, sem novo parsing no command.

Verificação após a limpeza dos requests e a projeção de scope: `mvn --offline --batch-mode --no-transfer-progress -Dit.test=ApplicationApiIT,ApplicationPersistenceIT,SharedApiIT verify`; 87 testes unitários/arquiteturais e 68 de integração passaram. O teste de conversão do request removido foi eliminado. Log: `/private/tmp/application-scope-projection-verify.log`.

Fluxo de criação/edição: JSON original → normalização textual preservando tipos/campos → schema da biblioteca → request tipado → input tipado → autorização da facade → regras de negócio do command. A validação estrutural fica no adapter HTTP comum; as anotações de schema foram removidas desses commands para evitar validação duplicada.

Verificação final dos requests tipados: clean verify passou com 87 testes unitários/arquiteturais e 168 de integração (255 no total). Os novos casos rejeitam tipos inválidos e atributos desconhecidos antes da desserialização em criação e edição de ambos os módulos.

A validação HTTP agora é autoconfigurada pela platform-schema-validation: requests usam ValidateResourceSchemaRequest e os normalizadores implementam SchemaPayloadNormalizer. Advice, anotação e enum locais foram removidos. Biblioteca instalada no Maven local; 80 testes do módulo e os 255 testes do serviço passaram, incluindo a regressão de registro tardio do ObjectMapper.
