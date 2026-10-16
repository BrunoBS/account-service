> Histórico. Consulte [Segmentation](../segmentation/SEGMENTATION.md) para nomes atuais.

# Segmentos e categorias — implementação V1

Referência funcional: `guideline-java/docs/refinamento/15-segmentos/segmentos-categorias-refinement-v1.md`.
Critérios: [padrão de revisão](../../REVIEW-STANDARD.md).

## Escopo e organização

Catálogo global em `feature.segment`, com responsabilidades internas `segment` e `category`. Não há vínculo com workspace ou aplicação. Os catálogos de tipos da Foundation continuam fornecendo o lifecycle; segmentos e categorias são recursos de domínio com identidade pública, auditoria e concorrência próprias. Não foi criada uma abstração genérica de CRUD nem adicionada dependência.

Cada responsabilidade possui entidade, repository, input/output, Finder, validator de negócio, command/query, facade e controller independente. A web expõe requests/responses específicos e usa `@ValidateResourceSchemaRequest` da biblioteca. `SegmentNormalizer` preserva o JSON original e normaliza somente strings conhecidas, antes da validação estrutural. O módulo não acessa repositories de outros módulos.

## Contratos HTTP

Bases: `/api/v1/segments` e `/api/v1/segments/{segmentIdentifier}/categories`. O segundo controller permanece independente; o segmento delimita o recurso pai no path em todas as operações, sem repetição em body ou query param. O path de segmento é UUID tipado; valor inválido retorna 400. Detalhe, update e transições consultam categoria dentro do segmento indicado; categoria de outro pai retorna 404, sem revelar ou alterar seus dados.

| Método / rota relativa | Comportamento |
| --- | --- |
| `POST /` | Cria ACTIVE, gera UUID, versão 0 e timestamps; retorna 201. |
| `GET /{identifier}` | Consulta administrativa, inclusive INACTIVE; retorna 200 ou 404. |
| `GET /` | Página Spring Data, tamanho padrão 20 e máximo global 100. |
| `PUT /{identifier}` | Altera nome, inclusive INACTIVE; exige a versão atual e mantém código e referência. |
| `PATCH /{identifier}/activate` | INACTIVE → ACTIVE; retorna representação após flush. |
| `PATCH /{identifier}/inactivate` | ACTIVE → INACTIVE; retorna representação após flush. |

Não há DELETE. Repetições de activate/inactivate no estado de destino são rejeitadas, conforme transições Golden existentes. Mutações exigem `OWNER`, com a ação correspondente. Leituras globais usam `OPEN/READ`, passam pelo provedor de autorização existente e exigem Bearer e correlation-id no modo integrado.

Criação de segmento:

```json
{"code":"mobile-pf","name":"Mobile PF"}
```

Criação de categoria via `POST /api/v1/segments/{segmentIdentifier}/categories`:

```json
{"code":"premium","name":"Premium"}
```

Edição usa o mesmo contrato, com `version` retornado pela consulta. O código é repetido para permitir a rejeição explícita de mudanças, como nas demais APIs Golden. O segmento é definido exclusivamente pelo path; repetir ou tentar trocá-lo no body é propriedade desconhecida e retorna 400. Trocar o pai no path não encontra a categoria original e retorna 404. UUID, IDs internos, lifecycle e timestamps não são aceitos no body. A categoria responde com `segmentIdentifier`; não há ID numérico público. Responses incluem `version`, `identifier`, `code`, `name`, `lifecycle`, `createdAt`, `updatedAt`; a categoria também inclui `available`, derivado de ambos os lifecycles ACTIVE. Nenhum código composto é persistido.

Listas usam `lifecycle=ACTIVE` como padrão; `INACTIVE` permite consulta administrativa de inativos. `QUARANTINED` é rejeitado, pois está fora do modelo V1. O filtro reutiliza a normalização Golden e responde `SEGMENT-0111` para valores vazios, desconhecidos ou fora do catálogo, indicando somente ACTIVE/INACTIVE em ambos os idiomas. Segmentos aceitam código exato normalizado e trecho de nome sem distinção de caixa; `%`, `_` e `!` são tratados literalmente no filtro de nome. Categorias usam o segmento UUID obrigatório do path e filtro opcional lifecycle; segmento inexistente retorna 404, e segmento existente sem categorias retorna página vazia. Um query param com outro segmento não altera o escopo definido pelo path. Sort permitido: `id`, `identifier`, `code`, `name`, `version`, `createdAt`, `updatedAt`. IDs são usados apenas como critério interno de ordenação, nunca como identificadores de API.

## Regras, persistência e concorrência

- `identifier` é UUID novo em toda criação, único em cada tabela e imutável. JPA marca UUID, código e `segment_id` como não atualizáveis; o domínio não expõe setters.
- Nome tem 150 caracteres e código 100. Código permite `[a-z0-9]+(?:-[a-z0-9]+)*`, inclusive início numérico. Trim e minúsculas seguem normalização existente; rejeitam-se pontos, espaços internos, underscores e hífens malformados.
- Nome não precisa ser único. Código de segmento é único globalmente; categoria reserva código dentro do segmento, inclusive em INACTIVE.
- Categoria só nasce quando o segmento está ACTIVE. O Finder trava o pai antes da validação e preserva o lock até o commit; a inativação do segmento usa o mesmo lock. A entidade também exige o pai ativo na criação.
- Transições do segmento não carregam nem alteram categorias. A categoria pode ser ativada/inativada quando o pai está INACTIVE. Sua disponibilidade efetiva permanece falsa enquanto qualquer um estiver INACTIVE.
- Updates usam versão do cliente, `@Version` e lock exclusivo. O Finder faz leitura escalar com lock, remove eventual entidade antiga do contexto JPA e relê o estado atual com lock; não depende de um snapshot anterior sob REPEATABLE READ.
- Mutações de categoria adquirem locks na ordem segmento → categoria e recarregam ambos os recursos. O pai é recarregado também em update/activate/inactivate para que `available` e o snapshot do comando reflitam seu estado atual, mesmo se já havia uma entidade antiga no contexto JPA. Isso não exige pai ACTIVE para editar ou transicionar a categoria. São duas leituras com lock por recurso, antes da escrita; listagens continuam com projeções, sem esse custo.
- Pré-consultas de unicidade no validator retornam mensagens específicas de negócio (`SEGMENT-0003` e `SEGMENT-0004`, HTTP 409). Constraints garantem a regra no banco, inclusive quando uma pré-consulta não observa uma criação concorrente. O command não interpreta erro de driver, código MySQL ou nome de índice: a exceção de integridade propaga com rollback e o handler central de `platform-messaging` responde `409 / GLOBAL-0004`, localizado e sem expor SQL ou constraint. Não se consulta o banco nem se continua a transação após uma falha de persistência.
- Consultas usam projeções com joins escalares e paginação no banco; listagens de categoria não fazem uma consulta ao pai por item. Há uma consulta constante para confirmar existência do segmento, seguida de conteúdo e count da página.
- `@Auditable` em create/update/activate/inactivate fornece snapshots do retorno após flush. Falha na outbox reverte os dados e a versão na mesma transação. O ator/correlação vêm do contexto confiável da capability.

## Migrations e operação

V66 cria `segment` e `segment_category` com versão/timestamps Golden, UUIDs únicos, unicidade de código, checks de formato/nome/lifecycle e FKs para lifecycle e segmento. Não há `ON DELETE CASCADE`. V67 publica os dois contratos `PLATFORM/segment` e `PLATFORM/segment-category` na infraestrutura existente de schema. Os arquivos JSON de referência e a definição publicada devem ser idênticos; a fixture de testes repõe V67 após a limpeza de catálogos/schema.

As migrations anteriores permanecem intactas. A implementação usa migrations aditivas; rollback de aplicação deve preservar V66/V67, suas tabelas e o histórico Flyway, executando a versão anterior do serviço. Não apagar dados nem editar checksum para voltar versão. Se uma DDL falhar parcialmente em MySQL, inspecionar histórico e objetos antes de corrigir; a correção de schema deve ser explícita e aditiva. Uma eventual retirada definitiva das tabelas exige planejamento próprio, preservação dos dados e migration futura; não faz parte desta entrega.

## Nova revisão do refinamento — 14/10/2026

A revisão percorreu os oito critérios de aceite e os fluxos web → facade → command/query → domínio/repository, confrontando-os com o padrão da plataforma. Encontrou e corrigiu dois gaps:

| Local / severidade | Evidência e impacto | Ajuste / verificação |
| --- | --- | --- |
| `SegmentCategoryFinder.findForUpdate` — média | Transação carregava o pai, outra transação alterava seu lifecycle e o update da categoria retornava `available` antigo. Dois testes reais falharam: true após inativação e false após ativação. O retorno também alimenta o snapshot de auditoria. | Reutilizar `SegmentFinder` para lock e recarga atual do pai antes da categoria. Os dois testes passam e protegem ambos os sentidos. Reprodução: `/private/tmp/segments-review-reproduce.log`. |
| `SegmentQueryValidation.resolveLifecycle` — baixa | Lifecycle vazio/desconhecido recebia mensagem da Foundation que sugeria QUARANTINED, inválido para a V1. | Preservar a normalização Golden e traduzir a rejeição para a chave do catálogo. API verifica vazio, desconhecido e QUARANTINED em segmento/categoria, pt-BR/en-US. |

Também foi ampliada a verificação de rollback de auditoria para ativação de ambos os recursos. A sanitização do erro de integridade verifica o código numérico do driver no campo de mensagem, evitando que um UUID aleatório com o trecho `1062` cause uma falha espúria.

| Critério do refinamento | Implementação e evidência |
| --- | --- |
| 1. Flyway, FKs, constraints e rollback operacional | V66/V67 aditivas; upgrade V65 → V67 preserva registros anteriores, compara schemas e testa constraints/FKs. Rollback de aplicação preserva dados e histórico, conforme seção de operação. |
| 2. CRUD sem DELETE, owner altera, consultas autorizadas | Seis operações por controller; policies explícitas em facade; API testa OWNER, leitura autenticada, 401/403 e DELETE 405; arquitetura protege contratos e autorização. |
| 3. Unicidade inclusive em INACTIVE | Constraints global/por pai e validator; API cobre inativos e códigos iguais em pais distintos; concorrência força pré-consultas a passar e prova rejeição pelo banco. |
| 4. UUID público e imutabilidade | UUID novo/unique; ausência de setters e colunas JPA não atualizáveis; schema rejeita IDs/UUID/pai no body; API rejeita troca de código e acesso à categoria por outro pai. |
| 5. Criação sob pai ACTIVE e transições independentes | Regra no domínio; lock/releitura do pai na criação; API e concorrência verificam inativação antes/depois da criação; categoria transiciona sob pai INACTIVE. |
| 6. Sem cascata e disponibilidade derivada | Relação lazy sem cascatas; domínio/API verificam combinações de lifecycle; novos testes protegem o retorno após mudança concorrente do pai. |
| 7. Paginação, validação, mensagens e testes Golden | Projeções e paginação Spring Data, três statements constantes na página de categoria; JSON original, normalização, limites, filtros, imutabilidade/versão, idiomas, auditoria e arquitetura testados. Commands usam saveAndFlush antes do output. |
| 8. Escopo V1 sem dependências ou outros serviços | Sem alterações em POM ou outros serviços; arquitetura restringe dependências do módulo. Publicação/distribuição/cache e toggles permanecem fora desta entrega. A revisão de persistência em outros módulos corresponde ao pedido separado do usuário. |

## Verificação

| Teste | Evidências cobertas |
| --- | --- |
| `SegmentDomainTest` | Identidade, transições, elegibilidade na criação e disponibilidade sem cascata. |
| `SegmentArchitectureTest` + arquitetura Golden | Fronteiras do módulo, políticas por operação, contratos tipados e ausência de DELETE. |
| `SegmentApiIT` | HTTP/MySQL, schemas antes da coerção, limites, filtros/páginas, inativos, autorização, imutabilidade/versão, mensagens e auditoria/rollback. Inclui o handler central após falha na constraint, resposta sanitizada e ausência de evento da criação rejeitada. |
| `SegmentConcurrencyIT` | Unicidade protegida pelo banco quando pré-consultas passam, updates concorrentes, snapshots antigos, criação versus inativação nos dois sentidos e disponibilidade atual do pai após ativação/inativação concorrente. |
| `SegmentMigrationIT` | Upgrade V65 → V67, preservação de dados antigos, schemas publicados idênticos, UUIDs/constraints/FKs e ausência de cascata. |

Verificação focada com `mvn --offline --batch-mode --no-transfer-progress -Dit.test=SegmentApiIT,SegmentConcurrencyIT,SegmentMigrationIT verify`: **BUILD SUCCESS**, **141 testes** (115 unitários/arquiteturais + 26 integrações: 15 API, 10 concorrência e 1 upgrade), sem falhas, erros ou skips. Inclui os dois testes que reproduziram o gap de disponibilidade e as mensagens de lifecycle corrigidas. Log local: `/private/tmp/segments-focused-verify.log`.

Verificação completa desta revisão com `mvn --offline --batch-mode --no-transfer-progress clean verify`: **BUILD SUCCESS**, **481 testes** (115 unitários/arquiteturais + 366 integrações), sem falhas, erros, skips ou flakes. Concluída em 14/10/2026 às 15:50:42 (America/Sao_Paulo), em 33min41s. Log local: `/private/tmp/segments-full-verify.log`. Os relatórios Surefire/Failsafe confirmam os totais. Inclui a ampliação do rollback de auditoria para ativação, regressões dos módulos existentes, inicialização Spring/JPA, migrations em banco limpo/upgrade, checksums históricos e Maven Enforcer/convergence. `git diff --check` passou.

Conclusão: os dois achados foram resolvidos e não foram identificados gaps pendentes no escopo V1 revisado. Os oito critérios possuem implementação e evidência de verificação; não há decisão de produto pendente nesta entrega. As integrações futuras descritas no refinamento continuam fora da V1.

A [revisão geral de persistência](../persistence/SAVE-AND-FLUSH-REVIEW.md) registra o escopo de core/feature e o resultado por módulo. A validação local usa capabilities já disponíveis no repositório Maven; a resolução remota em repositório vazio e a integração com o provedor real de autorização dependem do pipeline/ambiente integrado.

## Arquivos alterados

- `feature/segment`: domínio de segmento/categoria, repositories, Finders, normalizador, validators, inputs/outputs, commands/queries e facades.
- `entrypoint/web/segment`: `SegmentController`, `SegmentCategoryController` e seus requests/responses; o segmento da categoria é `@PathVariable UUID`.
- `db/migration/V66__create_segments_and_categories.sql`, `V67__segment_resource_schemas.sql` e `schemas/segment*.json`.
- Bundles `META-INF/platform-messages/messages_{pt_BR,en}.properties`.
- `SegmentDomainTest`, `SegmentArchitectureTest`, `SegmentApiIT`, `SegmentConcurrencyIT`, `SegmentMigrationIT` e atualização de `SchemaDefaultFixture`.
- README e este registro de implementação/revisão.

## Entregas futuras

Distribuição, publicação, cache, HTTP entre serviços, associação com toggles e regras de avaliação permanecem fora da V1. `available` é uma informação derivada para consulta, sem implementar novos vínculos. Não há aprovação nem exclusão física.
